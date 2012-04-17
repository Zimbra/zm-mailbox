/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Group.GroupOwner;
import com.zimbra.cs.gal.GalGroupMembers;
import com.zimbra.cs.gal.GalGroupMembers.DLMembers;
import com.zimbra.cs.gal.GalGroupMembers.DLMembersResult;
import com.zimbra.cs.gal.GalGroupMembers.LdapDLMembers;
import com.zimbra.cs.gal.GalGroupMembers.ProxiedDLMembers;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author pshao
 */
public class GetDistributionListMembers extends GalDocumentHandler {
    private static final String A_LDAP_FALLBACK = "__ldap_fallback__";
    
    @Override
    protected Element proxyIfNecessary(Element request, Map<String, Object> context) 
    throws ServiceException {
        boolean internal = request.getAttributeBool(A_LDAP_FALLBACK, false);
        
        if (internal) {
            // the request was proxied here because this is the home server of the 
            // group, always execute locally
            return null;
        } else {
            return super.proxyIfNecessary(request, context);
        }
    }
    
    public Element handle(Element request, Map<String, Object> context) 
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        
        if (!canAccessAccount(zsc, account)) {
            throw ServiceException.PERM_DENIED("can not access account");
        }
        
        Element d = request.getElement(AdminConstants.E_DL);
        String dlName = d.getText();
        
        int offset = getOffset(request);
        int limit = getLimit(request);
        
        // null offset/limit and set _offset/_limit before calling searchGal().
        request.addAttribute(MailConstants.A_QUERY_OFFSET, (String)null);
        request.addAttribute(MailConstants.A_LIMIT, (String)null);
        request.addAttribute(AccountConstants.A_OFFSET_INTERNAL, offset);
        request.addAttribute(AccountConstants.A_LIMIT_INTERNAL, limit);
        
        boolean ldapFallback = request.getAttributeBool(A_LDAP_FALLBACK, false);
        DLMembersResult dlMembersResult = ldapFallback ? 
                getMembersFromLdap(context, request, account, dlName, ldapFallback) : 
                GalGroupMembers.searchGal(zsc, account, dlName, request);
        
        if (dlMembersResult == null) {
            /*
             * bug 66234: if the group is a Zimbra group and is hideInGal, it won't be 
             *            returned from GAL search.  Groups owners should be allowed to
             *            see members in hideInGal groups.
             *             
             * bug 72482: if the group is a newly created user delegated group, it cannot 
             *            be found in GAL if GSA is enabled and the group is not synced into 
             *            the GSA yet.   Group owners and members should be allowed to 
             *            see members before the group is synced in GSA.  
             *            Non-owner, non-members users will have to wait till the groups 
             *            is synced into the GSA.
             *                
             * Do this only when the request is not already a A_LDAP_FALLBACK request.
             */
            if (!ldapFallback) {
                dlMembersResult = 
                    getMembersFromLdap(context, request, account, dlName, ldapFallback);
            }
        }
        
        if (dlMembersResult == null) {
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(dlName);
        }
        
