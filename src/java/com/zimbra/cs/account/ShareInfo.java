/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.mime.shim.JavaMailMimeBodyPart;
import com.zimbra.common.mime.shim.JavaMailMimeMultipart;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.AclGroups;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.PublishedShareInfoVisitor;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.FolderNode;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.ACL.Grant;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;


public class ShareInfo {

    private static String S_DELIMITER = ";";

    // these two keys are set on our owne Metadata object
    private static final String MD_OWNER_EMAIL = "e";
    private static final String MD_OWNER_DISPLAY_NAME = "d";
    private static final String MD_FOLDER_PATH  = "f";
    private static final String MD_FOLDER_DEFAULT_VIEW  = "v";

    /*
     * note: these two keys are set on the same Metadata object as the
     *       one set by ACL.  make sure name is not clashed.
     */
    // for usr/group/guest grantees, this would be the email,
    // for other grantees e.g. cos, domain, this would be name of the cos/domain
    // currently, we can only publish on DL(group), so MD_GRANTEE_NAME will
    // contain the email address of the DL, and MD_GRANTEE_DISPLAY_NAME will
    // contain the displayName of the DL if set, otherwise empty.
    private static final String MD_GRANTEE_NAME = "gn";
    private static final String MD_GRANTEE_DISPLAY_NAME = "gd";

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

    private ShareInfo(ShareInfoData shareInfoData) {
        mData = shareInfoData;
    }


    public boolean hasGrant() {
        return (mGrants != null);
    }

    /**
     * serialize this ShareInfo into String persisted in LDAP
     *
     * The format is:
     * owner-zimbraId:itemId:btencoded-metadata
     *
     * @return
     */
    protected String serialize() throws ServiceException {
        // callsites should *not* call this if validateAndDiscoverGrants return false.
        if (mGrants == null)
            throw ServiceException.FAILURE("internal error, no matching grants", null);

        StringBuilder sb = new StringBuilder();
        sb.append(serializeOwnerAndFolder());
        sb.append(S_DELIMITER);
        sb.append(mGrants.toString());

        return sb.toString();
    }

    protected String serializeOwnerAndFolder() throws ServiceException {
        if (mGrants == null)
            throw ServiceException.FAILURE("internal error, no matching grants", null);

        StringBuilder sb = new StringBuilder();
        sb.append(mData.getOwnerAcctId());
        sb.append(S_DELIMITER);
        sb.append(mData.getFolderId());

        return sb.toString();
    }

