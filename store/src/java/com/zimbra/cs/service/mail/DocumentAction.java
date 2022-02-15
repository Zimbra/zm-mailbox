/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2021 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.OctopusXmlConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.MailTarget;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.cache.WatchCache;
import com.zimbra.cs.mailbox.event.EventLogger;
import com.zimbra.cs.mailbox.event.ItemEventLog;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.Session.Type;
import com.zimbra.cs.session.WatchNotification;
import com.zimbra.soap.ZimbraSoapContext;

public class DocumentAction extends ItemAction {

    public static final String OP_WATCH = "watch";
    public static final String OP_UNWATCH = "!watch";
    public static final String OP_GRANT = "grant";
    public static final String OP_REVOKE = "!grant";
    public static final String OP_VIEW = "view";
    private static final Set<String> OPS = ImmutableSet.of(OP_WATCH, OP_UNWATCH, OP_GRANT, OP_REVOKE, OP_VIEW);

    @Override
    protected String[] getProxiedIdPath(Element request) { return TARGET_ITEM_PATH; }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element action = request.getElement(MailConstants.E_ACTION);
        String operation = action.getAttribute(MailConstants.A_OPERATION).toLowerCase();

        Element response = zsc.createElement(OctopusXmlConstants.DOCUMENT_ACTION_RESPONSE);
        Element result = response.addUniqueElement(MailConstants.E_ACTION);

        String successes = null;
        if (OPS.contains(operation)) {
            successes = handleDocument(context, request, operation, result);
        } else {
            successes = Joiner.on(",").join(handleCommon(context, request, MailItem.Type.DOCUMENT).getSuccessIds());
        }

