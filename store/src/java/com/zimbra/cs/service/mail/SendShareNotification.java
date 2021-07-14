/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.MailTarget;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GroupMemberEmailAddrs;
import com.zimbra.cs.account.ShareInfo;
import com.zimbra.cs.account.ShareInfoData;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.SendShareNotificationRequest;
import com.zimbra.soap.mail.message.SendShareNotificationRequest.Action;
import com.zimbra.soap.mail.type.EmailAddrInfo;

public class SendShareNotification extends MailDocumentHandler {

    private static final Log sLog = LogFactory.getLog(SendShareNotification.class);

    private static final String[] TARGET_ITEM_PATH = new String[] {
            MailConstants.E_ITEM, MailConstants.A_ID };

    @Override protected String[] getProxiedIdPath(Element request) {
        return TARGET_ITEM_PATH;
    }

    private static final String REVOKE = "revoke";

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);
        Account account = getRequestedAccount(zsc);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account, false);

        // validate the share specified in the request and build a share info if all is valid
        Collection<ShareInfoData> shareInfos = validateRequest(zsc, context, octxt, mbox, request);

        // grab notes if there is one
        Element eNotes = request.getOptionalElement(MailConstants.E_NOTES);
        Action action = Action.fromString(request.getAttribute(MailConstants.A_ACTION, null));
        String notes = eNotes==null ? null : eNotes.getText();
        SendShareNotificationRequest req = zsc.elementToJaxb(request);
        MailItem reqItem = null;
        // if item element present
        // get the requested item in the mailbox
        if (req.getItem() != null) {
            ItemId iid = new ItemId(req.getItem().getId(), zsc);
            reqItem = mbox.getItemById(octxt, iid.getId(), MailItem.Type.UNKNOWN);
        }

        // send the messages
        try {
            Account authAccount = getAuthenticatedAccount(zsc);
            Collection<ShareInfoData> sharesWithGroupGrantee = Lists.newArrayList();
            for (ShareInfoData sid : shareInfos) {
                // set aside shares to groups
                if (ACL.GRANTEE_GROUP == sid.getGranteeTypeCode()) {
                    sharesWithGroupGrantee.add(sid);
                } else {
                    sendNotificationEmail(octxt, mbox, authAccount, account, sid, notes, action, null, null, reqItem);
                }
            }

            // send to group grantees
            sendNotificationEmailToGroupGrantees(octxt, mbox, authAccount, account,
                    sharesWithGroupGrantee, notes, action, reqItem);

        } catch (MessagingException e) {
            throw ServiceException.FAILURE(
                    "Messaging Exception while sending share notification message", e);
        }

        return zsc.createElement(MailConstants.SEND_SHARE_NOTIFICATION_RESPONSE);
    }

    private Collection<ShareInfoData> validateRequest(ZimbraSoapContext zsc,
            Map<String, Object> context, OperationContext octxt,
            Mailbox mbox, Element request) throws ServiceException {


        Element eShare = request.getOptionalElement(MailConstants.E_SHARE);
        if (eShare != null) {
            return Arrays.asList(validateShareRecipient(zsc, context, octxt, mbox, eShare));
        }

        String action = request.getAttribute(MailConstants.A_ACTION, null);
        ArrayList<ShareInfoData> shareInfos = new ArrayList<ShareInfoData>();
        SendShareNotificationRequest req = zsc.elementToJaxb(request);
        ItemId iid = new ItemId(req.getItem().getId(), zsc);
        MailItem item = mbox.getItemById(octxt, iid.getId(), MailItem.Type.UNKNOWN);
        Provisioning prov = Provisioning.getInstance();
        Account account = getRequestedAccount(zsc);
        if (item instanceof Mountpoint) {
            Mountpoint mp = (Mountpoint)item;
            account = prov.get(AccountBy.id, mp.getOwnerId());
        }
        for (EmailAddrInfo email : req.getEmailAddresses()) {
            // add the non-existing grantee as type GRANTEE_GUEST for share notification.
            // for revoke notifications return the non-existing grantees only
            Pair<NamedEntry, String> grantee;
            boolean guestGrantee = false;
            byte granteeType = ACL.GRANTEE_USER;
            String granteeId = null;
            String granteeEmail = email.getAddress();
            String granteeDisplayName = null;
            try {
                grantee = getGrantee(zsc, granteeType, granteeId, granteeEmail);
                NamedEntry entry = grantee.getFirst();
                if (entry instanceof MailTarget) {
                    Domain domain = prov.getDomain(account);
                    String granteeDomainName = ((MailTarget) entry).getDomainName();
                    if (domain.isInternalSharingCrossDomainEnabled() ||
                            domain.getName().equals(granteeDomainName) ||
                            Sets.newHashSet(domain.getInternalSharingDomain()).contains(granteeDomainName)) {
                        if (entry instanceof Group) {
                            granteeType = ACL.GRANTEE_GROUP;
                        }
                        granteeId = entry.getId();
                        granteeDisplayName = grantee.getSecond();
                    } else {
                        guestGrantee = true;
                    }
                }
            } catch (ServiceException e) {
                if (!e.getCode().equals(MailServiceException.NO_SUCH_GRANTEE)) {
                    throw e;
                }
                guestGrantee = true;
            }
            if (guestGrantee) {
                granteeType = ACL.GRANTEE_GUEST;
                // if guest, granteeId is the same as granteeEmail
                granteeId = granteeEmail;
            }
            shareInfos.add(getShareInfoData(zsc, context, account, octxt, granteeType,
                    granteeEmail, granteeId, granteeDisplayName, item, REVOKE.equals(action)));
        }

        return shareInfos;
    }

    @Deprecated
    private ShareInfoData validateShareRecipient(ZimbraSoapContext zsc,
            Map<String,Object> context, OperationContext octxt, Mailbox mbox, Element eShare)
    throws ServiceException {

        Provisioning prov = Provisioning.getInstance();

        //
        // grantee
        //
        byte granteeType = ACL.stringToType(eShare.getAttribute(MailConstants.A_GRANT_TYPE));
        String granteeId = eShare.getAttribute(MailConstants.A_ZIMBRA_ID, null);
        String granteeName = eShare.getAttribute(MailConstants.A_DISPLAY, null);

        String matchingId;         // grantee id to match grant
        String granteeEmail;       // email of the grantee, will be he recipient or the share notif message
        String granteeDisplayName; // display name, if set, of the grantee

        if (granteeType == ACL.GRANTEE_GUEST) {
            if (granteeName == null) {
                throw ServiceException.INVALID_REQUEST(
                        "must specify grantee name for guest grantee type", null);
            }

            // if guest, matchingId is the same as granteeEmail
            matchingId = granteeName;
            granteeEmail = granteeName;
            granteeDisplayName = granteeEmail;
        } else {
            Pair<NamedEntry, String> grantee;
            try {
                grantee = getGrantee(zsc, granteeType, granteeId, granteeName);
            } catch (ServiceException e) {
                if (e.getCode().equals(MailServiceException.NO_SUCH_GRANTEE)) {
                    throw ServiceException.INVALID_REQUEST("no such grantee", e);
                }
                throw e;
            }

            NamedEntry granteeEntry = grantee.getFirst();
            matchingId = granteeEntry.getId();
            granteeEmail = granteeEntry.getName();
            granteeDisplayName = grantee.getSecond();
        }

        //
        // folder
        //
        Account account = getRequestedAccount(zsc);
        Folder folder = getFolder(octxt, account, mbox, eShare);

        Account ownerAcct = account;

        // if the folder is a mountpoint, set correct ownerAcct and the folder id in the owner's mailbox
        if (folder instanceof Mountpoint) {
            Mountpoint mp = (Mountpoint)folder;
            ownerAcct = prov.get(AccountBy.id, mp.getOwnerId());
            folder = mp;
        }

        return getShareInfoData(zsc, context, ownerAcct, octxt, granteeType, granteeEmail,
                matchingId, granteeDisplayName, folder, false);
    }

    private ShareInfoData getShareInfoData(
            ZimbraSoapContext zsc,
            Map<String,Object> context,
            Account ownerAcct,
            OperationContext octxt,
            byte granteeType,
            String granteeEmail,
            String granteeId,
            String granteeDisplayName,
            MailItem item,
            boolean revoke) throws ServiceException {

        MatchingGrant matchingGrant = null;

        Mountpoint mpt = item instanceof Mountpoint ? (Mountpoint) item : null;

        if (item instanceof Document) {
            ZimbraLog.account.debug("Adding sharing information for document %s", item.getName());
            Document doc = (Document) item;
            ACL acl = doc.getEffectiveACL();
            if (acl != null) {
                for (ACL.Grant grant : acl.getGrants()) {
                    if (grant.getGranteeType() == granteeType && grant.getGranteeId().equals(granteeId)) {
                        matchingGrant =  new MatchingGrant(grant);
                    }
                }
            }
        } else {
            // see if the share specified in the request is real
            if (Provisioning.onLocalServer(ownerAcct)) {
                matchingGrant = getMatchingGrantLocal(octxt, item, granteeType, granteeId, ownerAcct);
            } else {
                matchingGrant = getMatchingGrantRemote(zsc, context, granteeType, granteeId, ownerAcct,
                        mpt == null ? item.getId() : mpt.getRemoteId());
            }
        }

        if (!revoke && matchingGrant == null) {
            throw ServiceException.INVALID_REQUEST("no matching grant", null);
        }

        //
        // all is well, setup our ShareInfoData object
        //
        ShareInfoData sid = new ShareInfoData();

        sid.setOwnerAcctId(ownerAcct.getId());
        sid.setOwnerAcctEmail(ownerAcct.getName());
        sid.setOwnerAcctDisplayName(ownerAcct.getDisplayName());

        // folder id/uuid used for mounting
        sid.setItemId(mpt == null ? item.getId() : mpt.getRemoteId());
        sid.setItemUuid(mpt == null ? item.getUuid() : mpt.getRemoteUuid());

        //
        // just a display name for the shared folder for the grantee to see.
        // the mountpoint will be created using the folder id.
        //
        // if user2 is sharing with user3 a mountpoint that belongs to user1,
        // we should show user3 the folder(mountpoint) name in user2's mailbox,
        // not the folder name in user1's mailbox.
        String path = (item instanceof Folder) ? ((Folder)item).getPath() : item.getName();
        sid.setPath(path);
        sid.setFolderDefaultView((item instanceof Folder) ?
                ((Folder)item).getDefaultView() : item.getType());

        // grantee
        sid.setGranteeType(granteeType);
        sid.setGranteeId(granteeId);
        sid.setGranteeName(granteeEmail);
        sid.setGranteeDisplayName(granteeDisplayName);

        if (revoke) {
            sid.setGranteeName(granteeEmail);
            return sid;
        }

        // rights
        sid.setRights(matchingGrant.getGrantedRights());

        // if the grantee is a guest, set URL and password
        if (granteeType == ACL.GRANTEE_GUEST) {
            String url = UserServlet.getRestUrl(ownerAcct) + path;
            sid.setUrl(url);  // hmm, for mountpoint this should be the path in the owner's mailbox  TODO
            sid.setGuestPassword(matchingGrant.getPassword());
        }

        return sid;
    }

    /*
     * utility class for passing grant info back from getMatchingGrant/getMatchingGrantRemote
     */
    private static class MatchingGrant {

        MatchingGrant(ACL.Grant grant) {
            mGrant = grant;
        }

        MatchingGrant(String zimbraId, byte type, short rights) {
            mGrantee = zimbraId;
            mType    = type;
            mRights  = rights;
        }
        void setGranteeName(String name) { mName = name; }
        void setPassword(String password) { mSecret = password; }

        /*
         * either mGrant or mGrantee/mName/mType/mRights/mSecret can be set
         */

        // used when the owner of the folder is on local server
        ACL.Grant mGrant;

        // used when the owner of the folder is not on local server
        // and we need to fetch the grant of the owner folder by SOAP.
        // instead of making ACL.Grant methods public, just gather grant attrs collected
        // from SOAP (for grants on remote owner mailbox) in this class that mirrors ACL.Grant
        String mGrantee;
        String mName;
        byte mType;
        short mRights;
        String mSecret;

        short getGrantedRights() {
            return  mGrant==null ? mRights : mGrant.getGrantedRights();
        }

        String getPassword() {
            return mGrant==null ? mSecret : mGrant.getPassword();
        }
    }

    private MatchingGrant getMatchingGrantLocal(OperationContext octxt, MailItem item,
            byte granteeType, String granteeId, Account ownerAcct) throws ServiceException {
        if (item instanceof Mountpoint) {
            Mailbox ownerMbox = MailboxManager.getInstance().getMailboxByAccount(ownerAcct, false);
            if (ownerMbox == null) {
                throw ServiceException.FAILURE("mailbox not found for account " + ownerAcct.getId(), null);
            }
            item = ownerMbox.getItemById(octxt, ((Mountpoint) item).getRemoteId(), MailItem.Type.UNKNOWN);
        }

        ACL acl = item.getEffectiveACL();
        if (acl == null) {
            return null;
        }

        for (ACL.Grant grant : acl.getGrants()) {
            if (grant.getGranteeType() == granteeType && grant.getGranteeId().equals(granteeId)) {
                return new MatchingGrant(grant);
            }
        }

        return null;
    }

    private MatchingGrant getMatchingGrantRemote(ZimbraSoapContext zsc, Map<String, Object> context,
            byte granteeType, String granteeId, Account ownerAcct, int remoteFolderId)
    throws ServiceException {

        Element remote = fetchRemoteFolder(zsc, context, ownerAcct.getId(), remoteFolderId);
        Element eAcl = remote.getElement(MailConstants.E_ACL);
        if (eAcl != null) {
            for (Element eGrant : eAcl.listElements(MailConstants.E_GRANT)) {
                try {
                    byte gt = ACL.stringToType(eGrant.getAttribute(MailConstants.A_GRANT_TYPE));
                    if (gt != granteeType) {
                        continue;
                    }
                    short rights = ACL.stringToRights(eGrant.getAttribute(MailConstants.A_RIGHTS));

                    MatchingGrant grant = null;

                    // check for only user, group, and guest grants

                    if (gt == ACL.GRANTEE_GUEST) {
                        // displayName is the email, and is the "grantee id"
                        String gid = eGrant.getAttribute(MailConstants.A_DISPLAY);
                        if (gid.equals(granteeId)) {
                            grant = new MatchingGrant(gid, gt, rights);
                            // set optional password
                            grant.setPassword(eGrant.getAttribute(MailConstants.A_PASSWORD, null));
                        }
                    } else if (gt == ACL.GRANTEE_USER || gt == ACL.GRANTEE_GROUP) {
                        // zid is required
                        String gid = eGrant.getAttribute(MailConstants.A_ZIMBRA_ID);
                        if (gid.equals(granteeId)) {
                            grant = new MatchingGrant(gid, gt, rights);
                            // set optional display name
                            grant.setGranteeName(eGrant.getAttribute(MailConstants.A_DISPLAY, null));
                        }
                    }
                    if (grant != null) {
                        return grant;
                    }

                } catch (ServiceException e) {
                    // for some reason the soap response cannot by parsed as expected
                    // ignore and go on
                    sLog.warn("cannot parse soap response for remote grant", e);
                }
            }
        }
        return null;
    }


    private Element fetchRemoteFolder(ZimbraSoapContext zsc, Map<String, Object> context,
            String ownerId, int remoteId)
    throws ServiceException {
        Element request = zsc.createRequestElement(MailConstants.GET_FOLDER_REQUEST);
        request.addElement(MailConstants.E_FOLDER).addAttribute(MailConstants.A_FOLDER, remoteId);

        Element response = proxyRequest(request, context, ownerId);
        Element remote = response.getOptionalElement(MailConstants.E_FOLDER);
        if (remote == null)
            throw ServiceException.INVALID_REQUEST("cannot mount a search or mountpoint", null);
        return remote;
    }

    /**
     * returns the grantee entry and displayName if there is one
     *
     * @param zsc
     * @param granteeType
     * @param granteeId
     * @param granteeName
     * @return
     * @throws ServiceException
     */
    private Pair<NamedEntry, String> getGrantee(ZimbraSoapContext zsc, byte granteeType,
            String granteeId, String granteeName)
    throws ServiceException {

        NamedEntry entryById = null;
        NamedEntry entryByName = null;

        if (granteeId != null) {
            entryById = FolderAction.lookupGranteeByZimbraId(granteeId, granteeType);
            if (entryById == null)
                throw MailServiceException.NO_SUCH_GRANTEE(granteeId, null);
        }

        if (granteeName != null) {
            try {
                entryByName = FolderAction.lookupGranteeByName(granteeName, granteeType, zsc);
            } catch (ServiceException se) {
                throw MailServiceException.NO_SUCH_GRANTEE(granteeName, null);
            }
        }

        if (entryById == null && entryByName == null) {
            throw MailServiceException.NO_SUCH_GRANTEE("", null);
        }

        if (entryById != null && entryByName != null &&
                !entryById.getId().equals(entryByName.getId())) {
            throw ServiceException.INVALID_REQUEST("grantee name does not match grantee id", null);
        }

        NamedEntry grantee = (entryById != null)? entryById : entryByName;

        String displayName;
        if (grantee instanceof Account) {
            displayName = ((Account)grantee).getDisplayName();
        } else if (grantee instanceof Group) {
            displayName = ((Group)grantee).getDisplayName();
        } else {
            throw ServiceException.INVALID_REQUEST(
                    "unsupported grantee type for sending share notification email", null);
        }

        return new Pair<NamedEntry, String>(grantee, displayName);
    }

    private Folder getFolder(OperationContext octxt, Account authAccount, Mailbox mbox, Element eShare)
    throws ServiceException {
        String folderId = eShare.getAttribute(MailConstants.A_FOLDER, null);
        String folderPath = eShare.getAttribute(MailConstants.A_PATH, null);

        if (folderId != null && folderPath != null)
            throw ServiceException.INVALID_REQUEST("only one of " + MailConstants.A_FOLDER + " or " +
                    MailConstants.A_PATH + " can be specified", null);

        Folder folder;
        if (folderId != null) {
            try {
                int fid = Integer.parseInt(folderId);
                folder = mbox.getFolderById(octxt, fid);
                if (folder == null)
                    throw MailServiceException.NO_SUCH_FOLDER(folderId);
            } catch (NumberFormatException nfe) {
                throw ServiceException.INVALID_REQUEST("malformed item ID: " + folderId, nfe);
            }
        } else {
            folder = mbox.getFolderByPath(octxt, folderPath);
            if (folder == null)
                throw MailServiceException.NO_SUCH_FOLDER(folderPath);
        }

        return folder;
    }

    protected MimeMessage generateShareNotification(Account authAccount, Account ownerAccount,
            ShareInfoData sid, String notes, Action action, Collection<String> internalRecipients,
            String externalRecipient) throws ServiceException, MessagingException {
        return generateShareNotification(authAccount, ownerAccount, sid, notes, action, internalRecipients, externalRecipient, false);
    }

    protected MimeMessage generateShareNotification(Account authAccount, Account ownerAccount,
        ShareInfoData sid, String notes, Action action, Collection<String> internalRecipients,
        String externalRecipient, boolean notifyForDocument) throws ServiceException, MessagingException {
        Locale locale = authAccount.getLocale();
        String charset = authAccount.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset,
            MimeConstants.P_CHARSET_UTF8);

        MsgKey subjectKey;
        if (action == null) {
            subjectKey = MsgKey.shareNotifSubject;
        } else {
            switch (action) {
            case edit:
                subjectKey = MsgKey.shareModifySubject;
                break;
            case revoke:
                subjectKey = MsgKey.shareRevokeSubject;
                break;
            case expire:
                subjectKey = MsgKey.shareExpireSubject;
                break;
            default:
                subjectKey = MsgKey.shareNotifSubject;
            }
        }
        String subject = L10nUtil.getMessage(subjectKey, locale);
        String ownerAcctDisplayName = ownerAccount.getDisplayName();
        if (ownerAcctDisplayName == null) {
            ownerAcctDisplayName = ownerAccount.getName();
        }
        subject += L10nUtil.getMessage(MsgKey.sharedBySubject, locale, sid.getName(),
            ownerAcctDisplayName);
        String recipient = sid.getGranteeName();
        String extUserShareAcceptUrl = null;
        String extUserLoginUrl = null;
        String externalGranteeName = null;
        if (sid.getGranteeTypeCode() == ACL.GRANTEE_GUEST) {
            externalGranteeName = sid.getGranteeName();
        } else if (sid.getGranteeTypeCode() == ACL.GRANTEE_GROUP && externalRecipient != null) {
            externalGranteeName = externalRecipient;
        }
        // this mail will go to external email address
        boolean goesToExternalAddr = (externalGranteeName != null);
        if (action == null && goesToExternalAddr) {
            Account owner = Provisioning.getInstance().getAccountById(sid.getOwnerAcctId());
            extUserShareAcceptUrl = AccountUtil.getShareAcceptURL(owner, sid.getItemId(), externalGranteeName);
            extUserLoginUrl = AccountUtil.getExtUserLoginURL(owner);
        }
        String mimePartText = ShareInfo.NotificationSender.getMimePartText(sid, notes, locale,
            action, extUserShareAcceptUrl, extUserLoginUrl, notifyForDocument);
        String mimePartHtml = ShareInfo.NotificationSender.getMimePartHtml(sid, notes, locale,
            action, extUserShareAcceptUrl, extUserLoginUrl, notifyForDocument);

        String mimePartXml = null;
        if (!goesToExternalAddr && !notifyForDocument) {
            mimePartXml = ShareInfo.NotificationSender.genXmlPart(sid, notes, null, action);
        }

        // if notifyForDocument = true, do not attach the xml mimepart.
        MimeMultipart mmp = AccountUtil.generateMimeMultipart(mimePartText, mimePartHtml,
                notifyForDocument ? null : mimePartXml);
        MimeMessage mm = AccountUtil.generateMimeMessage(authAccount, ownerAccount, subject,
            charset, internalRecipients, externalRecipient, recipient, mmp);
        return mm;
    }

    private static long timestamp;
    private static String template;

    private void sendNotificationEmail(OperationContext octxt, Mailbox mbox,
            Account authAccount, Account ownerAccount,
            ShareInfoData sid, String notes, Action action,
            Collection<String> internalRecipients, String externalRecipient, MailItem reqItem)
    throws ServiceException, MessagingException  {
        ZimbraLog.account.debug("Item being Shared : %s ",reqItem);
        MimeMessage mm = null;
        if (reqItem instanceof Document) {
            ZimbraLog.account.debug("Sending share notification to [ %s ] for document %s", sid.getGranteeName(), reqItem.getName());
            mm = generateShareNotification(authAccount, ownerAccount, sid, notes, action,
                    internalRecipients, externalRecipient, true);
        } else {
            mm = generateShareNotification(authAccount, ownerAccount, sid, notes, action,
                    internalRecipients, externalRecipient);
        }
        mbox.getMailSender().sendMimeMessage(octxt, mbox, true, mm, null, null, null, null, false);
    }


    private void sendNotificationEmailToGroupGrantees(OperationContext octxt, Mailbox mbox,
            Account authAccount, Account ownerAccount, Collection<ShareInfoData> sids,
            String notes, Action action, MailItem reqItem)
    throws ServiceException, MessagingException {
        Provisioning prov = Provisioning.getInstance();

        for (ShareInfoData sid : sids) {
            String granteeId = sid.getGranteeId();
            Group group = prov.getGroupBasic(DistributionListBy.id, granteeId);
            if (group == null) {
                // huh?  grantee type is GROUP but the group cannot be found by id.
                // just log a warning and do not send.
                // This is not likely to happen because validateRequest had already
                // checked existance of the grantee.
                sLog.warn("Group not found for sending share notificaiton to: " +
                        granteeId + "(" + sid.getGranteeNotifName() + ")" +
                        ", share notification not sent");
                continue;
            }

            /*
             * send to group members
             */
            GroupMemberEmailAddrs addrs = prov.getMemberAddrs(group);
            if (addrs.groupAddr() != null) {
                // just send to the group's address, no treatment needed for recipients
                sendNotificationEmail(octxt, mbox, authAccount, ownerAccount, sid,
                        notes, action, null, null, reqItem);
            } else {
                // send one common notif email to all internal members,
                if (addrs.internalAddrs() != null) {
                    sendNotificationEmail(octxt, mbox, authAccount, ownerAccount, sid,
                            notes, action, addrs.internalAddrs(), null, reqItem);
                }

                // send one personalized notif email to each external member
                Collection<String> extMembers = addrs.externalAddrs();
                if (extMembers != null) {
                    if (extMembers.size() <= DebugConfig.sendGroupShareNotificationSynchronouslyThreshold) {
                        // send synchronously
                        sendNotificationEmailToGroupExternalMembers(octxt, mbox,
                                authAccount, ownerAccount, sid, notes, action, extMembers, reqItem);
                    } else {
                        // send asynchronously in a separate thread to avoid holding up the request
                        sendNotificationEmailToGroupExternalMembersAsync(octxt, mbox,
                                authAccount, ownerAccount, sid, notes, action, extMembers, reqItem);
                    }
                }
            }
        }
    }

    private void sendNotificationEmailToGroupExternalMembers(
            OperationContext octxt, Mailbox mbox,
            Account authAccount, Account ownerAccount, ShareInfoData sid,
            String notes, Action action, Collection<String> extMembers, MailItem reqItem) {
        for (String extMember : extMembers) {
            try {
                sendNotificationEmail(octxt, mbox, authAccount, ownerAccount, sid,
                        notes, action, null, extMember, reqItem);
            } catch (ServiceException e) {
                sLog.warn("Ignoring error while sending share notification to external group member " + extMember , e);
            } catch (MessagingException e) {
                sLog.warn("Ignoring error while sending share notification to external group member " + extMember , e);
            }
        }
    }

    private void sendNotificationEmailToGroupExternalMembersAsync(
            final OperationContext octxt, final Mailbox mbox,
            final Account authAccount, final Account ownerAccount, final ShareInfoData sid,
            final String notes, final Action action, final Collection<String> extMembers, MailItem reqItem) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    sendNotificationEmailToGroupExternalMembers(octxt, mbox,
                            authAccount, ownerAccount, sid, notes, action, extMembers, reqItem);
                } catch (OutOfMemoryError e) {
                    Zimbra.halt("OutOfMemoryError while sending share notification to external group members", e);
                }
            }
        };
        Thread senderThread = new Thread(r, "SendShareNotification");
        senderThread.setDaemon(true);
        senderThread.start();
    }

}

