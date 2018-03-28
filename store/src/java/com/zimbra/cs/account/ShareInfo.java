/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.account;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import com.google.common.base.Strings;
import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning.AccountStatus;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants.ShareConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeBodyPart;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.account.Provisioning.GroupMembership;
import com.zimbra.cs.account.Provisioning.PublishedShareInfoVisitor;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.FolderNode;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.acl.AclPushSerializer;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.AccountUtil.HtmlPartDataSource;
import com.zimbra.cs.util.AccountUtil.XmlPartDataSource;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.mail.message.SendShareNotificationRequest.Action;


public class ShareInfo {

//    private static String S_DELIMITER = ";";

    protected ShareInfoData mData;


    //
    // Grants that are applicable to the entry the share info is for.
    //
    // It is a list(MetadataList) instead of a single object(Metadata) because the entry we are
    // publishing share info for could belong to multiple groups, and each group can have different
    // rights(e.g. r, w, a, ...) on the folder.  The effective folder rights is union of all grants.
    // But when we publish share info we probably want to record where a right is from.   Ee keep
    // a list of all grants that apply to the entry we are publishing share info for.   In the future
    // when we support share info from cos/domain/all authed users, they will be added in the list too.
    //
    // e.g.
    //    - group dl2 is a member of group dl1
    //    - owner shares /Inbox to dl2 for rw rights
    //    - owner shares /Inbox to dl1 for aid rights
    //
    //    If we are publishing share info on dl2, the mGrants will contain two shares for /Inbox
    //
    protected MetadataList mGrants;

    public ShareInfo() {
        mData = new ShareInfoData();
    }

    public boolean hasGrant() {
        return (mGrants != null);
    }

//    /**
//     * serialize this ShareInfo into String persisted in LDAP
//     *
//     * The format is:
//     * owner-zimbraId:itemId:btencoded-metadata
//     *
//     * @return
//     */
//    protected String serialize() throws ServiceException {
//        // callsites should *not* call this if validateAndDiscoverGrants return false.
//        if (mGrants == null)
//            throw ServiceException.FAILURE("internal error, no matching grants", null);
//
//        StringBuilder sb = new StringBuilder();
//        sb.append(serializeOwnerAndFolder());
//        sb.append(S_DELIMITER);
//        sb.append(mGrants.toString());
//
//        return sb.toString();
//    }
//
//    protected String serializeOwnerAndFolder() throws ServiceException {
//        if (mGrants == null)
//            throw ServiceException.FAILURE("internal error, no matching grants", null);
//
//        StringBuilder sb = new StringBuilder();
//        sb.append(mData.getOwnerAcctId());
//        sb.append(S_DELIMITER);
//        sb.append(mData.getItemId());
//        sb.append(S_DELIMITER);
//        sb.append(mData.getItemUuid());
//
//        return sb.toString();
//    }
//
//    protected void deserialize(String encodedShareInfo) throws ServiceException {
//
//        String[] parts = encodedShareInfo.split(S_DELIMITER);
//        if (parts.length != 4) {   // <----------------------------------------- EDITED: 3 -> 4
//            throw ServiceException.FAILURE("malformed share info: " + encodedShareInfo, null);
//        }
//
//        mData.setOwnerAcctId(parts[0]);
//        mData.setItemId(Integer.valueOf(parts[1]));
//        mData.setItemUuid(parts[2]);  // <-------------------------------------- NEW LINE
//
//        String encodedMetadata = parts[3];  // <-------------------------------- EDITED: 2 -> 3
//        mGrants = new MetadataList(encodedMetadata);
//    }

    private static boolean matchesGranteeType(byte onTheGrant, byte wanted) {
        return (wanted == 0 ) || (onTheGrant == wanted);
    }

    /*
     * a convenient method to get the grantee name since Mailbox.ACL.Grant does not set
     * it.  This method is not meant to validate the grant.  If the grantee cannot be found
     * just return empty string.
     */
    private static String granteeName(Provisioning prov, ACL.Grant grant) throws ServiceException {
        String granteeId = grant.getGranteeId();
        byte granteeType = grant.getGranteeType();

        String granteeName = "";
        if (granteeType == ACL.GRANTEE_USER) {
            Account acct = prov.get(AccountBy.id, granteeId);
            if (acct != null)
                granteeName = acct.getName();
        } else if (granteeType == ACL.GRANTEE_GROUP) {
            Group group = prov.getGroupBasic(Key.DistributionListBy.id, granteeId);
            if (group != null)
                granteeName = group.getName();
        } else if (granteeType == ACL.GRANTEE_COS) {
            Cos cos = prov.get(Key.CosBy.id, granteeId);
            if (cos != null)
                granteeName = cos.getName();
        } else if (granteeType == ACL.GRANTEE_DOMAIN) {
            Domain domain = prov.get(Key.DomainBy.id, granteeId);
            if (domain != null)
                granteeName = domain.getName();
        } else {
            // GRANTEE_AUTHUSER, GRANTEE_PUBLIC, GRANTEE_GUEST
            granteeName = ACL.typeToString(granteeType);  // good enough
        }
        return granteeName;
    }