        result.addAttribute(MailConstants.A_ID, successes);
        result.addAttribute(MailConstants.A_OPERATION, operation);
        return response;
    }

    private String handleDocument(Map<String,Object> context, Element request, String operation, Element result) throws ServiceException {
        Element action = request.getElement(MailConstants.E_ACTION);

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);
        ItemId iid = new ItemId(action.getAttribute(MailConstants.A_ID), zsc);
        WatchNotification notification = null;
        long timestamp = System.currentTimeMillis();
        Account authAccount = Provisioning.getInstance().getAccountById(zsc.getAuthtokenAccountId());

        // check the access and make sure the item exists and the authUser has access to it
        MailItem watchedItem = mbox.getItemById(octxt, iid.getId(), MailItem.Type.DOCUMENT);
        Document doc = (Document) watchedItem;

        if (operation.equals(OP_WATCH)) {
            // add the watch mapping
            WatchCache.get(authAccount).watch(iid.getAccountId(), iid.getId());
            notification = new WatchNotification(MailboxOperation.Watch, octxt.getAuthenticatedUser(), octxt.getUserAgent(), timestamp, watchedItem);
            mbox.markMetadataChanged(octxt, iid.getId());
        } else if (operation.equals(OP_UNWATCH)) {
            // remove the watch mapping
            WatchCache.get(authAccount).unwatch(iid.getAccountId(), iid.getId());
            notification = new WatchNotification(MailboxOperation.Unwatch, octxt.getAuthenticatedUser(), octxt.getUserAgent(), timestamp, watchedItem);
            mbox.markMetadataChanged(octxt, iid.getId());
        } else if (operation.equals(OP_REVOKE)) {
            String zid = action.getAttribute(MailConstants.A_ZIMBRA_ID);
            mbox.revokeAccess(octxt, iid.getId(), zid);
        } else if (operation.equals(OP_GRANT)) {
            // file level shares can be granted to all users, public or specific users
            Element grant = action.getElement(MailConstants.E_GRANT);
            short rights = ACL.stringToRights(grant.getAttribute(MailConstants.A_RIGHTS));
            String gtype = grant.getAttribute(MailConstants.A_GRANT_TYPE);
            String zid = null;
            String secret = null;
            long expiry = grant.getAttributeLong(MailConstants.A_EXPIRY, 0);
            byte gtypeByte = ACL.stringToType(gtype);
            switch (gtypeByte) {
            case ACL.GRANTEE_AUTHUSER:
                zid = GuestAccount.GUID_AUTHUSER;
                break;
            case ACL.GRANTEE_PUBLIC:
                zid = GuestAccount.GUID_PUBLIC;
                expiry = validateGrantExpiry(grant.getAttribute(MailConstants.A_EXPIRY, null),
                        mbox.getAccount().getFilePublicShareLifetime());
                break;
            case ACL.GRANTEE_USER:
                zid = getZimbraId(grant, zsc, mbox, result);
                break;
            default:
                throw ServiceException.INVALID_REQUEST("unsupported gt: " + gtype, null);
            }

            ZimbraLog.soap.debug("The user: %s has been granted: %s for item:  %s", zid, ACL.rightsToString(rights),
                iid.getId());
            mbox.grantAccess(octxt, iid.getId(), zid, gtypeByte, rights, secret, expiry);
        } else if (operation.equals(OP_VIEW) && allowDocumentAccessedTimeLogging(octxt)) {
                notification = new WatchNotification(MailboxOperation.View, octxt.getAuthenticatedUser(), octxt.getUserAgent(), timestamp, watchedItem, doc.getVersion());
        } else {
            throw ServiceException.INVALID_REQUEST("unknown operation: " + operation, null);
        }

        if (notification != null) {
//            for (Session s : mbox.getNotificationPubSub().getSubscriber().getListeners(Type.SOAP)) {
//                s.notifyExternalEvent(notification);
//            }
            for (Session s : mbox.getListeners(Type.SOAP)) {
                s.notifyExternalEvent(notification);
            }

            EventLogger logger = EventLogger.getInstance();
            ItemEventLog log = logger.getLog(watchedItem);
            log.addEvent(notification.toActivity());
        }

        return ifmt.formatItemId(iid);
    }

    private boolean allowDocumentAccessedTimeLogging(OperationContext octxt) {
        if (octxt != null) {
            Account account = octxt.getAuthenticatedUser();
            if (account != null && !account.getId().equals(GuestAccount.GUID_PUBLIC)
                    && !account.getId().equals(GuestAccount.GUID_AUTHUSER)
                    && account.isDocumentRecentlyViewedEnabled()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param grant Required grant
     * @param zsc ZimbraSoapContext
     * @param mbox mailbox
     * @return zid
     * @throws ServiceException
     */
    private String getZimbraId(Element grant, ZimbraSoapContext zsc, Mailbox mbox, Element result) throws ServiceException {
        String zid = grant.getAttribute(MailConstants.A_ZIMBRA_ID, null);
        NamedEntry nentry = null;
        if (zid == null) {
            // if zid == null, check if email exist in the request and try to get the account using the email
            String email = grant.getAttribute(MailConstants.A_DISPLAY, null);
            if (email == null || email == "") {
                throw ServiceException.FAILURE("Either zid or email is missing", null);
            } else if (email.indexOf('@') < 0) {
                throw ServiceException.INVALID_REQUEST("invalid email id", null);
            } else {
                try {
                    nentry = FolderAction.lookupGranteeByName(email, ACL.GRANTEE_USER, zsc);
                    if (nentry instanceof MailTarget) {
                        Domain domain = Provisioning.getInstance().getDomain(mbox.getAccount());
                        String granteeDomainName = ((MailTarget) nentry).getDomainName();
                        if (domain.isInternalSharingCrossDomainEnabled() ||
                                domain.getName().equals(granteeDomainName) ||
                                Sets.newHashSet(domain.getInternalSharingDomain()).contains(granteeDomainName)) {
                            zid = nentry.getId();
                            result.addAttribute(MailConstants.A_ZIMBRA_ID, zid);
                            result.addAttribute(MailConstants.A_DISPLAY, nentry.getName());
                        }
                    }
                } catch (ServiceException se) {
                    // this is the normal path, where lookupGranteeByName throws account.NO_SUCH_USER
                    ZimbraLog.soap.debug("Exception while lookupGranteeByName : %s", se);
                    throw se;
                }
            }
        }
        return zid;
    }
}