    protected void deserialize(String encodedShareInfo) throws ServiceException {

        String[] parts = encodedShareInfo.split(S_DELIMITER);
        if (parts.length != 3) {
            throw ServiceException.FAILURE("malformed share info: " + encodedShareInfo, null);
        }

        mData.setOwnerAcctId(parts[0]);
        mData.setFolderId(Integer.valueOf(parts[1]));

        String encodedMetadata = parts[2];
        mGrants = new MetadataList(encodedMetadata);
    }

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
            DistributionList dl = prov.get(DistributionListBy.id, granteeId);
            if (dl != null)
                granteeName = dl.getName();
        } else if (granteeType == ACL.GRANTEE_COS) {
            Cos cos = prov.get(CosBy.id, granteeId);
            if (cos != null)
                granteeName = cos.getName();
        } else if (granteeType == ACL.GRANTEE_DOMAIN) {
            Domain domain = prov.get(DomainBy.id, granteeId);
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
            DistributionList dl = prov.get(DistributionListBy.id, granteeId);
            if (dl != null)
                granteeDisplay = dl.getDisplayName();
        } else if (granteeType == ACL.GRANTEE_COS) {
            Cos cos = prov.get(CosBy.id, granteeId);
            if (cos != null)
                granteeDisplay = cos.getName();
        } else if (granteeType == ACL.GRANTEE_DOMAIN) {
            Domain domain = prov.get(DomainBy.id, granteeId);
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
        private Map<String, Integer> mMountedFolders;

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
         * @param mbox
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

            synchronized (mbox) {
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

            // short-circuit if we know that this won't be in the output
            List<Folder> subfolders = folder.getSubfolders(null);
            if (!isVisible && subfolders.isEmpty())
                return;

            if (folder instanceof Mountpoint) {
                Mountpoint mpt = (Mountpoint)folder;
                String mid =  getKey(mpt.getOwnerId(), mpt.getRemoteId());
                mountpoints.put(mid, mpt.getId());
            }

            // if this was the last visible folder overall, no need to look at children
            if (isVisible && visible != null && visible.isEmpty())
                return;

            // write the subfolders' data to the response
            for (Folder subfolder : subfolders) {
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
                    si.mData.setFolderId(folder.getId());
                    si.mData.setFolderPath(folder.getPath());
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
     *          Publishing
     * ===========================
     */
    public static class Publishing extends ShareInfo {

        public static void publish(Provisioning prov, OperationContext octxt,
                NamedEntry publishingOnEntry,
                Account ownerAcct, Folder folder) throws ServiceException {

            if (folder == null) {
                // no folder descriptor, do the entire folder tree

                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(ownerAcct, false);
                if (mbox == null)
                    throw ServiceException.FAILURE("mailbox not found for account " + ownerAcct.getId(), null);

                Set<Folder> folders = getVisibleFolders(octxt, mbox);
                for (Folder f : folders)
                    doPublish(prov, publishingOnEntry,  ownerAcct, f);
            } else {
                doPublish(prov, publishingOnEntry, ownerAcct, folder);
            }
        }

        private static void doPublish(Provisioning prov,
                NamedEntry publishingOnEntry,
                Account ownerAcct, Folder folder) throws ServiceException {

            ShareInfo.Publishing si = new ShareInfo.Publishing(ownerAcct, folder);
            si.discoverGrants(folder, publishingOnEntry);
            if (si.hasGrant())
                si.persist(prov, publishingOnEntry);
        }

        private Publishing(Account ownerAcct, Folder folder) {
            mData.setOwnerAcctId(ownerAcct.getId());
            mData.setOwnerAcctEmail(ownerAcct.getName());
            mData.setOwnerAcctDisplayName(ownerAcct.getDisplayName());
            mData.setFolderId(folder.getId());
        }

        /**
         * discover all grants on the folder that matches the publishingOnEntry.
         * for each matched grant, add it to the MetadataList
         *
         * @param folder
         * @param publishingOnEntry
         * @throws ServiceException
         */
        private void discoverGrants(Folder folder, NamedEntry publishingOnEntry) throws ServiceException {
            Provisioning prov = Provisioning.getInstance();

            ACL acl = folder.getEffectiveACL();

            if (acl == null)
                return;

            for (ACL.Grant grant : acl.getGrants()) {
                if (matches(grant, publishingOnEntry)) {
                    if (mGrants == null) {
                        mGrants = new MetadataList();

                        /*
                         * we have:
                         *     owner's zimbraId
                         *     folder id
                         *
                         * when reporting the share info, we will need:
                         *     owner email
                         *     owner displayName
                         *     folder path
                         *
                         * we put owner email/displayName and folder path in the first item of our metadata list,
                         * because it is not convenient (too much LDAP hits if there are lots of published shares
                         * on the entry, and owner accounts are no in cache; for folder name, will need a GetFoler
                         * if the owner mailbox is on differernt server) to get owner name/display from owner id
                         * and get folder path from folder id when we need them.
                         *
                         * cons: if the owner email/display or folder path is renamed, we will have
                         *       stale metadata, but taht be be fixed by letting the admin know and re-publish
                         *       for the owner.
                         *
                         * pros: we save excessive LDAP searches for finding the owner
                         *       name from owner id if the owner is not in cache; and saves
                         *       proxying to other host just to find the folder from folder id.
                         */
                        Metadata md = new Metadata();
                        md.put(MD_OWNER_EMAIL, mData.getOwnerAcctEmail());
                        md.put(MD_OWNER_DISPLAY_NAME, mData.getOwnerAcctDisplayName());

                        // We record the folder path of the folder we are discovering shars for.
                        // It would be nice if we can know the folder id/path on the folder of
                        // the grant (matters when the folder arg came to this method is a sub folder
                        // of the folder of the grant), but there is no convenient way to get it.
                        md.put(MD_FOLDER_PATH, folder.getPath());

                        // for the same reason, we encode the default view for the folder in our metadata
                        md.put(MD_FOLDER_DEFAULT_VIEW, folder.getDefaultView());

                        mGrants.add(md);
                    }

                    Metadata metadata = grant.encode();

                    // ouch, ACL.Grant does not set the grantee name
                    // we do our own here
                    metadata.put(MD_GRANTEE_NAME, granteeName(prov, grant));
                    metadata.put(MD_GRANTEE_DISPLAY_NAME, granteeDisplay(prov, grant));

                    mGrants.add(metadata);
                }
            }
        }


        private boolean matches(ACL.Grant grant, NamedEntry publishingOnEntry) throws ServiceException {
            String granteeId = grant.getGranteeId();  // note: granteeId can be null if this is a pub or all grant!
            byte granteeType = grant.getGranteeType();

            if (publishingOnEntry instanceof DistributionList) {
                if (granteeType != ACL.GRANTEE_GROUP || granteeId == null)
                    return false;

                return (granteeId.equals(publishingOnEntry.getId()) ||
                        Provisioning.getInstance().inDistributionList((DistributionList)publishingOnEntry, granteeId));
            } else
                throw ServiceException.FAILURE("internal", null); // can only publish on group for now

            /*
             * else if (publishingOnEntry instanceof Cos)
             *     return grant.getGranteeId().equals(cos.getId());
             * else if (publishingOnEntry instanceof domain)
             *     return grant.getGranteeId().equals(domain.getId());
             */
        }

        /**
         * persists shareInfo in LDAP on the publishingOnEntry entry
         *
         * @param prov
         * @param publishingOnEntry
         * @param shareInfo
         * @throws ServiceException
         */
        public void persist(Provisioning prov, NamedEntry publishingOnEntry)
            throws ServiceException {

            Set<String> curShareInfo = publishingOnEntry.getMultiAttrSet(Provisioning.A_zimbraShareInfo);
            String addKey    = "+" + Provisioning.A_zimbraShareInfo;
            String removeKey = "-" + Provisioning.A_zimbraShareInfo;

            Map<String, Object> attrs = new HashMap<String, Object>();
            String value = serialize();
            attrs.put(addKey, value);

            /*
             * replace existing share info for the same owner:folder
             * we remove any value that starts with the same owner:folder
             * (there should only be one, but we go through all in case
             *  the data got in via some unexpected way)
             *
             * one caveat: if we are adding an existing owner:folder and the share info have not
             * changed since last published, we do not want to put a - in the mod map, because
             * that will remove the value put in above.
             */
            Set<String> toRemove = new HashSet<String>();
            String ownerAndFoler = serializeOwnerAndFolder();
            for (String curSi : curShareInfo) {
                if (curSi.startsWith(ownerAndFoler) && !curSi.equals(value))
                    toRemove.add(curSi);
            }
            if (!toRemove.isEmpty())
                attrs.put(removeKey, toRemove);

            prov.modifyAttrs(publishingOnEntry, attrs);
        }
    }


    /*
     * ===========================
     *          Published
     * ===========================
     */
    public static class Published extends ShareInfo {


        private String mDigest; // for deduping

        /*
         * returns a list of Published from an encoded string
         *
         * each zimbraShareInfo value can expand to *multiple* Published share info, because
         * for each owner:folder, there could be multiple matched grantees
         * e.g.
         *    - group dl2 is a member of group dl1
         *    - owner shares /inbox to dl2 for rw rights
         *    - owner shares /inbox to dl1 for aid rights
         */
        private static List<Published> decodeMetadata(String encoded) throws ServiceException {
            List<Published> siList = new ArrayList<Published>();

            // deserialize encoded to a dummy Published first
            Published si = new Published(encoded);

            //
            // split the dummy to multiple, see comments on
            // "It is a list(MetadataList) instead of a single object(Metadata) because ..."
            //

            // data not btencoded in metadata
            String ownerAcctId = si.mData.getOwnerAcctId();
            int folderId = si.mData.getFolderId();

            // data btencoded in metadata by us (ShareInfo.Publishing)
            Metadata metadata = si.mGrants.getMap(0);
            String ownerAcctEmail = metadata.get(MD_OWNER_EMAIL);
            String ownerAcctDisplayName = metadata.get(MD_OWNER_DISPLAY_NAME, null);
            String folderPath = metadata.get(MD_FOLDER_PATH);
            MailItem.Type folderDefaultView = MailItem.Type.of((byte) metadata.getLong(MD_FOLDER_DEFAULT_VIEW));

            // data encoded by ACL.grant
            for (int i = 1; i < si.mGrants.size(); i++) {
                metadata = si.mGrants.getMap(i);

                Grant grant = new Grant(metadata);

                short rights = grant.getGrantedRights();
                byte granteeType = grant.getGranteeType();
                String granteeId = grant.getGranteeId();

                // Mailbox.ACL never sets it, get it from our key in the metadata
                String granteeName = metadata.get(MD_GRANTEE_NAME);

                // get display name from our metadata too
                String granteeDisplayName = metadata.get(MD_GRANTEE_DISPLAY_NAME, null);

                Published p = new Published(ownerAcctId, ownerAcctEmail, ownerAcctDisplayName,
                                            folderId, folderPath, folderDefaultView,
                                            rights, granteeType, granteeId, granteeName, granteeDisplayName);
                siList.add(p);
            }

            return siList;
        }

        // only used for the constructing the dummy for spliting
        private Published(String encodedShareInfo) throws ServiceException {
            deserialize(encodedShareInfo);
        }

        private Published() {
        }

        private Published(String ownerAcctId, String ownerAcctEmail, String ownerAcctDisplayName,
                int folderId, String folderPath, MailItem.Type folderDefaultView,
                short rights, byte granteeType, String granteeId, String granteeName, String granteeDisplayName) {

            mData.setOwnerAcctId(ownerAcctId);
            mData.setOwnerAcctEmail(ownerAcctEmail);
            mData.setOwnerAcctDisplayName(ownerAcctDisplayName);
            mData.setFolderId(folderId);
            mData.setFolderPath(folderPath);
            mData.setFolderDefaultView(folderDefaultView);
            mData.setRights(rights);
            mData.setGranteeType(granteeType);
            mData.setGranteeId(granteeId);
            mData.setGranteeName(granteeName);
            mData.setGranteeDisplayName(granteeDisplayName);

            mDigest = mData.getOwnerAcctEmail() +
                      mData.getFolderPath() +
                      mData.getRightsCode() +
                      mData.getGranteeType() +
                      mData.getGranteeId() +
                      mData.getGranteeName();
        }



        private String getDigest() {
            return mDigest;
        }

        /**
         *
         * @param acct
         * @param granteeType  if not null, return only shares granted to the granteeType
         * @param owner        if not null, return only shares granted to the owner
         * @param visitor
         * @throws ServiceException
         */
        public static void get(Provisioning prov, Account acct, byte granteeType, Account owner, PublishedShareInfoVisitor visitor)
            throws ServiceException {

            Set<String> visited = new HashSet<String>();

            if (granteeType == 0) {
                // no grantee type specified, return all published shares

                // only group shares can be published for now, so just
                // retrieve the group shares
                AclGroups aclGroups = prov.getAclGroups(acct, false);
                getSharesPublishedOnGroups(prov, visitor, aclGroups, owner, visited);

                /*
                 * if we support publishing cos and domain shares, include them here
                 */
                // cos
                // getSharesPublishedOnCos(...);

                // domain
                // getSharesPublishedOnDomain(...);

            } else if (granteeType == ACL.GRANTEE_USER) {
                // cannot publish on account, be tolerant just return instead of throw

            } else if (granteeType == ACL.GRANTEE_GROUP) {
                AclGroups aclGroups = prov.getAclGroups(acct, false);
                getSharesPublishedOnGroups(prov, visitor, aclGroups, owner, visited);

            } else {
                throw ServiceException.INVALID_REQUEST("unsupported grantee type", null);
            }
        }

        // for admin only, like the above, also called for sending emails
        public static void getPublished(Provisioning prov, DistributionList dl, boolean directOnly, Account owner, PublishedShareInfoVisitor visitor)
            throws ServiceException {

            Set<String> visited = new HashSet<String>();

            // get shares published on the dl
            getPublishedShares(visitor, dl, owner, visited);

            if (!directOnly) {
                // call prov.getAclGroups instead of prov.getDistributionLists
                // because getAclGroups returns cached data, while getDistributionLists
                // does LDAP searches each time

                // get shares published on parents of this dl
                if (!dl.isAclGroup())
                    dl = prov.getAclGroup(DistributionListBy.id, dl.getId());
                AclGroups aclGroups = prov.getAclGroups(dl, false);
                getSharesPublishedOnGroups(prov, visitor, aclGroups, owner, visited);
            }
        }

        /**
         * We should allow removing (un-publishing) share info even when the owner
         * account/mailbox/folder does not exist.  They could've been deleted.
         *
         * We match the request with published as much as we can.
         * For perf reason, owner email and folder name is also persisted in the
         * published share info.
         *
         * @param prov
         * @param unpublishingOnEntry
         * @param ownerAcctId
         * @param ownerAcctEmail
         * @param allOwners
         * @param folderId
         * @param folderPath
         * @param allFolders
         * @throws ServiceException
         */
        public static void unpublish(Provisioning prov,
                DistributionList dl,
                String ownerAcctId, String ownerAcctEmail, boolean allOwners,
                String folderId, String folderPath, boolean allFolders) throws ServiceException {

            Matcher matcher = new Matcher(ownerAcctId, ownerAcctEmail, allOwners,
                     folderId,  folderPath,  allFolders);

            Set<String> publishedShareInfo = dl.getMultiAttrSet(Provisioning.A_zimbraShareInfo);
            String removeKey = "-" + Provisioning.A_zimbraShareInfo;

            Set<String> toRemove = new HashSet<String>();

            for (String psi : publishedShareInfo) {

                try {
                    // each zimbraShareInfo value can expand to *multiple* Published share info,
                    // see comments for decodeMetadata
                    List<Published> siList = decodeMetadata(psi);

                    for (Published si : siList) {
                        if (matcher.matches(si.mData)) {
                            toRemove.add(psi);
                            break;
                        }
                    }

                } catch (ServiceException e) {
                    // probably encountered malformed share info, log and ignore
                    // should remove the value from LDAP?
                    ZimbraLog.account.warn("unable to process share info", e);
                }
            }

            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(removeKey, toRemove);
            prov.modifyAttrs(dl, attrs);
        }

        /*
         * matcher for unpublishing
         */
        private static class Matcher {

            private String mOwnerAcctId;
            private String mOwnerAcctEmail;
            private boolean mAllOwners;
            private String mFolderId;
            private String mFolderPath;
            private boolean mAllFolders;

            private Matcher(String ownerAcctId, String ownerAcctEmail, boolean allOwners,
                    String folderId, String folderPath, boolean allFolders) {
                mOwnerAcctId = ownerAcctId;
                mOwnerAcctEmail = ownerAcctEmail;
                mAllOwners = allOwners;
                mFolderId = folderId;
                mFolderPath = folderPath;
                mAllFolders = allFolders;
            }

            private boolean matches(ShareInfoData sid) {
                return matchOwner(sid) && matchFolder(sid);
            }

            private boolean matchOwner(ShareInfoData sid) {
                if (mAllOwners)
                    return true;

                // match id if provided
                if (mOwnerAcctId != null)
                    return mOwnerAcctId.equals(sid.getOwnerAcctId());

                // match email if provided
                if (mOwnerAcctEmail != null)
                    return mOwnerAcctEmail.equals(sid.getOwnerAcctEmail());

                // not matched
                return false;
            }

            private boolean matchFolder(ShareInfoData sid) {
                if (mAllFolders)
                    return true;

                // match folder id if provided
                if (mFolderId != null)
                    return mFolderId.equals(String.valueOf(sid.getFolderId()));

                // match folder path if provided
                if (mFolderPath != null)
                    return mFolderPath.equals(sid.getFolderPath());

                // not matched
                return false;
            }
        }

        private static void getSharesPublishedOnGroups(Provisioning prov, PublishedShareInfoVisitor visitor,
                AclGroups aclGroups, Account owner, Set<String> visited)
            throws ServiceException {

            /*
             * getAclGroup(by, key)
             *    - objects cached in LdapProvisioning
             *    - currently cached objects do not cache contain zimbraShareInfo,
             *      which can be big?
             *
             * getDistributionList(by, key)
             *    - not cached in LdapProvisioning, will trigger an
             *      LDAP search when called each time.
             *
             * call getDistributionList or now, so we don't increase memory
             * usage.  If the LDAP search becomes a problem, then cache zimbraShareInfo
             * in the object returned by getAclGroup(LdapProvisioning sMinimalDlAttrs),
             * and change the following call to prov.getAclGroup.
             *
             */
            for (String groupId : aclGroups.groupIds()) {
                DistributionList group = prov.get(DistributionListBy.id, groupId);
                getPublishedShares(visitor, group, owner, visited);
            }
        }

        /**
         * get shares published on the entry
         *
         * @param visitor
         * @param entry
         * @param owner if not null, include only shares owned by the owner
         *              if null, include all shares published on the entry
         */
        private static void getPublishedShares(PublishedShareInfoVisitor visitor, NamedEntry entry, Account owner, Set<String> visited) {
            Set<String> publishedShareInfo = entry.getMultiAttrSet(Provisioning.A_zimbraShareInfo);

            for (String psi : publishedShareInfo) {

                try {
                    // each zimbraShareInfo value can expand to *multiple* Published share info,
                    // see comments for decodeMetadata
                    List<Published> siList = decodeMetadata(psi);

                    for (Published si : siList) {
                        if (owner != null) {
                            if (!owner.getId().equals(si.mData.getOwnerAcctId()))
                                continue;
                        }

                        /*
                         * dedup
                         *
                         * It is possible that the same share is published on a group, and
                         * again on a sub group.  We return only  one instance of all the
                         * identical shares.
                         */
                        if (visited.contains(si.getDigest()))
                            continue;

                        visitor.visit(si.mData);
                        visited.add(si.getDigest());
                    }

                } catch (ServiceException e) {
                    // probably encountered malformed share info, log and ignore
                    // should remove the value from LDAP?
                    ZimbraLog.account.warn("unable to process share info", e);
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

        private static final short ROLE_VIEW  = ACL.RIGHT_READ;
        private static final short ROLE_ADMIN = ACL.RIGHT_READ |
                                                ACL.RIGHT_WRITE |
                                                ACL.RIGHT_INSERT |
                                                ACL.RIGHT_DELETE |
                                                ACL.RIGHT_ACTION |
                                                ACL.RIGHT_ADMIN;
        private static final short ROLE_MANAGER = ACL.RIGHT_READ |
                                                  ACL.RIGHT_WRITE |
                                                  ACL.RIGHT_INSERT |
                                                  ACL.RIGHT_DELETE |
                                                  ACL.RIGHT_ACTION;


        public static MimeMultipart genNotifBody(ShareInfoData sid, MsgKey intro, String notes, Locale locale) throws MessagingException {

            // Body
            MimeMultipart mmp = new JavaMailMimeMultipart("alternative");

            // TEXT part (add me first!)
            MimeBodyPart textPart = new JavaMailMimeBodyPart();
            textPart.setText(genTextPart(sid, intro, notes, locale, null), MimeConstants.P_CHARSET_UTF8);
            mmp.addBodyPart(textPart);

            // HTML part
            MimeBodyPart htmlPart = new JavaMailMimeBodyPart();
            htmlPart.setDataHandler(new DataHandler(new HtmlPartDataSource(genHtmlPart(sid, intro, notes, locale, null))));
            mmp.addBodyPart(htmlPart);

            // XML part
            MimeBodyPart xmlPart = new JavaMailMimeBodyPart();
            xmlPart.setDataHandler(new DataHandler(new XmlPartDataSource(genXmlPart(sid, notes, locale, null))));
            mmp.addBodyPart(xmlPart);

            return mmp;
        }


        private static String genTextPart(ShareInfoData sid, MsgKey intro, String senderNotes, Locale locale, StringBuilder sb) {
            if (sb == null)
                sb = new StringBuilder();

            sb.append("\n");
            if (intro != null) {
                sb.append(L10nUtil.getMessage(intro, locale));
                sb.append("\n\n");
            }

            sb.append(formatTextShareInfo(MsgKey.shareNotifBodySharedItem, sid.getFolderName(), locale, formatFolderDesc(locale, sid)));
            sb.append(formatTextShareInfo(MsgKey.shareNotifBodyOwner, sid.getOwnerNotifName(), locale, null));
            sb.append("\n");
            sb.append(formatTextShareInfo(MsgKey.shareNotifBodyGrantee, sid.getGranteeNotifName(), locale, null));
            sb.append(formatTextShareInfo(MsgKey.shareNotifBodyRole, getRoleFromRights(sid, locale), locale, null));
            sb.append(formatTextShareInfo(MsgKey.shareNotifBodyAllowedActions, getRightsText(sid, locale), locale, null));
            sb.append("\n");

            String notes = null;
            if (sid.getGranteeTypeCode() == ACL.GRANTEE_GUEST) {
                StringBuilder guestNotes = new StringBuilder();
                guestNotes.append("URL: " + sid.getUrl() + "\n");
                guestNotes.append("Username: " + sid.getGranteeName() + "\n");
                guestNotes.append("Password: " + sid.getGuestPassword() + "\n");
                guestNotes.append("\n");
                notes = guestNotes + (senderNotes==null?"":senderNotes) + "\n";
            } else
                notes = senderNotes;

            if (notes != null) {
                sb.append("*~*~*~*~*~*~*~*~*~*\n");
                sb.append(notes +  "\n");
            }

            return sb.toString();
        }

        private static String genHtmlPart(ShareInfoData sid, MsgKey intro, String senderNotes, Locale locale, StringBuilder sb) {
            if (sb == null)
                sb = new StringBuilder();

            if (intro != null) {
                sb.append("<h3>" + L10nUtil.getMessage(intro, locale) + "</h3>\n");
            }

            sb.append("<p>\n");
            sb.append("<table border=\"0\">\n");
            sb.append(formatHtmlShareInfo(MsgKey.shareNotifBodySharedItem, sid.getFolderName(), locale, formatFolderDesc(locale, sid)));
            sb.append(formatHtmlShareInfo(MsgKey.shareNotifBodyOwner, sid.getOwnerNotifName(), locale, null));
            sb.append("</table>\n");
            sb.append("</p>\n");

            sb.append("<table border=\"0\">\n");
            sb.append(formatHtmlShareInfo(MsgKey.shareNotifBodyGrantee, sid.getGranteeNotifName(), locale, null));
            sb.append(formatHtmlShareInfo(MsgKey.shareNotifBodyRole, getRoleFromRights(sid, locale), locale, null));
            sb.append(formatHtmlShareInfo(MsgKey.shareNotifBodyAllowedActions, getRightsText(sid, locale), locale, null));
            sb.append("</table>\n");

            if (sid.getGranteeTypeCode() == ACL.GRANTEE_GUEST) {
                sb.append("<p>\n");
                sb.append("<table border=\"0\">\n");
                sb.append("<tr valign=\"top\"><th align=\"left\">" +
                        L10nUtil.getMessage(MsgKey.shareNotifBodyNotes) + ":" + "</th><td>" +
                        "URL: " + sid.getUrl() + "<br>" +
                        "Username: " + sid.getGranteeName() + "<br>" +
                        "Password: " + sid.getGuestPassword() + "<br><br>");
                if (senderNotes != null)
                    sb.append(senderNotes);
                sb.append("</td></tr></table>\n");
                sb.append("</p>\n");
            } else if (senderNotes != null) {
                sb.append("<p>\n");
                sb.append("<table border=\"0\">\n");
                sb.append("<tr valign=\"top\"><th align=\"left\">" +
                        L10nUtil.getMessage(MsgKey.shareNotifBodyNotes) + ":" + "</th><td>" +
                        senderNotes + "</td></tr></table>\n");
                sb.append("</p>\n");
            }

            return sb.toString();
        }

        private static String genXmlPart(ShareInfoData sid, String senderNotes, Locale locale, StringBuilder sb) {
            if (sb == null)
                sb = new StringBuilder();
            /*
             * from ZimbraWebClient/WebRoot/js/zimbraMail/share/model/ZmShare.js

               ZmShare.URI = "urn:zimbraShare";
               ZmShare.VERSION = "0.1";
               ZmShare.NEW     = "new";
            */
            final String URI = "urn:zimbraShare";
            final String VERSION = "0.1";

            String notes = null;
            if (sid.getGranteeTypeCode() == ACL.GRANTEE_GUEST) {
                StringBuilder guestNotes = new StringBuilder();
                guestNotes.append("URL: " + sid.getUrl() + "\n");
                guestNotes.append("Username: " + sid.getGranteeName() + "\n");
                guestNotes.append("Password: " + sid.getGuestPassword() + "\n");
                guestNotes.append("\n");
                notes = guestNotes + (senderNotes==null?"":senderNotes) + "\n";
            } else
                notes = senderNotes;

            sb.append("<share xmlns=\"" + URI + "\" version=\"" + VERSION + "\" action=\"new\">\n");
            sb.append("  <grantee id=\"" + sid.getGranteeId() + "\" email=\"" + sid.getGranteeName() + "\" name=\"" + sid.getGranteeNotifName() +"\"/>\n");
            sb.append("  <grantor id=\"" + sid.getOwnerAcctId() + "\" email=\"" + sid.getOwnerAcctEmail() + "\" name=\"" + sid.getOwnerNotifName() +"\"/>\n");
            sb.append("  <link id=\"" + sid.getFolderId() + "\" name=\"" + sid.getFolderName() + "\" view=\"" + sid.getFolderDefaultView() + "\" perm=\"" + ACL.rightsToString(sid.getRightsCode()) + "\"/>\n");
            sb.append("  <notes>" + (notes==null?"":notes) + "</notes>\n");
            sb.append("</share>\n");

            return sb.toString();
        }

        private static String formatTextShareInfo(MsgKey key, String value, Locale locale, String extra) {
            return L10nUtil.getMessage(key, locale) + ": " + value + (extra==null?"":" "+extra) + "\n";
        }

        private static String formatHtmlShareInfo(MsgKey key, String value, Locale locale, String extra) {
            return "<tr>" +
                   "<th align=\"left\">" + L10nUtil.getMessage(key, locale) + ":" + "</th>" +
                   "<td align=\"left\">" + value + (extra==null?"":" "+extra) + "</td>" +
                   "</tr>\n";
        }

        private static void appendCommaSeparated(StringBuffer sb, String s) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(s);
        }

        private static String getRoleFromRights(ShareInfoData sid, Locale locale) {
            short rights = sid.getRightsCode();
            if (ROLE_VIEW == rights)
                return L10nUtil.getMessage(MsgKey.shareNotifBodyGranteeRoleViewer, locale);
            else if (ROLE_ADMIN == rights)
                return L10nUtil.getMessage(MsgKey.shareNotifBodyGranteeRoleAdmin, locale);
            else if (ROLE_MANAGER == rights)
                return L10nUtil.getMessage(MsgKey.shareNotifBodyGranteeRoleManager, locale);
            else
                return "";
        }

        private static String getRightsText(ShareInfoData sid, Locale locale) {
            short rights = sid.getRightsCode();
            StringBuffer r = new StringBuffer();
            if ((rights & ACL.RIGHT_READ) != 0)      appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareNotifBodyActionRead, locale));
            if ((rights & ACL.RIGHT_WRITE) != 0)     appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareNotifBodyActionWrite, locale));
            if ((rights & ACL.RIGHT_INSERT) != 0)    appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareNotifBodyActionInsert, locale));
            if ((rights & ACL.RIGHT_DELETE) != 0)    appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareNotifBodyActionDelete, locale));
            if ((rights & ACL.RIGHT_ACTION) != 0)    appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareNotifBodyActionAction, locale));
            if ((rights & ACL.RIGHT_ADMIN) != 0)     appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareNotifBodyActionAdmin, locale));
            if ((rights & ACL.RIGHT_PRIVATE) != 0)   appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareNotifBodyActionPrivate, locale));
            if ((rights & ACL.RIGHT_FREEBUSY) != 0)  appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareNotifBodyActionFreebusy, locale));
            if ((rights & ACL.RIGHT_SUBFOLDER) != 0) appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareNotifBodyActionSubfolder, locale));

            return r.toString();
        }

        private static String formatFolderDesc(Locale locale, ShareInfoData sid) {
            MailItem.Type view = sid.getFolderDefaultViewCode();

            String folderView;  // need to L10N these?
            switch (view) {
            case MESSAGE:
                folderView = "Mail";
                break;
            case APPOINTMENT:
                folderView = "Calendar";
                break;
            case TASK:
                folderView = "Task";
                break;
            case CONTACT:
                folderView = "Addres";
                break;
            case WIKI:
                folderView = "Notebook";
                break;
            default:
                folderView = sid.getFolderDefaultView();
                break;
            }

            return L10nUtil.getMessage(MsgKey.shareNotifBodyFolderDesc, locale, folderView);
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
                        genTextPart(sid, null, null, locale, sb);
                    }
                } else
                    genTextPart(mShares.get(idx.intValue()), null, null, locale, sb);

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
                        genHtmlPart(sid, null, null, locale, sb);
                    }
                } else
                    genHtmlPart(mShares.get(idx.intValue()), null, null, locale, sb);

                return sb.toString();
            }

            private String genXml(String dlName, Locale locale, Integer idx) {
                StringBuilder sb = new StringBuilder();

                 if (idx == null) {
                    for (ShareInfoData sid : mShares) {
                        genXmlPart(sid, null, locale, sb);
                    }
                } else
                    genXmlPart(mShares.get(idx.intValue()), null, locale, sb);

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

            try {
                // send a separate mail to each member being added instead of sending one mail to all members being added
                for (String member : members)
                    sendMessage(prov, authedAcct, dl, member, visitor);
            } catch (ServiceException e) {
                ZimbraLog.account.warn("failed to send share info message", e);
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
        private static Pair<Address, Address> getFromAndReplyToAddr(Provisioning prov, Account fromAcct, DistributionList dl) throws AddressException {

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
                } catch (AddressException e) {
                }
            }

            // 3. otherwise use the DL's address.
            addr = new JavaMailInternetAddress(dl.getName());
            return new Pair<Address, Address>(addr, addr);
        }

        private static MimeMultipart buildMailContent(DistributionList dl, MailSenderVisitor visitor, Locale locale, Integer idx)
            throws MessagingException {

            String shareInfoText = visitor.genText(dl.getName(), locale, idx);
            String shareInfoHtml = visitor.genHtml(dl.getName(), locale, idx);
            String shareInfoXml = null;
            if (idx != null)
                shareInfoXml = visitor.genXml(dl.getName(), locale, idx);

            // Body
            MimeMultipart mmp = new JavaMailMimeMultipart("alternative");

            // TEXT part (add me first!)
            MimeBodyPart textPart = new JavaMailMimeBodyPart();
            textPart.setText(shareInfoText, MimeConstants.P_CHARSET_UTF8);
            mmp.addBodyPart(textPart);

            // HTML part
            MimeBodyPart htmlPart = new JavaMailMimeBodyPart();
            htmlPart.setDataHandler(new DataHandler(new HtmlPartDataSource(shareInfoHtml)));
            mmp.addBodyPart(htmlPart);

            // XML part
            if (shareInfoXml != null) {
                MimeBodyPart xmlPart = new JavaMailMimeBodyPart();
                xmlPart.setDataHandler(new DataHandler(new XmlPartDataSource(shareInfoXml)));
                mmp.addBodyPart(xmlPart);
            }

            return mmp;
        }

        private static void buildContentAndSend(SMTPMessage out, DistributionList dl, MailSenderVisitor visitor, Locale locale, Integer idx)
            throws MessagingException {

            MimeMultipart mmp = buildMailContent(dl, visitor, locale, Integer.valueOf(idx));
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
                                        MailSenderVisitor visitor) throws ServiceException {
            try {
                SMTPMessage out = new SMTPMessage(JMSession.getSmtpSession());

                Pair<Address, Address> senderAddrs = getFromAndReplyToAddr(prov, fromAcct, dl);
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
                        buildContentAndSend(out, dl, visitor, locale, Integer.valueOf(idx));
                    }
                } else {
                    // send only one message that includes all shares
                    // the message will have only text/html parts, no xml part
                    buildContentAndSend(out, dl, visitor, locale, null);
                }

            } catch (MessagingException e) {
                ZimbraLog.account.warn("send share info notification failed rcpt='" + toAddr +"'", e);
            }
        }

        private static abstract class MimePartDataSource implements DataSource {

            private String mText;
            private byte[] mBuf = null;

            public MimePartDataSource(String text) {
                mText = text;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                synchronized(this) {
                    if (mBuf == null) {
                        ByteArrayOutputStream buf = new ByteArrayOutputStream();
                        OutputStreamWriter wout =
                            new OutputStreamWriter(buf, MimeConstants.P_CHARSET_UTF8);
                        String text = mText;
                        wout.write(text);
                        wout.flush();
                        mBuf = buf.toByteArray();
                    }
                }
                ByteArrayInputStream in = new ByteArrayInputStream(mBuf);
                return in;
            }

            @Override
            public OutputStream getOutputStream() {
                throw new UnsupportedOperationException();
            }
        }

        private static class HtmlPartDataSource extends MimePartDataSource {
            private static final String CONTENT_TYPE =
                MimeConstants.CT_TEXT_HTML + "; " + MimeConstants.P_CHARSET + "=" + MimeConstants.P_CHARSET_UTF8;
            private static final String NAME = "HtmlDataSource";

            HtmlPartDataSource(String text) {
                super(text);
            }

            @Override
            public String getContentType() {
                return CONTENT_TYPE;
            }

            @Override
            public String getName() {
                return NAME;
            }
        }

        private static class XmlPartDataSource extends MimePartDataSource {
            private static final String CONTENT_TYPE =
                MimeConstants.CT_XML_ZIMBRA_SHARE + "; " + MimeConstants.P_CHARSET + "=" + MimeConstants.P_CHARSET_UTF8;
            private static final String NAME = "XmlDataSource";

            XmlPartDataSource(String text) {
                super(text);
            }

            @Override
            public String getContentType() {
                return CONTENT_TYPE;
            }

            @Override
            public String getName() {
                return NAME;
            }
        }
    }

    /*
     * for debugging/unittest
     */
    public static class DumpShareInfoVisitor implements PublishedShareInfoVisitor {

        private static final String mFormat =
            "%-36.36s %-15.15s %-15.15s %-5.5s %-20.20s %-10.10s %-10.10s %-5.5s %-5.5s %-36.36s %-15.15s %-15.15s\n";

        public static void printHeadings() {
            System.out.printf(mFormat,
                              "owner id",
                              "owner email",
                              "owner display",
                              "fid",
                              "folder path",
                              "view",
                              "rights",
                              "mid",
                              "gt",
                              "grantee id",
                              "grantee name",
                              "grantee display");

            System.out.printf(mFormat,
                              "------------------------------------",      // owner id
                              "---------------",                           // owner email
                              "---------------",                           // owner display
                              "-----",                                     // folder id
                              "--------------------",                      // folder path
                              "----------",                                // default view
                              "----------",                                // rights
                              "-----",                                     // mountpoint id if mounted
                              "-----",                                     // grantee type
                              "------------------------------------",      // grantee id
                              "---------------",                           // grantee name
                              "---------------");                          // grantee display
        }

        @Override
        public void visit(ShareInfoData shareInfoData) throws ServiceException {
            System.out.printf(mFormat,
                    shareInfoData.getOwnerAcctId(),
                    shareInfoData.getOwnerAcctEmail(),
                    shareInfoData.getOwnerAcctDisplayName(),
                    String.valueOf(shareInfoData.getFolderId()),
                    shareInfoData.getFolderPath(),
                    shareInfoData.getFolderDefaultView(),
                    shareInfoData.getRights(),
                    shareInfoData.getMountpointId_zmprov_only(),
                    shareInfoData.getGranteeType(),
                    shareInfoData.getGranteeId(),
                    shareInfoData.getGranteeName(),
                    shareInfoData.getGranteeDisplayName());
        }
    };
}