    private static String granteeDisplay(Provisioning prov, ACL.Grant grant) throws ServiceException {
        String granteeId = grant.getGranteeId();
        byte granteeType = grant.getGranteeType();

        String granteeDisplay = "";
        if (granteeType == ACL.GRANTEE_USER) {
            Account acct = prov.get(AccountBy.id, granteeId);
            if (acct != null)
                granteeDisplay = acct.getDisplayName();
        } else if (granteeType == ACL.GRANTEE_GROUP) {
            Group group = prov.getGroupBasic(Key.DistributionListBy.id, granteeId);
            if (group != null)
                granteeDisplay = group.getDisplayName();
        } else if (granteeType == ACL.GRANTEE_COS) {
            Cos cos = prov.get(Key.CosBy.id, granteeId);
            if (cos != null)
                granteeDisplay = cos.getName();
        } else if (granteeType == ACL.GRANTEE_DOMAIN) {
            Domain domain = prov.get(Key.DomainBy.id, granteeId);
            if (domain != null)
                granteeDisplay = domain.getName();
        } else {
            // GRANTEE_AUTHUSER, GRANTEE_PUBLIC, GRANTEE_GUEST
            granteeDisplay = ACL.typeToString(granteeType);  // good enough
        }
        return granteeDisplay;
    }

    private static Set<Folder> getVisibleFolders(OperationContext octxt, Mailbox mbox) throws ServiceException {

        // use the easy one first
        Set<Folder> folders = mbox.getVisibleFolders(octxt);

        // if an admin  is doing this, it can see the entire mailbox of the owner,
        // and mbox.getVisibleFolders will return null
        // in this case get the entire folder tree

        if (folders == null) {
            FolderNode root = mbox.getFolderTree(octxt, null, false);
            // flatten it
            folders = flattenAndSortFolderTree(root);
        }
        return folders;
    }

    private static Set<Folder> flattenAndSortFolderTree(FolderNode root) {
        Set<Folder> folders = new HashSet<Folder>();
        flattenAndSortFolderTree(root, folders);
        return folders;
    }

    private static void flattenAndSortFolderTree(FolderNode node, Set<Folder> flattened) {
        if (node.mFolder != null)
            flattened.add(node.mFolder);
        for (FolderNode subNode : node.mSubfolders)
            flattenAndSortFolderTree(subNode, flattened);
    }


    /*
     * ===========================
     *          MountedFolders
     * ===========================
     */
    public static class MountedFolders {

        /*
         * a map of mounted folders of the account(passed to the ctor) with:
         *     key: {owner-acct-id}:{remote-folder-id}
         *     value: {local-folder-id}
         */
        private final Map<String, Integer> mMountedFolders;

        public MountedFolders(OperationContext octxt, Account acct) throws ServiceException {
            mMountedFolders = getLocalMountpoints(octxt, acct);
        }

        public Integer getLocalFolderId(String ownerAcctId, int remoteFolderId) {
            if (mMountedFolders == null)
                return null;
            else {
                String key = getKey(ownerAcctId, remoteFolderId);
                return mMountedFolders.get(key);
            }
        }

        private String getKey(String ownerAcctId, int remoteFolderId) {
            return ownerAcctId + ":" + remoteFolderId;
        }

        /**
         * returns a map of:
         *     key: {owner-acct-id}:{remote-folder-id}
         *     value: {local-folder-id}
         *
         * @param octxt
         * @param acct
         * @return
         * @throws ServiceException
         */
        private Map<String, Integer> getLocalMountpoints(OperationContext octxt, Account acct) throws ServiceException {
            if (octxt == null)
                return null;

            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct, false);
            if (mbox == null)
                throw ServiceException.FAILURE("mailbox not found for account " + acct.getId(), null);

            return getLocalMountpoints(octxt, mbox);

        }

        private Map<String, Integer> getLocalMountpoints(OperationContext octxt, Mailbox mbox) throws ServiceException {

            Map<String, Integer> mountpoints = new HashMap<String, Integer>();

            try (final MailboxLock l = mbox.getReadLockAndLockIt()) {
                // get the root node...
                int folderId = Mailbox.ID_FOLDER_USER_ROOT;
                Folder folder = mbox.getFolderById(octxt, folderId);

                // for each subNode...
                Set<Folder> visibleFolders = mbox.getVisibleFolders(octxt);
                getLocalMountpoints(folder, visibleFolders, mountpoints);
            }

            return mountpoints;
        }

