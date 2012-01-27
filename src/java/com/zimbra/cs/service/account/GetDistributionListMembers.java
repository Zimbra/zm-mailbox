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

public class GetDistributionListMembers extends GalDocumentHandler {
    private static final String A_HIDE_IN_GAL_INTERNAL = "__hide_in_gal_internal__";
    
    @Override
    protected Element proxyIfNecessary(Element request, Map<String, Object> context) 
    throws ServiceException {
        boolean internal = request.getAttributeBool(A_HIDE_IN_GAL_INTERNAL, false);
        
        if (internal) {
            // the request was proxied here because this is the home server of the 
            // hideInGal group, always execute locally
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
        
        boolean hideInGalInternal = request.getAttributeBool(A_HIDE_IN_GAL_INTERNAL, false);
        DLMembersResult dlMembersResult = hideInGalInternal ? 
                getMembersFromLdapForOwner(context, request, account, dlName, hideInGalInternal) : 
                GalGroupMembers.searchGal(zsc, account, dlName, request);
        
        if (dlMembersResult == null) {
            /*
             * bug 66234
             * If the list is a Zimbra list and is hideInGal, it won't be returned from 
             * GAL search.  In this case, allow the request for list owners.
             * Do this only when the request is not already a A_HIDE_IN_GAL_INTERNAL request.
             */
            if (!hideInGalInternal) {
                dlMembersResult = 
                    getMembersFromLdapForOwner(context, request, account, dlName, hideInGalInternal);
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
     * If internal is true, this is request was proxied to this server because 
     * it is hideInGal.  Return members from LDAP if if user is an owner.
     * 
     * If internal is false, we couldn't find the group in GAL.  This server is either 
     * the home server of the GAL sync account, or the user's home server if GAL sync 
     * account is not enabled).  If this is not the home server of the group, 
     * we need to proxy the request to the home server of the group with the internal flag.
     * 
     * We could've just rely on the Provisioning.onLocalServer(group), the internal flag 
     * is just an extra safety latch to make sure we don't get to a proxy loop.
     */
    private DLMembersResult getMembersFromLdapForOwner(Map<String, Object> context,
            Element request, Account account, String dlName, boolean hideInGalInternal) 
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        if (prov.isDistributionList(dlName)) {
            Group group = prov.getGroupBasic(DistributionListBy.name , dlName);
            if (group != null && group.hideInGal()) {
                ZimbraLog.account.debug("handling hideInGal group " + group.getName());
                
                // proxy to the home server of the group to get members from LDAP 
                // if the user is an owner of the group.  The isOwner check will be 
                // done on the home server of the group, which has the most 
                // up-to-date data
                
                boolean needsProxy = !hideInGalInternal && !Provisioning.onLocalServer(group);
                
                if (needsProxy) {
                    Server server = group.getServer();
                    if (server == null) {
                        // just execute locally
                        ZimbraLog.account.warn(String.format(
                                "unable to find home server (%s) for hideInGal group %s, " + 
                                "getting members from LDAP on local server" ,
                                group.getAttr(Provisioning.A_zimbraMailHost), group.getName()));
                        return getMembersFromLdapForOwner(account, group);
                    } else {
                        // proxy to the home server of the group
                        ZimbraLog.account.debug(
                                String.format("Proxying request to home server (%s) of hideInGal group %s",
                                server.getName(), group.getName()));
                        
                        request.addAttribute(A_HIDE_IN_GAL_INTERNAL, true);
                        Element resp = proxyRequest(request, context, server);
                        return new ProxiedDLMembers(resp);
                    }
                } else {
                    // do it locally
                    return getMembersFromLdapForOwner(account, group);
                }
            }
        }
        return null;
    }
    
    private DLMembersResult getMembersFromLdapForOwner(Account account, Group group) 
    throws ServiceException {
        boolean isOwner = GroupOwner.isOwner(account, group);
        if (isOwner) {
            ZimbraLog.account.debug("Retrieving group members from LDAP for hideInGal group " + group.getName());
            return new LdapDLMembers(group);
        } else {
            ZimbraLog.account.debug("account " + account.getName() + 
                    " is not an owner of hideInGal group " + group.getName());
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
