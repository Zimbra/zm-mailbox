/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
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
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.AclGroups;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.ACL.Grant;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.mail.GetFolder;
import com.zimbra.cs.service.mail.GetFolder.FolderNode;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;


public class ShareInfo {
    
    private static String S_DELIMITER = ";";
    
    // these two keys are set on our owne Metadata object
    private static final String MD_OWNER_EMAIL = "e";
    private static final String MD_FOLDER_PATH  = "f";
    private static final String MD_FOLDER_DEFAULT_VIEW  = "v";
    
    // note: this key is set on the same Metadata object as the
    //       one set by ACL.  make sure name is not clashed.
    private static final String MD_GRANTEE_NAME = "gn";  
    
    // owner
    private String mOwnerAcctId;
    private String mOwnerAcctEmail;
    
    // folder 
    private int mFolderId;
    private String mFolderPath;
    private byte mFolderDefaultView;
    
    // rights
    private short mRights;
    
    // grantee
    private byte mGranteeType;
    private String mGranteeId;
    private String mGranteeName;
    
    // mountpointid
    // Note: 
    //    only used by zmprov/SoapProvisioning  to construct a 
    //    ShareInfo from SOAP response to pass back just to be displayed
    //  
    //    NOT used by the any of the ShareInfo code on the server side  
    //    On the server side, the mountpoint id is not part of the ShareInfo,
    //    it is a property of the target account, and is encoded in the 
    //    SOAP response by the visitor.
    //    
    private String mMountpointId_zmprov_only;
    private void setMountpointId_zmprov_only(String mptId) {
        mMountpointId_zmprov_only = mptId;
    }
    public String getMountpointId_zmprov_only() {
        if (mMountpointId_zmprov_only == null)
            return "";
        else
            return mMountpointId_zmprov_only;
    }
    ////////////////////////////////////
    
    
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
    
    protected ShareInfo() {
    }
    
    protected void setOwnerAcctId(String ownerAcctId) {
        mOwnerAcctId = ownerAcctId;
    }
    
    public String getOwnerAcctId() {
        return mOwnerAcctId;
    }
    
    protected void setOwnerAcctEmail(String ownerAcctEmail) {
        mOwnerAcctEmail = ownerAcctEmail;
    }

    public String getOwnerAcctEmail() {
        return mOwnerAcctEmail;
    }
    
    protected void setFolderId(int folderId) {
        mFolderId = folderId;
    }
    
    public int getFolderId() {
        return mFolderId;
    }

    protected void setFolderPath(String folderPath) {
        mFolderPath = folderPath;
    }
    
    public String getFolderPath() {
        return mFolderPath;
    }
    
    // returns the leaf folder name
    public String getFolderName() {
        String[] fn = mFolderPath.split("/");
        return fn[fn.length - 1];
    }
            
    protected void setFolderDefaultView(byte folderDefaultView) {
        mFolderDefaultView = folderDefaultView;
    }
    
    public String getFolderDefaultView() {
        return MailItem.getNameForType(mFolderDefaultView);
    }
    
    protected void setRights(short rights) {
        mRights = rights;
    }
    
    public String getRights() {
        return ACL.rightsToString(mRights);
    }
    
    public short getRightsCode() {
        return mRights;
    }
    
    protected void setGranteeType(byte granteeType) {
        mGranteeType = granteeType;
    }
    
    public String getGranteeType() {
        return ACL.typeToString(mGranteeType);
    }
    
    protected void setGranteeId(String granteeId) {
        mGranteeId = granteeId;
    }
    
    public String getGranteeId() {
        return mGranteeId;
    }
    
    protected void setGranteeName(String granteeName) {
        mGranteeName = granteeName;
    }
    
    public String getGranteeName() {
        return mGranteeName;
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
        sb.append(mOwnerAcctId);
        sb.append(S_DELIMITER);
        sb.append(mFolderId);
        
        return sb.toString();
    }
    