        if (dlMembersResult instanceof ProxiedDLMembers) {
            return ((ProxiedDLMembers)dlMembersResult).getResponse();
        } else if (dlMembersResult instanceof DLMembers) {
            return processDLMembers(zsc, dlName, account , limit, offset, (DLMembers)dlMembersResult);
        } else {
            throw ServiceException.FAILURE("unsopported DLMembersResult class: " + 
                    dlMembersResult.getClass().getCanonicalName(), null);
        }
    }
    
    /*
     * We got here if the group could not be found in GAL.   This could be because 
     * (1) the group is hideInGal - owners should be able to see members of the group.
     * or
     * (2) the groups is newly created and not synced into GSA yet. 
     *     owners and members(because the group is visible to members in the UI as soon 
     *     as the groups is created, if user refresh the UI) should be able to see 
     *     members of the group.
     * 
     * If ldapFallback is true, this request was proxied to this server because the 
     * group could not be found in GAL and this is the home server of the group.
     * 
     * If ldapFallback is false, we couldn't find the group in GAL.  This server is 
     * either the home server of the GAL sync account, or the user's home server 
     * if GAL sync account is not enabled.  If this is not the home server 
     * of the group, we need to proxy the request to the home server of the group 
     * with the ldapFallback flag.
     * 
     * We could've just rely on the Provisioning.onLocalServer(group), the 
     * ldapFallback flag is just an extra safety latch to make sure we don't get 
     * into a proxy loop.
     */
    private DLMembersResult getMembersFromLdap(Map<String, Object> context,
            Element request, Account account, String dlName, boolean ldapFallback) 
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        if (prov.isDistributionList(dlName)) {
            Group group = prov.getGroupBasic(DistributionListBy.name , dlName);
            if (group != null) {
                
                // proxy to the home server of the group to get members from LDAP. 
                // The isOwner/isMember/isHideInGal check will be executed on the 
                // home server of the group, which has the most up-to-date data.  
                
                boolean needsProxy = !ldapFallback && !Provisioning.onLocalServer(group);
                
                if (needsProxy) {
                    Server server = group.getServer();
                    if (server == null) {
                        // just execute locally
                        ZimbraLog.account.warn(String.format(
                                "unable to find home server (%s) for group %s, " + 
                                "getting members from LDAP on local server" ,
                                group.getAttr(Provisioning.A_zimbraMailHost), group.getName()));
                        // do it locally
                        return getMembersFromLdap(account, group);
                    } else {
                        // proxy to the home server of the group
                        ZimbraLog.account.debug(
                                String.format("Proxying request to home server (%s) of group %s",
                                server.getName(), group.getName()));
                        
                        request.addAttribute(A_LDAP_FALLBACK, true);
                        Element resp = proxyRequest(request, context, server);
                        return new ProxiedDLMembers(resp);
                    }
                } else {
                    // do it locally
                    return getMembersFromLdap(account, group);
                }
            }
        }
        return null;
    }
    
    private DLMembersResult getMembersFromLdap(Account account, Group group) 
    throws ServiceException {
        boolean allow = false;
        if (group.hideInGal()) {
            allow = GroupOwner.isOwner(account, group);
        } else {
            allow = GroupOwner.isOwner(account, group) || group.isMemberOf(account);
        }
        
        if (allow) {
            ZimbraLog.account.debug("Retrieving group members from LDAP for group " + group.getName());
            return new LdapDLMembers(group);
        } else {
            ZimbraLog.account.debug("account " + account.getName() + 
                    " is not allowed to get members from ldap for group " + group.getName());
            return null;
        }
    }
    
    /*
     * For SOAP API cleanness purpose, we want to support offset/limit attributes.
     * 
     * But that will interfere with the GAL search if GSA is in place, 
     * because the mailbox search code would take them as the offset/limit for 
     * the contact search.
     * 
     * If we simply null the offset/limit when passing the request object to 
     * GalGroupMembers.searchGal(), the original value passed from the client 
     * will be lost if searchGal() proxies the request to the GSA's home server. 
     * 
     * We could've chosen to use different attributes for offset/limit for this 
     * SOAP request, but it is not as ideal/pretty from API point of view.
     *  
     * To fix it: 
     * Before calling GalGroupMembers.searchGal(), we read the offset/limit 
     * from the request, set them on internal attributes _limit/_offset, and 
     * null the limit/offset on the request object.
     * 
     * In the handler, we first look for _limit/_offset, if set(must have been 
     * proxied to this server by the GSA code), honor them.  
     * If not set(the request is from the client client), honor limit/offset 
     * if they are present.
     * 
     * 
     */
    private int getOffset(Element request) throws ServiceException {
        
        int offset = 0;
        
        // see if the internal attrs are set, use them if set.
        String offsetStr = request.getAttribute(AccountConstants.A_OFFSET_INTERNAL, null);
        if (offsetStr != null) {
            offset = (int) Element.parseLong(AccountConstants.A_OFFSET_INTERNAL, offsetStr);
        } else {
            // otherwise, see if it is set by the client, use them if set
            offsetStr = request.getAttribute(MailConstants.A_QUERY_OFFSET, null);
            if (offsetStr != null) {
                offset = (int) Element.parseLong(MailConstants.A_QUERY_OFFSET, offsetStr);
            }
        }
        
        if (offset < 0) {
            throw ServiceException.INVALID_REQUEST("offset" + offset + " is negative", null);
        }
        
        return offset;
    }
    
    private int getLimit(Element request) throws ServiceException {
        
        int limit = 0;
        
        // see if the internal attrs are set, use them if set.
        String limitStr = request.getAttribute(AccountConstants.A_LIMIT_INTERNAL, null);
        if (limitStr != null) {
            limit = (int) Element.parseLong(AccountConstants.A_LIMIT_INTERNAL, limitStr);
        } else {
            // otherwise, see if it is set by the client, use them if set
            limitStr = request.getAttribute(MailConstants.A_LIMIT, null);
            if (limitStr != null) {
                limit = (int) Element.parseLong(MailConstants.A_LIMIT, limitStr);
            }
        }
        
        if (limit < 0) {
            throw ServiceException.INVALID_REQUEST("limit" + limit + " is negative", null);
        }
        
        return limit;
    }

    
    protected Element processDLMembers(ZimbraSoapContext zsc, String dlName, Account account, 
            int limit, int offset, DLMembers dlMembers) throws ServiceException {
          
        if (!GalSearchControl.canExpandGalGroup(dlName, dlMembers.getDLZimbraId(), account)) {
            throw ServiceException.PERM_DENIED("can not access dl members: " + dlName);
        }
        
        Element response = zsc.createElement(AccountConstants.GET_DISTRIBUTION_LIST_MEMBERS_RESPONSE);
        if (dlMembers != null) {
            int numMembers = dlMembers.getTotal();
            
            if (offset > 0 && offset >= numMembers) {
                throw ServiceException.INVALID_REQUEST("offset " + offset + " greater than size " + numMembers, null);
            }
            
            int endIndex = offset + limit;
            if (limit == 0) {
                endIndex = numMembers;
            }
            if (endIndex > numMembers) {
                endIndex = numMembers;
            }
            
            dlMembers.encodeMembers(offset, endIndex, response);
            
            response.addAttribute(AccountConstants.A_MORE, endIndex < numMembers);
            response.addAttribute(AccountConstants.A_TOTAL, numMembers);
        }
        
        return response;
    }


}
