/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.service.mail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.CharsetUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.ShareInfo;
import com.zimbra.cs.account.ShareInfoData;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.SendShareNotificationRequest;
import com.zimbra.soap.mail.type.EmailAddrInfo;

public class SendShareNotification extends MailDocumentHandler {

    private static final Log sLog = LogFactory.getLog(SendShareNotification.class);

    private static final String[] TARGET_ITEM_PATH = new String[] { MailConstants.E_ITEM, MailConstants.A_ID };
    @Override protected String[] getProxiedIdPath(Element request) {
        return TARGET_ITEM_PATH;
    }

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
        String notes = (eNotes==null)?null:eNotes.getText();

        // send the messages
        try {
            for (ShareInfoData sid : shareInfos) {
                sendShareNotif(octxt, account, mbox, sid, notes);
            }
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(
                    "Messaging Exception while sending share notification message", e);
        }

        Element response = zsc.createElement(MailConstants.SEND_SHARE_NOTIFICATION_RESPONSE);
        return response;
    }

    private Collection<ShareInfoData> validateRequest(ZimbraSoapContext zsc, Map<String, Object> context, OperationContext octxt,
            Mailbox mbox, Element request) throws ServiceException {


        Element eShare = request.getOptionalElement(MailConstants.E_SHARE);
        if (eShare != null) {
            return Arrays.asList(validateShareRecipient(zsc, context, octxt, mbox, eShare));
        }

        ArrayList<ShareInfoData> shareInfos = new ArrayList<ShareInfoData>();
        SendShareNotificationRequest req = JaxbUtil.elementToJaxb(request);
        ItemId iid = new ItemId(req.getItem().getId(), zsc);
        MailItem item = mbox.getItemById(octxt, iid.getId(), MailItem.Type.UNKNOWN);
        Provisioning prov = Provisioning.getInstance();
        Account account = getRequestedAccount(zsc);
        if (item instanceof Mountpoint) {
            Mountpoint mp = (Mountpoint)item;
            account = prov.get(AccountBy.id, mp.getOwnerId());
        }
        for (EmailAddrInfo email : req.getEmailAddresses()) {
            // treat the non-existing accounts as guest for now
            Pair<NamedEntry, String> grantee;
            byte granteeType = ACL.GRANTEE_USER;
            String granteeId = null;
            String granteeEmail = email.getAddress();
            String granteeDisplayName = null;
            try {
                grantee = getGrantee(zsc, granteeType, granteeId, granteeEmail);
                if (grantee.getFirst() instanceof com.zimbra.cs.account.Group) {
                    granteeType = ACL.GRANTEE_GROUP;
                }
                granteeId = grantee.getFirst().getId();
                granteeDisplayName = grantee.getSecond();
            } catch (ServiceException e) {
                if (!e.getCode().equals(MailServiceException.NO_SUCH_GRANTEE)) {
                    throw e;
                }
                granteeType = ACL.GRANTEE_GUEST;
                // if guest, granteeId is the same as granteeEmail
                granteeId = granteeEmail;
            }
            shareInfos.add(getShareInfoData(zsc, context, account, octxt, granteeType, granteeEmail, granteeId, granteeDisplayName, item));
        }
        
        return shareInfos;
    }

    @Deprecated
    private ShareInfoData validateShareRecipient(ZimbraSoapContext zsc, Map<String,Object> context, OperationContext octxt, Mailbox mbox, Element eShare) throws ServiceException {

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
            if (granteeName == null)
                throw ServiceException.INVALID_REQUEST("must specify grantee name for guest grantee type", null);

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

        return getShareInfoData(zsc, context, ownerAcct, octxt, granteeType, granteeEmail, matchingId, granteeDisplayName, folder);
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
            MailItem item) throws ServiceException {
        
        Provisioning prov = Provisioning.getInstance();
        MatchingGrant matchingGrant;

        // see if the share specified in the request is real
        if (Provisioning.onLocalServer(ownerAcct))
            matchingGrant = getMatchingGrant(octxt, prov, item, granteeType, granteeId, ownerAcct);
        else
            matchingGrant = getMatchingGrantRemote(zsc, context, granteeType, granteeId, ownerAcct, item.getId());

        if (matchingGrant == null)
            throw ServiceException.INVALID_REQUEST("no matching grant", null);

        //
        // all is well, setup our ShareInfoData object
        //
        ShareInfoData sid = new ShareInfoData();

        sid.setOwnerAcctId(ownerAcct.getId());
        sid.setOwnerAcctEmail(ownerAcct.getName());
        sid.setOwnerAcctDisplayName(ownerAcct.getDisplayName());

        // folder id used for mounting
        sid.setItemId(item.getId());

        //
        // just a display name for the shared folder for the grantee to see.
        // the mountpoint will be created using the folder id.
        //
        // if user2 is sharing with user3 a mountpoint that belongs to user1,
        // we should show user3 the folder(mountpoint) name in user2's mailbox,
        // not the folder name in user1's mailbox.
        String path = (item instanceof Folder) ? ((Folder)item).getPath() : item.getName();
        sid.setPath(path);
        sid.setFolderDefaultView((item instanceof Folder) ? ((Folder)item).getDefaultView() : item.getType());

        // rights
        sid.setRights(matchingGrant.getGrantedRights());

        // grantee
        sid.setGranteeType(granteeType);
        sid.setGranteeId(granteeId);
        sid.setGranteeName(granteeEmail);
        sid.setGranteeDisplayName(granteeDisplayName);

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

        short getGrantedRights() { return  mGrant==null ? mRights : mGrant.getGrantedRights(); }
        String getPassword() { return mGrant==null ? mSecret : mGrant.getPassword(); }
    }

    private MatchingGrant getMatchingGrant(OperationContext octxt, Provisioning prov, MailItem item,
            byte granteeType, String granteeId, Account ownerAcct) throws ServiceException {
        ACL acl = item.getEffectiveACL();
        if (acl == null)
            throw ServiceException.INVALID_REQUEST("no grant on folder", null);

        for (ACL.Grant grant : acl.getGrants()) {
            if (grant.getGranteeType() == granteeType && grant.getGranteeId().equals(granteeId)) {
                return new MatchingGrant(grant);
            }
        }

        return null;
    }

    private MatchingGrant getMatchingGrantRemote(ZimbraSoapContext zsc, Map<String, Object> context,
            byte granteeType, String granteeId, Account ownerAcct, int remoteFolderId) throws ServiceException {

        Element remote = fetchRemoteFolder(zsc, context, ownerAcct.getId(), remoteFolderId);
        Element eAcl = remote.getElement(MailConstants.E_ACL);
        if (eAcl != null) {
            for (Element eGrant : eAcl.listElements(MailConstants.E_GRANT)) {
                try {
                    byte gt = ACL.stringToType(eGrant.getAttribute(MailConstants.A_GRANT_TYPE));
                    if (gt != granteeType)
                        continue;
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
                    if (grant != null)
                        return grant;

                } catch (ServiceException e) {
                    // for some reason the soap response cannot by parsed as expected
                    // ignore and go on
                    sLog.warn("cannot parse soap response for remote grant", e);
                }
            }
        }
        return null;
    }


    private Element fetchRemoteFolder(ZimbraSoapContext zsc, Map<String, Object> context, String ownerId, int remoteId)
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
    private Pair<NamedEntry, String> getGrantee(ZimbraSoapContext zsc, byte granteeType, String granteeId, String granteeName)
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

        if (entryById == null && entryByName == null)
            throw MailServiceException.NO_SUCH_GRANTEE("", null);

        if (entryById != null && entryByName != null &&
                !entryById.getId().equals(entryByName.getId()))
            throw ServiceException.INVALID_REQUEST("grantee name does not match grantee id", null);

        NamedEntry grantee = (entryById != null)? entryById : entryByName;

        String displayName;
        if (grantee instanceof Account)
            displayName = ((Account)grantee).getDisplayName();
        else if (grantee instanceof DistributionList)
            displayName = ((DistributionList)grantee).getDisplayName();
        else
            throw ServiceException.INVALID_REQUEST("unsupported grantee type for sending share notification email", null);

        return new Pair<NamedEntry, String>(grantee, displayName);
    }

    private Folder getFolder(OperationContext octxt, Account authAccount, Mailbox mbox, Element eShare) throws ServiceException {
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


    //
    // send using MailSender
    //
    private void sendShareNotif(OperationContext octxt, Account authAccount, Mailbox mbox, ShareInfoData sid, String notes)
            throws ServiceException, MessagingException {

        Locale locale = authAccount.getLocale();
        String charset = authAccount.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, MimeConstants.P_CHARSET_UTF8);

        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());

        String subject = L10nUtil.getMessage(MsgKey.shareNotifSubject, locale);
        subject += L10nUtil.getMessage(MsgKey.sharedBySubject, locale, sid.getName(), sid.getOwnerNotifName());
        mm.setSubject(subject, CharsetUtil.checkCharset(subject, charset));
        mm.setSentDate(new Date());

        // from the auth account
        mm.setFrom(AccountUtil.getFriendlyEmailAddress(authAccount));

        // to the grantee
        String recipient = sid.getGranteeName();
        mm.setRecipient(javax.mail.Message.RecipientType.TO, new JavaMailInternetAddress(recipient));

        MimeMultipart mmp = ShareInfo.NotificationSender.genNotifBody(
                sid, notes, locale);
        mm.setContent(mmp);
        mm.saveChanges();

        if (sLog.isDebugEnabled()) {
            // log4j.logger.com.zimbra.cs.service.mail=DEBUG

            try {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                mm.writeTo(buf);
                String mmDump = new String(buf.toByteArray());
                sLog.debug("********\n" + mmDump);
            } catch (MessagingException e) {
                sLog.debug("failed log debug share notification message", e);
            } catch (IOException e) {
                sLog.debug("failed log debug share notification message", e);
            }
        }

        mbox.getMailSender().sendMimeMessage(octxt, mbox, true, mm, null, null, null, null, false);
        // also send a copy of the message out to relay MTA
        if (Provisioning.getInstance().getLocalServer().isShareNotificationMtaEnabled()) {
            MailSender.relayMessage(mm);
        }
    }
}