    protected void deserialize(String encodedShareInfo) throws ServiceException {

        String[] parts = encodedShareInfo.split(S_DELIMITER);
        if (parts.length != 3) {
            throw ServiceException.FAILURE("malformed share info: " + encodedShareInfo, null);
        }
        
        mOwnerAcctId = parts[0];
        mFolderId = Integer.valueOf(parts[1]);
        
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

    public static ShareInfo fromXML(Element eShare) throws ServiceException {
        ShareInfo si = new ShareInfo();
        
        si.setOwnerAcctId(eShare.getAttribute(AccountConstants.A_OWNER_ID, null));
        si.setOwnerAcctEmail(eShare.getAttribute(AccountConstants.A_OWNER_NAME, null));
        si.setFolderId(Integer.valueOf(eShare.getAttribute(AccountConstants.A_FOLDER_ID)));
        si.setFolderPath(eShare.getAttribute(AccountConstants.A_FOLDER_PATH, null));
        si.setFolderDefaultView(MailItem.getTypeForName(eShare.getAttribute(MailConstants.A_DEFAULT_VIEW, null)));
        si.setRights(ACL.stringToRights(eShare.getAttribute(AccountConstants.A_RIGHTS)));
        si.setGranteeType(ACL.stringToType(eShare.getAttribute(AccountConstants.A_GRANTEE_TYPE)));
        si.setGranteeId(eShare.getAttribute(AccountConstants.A_GRANTEE_ID, null));
        si.setGranteeName(eShare.getAttribute(AccountConstants.A_GRANTEE_NAME, null));
        
        // and this ugly thing
        si.setMountpointId_zmprov_only(eShare.getAttribute(AccountConstants.A_MOUNTPOINT_ID, null));
        
        return si;
    }
    
    public void toXML(Element eShare, Integer mptId) {
        eShare.addAttribute(AccountConstants.A_OWNER_ID,     getOwnerAcctId());
        eShare.addAttribute(AccountConstants.A_OWNER_NAME,   getOwnerAcctEmail());
        eShare.addAttribute(AccountConstants.A_FOLDER_ID,    getFolderId());
        eShare.addAttribute(AccountConstants.A_FOLDER_PATH,  getFolderPath());
        eShare.addAttribute(MailConstants.A_DEFAULT_VIEW,    getFolderDefaultView());
        eShare.addAttribute(AccountConstants.A_RIGHTS,       getRights());
        eShare.addAttribute(AccountConstants.A_GRANTEE_TYPE, getGranteeType());
        eShare.addAttribute(AccountConstants.A_GRANTEE_ID,   getGranteeId());
        eShare.addAttribute(AccountConstants.A_GRANTEE_NAME, getGranteeName());
        
        if (mptId != null)
            eShare.addAttribute(AccountConstants.A_MOUNTPOINT_ID, mptId.toString());
    }
    
    private static Set<Folder> getVisibleFolders(OperationContext octxt, Mailbox mbox) throws ServiceException {
        
        // use the easy one first
        Set<Folder> folders = mbox.getVisibleFolders(octxt);
        
        // if an admin  is doing this, it can see the entire mailbox of the owner, 
        // and mbox.getVisibleFolders will return null
        // in this case get the entire folder tree
        
        if (folders == null) {
            GetFolder.FolderNode root = GetFolder.getFolderTree(octxt, mbox, null, false);
            // flatten it
            folders = flattenAndSortFolderTree(root);
        } 
        return folders;
    }
    
    private static Set<Folder> flattenAndSortFolderTree(GetFolder.FolderNode root) {
        Set<Folder> folders = new HashSet<Folder>();
        flattenAndSortFolderTree(root, folders);
        return folders;
    }
    
    private static void flattenAndSortFolderTree(GetFolder.FolderNode node, Set<Folder> flattened) {
        if (node.mFolder != null)
            flattened.add(node.mFolder);
        for (FolderNode subNode : node.mSubfolders)
            flattenAndSortFolderTree(subNode, flattened);
    }
    
    /*
     * ================================
     *          Visitor interface 
     * ================================
     */
    public static interface Visitor {
        public void visit(ShareInfo shareInfo) throws ServiceException;
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
            
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(acct.getId(), false);
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
                byte granteeType, Account ownerAcct, Visitor visitor) throws ServiceException {

            Mailbox ownerMbox = MailboxManager.getInstance().getMailboxByAccountId(ownerAcct.getId(), false);
            if (ownerMbox == null)
                throw ServiceException.FAILURE("mailbox not found for account " + ownerAcct.getId(), null);
               
            Set<Folder> folders = getVisibleFolders(octxt, ownerMbox);
            for (Folder folder : folders)
                doDiscover(prov, targetAcct, granteeType, ownerAcct, folder, visitor);
        }
        
        private static void doDiscover(Provisioning prov, Account targetAcct,
                byte granteeType, Account ownerAcct, Folder folder, Visitor visitor) 
            throws ServiceException {
            
            ACL acl = folder.getEffectiveACL();
            
            if (acl == null)
                return;
            
            for (ACL.Grant grant : acl.getGrants()) {
                if (grant.matches(targetAcct) && 
                    matchesGranteeType(grant.getGranteeType(), granteeType)) {
                    ShareInfo si = new ShareInfo();
                    
                    si.setOwnerAcctId(ownerAcct.getId());
                    si.setOwnerAcctEmail(ownerAcct.getName());
                    si.setFolderId(folder.getId());
                    si.setFolderPath(folder.getPath());
                    si.setFolderDefaultView(folder.getDefaultView());
                    si.setRights(grant.getGrantedRights());
                    si.setGranteeType(grant.getGranteeType());
                    si.setGranteeId(grant.getGranteeId());
                    si.setGranteeName(granteeName(prov, grant));

                    visitor.visit(si);
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
                NamedEntry publishingOnEntry, Publishing.Action action, Account ownerAcct, 
                Folder folder) throws ServiceException {
            
            if (folder == null) {
                // no folder descriptor, do the entire folder tree
                
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(ownerAcct.getId(), false);
                if (mbox == null)
                    throw ServiceException.FAILURE("mailbox not found for account " + ownerAcct.getId(), null);
                
                Set<Folder> folders = getVisibleFolders(octxt, mbox);
                for (Folder f : folders)
                    doPublish(prov, publishingOnEntry, action, ownerAcct, f);
            } else {
                doPublish(prov, publishingOnEntry, action, ownerAcct, folder);
            }
        }
        
        private static void doPublish(Provisioning prov, 
                NamedEntry publishingOnEntry, Publishing.Action action, Account ownerAcct, 
                Folder folder) throws ServiceException {
            
            ShareInfo.Publishing si = new ShareInfo.Publishing(action, ownerAcct, folder);
            si.discoverGrants(folder, publishingOnEntry);
            if (si.hasGrant())
                si.persist(prov, publishingOnEntry);
        }
        
        public static enum Action {
            add,
            remove;
            
            public static Action fromString(String action) throws ServiceException {
                try {
                    return Action.valueOf(action);
                } catch (IllegalArgumentException e) {
                    throw ServiceException.INVALID_REQUEST("unknown ShareInfo action: " + action, e);
                }
            }
        }
    
        private Action mAction;
        private Publishing(Action action, Account ownerAcct, Folder folder) {
            mAction = action;
            setOwnerAcctId(ownerAcct.getId());
            setOwnerAcctEmail(ownerAcct.getName());
            setFolderId(folder.getId());
        }
            
        public Action getAction() {
            return mAction;
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
                         *     owner name
                         *     folder path
                         *   
                         * we put owner name and folder path in the first item of our metadata list, 
                         * because it is not convenient to get owner name from owner id and get 
                         * folder path from folder id when we need them.
                         *  
                         * cons: if the owner name/folder path is renamed, we will have
                         *       stale metadata.  
                         *
                         * pros: we save excessive LDAP searches for finding the owner 
                         *       name from owner id if the owner is not in cache; and saves 
                         *       proxying to other host just to find the folder from folder id. 
                         */
                        Metadata md = new Metadata();
                        md.put(MD_OWNER_EMAIL, getOwnerAcctEmail());
                        
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
                    
                    mGrants.add(metadata);
                }
            }
        }

        
        private boolean matches(ACL.Grant grant, NamedEntry publishingOnEntry) throws ServiceException {
            String granteeId = grant.getGranteeId();
            byte granteeType = grant.getGranteeType();
            
            if (publishingOnEntry instanceof DistributionList) {
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
            if (getAction() == Publishing.Action.add) {
                attrs.put(addKey, value);
            }
                
            /*
             * if adding, replace existing share info for the same owner:folder
             * if removing, delete all(there should be only one) share info for the same owner:folder
             * 
             * for both case, we remove any value that starts with the same owner:folder
             * 
             * one caveat: if we are adding an existing owner:folder and the share info have not 
             * changed since last published, we do not want to put a - in the mod map, because 
             * that will remove the value put in above.
             */
            String ownerAndFoler = serializeOwnerAndFolder();
            for (String curSi : curShareInfo) {
                if (curSi.startsWith(ownerAndFoler) && !curSi.equals(value)) {
                    attrs.put(removeKey, curSi);
                }
            }

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

        
        private static List<Published> decodeMetadata(String encoded) throws ServiceException {
            List<Published> siList = new ArrayList<Published>();
            
            // deserialize encoded to a dummy Published first
            Published si = new Published(encoded);
            
            //
            // split the dummy to multiple, see comments on 
            // "It is a list(MetadataList) instead of a single object(Metadata) because ..."
            //
            
            // data not btencoded in metadata
            String ownerAcctId = si.getOwnerAcctId();
            int folderId = si.getFolderId();
            
            // data btencoded in metadata by us (ShareInfo.Publishing)
            Metadata metadata = si.mGrants.getMap(0);
            String ownerAcctEmail = metadata.get(MD_OWNER_EMAIL);
            String folderPath = metadata.get(MD_FOLDER_PATH);
            byte folderDefaultView = (byte)metadata.getLong(MD_FOLDER_DEFAULT_VIEW);
                
            // data encoded by ACL.grant
            for (int i = 1; i < si.mGrants.size(); i++) { 
                metadata = si.mGrants.getMap(i);
                
                Grant grant = new Grant(metadata);
                
                short rights = grant.getGrantedRights();
                byte granteeType = grant.getGranteeType();
                String granteeId = grant.getGranteeId();
                
                // Mailbox.ACL never sets it, get it from our key in the metadata
                String granteeName = metadata.get(MD_GRANTEE_NAME);
                
                Published p = new Published(ownerAcctId, folderId,
                                            ownerAcctEmail, folderPath, folderDefaultView,
                                            rights, granteeType,
                                            granteeId, granteeName);
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
        
        private Published(String ownerAcctId, int folderId,
                String ownerAcctEmail, String folderPath, byte folderDefaultView,
                short rights, byte granteeType,
                String granteeId, String granteeName) {
            
            setOwnerAcctId(ownerAcctId);
            setOwnerAcctEmail(ownerAcctEmail);
            setFolderId(folderId);
            setFolderPath(folderPath);
            setFolderDefaultView(folderDefaultView);
            setRights(rights);
            setGranteeType(granteeType);
            setGranteeId(granteeId);
            setGranteeName(granteeName);
            
            mDigest = getOwnerAcctEmail() +
                      getFolderPath() +
                      getRightsCode() + 
                      getGranteeType() +
                      getGranteeId() +
                      getGranteeName();
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
        public static void get(Provisioning prov, Account acct, byte granteeType, Account owner, Visitor visitor) 
            throws ServiceException {
            
            Set<String> visited = new HashSet<String>();
    
            if (granteeType == 0) {
                // no grantee type specified, return all published shares
                
                // only group shares can be published for now, so just 
                // retrieve the group shares
                AclGroups aclGroups = prov.getAclGroups(acct, false); 
                getSharesGrantedToGroups(prov, visitor, aclGroups, owner, visited);
                
                /*
                 * if we support publishing cos and domain shares, include them here
                 */
                // cos
                // getSharesGrantedToGroupsCos(...);
                
                // domain
                // getSharesGrantedToDomain(...);
                
            } else if (granteeType == ACL.GRANTEE_USER) {
                // cannot publish on account, be tolerant just return instead of throw
                
            } else if (granteeType == ACL.GRANTEE_GROUP) {
                AclGroups aclGroups = prov.getAclGroups(acct, false); 
                getSharesGrantedToGroups(prov, visitor, aclGroups, owner, visited);
                
            } else {
                throw ServiceException.INVALID_REQUEST("unsupported grantee type", null);
            }
        }
        
        // for admin only, like the above, also called for sending emails 
        public static void get(Provisioning prov, DistributionList dl, Account owner, Visitor visitor) 
            throws ServiceException {
            
            Set<String> visited = new HashSet<String>();

            // get shares published on the dl 
            getShares(visitor, dl, owner, visited);
                
            // call prov.getAclGroups instead of prov.getDistributionLists
            // because getAclGroups returns cached data, while getDistributionLists
            // does LDAP searches each time
                
            // get shares published on parents of this dl
            if (!dl.isAclGroup())
                dl = prov.getAclGroup(DistributionListBy.id, dl.getId());
            AclGroups aclGroups = prov.getAclGroups(dl); 
            getSharesGrantedToGroups(prov, visitor, aclGroups, owner, visited);
        }

        private static void getSharesGrantedToGroups(Provisioning prov, Visitor visitor, 
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
                getShares(visitor, group, owner, visited);
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
        private static void getShares(Visitor visitor, NamedEntry entry, Account owner, Set<String> visited) {
            Set<String> publishedShareInfo = entry.getMultiAttrSet(Provisioning.A_zimbraShareInfo);
            
            for (String psi : publishedShareInfo) {
                
                try {
                    // each zimbraShareInfo value can expand to *multiple* Published share info, because
                    // for each owner:folder, there could be multiple matched grantees
                    // e.g. 
                    //    - group dl2 is a member of group dl1
                    //    - owner shares /inbox to dl2 for rw rights 
                    //    - owner shares /inbox to dl1 for aid rights
                    //
                    List<Published> siList = decodeMetadata(psi);
                        
                    for (Published si : siList) {    
                        if (owner != null) {
                            if (!owner.getId().equals(si.getOwnerAcctId()))
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
                        
                        visitor.visit(si);
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
        
        private static class MailSenderVisitor implements ShareInfo.Visitor {
            
            List<ShareInfo> mShares = new ArrayList<ShareInfo>();
            
            public void visit(ShareInfo shareInfo) throws ServiceException {
                mShares.add(shareInfo);
            }
            
            private int getNumShareInfo() {
                return mShares.size();
            }
            
            private void appendCommaSeparated(StringBuffer sb, String s) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(s);
            }
            
            private String getRightsText(ShareInfo si, Locale locale) {
                short rights = si.getRightsCode();
                StringBuffer r = new StringBuffer();
                if ((rights & ACL.RIGHT_READ) != 0)      appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareInfoActionRead, locale));
                if ((rights & ACL.RIGHT_WRITE) != 0)     appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareInfoActionWrite, locale));
                if ((rights & ACL.RIGHT_INSERT) != 0)    appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareInfoActionInsert, locale));
                if ((rights & ACL.RIGHT_DELETE) != 0)    appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareInfoActionDelete, locale));
                if ((rights & ACL.RIGHT_ACTION) != 0)    appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareInfoActionAction, locale));
                if ((rights & ACL.RIGHT_ADMIN) != 0)     appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareInfoActionAdmin, locale));
                if ((rights & ACL.RIGHT_PRIVATE) != 0)   appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareInfoActionPrivate, locale));
                if ((rights & ACL.RIGHT_FREEBUSY) != 0)  appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareInfoActionFreebusy, locale));
                if ((rights & ACL.RIGHT_SUBFOLDER) != 0) appendCommaSeparated(r, L10nUtil.getMessage(MsgKey.shareInfoActionCreateFolder, locale));
              
                return r.toString();
            }
            
            private String formatFolderDesc(Locale locale, ShareInfo si) {
                return "(" + L10nUtil.getMessage(MsgKey.shareInfoBodyFolder, locale, si.getFolderDefaultView()) + ")";
            }
            
            private String formatTextShareInfo(MsgKey key, String value, Locale locale, String extra) {
                return L10nUtil.getMessage(key, locale) + ": " + value + (extra==null?"":" "+extra) + "\n";
            }
            
            private void formatTextShare(StringBuilder sb, Locale locale, ShareInfo si) {
                sb.append(formatTextShareInfo(MsgKey.shareInfoBodySharedItem, si.getFolderName(), locale, formatFolderDesc(locale, si)));
                sb.append(formatTextShareInfo(MsgKey.shareInfoBodyOwner, si.getOwnerAcctEmail(), locale, null));
                sb.append(formatTextShareInfo(MsgKey.shareInfoBodyGrantee, si.getGranteeName(), locale, null));
                sb.append(formatTextShareInfo(MsgKey.shareInfoBodyAllowedActions, getRightsText(si, locale), locale, null));
                sb.append("\n");
            }
            
            private String formatText(String dlName, Locale locale, Integer idx) {
                StringBuilder sb = new StringBuilder();
                
                sb.append("\n");
                sb.append(L10nUtil.getMessage(MsgKey.shareInfoBodyAddedToGroup, locale, dlName));
                sb.append("\n\n");
                sb.append(L10nUtil.getMessage(MsgKey.shareInfoBodyIntroduction, locale, dlName));
                sb.append("\n\n");
                
                if (idx == null) {
                    for (ShareInfo si : mShares) {
                        formatTextShare(sb, locale, si);
                    }
                } else 
                    formatTextShare(sb, locale, mShares.get(idx.intValue()));
                
                sb.append("\n\n");
                return sb.toString();
            }
            
            private String formatHtmlShareInfo(MsgKey key, String value, Locale locale, String extra) {
                return "<tr>" +
                       "<th align=\"left\">" + L10nUtil.getMessage(key, locale) + ":" + "</th>" +
                       "<td align=\"left\">" + value + (extra==null?"":extra) + "</td>" +
                       "</tr>\n";
            }
            
            private void formatHtmlShare(StringBuilder sb, Locale locale, ShareInfo si) {
                sb.append("<p>\n");
                sb.append("<table border=\"0\">\n");
                sb.append(formatHtmlShareInfo(MsgKey.shareInfoBodySharedItem, si.getFolderName(), locale, formatFolderDesc(locale, si)));
                sb.append(formatHtmlShareInfo(MsgKey.shareInfoBodyOwner, si.getOwnerAcctEmail(), locale, null));
                sb.append(formatHtmlShareInfo(MsgKey.shareInfoBodyGrantee, si.getGranteeName(), locale, null));
                sb.append(formatHtmlShareInfo(MsgKey.shareInfoBodyAllowedActions, getRightsText(si, locale), locale, null));
                sb.append("</table>\n");
                sb.append("</p>\n");
            }
            
            private String formatHtml(String dlName, Locale locale, Integer idx) {
                StringBuilder sb = new StringBuilder();
                
                sb.append("<h4>\n");
                sb.append("<p>" + L10nUtil.getMessage(MsgKey.shareInfoBodyAddedToGroup, locale, dlName) + "</p>\n");
                sb.append("<p>" + L10nUtil.getMessage(MsgKey.shareInfoBodyIntroduction, locale, dlName) + "</p>\n");
                sb.append("</h4>\n");
                sb.append("\n");
                
                if (idx == null) {
                    for (ShareInfo si : mShares) {
                        formatHtmlShare(sb, locale, si);
                    }
                } else
                    formatHtmlShare(sb, locale, mShares.get(idx.intValue()));
                    
                return sb.toString();
            }
            
            /*
                <share xmlns="urn:zimbraShare" version="0.1" action="new">
                    <grantee id="569e4a9d-752f-4083-a43b-469e89e468bd" email="engineering@example.com" name="engineering"/>
                    <grantor id="16e4f67c-fc2f-405b-bba5-929f9964a62c" email="user1@example.com" name="Demo User One"/>
                    <link id="2" name="Inbox" view="message" perm="r"/>
                    <notes></notes>
                </share>
                
            */
            private void formatXmlShare(StringBuilder sb, Locale locale, ShareInfo si) {
                
                /* 
                 * from ZimbraWebClient/WebRoot/js/zimbraMail/share/model/ZmShare.js
 
                   ZmShare.URI = "urn:zimbraShare";
                   ZmShare.VERSION = "0.1";
                   ZmShare.NEW     = "new";
                */
                final String URI = "urn:zimbraShare";
                final String VERSION = "0.1";
                
                sb.append("<share xmlns=\"" + URI + "\" version=\"" + VERSION + "\" action=\"new\">\n");
                sb.append("  <grantee id=\"" + si.getGranteeId() + "\" email=\"" + si.getGranteeName() + "\" name=\"" + si.getGranteeName() +"\"/>\n");
                sb.append("  <grantor id=\"" + si.getOwnerAcctId() + "\" email=\"" + si.getOwnerAcctEmail() + "\" name=\"" + si.getOwnerAcctEmail() +"\"/>\n");
                sb.append("  <link id=\"" + si.getFolderId() + "\" name=\"" + si.getFolderName() + "\" view=\"" + si.getFolderDefaultView() + "\" perm=\"" + ACL.rightsToString(si.getRightsCode()) + "\"/>\n");
                sb.append("  <notes></notes>\n");
                sb.append("</share>\n");
            }
            
            private String formatXml(String dlName, Locale locale, Integer idx) {
                StringBuilder sb = new StringBuilder();
                
                 if (idx == null) {
                    for (ShareInfo si : mShares) {
                        formatXmlShare(sb, locale, si);
                    }
                } else
                    formatXmlShare(sb, locale, mShares.get(idx.intValue()));
                    
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
                Published.get(prov, dl, null, visitor);
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
                    addr = new InternetAddress(dlssmfa);
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
                        replyToAddr = new InternetAddress(replyTo);
                    return new Pair<Address, Address>(addr, replyToAddr);
                } catch (AddressException e) {
                }
            }

            // 3. otherwise use the DL's address.
            addr = new InternetAddress(dl.getName());
            return new Pair<Address, Address>(addr, addr);
        }
        
        private static MimeMultipart buildMailContent(DistributionList dl, MailSenderVisitor visitor, Locale locale, Integer idx) 
            throws MessagingException {
            
            String shareInfoText = visitor.formatText(dl.getName(), locale, idx);
            String shareInfoHtml = visitor.formatHtml(dl.getName(), locale, idx);
            String shareInfoXml = null;
            if (idx != null)
                shareInfoXml = visitor.formatXml(dl.getName(), locale, idx);
            
            // Body
            MimeMultipart mmp = new MimeMultipart("alternative");  // todo, verify alternative?

            // ///////
            // TEXT part (add me first!)
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(shareInfoText, Mime.P_CHARSET_UTF8);
            mmp.addBodyPart(textPart);

            // ///////
            // HTML part 
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setDataHandler(new DataHandler(new HtmlPartDataSource(shareInfoHtml)));
            mmp.addBodyPart(htmlPart);
            
            // ///////
            // XML part 
            if (shareInfoXml != null) {
                MimeBodyPart xmlPart = new MimeBodyPart();
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
                SMTPMessage out = new SMTPMessage(JMSession.getSession());
            
                Pair<Address, Address> senderAddrs = getFromAndReplyToAddr(prov, fromAcct, dl);
                Address fromAddr = senderAddrs.getFirst();
                Address replyToAddr = senderAddrs.getSecond();
                
                // From
                out.setFrom(fromAddr);
                
                // Reply-To
                out.setReplyTo(new Address[]{replyToAddr});
                
                // To
                out.setRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(toAddr));
                
                // Date
                out.setSentDate(new Date());
                
                // Subject
                Locale locale = getLocale(prov, fromAcct, toAddr);
                String subject = L10nUtil.getMessage(MsgKey.shareInfoSubject, locale);
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

            public InputStream getInputStream() throws IOException {
                synchronized(this) {
                    if (mBuf == null) {
                        ByteArrayOutputStream buf = new ByteArrayOutputStream();
                        OutputStreamWriter wout =
                            new OutputStreamWriter(buf, Mime.P_CHARSET_UTF8);
                        String text = mText;
                        wout.write(text);
                        wout.flush();
                        mBuf = buf.toByteArray();
                    }
                }
                ByteArrayInputStream in = new ByteArrayInputStream(mBuf);
                return in;
            }

            public OutputStream getOutputStream() {
                throw new UnsupportedOperationException();
            }
        }
        
        private static class HtmlPartDataSource extends MimePartDataSource {
            private static final String CONTENT_TYPE =
                Mime.CT_TEXT_HTML + "; " + Mime.P_CHARSET + "=" + Mime.P_CHARSET_UTF8;
            private static final String NAME = "HtmlDataSource";

            HtmlPartDataSource(String text) {
                super(text);
            }
            
            public String getContentType() {
                return CONTENT_TYPE;
            }

            public String getName() {
                return NAME;
            }
        }
        
        private static class XmlPartDataSource extends MimePartDataSource {
            private static final String CONTENT_TYPE =
                Mime.CT_XML_ZIMBRA_SHARE + "; " + Mime.P_CHARSET + "=" + Mime.P_CHARSET_UTF8;
            private static final String NAME = "XmlDataSource";

            XmlPartDataSource(String text) {
                super(text);
            }
            
            public String getContentType() {
                return CONTENT_TYPE;
            }

            public String getName() {
                return NAME;
            }
        }
    }
}



