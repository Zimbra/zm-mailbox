/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Group.GroupOwner;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.gal.GalGroupMembers;
import com.zimbra.cs.gal.GalGroupMembers.DLMembers;
import com.zimbra.cs.gal.GalGroupMembers.DLMembersResult;
import com.zimbra.cs.gal.GalGroupMembers.LdapDLMembers;
import com.zimbra.cs.gal.GalGroupMembers.LdapHABMembers;
import com.zimbra.cs.gal.GalGroupMembers.ProxiedDLMembers;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author pshao
 */
public class GetDistributionListMembers extends GalDocumentHandler {
    private static final String A_PROXIED_TO_HOME_OF_GROUP = "__proxied__";

    @Override
    protected Element proxyIfNecessary(Element request, Map<String, Object> context)
    throws ServiceException {

        boolean proxiedToHomeOfInternalGroup = request.getAttributeBool(A_PROXIED_TO_HOME_OF_GROUP, false);

        if (proxiedToHomeOfInternalGroup) {
            // the request was proxied here because this is the home server of the group, always execute locally
            return null;
        } else {
            // proxy to the home server of the GAL sync account if galAcctId is on the request
            return super.proxyIfNecessary(request, context);
        }
    }

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account)) {
            throw ServiceException.PERM_DENIED("can not access account");
        }

        Element eDL = request.getElement(AdminConstants.E_DL);
        String dlName = eDL.getText();

        int offset = getOffset(request);
        int limit = getLimit(request);

        // null offset/limit and set _offset/_limit before calling searchGal().
        request.addAttribute(MailConstants.A_QUERY_OFFSET, (String)null);
        request.addAttribute(MailConstants.A_LIMIT, (String)null);
        request.addAttribute(AccountConstants.A_OFFSET_INTERNAL, offset);
        request.addAttribute(AccountConstants.A_LIMIT_INTERNAL, limit);

        boolean proxiedToHomeOfInternalGroup = request.getAttributeBool(A_PROXIED_TO_HOME_OF_GROUP, false);

        DLMembersResult dlMembersResult = null;

        /*
         * see if the group is an internal group
         */
        Provisioning prov = Provisioning.getInstance();
        Group group = prov.getGroupBasic(DistributionListBy.name , dlName);
        if (group != null) {
            /*
             * Is an internal group, get members from LDAP instead of GAL
             * for group owners and members.
             *
             * This makes updates on internal groups reflected in the UI
             * sooner than the next GSA delta sync cycle for group owners and members.
             * (bug 72482 and bug 73460).
             */
            dlMembersResult = getMembersFromLdap(context, request, account, group, proxiedToHomeOfInternalGroup);
        }

        if (dlMembersResult == null) {
            /*
             * - this is not an internal group, or
             * - this is an internal group but the requesting account is not an owner
             *   or member of the group, or
             * - this is an internal hideInGal group and the requesting account is not
             *   an owner of the group.
             *
             * Do GAL search if this is not a hideInGal group.  This check is a small
             * optimization and is not necessary, because if the group is hideInGal, it
             * won't be found in GAL anyway, so just save the GAL search.
             *
             * If proxiedToHomeOfInternalGroup is true, we were proxied here from inside the
             * getMembersFromLdap() path in the previous hop.  We must have gone into the above
             * getMembersFromLdap() in this hop, and it returned null (or in a nearly
             * impossible case the group cannot be found in this server and we did not
             * even go into the above getMembersFromLdap).  In this case, do not do
             * a GAL search, because the GAL search could once again proxy the request
             * to the GAL sync account's home server.  We will just leave dlMembersResult
             * null and let the NO_SUCH_DISTRIBUTION_LIST be thrown.  When the
             * NO_SUCH_DISTRIBUTION_LIST got back to the previous hop, in
             * getMembersFromLdap, it will return null and control will reach here again
             * (in the "previous" hop).   Now, since the proxiedToHomeOfInternalGroup
             * flag is not on, we will proceed to do the GAL search if the group is not hideInGal.
             */
            boolean hideInGal = (group != null && group.hideInGal());
            if (!hideInGal && !proxiedToHomeOfInternalGroup) {
                dlMembersResult = GalGroupMembers.searchGal(zsc, account, dlName, request);
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
     * We got here because the group is an internal group.
     *
     * If proxiedToHomeOfInternalGroup is true, this request was proxied to this server because
     * this is the home server of the group.  Just execute locally.
     *
     * If proxiedToHomeOfInternalGroup is false, we need to proxy to the home server of the group
     * if this is not the home server of the group.
     *
     * We could've just rely on the Provisioning.onLocalServer(group), the
     * proxiedToHomeOfInternalGroup flag is just an extra safety latch to ensure we don't get
     * into a proxy loop.
     */
    private DLMembersResult getMembersFromLdap(Map<String, Object> context,
            Element request, Account account, Group group, boolean proxiedToHomeOfInternalGroup)
    throws ServiceException {
        // proxy to the home server of the group to get members from LDAP.
        // The isOwner/isMember/isHideInGal check will be executed on the
        // home server of the group, which has the most up-to-date data.

        boolean local = proxiedToHomeOfInternalGroup || Provisioning.onLocalServer(group);
        if (local) {
            // do it locally
            return getMembersFromLdap(account, group);
        } else {
            Server server = group.getServer();
            if (server == null) {
                // just execute locally
                ZimbraLog.account.warn(
                        "unable to find home server (%s) for group %s, getting members from LDAP on local server",
                        group.getAttr(Provisioning.A_zimbraMailHost), group.getName());
                // do it locally
                return getMembersFromLdap(account, group);
            } else {
                // proxy to the home server of the group
                ZimbraLog.account.info("Proxying request: dl.name=%s reason: target=%s",
                        group.getName(), server.getName());

                request.addAttribute(A_PROXIED_TO_HOME_OF_GROUP, true);
                try {
                    Element resp = proxyRequest(request, context, server);
                    return new ProxiedDLMembers(resp);
                } catch (ServiceException e) {
                    // delete attribute to avoid confusion in future proxied requests
                    request.addAttribute(A_PROXIED_TO_HOME_OF_GROUP, (String) null);
                    // if we encounter any error(including NO_SUCH_DISTRIBUTION_LIST,
                    // in this case, it could be because the account is not a owner/member
                    // of the group), just return null and let the callsite proceed to do
                    // the GAL search if appropriate.
                    return null;
                }
            }
        }
    }

    private DLMembersResult getMembersFromLdap(Account account, Group group)
    throws ServiceException {
        if (group.isHABGroup()) {
            ZimbraLog.account.debug("Retrieving hab group members from LDAP for group %s", group.getName());
            return new LdapHABMembers(group);
        } else {
            boolean allow = false;
            if (group.hideInGal()) {
                allow = GroupOwner.isOwner(account, group);
            } else {
                allow = GroupOwner.isOwner(account, group) || group.isMemberOf(account);
            }
            if (allow) {
                ZimbraLog.account.debug("Retrieving group members from LDAP for group %s", group.getName());
                return new LdapDLMembers(group);
            } else {
                ZimbraLog.account.debug("account %s is not allowed to get members from ldap for group %s",
                        account.getName(), group.getName());
                return null;
            }
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
            ZimbraLog.misc.warn("dlName: %s, dlMembers(%s) size %d, id %s", dlName, dlMembers.getClass(), dlMembers.getTotal(), dlMembers.getDLZimbraId());
            Group dl = Provisioning.getInstance().getGroup(DistributionListBy.name, dlName);
            if (dl == null) {
                throw ServiceException.NOT_FOUND(dlName + " does not exist");
            } else {
                throw ServiceException.PERM_DENIED("can not access dl members: " + dlName);
            }
        }

        Element response = zsc.createElement(AccountConstants.GET_DISTRIBUTION_LIST_MEMBERS_RESPONSE);
        if (dlMembers != null) {
            int numMembers = dlMembers.getTotal();

            if (offset > 0 && offset >= numMembers) {
                throw ServiceException.INVALID_REQUEST("offset " + offset +
                        " greater than size " + numMembers, null);
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