        private void getLocalMountpoints(Folder folder, Set<Folder> visible, Map<String, Integer> mountpoints) throws ServiceException {
            boolean isVisible = visible == null || visible.remove(folder);
            if (!isVisible)
                return;

            if (folder instanceof Mountpoint) {
                Mountpoint mpt = (Mountpoint)folder;
                String mid =  getKey(mpt.getOwnerId(), mpt.getRemoteId());
                mountpoints.put(mid, mpt.getId());
            }

            // if this was the last visible folder overall, no need to look at children
            if (visible != null && visible.isEmpty())
                return;

            // write the subfolders' data to the response
            for (Folder subfolder : folder.getSubfolders(null)) {
                getLocalMountpoints(subfolder, visible, mountpoints);
            }
        }
    }


    /*
     * ===========================
     *          Discover
     * ===========================
     */
    public static class Discover extends ShareInfo {

        public static void discover(OperationContext octxt, Provisioning prov, Account targetAcct,
                byte granteeType, Account ownerAcct, PublishedShareInfoVisitor visitor) throws ServiceException {

            Mailbox ownerMbox = MailboxManager.getInstance().getMailboxByAccount(ownerAcct, false);
            if (ownerMbox == null)
                throw ServiceException.FAILURE("mailbox not found for account " + ownerAcct.getId(), null);

            Set<Folder> folders = getVisibleFolders(octxt, ownerMbox);
            for (Folder folder : folders)
                doDiscover(prov, targetAcct, granteeType, ownerAcct, folder, visitor);
        }

        private static void doDiscover(Provisioning prov, Account targetAcct,
                byte granteeType, Account ownerAcct, Folder folder, PublishedShareInfoVisitor visitor)
            throws ServiceException {

            ACL acl = folder.getEffectiveACL();

            if (acl == null)
                return;

            for (ACL.Grant grant : acl.getGrants()) {
                if ((targetAcct == null || grant.matches(targetAcct)) &&
                    matchesGranteeType(grant.getGranteeType(), granteeType)) {
                    ShareInfo si = new ShareInfo();

                    si.mData.setOwnerAcctId(ownerAcct.getId());
                    si.mData.setOwnerAcctEmail(ownerAcct.getName());
                    si.mData.setOwnerAcctDisplayName(ownerAcct.getDisplayName());
                    si.mData.setItemId(folder.getId());
                    si.mData.setItemUuid(folder.getUuid());
                    si.mData.setPath(folder.getPath());
                    si.mData.setFolderDefaultView(folder.getDefaultView());
                    si.mData.setRights(grant.getGrantedRights());
                    si.mData.setGranteeType(grant.getGranteeType());
                    si.mData.setGranteeId(grant.getGranteeId());
                    si.mData.setGranteeName(granteeName(prov, grant));
                    si.mData.setGranteeDisplayName(granteeDisplay(prov, grant));

                    visitor.visit(si.mData);
                }
            }
        }
    }


    /*
     * ===========================
     *          Published
     * ===========================
     */
    public static class Published extends ShareInfo {

        /**
         * @param prov
         * @param acct
         * @param granteeType  if not null, return only shares granted to the granteeType
         * @param owner        if not null, return only shares granted by the owner
         * @param visitor
         * @throws ServiceException
         */
        public static void get(
                Provisioning prov, Account acct, byte granteeType, Account owner, PublishedShareInfoVisitor visitor)
            throws ServiceException {

            List<String> granteeIds = new LinkedList<String>();
            boolean includePublicShares = false;
            boolean includeAllAuthedShares = false;
            String guestAcctDomainId = null;
            if (granteeType == 0) {
                // no grantee type specified, return all accessible shares
                granteeIds.add(acct.getId());
                GroupMembership aclGroups = prov.getGroupMembership(acct, false);
                granteeIds.addAll(aclGroups.groupIds());
                granteeIds.add(prov.getDomain(acct).getId());
                Cos cos = prov.getCOS(acct);
                if (cos != null) {
                    granteeIds.add(cos.getId());
                }
                includePublicShares = true;
                includeAllAuthedShares = true;

            } else if (granteeType == ACL.GRANTEE_USER) {
                granteeIds.add(acct.getId());

            } else if (granteeType == ACL.GRANTEE_GROUP) {
                GroupMembership aclGroups = prov.getGroupMembership(acct, false);
                granteeIds.addAll(aclGroups.groupIds());

            } else if (granteeType == ACL.GRANTEE_GUEST && acct.isIsExternalVirtualAccount()) {
                granteeIds.add(acct.getExternalUserMailAddress());
                guestAcctDomainId = prov.getDomain(acct).getId();

            } else if (granteeType == ACL.GRANTEE_PUBLIC) {
                includePublicShares = true;

            } else if (granteeType == ACL.GRANTEE_DOMAIN) {
                granteeIds.add(prov.getDomain(acct).getId());

            } else if (granteeType == ACL.GRANTEE_COS) {
                Cos cos = prov.getCOS(acct);
                if (cos != null) {
                    granteeIds.add(cos.getId());
                }

            } else if (granteeType == ACL.GRANTEE_AUTHUSER) {
                includeAllAuthedShares = true;

            } else {
                throw ServiceException.INVALID_REQUEST(
                        "unsupported grantee type: " + ACL.typeToString(granteeType), null);
            }

            getSharesPublished(prov, visitor, owner, granteeIds, includePublicShares, includeAllAuthedShares,
                    guestAcctDomainId);
        }

        public static void getPublished(Provisioning prov, DistributionList dl, boolean directOnly, Account owner,
                                        PublishedShareInfoVisitor visitor)
            throws ServiceException {

            List<String> granteeIds = new LinkedList<String>();
            granteeIds.add(dl.getId());
            if (!directOnly) {
                granteeIds.addAll(prov.getGroupMembership(dl, false).groupIds());
            }
            getSharesPublished(prov, visitor, owner, granteeIds, false, false, null);
        }

        private static void getSharesPublished(Provisioning prov, PublishedShareInfoVisitor visitor, Account owner,
                List<String> granteeIds, boolean includePublicShares, boolean includeAllAuthedShares,
                String guestAcctDomainId)
                throws ServiceException {

            if (granteeIds.isEmpty() && !includePublicShares && !includeAllAuthedShares) {
                return;
            }

            SearchAccountsOptions searchOpts = new SearchAccountsOptions(
                    new String[] {
                           Provisioning.A_zimbraId,
                           Provisioning.A_displayName,
                           Provisioning.A_zimbraSharedItem,
                           Provisioning.A_zimbraAccountStatus});
           searchOpts.setFilter(ZLdapFilterFactory.getInstance().accountsByGrants(
                   granteeIds, includePublicShares, includeAllAuthedShares));
           List<NamedEntry> accounts = prov.searchDirectory(searchOpts);

            //TODO - check for dups
            for (NamedEntry ne : accounts) {
                Account account = (Account) ne;
                if (owner != null) {
                    if (!owner.getId().equals(account.getId())) {
                        continue;
                    }
                }
                if (guestAcctDomainId != null && !guestAcctDomainId.equals(prov.getDomain(account).getId())) {
                    continue;
                }
                AccountStatus status = account.getAccountStatus();
                if (status != null && !status.isActive()) {
                    continue;
                }
                String[] sharedItems = account.getSharedItem();
                for (String sharedItem : sharedItems) {
                    ShareInfoData shareData = AclPushSerializer.deserialize(sharedItem);
                    if (!shareData.isExpired() &&
                            (granteeIds.contains(shareData.getGranteeId()) ||
                            (includePublicShares && shareData.getGranteeTypeCode() == ACL.GRANTEE_PUBLIC) ||
                            (includeAllAuthedShares && shareData.getGranteeTypeCode() == ACL.GRANTEE_AUTHUSER))) {
                        shareData.setOwnerAcctId(account.getId());
                        shareData.setOwnerAcctEmail(account.getName());
                        shareData.setOwnerAcctDisplayName(account.getDisplayName());
                        visitor.visit(shareData);
                    }
                }
            }
        }

    }

    /*
     * ===========================
     *      NotificationSender
     * ===========================
     */
    public static class NotificationSender {

        private static final String HTML_LINE_BREAK = "<br>";
        private static final String NEWLINE = "\n";

        public static MimeMultipart genNotifBody(ShareInfoData sid, String notes,
            Locale locale, Action action, String externalGroupMember)
        throws MessagingException, ServiceException {

            // Body
            MimeMultipart mmp = new ZMimeMultipart("alternative");

            String extUserShareAcceptUrl = null;
            String extUserLoginUrl = null;
            String externalGranteeName = null;
            if (sid.getGranteeTypeCode() == ACL.GRANTEE_GUEST) {
                externalGranteeName = sid.getGranteeName();
            } else if (sid.getGranteeTypeCode() == ACL.GRANTEE_GROUP && externalGroupMember != null) {
                externalGranteeName = externalGroupMember;
            }
            // this mail will go to external email address
            boolean goesToExternalAddr = (externalGranteeName != null);
            if (action == null && goesToExternalAddr) {
                Account owner = Provisioning.getInstance().getAccountById(sid.getOwnerAcctId());
                extUserShareAcceptUrl = AccountUtil.getShareAcceptURL(owner, sid.getItemId(), externalGranteeName);
                extUserLoginUrl = AccountUtil.getExtUserLoginURL(owner);
            }

            // TEXT part (add me first!)
            String mimePartText;
            if (action == Action.revoke) {
                mimePartText = genRevokePart(sid, locale, false);
            } else if (action == Action.expire) {
                mimePartText = genExpirePart(sid, locale, false);
            } else {
                mimePartText = genPart(sid, action == Action.edit, notes, extUserShareAcceptUrl, extUserLoginUrl,
                        locale, null, false);
            }
            MimeBodyPart textPart = new ZMimeBodyPart();
            textPart.setText(mimePartText, MimeConstants.P_CHARSET_UTF8);
            mmp.addBodyPart(textPart);

            // HTML part
            if (action == Action.revoke) {
                mimePartText = genRevokePart(sid, locale, true);
            } else if (action == Action.expire) {
                mimePartText = genExpirePart(sid, locale, true);
            } else {
                mimePartText = genPart(sid, action == Action.edit, notes, extUserShareAcceptUrl, extUserLoginUrl,
                        locale, null, true);
            }
            MimeBodyPart htmlPart = new ZMimeBodyPart();
            htmlPart.setDataHandler(new DataHandler(new HtmlPartDataSource(mimePartText)));
            mmp.addBodyPart(htmlPart);

            // XML part
            if (!goesToExternalAddr) {
                MimeBodyPart xmlPart = new ZMimeBodyPart();
                xmlPart.setDataHandler(
                        new DataHandler(new XmlPartDataSource(genXmlPart(sid, notes, null, action))));
                mmp.addBodyPart(xmlPart);
            }

            return mmp;
        }

        public static String getMimePartHtml(ShareInfoData sid, String notes, Locale locale,
            Action action, String extUserShareAcceptUrl, String extUserLoginUrl) throws MessagingException, ServiceException {

            String mimePartHtml;
            if (action == Action.revoke) {
                mimePartHtml = genRevokePart(sid, locale, true);
            } else if (action == Action.expire) {
                mimePartHtml = genExpirePart(sid, locale, true);
            } else {
                mimePartHtml = genPart(sid, action == Action.edit, notes, extUserShareAcceptUrl,
                    extUserLoginUrl, locale, null, true);
            }

            return mimePartHtml;
        }



        public static String getMimePartText(ShareInfoData sid, String notes, Locale locale,
            Action action, String extUserShareAcceptUrl, String extUserLoginUrl) throws MessagingException, ServiceException {
            String mimePartText;
            if (action == Action.revoke) {
                mimePartText = genRevokePart(sid, locale, false);
            } else if (action == Action.expire) {
                mimePartText = genExpirePart(sid, locale, false);
            } else {
                mimePartText = genPart(sid, action == Action.edit, notes, extUserShareAcceptUrl,
                    extUserLoginUrl, locale, null, false);
            }
            return mimePartText;
        }

        private static String genPart(ShareInfoData sid, boolean shareModified, String senderNotes,
                String extUserShareAcceptUrl, String extUserLoginUrl, Locale locale, StringBuilder sb, boolean html) {
            if (sb == null) {
                sb = new StringBuilder();
            }
            String externalShareInfo = null;
            if (extUserShareAcceptUrl != null) {
                assert(extUserLoginUrl != null);
                externalShareInfo = L10nUtil.getMessage(
                        html ? MsgKey.shareNotifBodyExternalShareHtml : MsgKey.shareNotifBodyExternalShareText,
                        locale, extUserShareAcceptUrl, extUserLoginUrl);
            }
            if (!Strings.isNullOrEmpty(senderNotes)) {
                if (!html) {
                    senderNotes = L10nUtil.getMessage(MsgKey.shareNotifBodyNotesText, locale,
                        senderNotes);
                } else {
                    senderNotes = senderNotes.replaceAll(NotificationSender.NEWLINE, HTML_LINE_BREAK);
                    senderNotes = L10nUtil.getMessage(MsgKey.shareNotifBodyNotesHtml, locale,
                        senderNotes);
                }
            }
            MsgKey msgKey;
            if (shareModified) {
                msgKey = html ? MsgKey.shareModifyBodyHtml : MsgKey.shareModifyBodyText;
            } else {
                msgKey = html ? MsgKey.shareNotifBodyHtml : MsgKey.shareNotifBodyText;
            }
            return sb.append(L10nUtil.getMessage(
                    msgKey, locale,
                    sid.getName(),
                    formatFolderDesc(locale, sid),
                    sid.getOwnerNotifName(),
                    sid.getGranteeNotifName(),
                    getRoleFromRights(sid, locale),
                    getRightsText(sid, locale),
                    Strings.nullToEmpty(externalShareInfo),
                    Strings.nullToEmpty(senderNotes))).
                    toString();
        }

        private static String genRevokePart(ShareInfoData sid, Locale locale, boolean html) {
            return L10nUtil.getMessage(html ? MsgKey.shareRevokeBodyHtml : MsgKey.shareRevokeBodyText,
                    sid.getName(),
                    formatFolderDesc(locale, sid),
                    sid.getOwnerNotifName());
        }

        private static String genExpirePart(ShareInfoData sid, Locale locale, boolean html) {
            return L10nUtil.getMessage((html ? MsgKey.shareExpireBodyHtml : MsgKey.shareExpireBodyText),
                    sid.getName(),
                    formatFolderDesc(locale, sid),
                    sid.getOwnerNotifName());
        }

        public static String genXmlPart(ShareInfoData sid, String senderNotes, StringBuilder sb, Action action)
                throws ServiceException {
            if (sb == null) {
                sb = new StringBuilder();
            }
            Element share;
            if (action == null || action == Action.edit) {
                share = Element.create(SoapProtocol.Soap12, ShareConstants.SHARE).
                        addAttribute(ShareConstants.A_VERSION, ShareConstants.VERSION).
                        addAttribute(ShareConstants.A_ACTION, action == null ?
                                ShareConstants.ACTION_NEW : ShareConstants.ACTION_EDIT);
            } else {
                share = Element.create(SoapProtocol.Soap12, ShareConstants.REVOKE).
                        addAttribute(ShareConstants.A_VERSION, ShareConstants.VERSION);
                if (action == Action.expire) {
                    share.addAttribute(ShareConstants.A_EXPIRE, true);
                }
            }
            share.addNonUniqueElement(ShareConstants.E_GRANTEE).
                    addAttribute(ShareConstants.A_ID, sid.getGranteeId()).
                    addAttribute(ShareConstants.A_EMAIL, sid.getGranteeName()).
                    addAttribute(ShareConstants.A_NAME, sid.getGranteeNotifName());
            share.addNonUniqueElement(ShareConstants.E_GRANTOR).
                    addAttribute(ShareConstants.A_ID, sid.getOwnerAcctId()).
                    addAttribute(ShareConstants.A_EMAIL, sid.getOwnerAcctEmail()).
                    addAttribute(ShareConstants.A_NAME, sid.getOwnerNotifName());
            Element link = share.addNonUniqueElement(ShareConstants.E_LINK);
            link.addAttribute(ShareConstants.A_ID, sid.getItemId()).
                    addAttribute(ShareConstants.A_NAME, sid.getName()).
                    addAttribute(ShareConstants.A_VIEW, sid.getFolderDefaultView());
            if (action == null || action == Action.edit) {
                link.addAttribute(ShareConstants.A_PERM, ACL.rightsToString(sid.getRightsCode()));
            }
            sb.append(share.prettyPrint());
            return sb.toString();
        }

        private static void appendCommaSeparated(StringBuffer sb, String s) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(s);
        }

        private static String getRoleFromRights(ShareInfoData sid, Locale locale) {
            String rights = sid.getRights();
            rights = rights.replace(ACL.rightsToString(ACL.RIGHT_PRIVATE), "");
            if (rights.equals(ACL.rightsToString(ACL.ROLE_ADMIN))) {
                return L10nUtil.getMessage(MsgKey.shareNotifBodyGranteeRoleAdmin, locale);
            }  else if (rights.equals(ACL.rightsToString(ACL.ROLE_MANAGER))) {
                 return L10nUtil.getMessage(MsgKey.shareNotifBodyGranteeRoleManager, locale);
            }  else if (rights.equals(ACL.rightsToString(ACL.ROLE_VIEW))) {
                return L10nUtil.getMessage(MsgKey.shareNotifBodyGranteeRoleViewer, locale);
            } else if (StringUtil.isNullOrEmpty(sid.getRights())){
                return L10nUtil.getMessage(MsgKey.shareNotifBodyGranteeRoleNone, locale);
            } else {
                return L10nUtil.getMessage(MsgKey.shareNotifBodyGranteeRoleCustom, locale);
            }
        }

        private static String getRightsText(ShareInfoData sid, Locale locale) {
            short rights = sid.getRightsCode();
            StringBuffer r = new StringBuffer();
            if ((rights & ACL.RIGHT_READ) != 0)      appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareNotifBodyActionRead, locale));
            if ((rights & ACL.RIGHT_WRITE) != 0)     appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareNotifBodyActionWrite, locale));
            if ((rights & ACL.RIGHT_INSERT) != 0)    appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareNotifBodyActionInsert, locale));
            if ((rights & ACL.RIGHT_DELETE) != 0)    appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareNotifBodyActionDelete, locale));
            if ((rights & ACL.RIGHT_ADMIN) != 0)     appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareNotifBodyActionAdmin, locale));
            if (r.toString().isEmpty()) {
                return L10nUtil.getMessage(MsgKey.shareNotifBodyActionNone, locale);
            } else {
                return r.toString();
            }
        }

        private static String formatFolderDesc(Locale locale, ShareInfoData sid) {
            MailItem.Type view = sid.getFolderDefaultViewCode();
            String folderView;
            switch (view) {
                case MESSAGE:
                    folderView = L10nUtil.getMessage(MsgKey.mail, locale);
                    break;
                case APPOINTMENT:
                    folderView = L10nUtil.getMessage(MsgKey.calendar, locale);
                    break;
                case TASK:
                    folderView = L10nUtil.getMessage(MsgKey.task, locale);
                    break;
                case CONTACT:
                    folderView = L10nUtil.getMessage(MsgKey.addressBook, locale);
                    break;
                case DOCUMENT:
                    folderView = L10nUtil.getMessage(MsgKey.briefcase, locale);
                    break;
                default:
                    folderView = sid.getFolderDefaultView();
            }
            MsgKey key = MsgKey.shareNotifBodyFolderDesc;
            try {
                if (Provisioning.getInstance().isOctopus()) {
                    key = MsgKey.octopus_share_notification_email_bodyFolderDesc;
                }
            } catch (ServiceException e) {
                ZimbraLog.account.warn("failed to retrieve Octopus info from LDAP " + e);
            }
            return L10nUtil.getMessage(key, locale, folderView);
        }

        private static class MailSenderVisitor implements PublishedShareInfoVisitor {

            List<ShareInfoData> mShares = new ArrayList<ShareInfoData>();

            @Override
            public void visit(ShareInfoData sid) throws ServiceException {
                mShares.add(sid);
            }

            private int getNumShareInfo() {
                return mShares.size();
            }

            private String genText(String dlName, Locale locale, Integer idx) {
                StringBuilder sb = new StringBuilder();

                sb.append("\n");
                sb.append(L10nUtil.getMessage(MsgKey.shareNotifBodyAddedToGroup1, locale, dlName));
                sb.append("\n\n");
                sb.append(L10nUtil.getMessage(MsgKey.shareNotifBodyAddedToGroup2, locale, dlName));
                sb.append("\n\n");

                if (idx == null) {
                    for (ShareInfoData sid : mShares) {
                        genPart(sid, false, null, null, null, locale, sb, false);
                    }
                } else
                    genPart(mShares.get(idx), false, null, null, null, locale, sb, false);

                sb.append("\n\n");
                return sb.toString();
            }


            private String genHtml(String dlName, Locale locale, Integer idx) {
                StringBuilder sb = new StringBuilder();

                sb.append("<h4>\n");
                sb.append("<p>" + L10nUtil.getMessage(MsgKey.shareNotifBodyAddedToGroup1, locale, dlName) + "</p>\n");
                sb.append("<p>" + L10nUtil.getMessage(MsgKey.shareNotifBodyAddedToGroup2, locale, dlName) + "</p>\n");
                sb.append("</h4>\n");
                sb.append("\n");

                if (idx == null) {
                    for (ShareInfoData sid : mShares) {
                        genPart(sid, false, null, null, null, locale, sb, true);
                    }
                } else
                    genPart(mShares.get(idx), false, null, null, null, locale, sb, true);

                return sb.toString();
            }

            private String genXml(Integer idx) throws ServiceException {
                StringBuilder sb = new StringBuilder();

                if (idx == null) {
                    for (ShareInfoData sid : mShares) {
                        genXmlPart(sid, null, sb, null);
                    }
                } else {
                    genXmlPart(mShares.get(idx), null, sb, null);
                }
                return sb.toString();
            }
        }

        /**
         * returns if we should send one mail per share or put all shares in one mail.
         *
         * if all shares are put in one mail, there is no XML part.
         *
         * @return
         */
        private static boolean sendOneMailPerShare() {
            return true;
        }

        public static void sendShareInfoMessage(OperationContext octxt, DistributionList dl, String[] members) {

            Provisioning prov = Provisioning.getInstance();
            Account authedAcct = octxt.getAuthenticatedUser();

            MailSenderVisitor visitor = new MailSenderVisitor();
            try {
                // get all shares published on the DL and all parent DLs
                Published.getPublished(prov, dl, false, null, visitor);
            } catch (ServiceException e) {
                ZimbraLog.account.warn("failed to retrieve share info for dl: " + dl.getName(), e);
                return;
            }

            // no published share, don't send the message.
            if (visitor.getNumShareInfo() == 0)
                return;

            for (String member : members) {
                try {
                    // send a separate mail to each member being added instead of sending one mail to all members being added
                    sendMessage(prov, authedAcct, dl, member, visitor);
                } catch (MessagingException e) {
                    ZimbraLog.account.warn("failed to send share info message", e);
                } catch (ServiceException e) {
                    ZimbraLog.account.warn("failed to send share info message", e);
                }
            }
        }

        private static Locale getLocale(Provisioning prov, Account fromAcct, String toAddr) throws ServiceException {
            Locale locale;
            Account rcptAcct = prov.get(AccountBy.name, toAddr);
            if (rcptAcct != null)
                locale = rcptAcct.getLocale();
            else if (fromAcct != null)
                locale = fromAcct.getLocale();
            else
                locale = prov.getConfig().getLocale();

            return locale;
        }

        /*
         * 1. if dl.zimbraDistributionListSendShareMessageFromAddress is set, use that.
         * 2. otherwise if the authed admin has a valid email address, use that.
         * 3. otherwise use the DL's address.
         */
        private static Pair<Address, Address> getFromAndReplyToAddr(Account fromAcct, DistributionList dl)
                throws AddressException {

            InternetAddress addr;

            // 1. if dl.zimbraDistributionListSendShareMessageFromAddress is set, use that.
            String dlssmfa = dl.getAttr(Provisioning.A_zimbraDistributionListSendShareMessageFromAddress);
            try {
                if (dlssmfa != null) {
                    addr = new JavaMailInternetAddress(dlssmfa);
                    return new Pair<Address, Address>(addr, addr);
                }
            } catch (AddressException e) {
                // log and try the next one
                ZimbraLog.account.warn("invalid address in " +
                        Provisioning.A_zimbraDistributionListSendShareMessageFromAddress +
                        " on distribution list entry " + dl.getName() +
                        ", ignored", e);
            }

            // 2. otherwise if the authed admin has a valid email address, use that.
            if (fromAcct != null) {
                addr = AccountUtil.getFriendlyEmailAddress(fromAcct);
                try {
                    // getFriendlyEmailAddress always return an Address, validate it
                    addr.validate();

                    Address replyToAddr = addr;
                    String replyTo = fromAcct.getAttr(Provisioning.A_zimbraPrefReplyToAddress);
                    if (replyTo != null)
                        replyToAddr = new JavaMailInternetAddress(replyTo);
                    return new Pair<Address, Address>(addr, replyToAddr);
                } catch (AddressException ignored) {
                }
            }

            // 3. otherwise use the DL's address.
            addr = new JavaMailInternetAddress(dl.getName());
            return new Pair<Address, Address>(addr, addr);
        }

        private static MimeMultipart buildMailContent(DistributionList dl, MailSenderVisitor visitor, Locale locale, Integer idx)
            throws MessagingException, ServiceException {

            String shareInfoText = visitor.genText(dl.getName(), locale, idx);
            String shareInfoHtml = visitor.genHtml(dl.getName(), locale, idx);
            String shareInfoXml = null;
            if (idx != null) {
                shareInfoXml = visitor.genXml(idx);
            }
            // Body
            MimeMultipart mmp = new ZMimeMultipart("alternative");

            // TEXT part (add me first!)
            MimeBodyPart textPart = new ZMimeBodyPart();
            textPart.setText(shareInfoText, MimeConstants.P_CHARSET_UTF8);
            mmp.addBodyPart(textPart);

            // HTML part
            MimeBodyPart htmlPart = new ZMimeBodyPart();
            htmlPart.setDataHandler(new DataHandler(new HtmlPartDataSource(shareInfoHtml)));
            mmp.addBodyPart(htmlPart);

            // XML part
            if (shareInfoXml != null) {
                MimeBodyPart xmlPart = new ZMimeBodyPart();
                xmlPart.setDataHandler(new DataHandler(new XmlPartDataSource(shareInfoXml)));
                mmp.addBodyPart(xmlPart);
            }

            return mmp;
        }

        private static void buildContentAndSend(SMTPMessage out, DistributionList dl, MailSenderVisitor visitor, Locale locale, Integer idx)
            throws MessagingException, ServiceException {

            MimeMultipart mmp = buildMailContent(dl, visitor, locale, idx);
            out.setContent(mmp);
            Transport.send(out);

            // log
            Address[] rcpts = out.getRecipients(javax.mail.Message.RecipientType.TO);
            StringBuilder rcptAddr = new StringBuilder();
            for (Address a : rcpts)
                rcptAddr.append(a.toString());
            ZimbraLog.account.info("share info notification sent rcpt='" + rcptAddr + "' Message-ID=" + out.getMessageID());
        }

        private static void sendMessage(Provisioning prov,
                                        Account fromAcct, DistributionList dl, String toAddr,
                                        MailSenderVisitor visitor) throws MessagingException, ServiceException {
            SMTPMessage out = new SMTPMessage(JMSession.getSmtpSession());

            Pair<Address, Address> senderAddrs = getFromAndReplyToAddr(fromAcct, dl);
            Address fromAddr = senderAddrs.getFirst();
            Address replyToAddr = senderAddrs.getSecond();

            // From
            out.setFrom(fromAddr);

            // Reply-To
            out.setReplyTo(new Address[]{replyToAddr});

            // To
            out.setRecipient(javax.mail.Message.RecipientType.TO, new JavaMailInternetAddress(toAddr));

            // Date
            out.setSentDate(new Date());

            // Subject
            Locale locale = getLocale(prov, fromAcct, toAddr);
            String subject = L10nUtil.getMessage(MsgKey.shareNotifSubject, locale);
            out.setSubject(subject);

            if (sendOneMailPerShare()) {
                // send a separate message per share
                // each message will have text/html/xml parts
                int numShareInfo = visitor.getNumShareInfo();
                for (int idx = 0; idx < numShareInfo; idx++) {
                    buildContentAndSend(out, dl, visitor, locale, idx);
                }
            } else {
                // send only one message that includes all shares
                // the message will have only text/html parts, no xml part
                buildContentAndSend(out, dl, visitor, locale, null);
            }
        }
    }
}



