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
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mailbox.ACL.Grant;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.mail.GetFolder;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.mail.GetFolder.FolderNode;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;


public class ShareInfo {
    
    private static String S_DELIMITER = ";";
    
    // these two keys are set on our owne Metadata object
    private static final String MD_OWNER_EMAIL = "e";
    private static final String MD_FOLDER_PATH  = "f";
    private static final String MD_FOLDER_DEFAULT_VIEW  = "v";
    
    // note: this ket is set on the same Metadata object as 
    //       one set by ACL.  make sure name is not clashed.
    private static final String MD_GRANTEE_NAME = "sign";  
    
    // owner's zimbra id
    private String mOwnerAcctId;
    
    // folder id of shared folder
    private int mFolderId;
    
    //
    // Grants that are applicable to the entry the share info is for.
    //
    // It is a list(MetadataList) instead of a single object(Metadata) because the entry we are 
    // publishing share info for could belong to multiple groups, and each group can have different 
    // rights(e.g. r, w, a, ...) on the folder.  The effective folder rights is union of all grants. 
    // But when we publish share info we probably need to state where is right from, thus we keep 
    // a list of all grants that apply to the entry we are publishing share info for.   In the future 
    // when we support share info from cos/domain/all authed users, they will be added in the list too.
    //
    // e.g. 
    //    - group dl2 is a member of group dl1
    //    - owner shares /inbox to dl2 for rw rights 
    //    - owner shares /inbox to dl1 for aid rights
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
    
    protected void setFolderId(int folderId) {
        mFolderId = folderId;
    }
    
    public int getFolderId() {
        return mFolderId;
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

    
    
    /*
     * ===========================
     *          Publishing
     * ===========================
     */
    public static class Publishing extends ShareInfo {
        
        public static List<ShareInfo.Publishing> publish(Provisioning prov, OperationContext octxt, 
                NamedEntry publishingOnEntry, Publishing.Action action, String ownerAcctId, 
                Folder folder) throws ServiceException {
            
            List<ShareInfo.Publishing> siList = new ArrayList<ShareInfo.Publishing>();
            ShareInfo.Publishing si;
            
            if (folder == null) {
                // no folder descriptor, do the entire folder tree
                
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(ownerAcctId, false);
                if (mbox == null)
                    throw ServiceException.FAILURE("mailbox not found for account " + ownerAcctId, null);
                
                GetFolder.FolderNode root = GetFolder.getFolderTree(octxt, mbox, null, false);
                doPublish(siList, prov, octxt, publishingOnEntry, action, ownerAcctId, root);
            } else {
                si = doPublish(prov, octxt, publishingOnEntry, action, ownerAcctId, folder);
                siList.add(si);
            }
            
            return siList;
        }
        
        private static void doPublish(List<ShareInfo.Publishing> siList, 
                Provisioning prov, OperationContext octxt, 
                NamedEntry publishingOnEntry, Publishing.Action action, String ownerAcctId, 
                GetFolder.FolderNode node) throws ServiceException  {
            
            if (node.mFolder != null) {
                ShareInfo.Publishing si = doPublish(prov, octxt, 
                        publishingOnEntry, action, ownerAcctId, node.mFolder);
                siList.add(si);
            }
            for (FolderNode subNode : node.mSubfolders)
                doPublish(siList, prov, octxt, publishingOnEntry, action, ownerAcctId, subNode);
        }
        
        private static ShareInfo.Publishing doPublish(Provisioning prov, OperationContext octxt, 
                NamedEntry publishingOnEntry, Publishing.Action action, String ownerAcctId, 
                Folder folder) throws ServiceException {
            ShareInfo.Publishing si = new ShareInfo.Publishing(action, ownerAcctId, folder);
            if (si.validateAndDiscoverGrants(octxt, publishingOnEntry, folder))
                si.persist(prov, publishingOnEntry);
            return si;
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
        private Publishing(Action action, String ownerAcctId, Folder folder) {
            mAction = action;
            setOwnerAcctId(ownerAcctId);
            setFolderId(folder.getId());
        }
            
        public Action getAction() {
            return mAction;
        }



        /**
         * validate ownerId and folder, and if all is well discover all grants of the owner/folder that 
         * match (i.e. are applicable to) the publishingOnEntry.
         * 
         * @param octxt operation context carrying the authed admin account credential
         * @param publishingOnEntry
         * @return true if the owner of the folder has any effective grant (note, a grant can be inherited 
         *         from a super folder) that matches the entry we are publishing share info for,
         *         false otherwise. 
         * @throws ServiceException
         */
        public boolean validateAndDiscoverGrants(OperationContext octxt, NamedEntry publishingOnEntry, Folder folder) 
            throws ServiceException {
            
            // validate
            String ownerAcctId = getOwnerAcctId();
            Account ownerAcct = Provisioning.getInstance().get(AccountBy.id, ownerAcctId);
            
            // sanity check, should not have come to here if owner does not exist
            // note: to defned against harvest attak, do not throw NO_SUCH_ACCOUNT here 
            if (ownerAcct == null)
                throw ServiceException.FAILURE("internal error", null); 
            
            String ownerAcctEmail = ownerAcct.getName();
            
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(ownerAcctId, false);
            if (mbox == null)
                throw ServiceException.FAILURE("mailbox not found for account " + ownerAcctId, null);
            
            // done validating, owner OK, folder OK
            // now, discover grants
            discoverGrants(ownerAcctEmail, folder, publishingOnEntry);
            
            return hasGrant();
        }
    
        /**
         * discover all grants on the folder that matches the publishingOnEntry.
         * for each matched grant, add it to the MetadataList 
         * 
         * @param folder
         * @param publishingOnEntry
         * @throws ServiceException
         */
        private void discoverGrants(String ownerAcctEmail, Folder folder, NamedEntry publishingOnEntry) throws ServiceException {
            Provisioning prov = Provisioning.getInstance();
            
            ACL acl = folder.getEffectiveACL();
            
            if (acl == null)
                return;
            
            for (ACL.Grant grant : acl.getGrants()) {
                if (grantMatchesEntry(grant, publishingOnEntry)) {
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
                        md.put(MD_OWNER_EMAIL, ownerAcctEmail);
                        
                        // We record the folder path of the folder we are discovering shars for.
                        // It would be nice if we can know the folder id/path on the folder of 
                        // the grant (matters when the folder arg came to this method is a sub folder
                        // of the folder of the grant), but there is no convenient way to get it.
                        md.put(MD_FOLDER_PATH, folder.getPath());
                        
                        // for the same reason, we encode the default view for the fodler in our metadata
                        md.put(MD_FOLDER_DEFAULT_VIEW, folder.getDefaultView());
                        
                        mGrants.add(md);
                    }
                    
                    Metadata metadata = grant.encode();
                    
                    // yuck, ACL.Grant does *never* set the grantee name even there is an API.
                    // we do our own here
                    metadata.put(MD_GRANTEE_NAME, getGranteeName(prov, grant));
                    
                    mGrants.add(metadata);
                }
            }
        }
        
        /*
         * just a courtesy method to get the grantee name since Mailbox.ACL.Grant does not set 
         * it.  This method is not meant to validate the grant.  If the grantee cannot be found
         * just return empty string.
         */
        private String getGranteeName(Provisioning prov, Grant grant) throws ServiceException {
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
            } else {
                // should never be here 
                throw ServiceException.FAILURE("internale error", null);
            }
            return granteeName;
        }
    
        /**
         * returns if a grant matches the entry we are publishing share info for
         * 
         * a grant matches the publishingOnEntry if one of the conditions meet:
         * 1. the grantee is the publishingOnEntry
         * 2. the grantee is a group the publishingOnEntry is a member of
         * 
         * We do *not* support cos/domain/all authed users grantees for now.
         * 
         * @param grant
         * @param publishingOnEntry
         * @return
         * @throws ServiceException
         */
        private boolean grantMatchesEntry(ACL.Grant grant, NamedEntry publishingOnEntry) 
            throws ServiceException {
            String granteeId = grant.getGranteeId();
            byte granteeType = grant.getGranteeType();
            
            // for now, only user and group grantees are supported
            if (granteeType == ACL.GRANTEE_USER)
                return granteeId.equals(publishingOnEntry.getId());
            else if (granteeType == ACL.GRANTEE_GROUP) {
                if (publishingOnEntry instanceof Account)
                    return Provisioning.getInstance().inDistributionList((Account)publishingOnEntry, granteeId);
                else if (publishingOnEntry instanceof DistributionList) {
                    return (granteeId.equals(publishingOnEntry.getId()) ||
                           Provisioning.getInstance().inDistributionList((DistributionList)publishingOnEntry, granteeId));
                }
            }
            return false;
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
        
        private String mOwnerAcctEmail;
        private String mFolderPath;
        private byte mFolderDefaultView;
        private short mRights;
        private byte mGranteeType;
        private String mGranteeId;
        private String mGranteeName;
        
        private String mDigest; // for deduping
        
        public static interface Visitor {
            public void visit(Published shareInfo) throws ServiceException;
        }
        
        public static Published fromXML(Element eShare) throws ServiceException {
            Published si = new Published();
            
            si.setOwnerAcctId(eShare.getAttribute(AccountConstants.A_OWNER_ID, null));
            si.mOwnerAcctEmail = eShare.getAttribute(AccountConstants.A_OWNER_NAME, null);
            si.setFolderId(Integer.valueOf(eShare.getAttribute(AccountConstants.A_FOLDER_ID)));
            si.mFolderPath = eShare.getAttribute(AccountConstants.A_FOLDER_PATH, null);
            si.mFolderDefaultView = MailItem.getTypeForName(eShare.getAttribute(MailConstants.A_DEFAULT_VIEW, null));
            si.mRights = ACL.stringToRights(eShare.getAttribute(AccountConstants.A_RIGHTS));
            si.mGranteeType = ACL.stringToType(eShare.getAttribute(AccountConstants.A_GRANTEE_TYPE));
            si.mGranteeId = eShare.getAttribute(AccountConstants.A_GRANTEE_ID, null);
            si.mGranteeName = eShare.getAttribute(AccountConstants.A_GRANTEE_NAME, null);
            
            return si;
        }
        
        public void toXML(Element eShare) {
            eShare.addAttribute(AccountConstants.A_OWNER_ID,     getOwnerAcctId());
            eShare.addAttribute(AccountConstants.A_OWNER_NAME,   getOwnerAcctEmail());
            eShare.addAttribute(AccountConstants.A_FOLDER_ID,    getFolderId());
            eShare.addAttribute(AccountConstants.A_FOLDER_PATH,  getFolderPath());
            eShare.addAttribute(MailConstants.A_DEFAULT_VIEW,    getFolderDefaultView());
            eShare.addAttribute(AccountConstants.A_RIGHTS,       getRights());
            eShare.addAttribute(AccountConstants.A_GRANTEE_TYPE, getGranteeType());
            eShare.addAttribute(AccountConstants.A_GRANTEE_ID,   getGranteeId());
            eShare.addAttribute(AccountConstants.A_GRANTEE_NAME, getGranteeName());
        }
        
        private static List<Published> decodeMetadata(String encoded) throws ServiceException {
            List<Published> siList = new ArrayList<Published>();
            
            // deserialize encoded to a dummy Published first
            Published si = new Published(encoded);
            
            //
            // split the dummy to multiple
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
            setFolderId(folderId);
            mOwnerAcctEmail = ownerAcctEmail;
            mFolderPath = folderPath;
            mFolderDefaultView = folderDefaultView;
            mRights = rights;
            mGranteeType = granteeType;
            mGranteeId = granteeId;
            mGranteeName = granteeName;
            
            mDigest = mOwnerAcctEmail +
                      mFolderPath +
                      mRights + 
                      ACL.typeToString(mGranteeType) +
                      mGranteeId +
                      mGranteeName;
        }
        
        public String getOwnerAcctEmail() {
            return mOwnerAcctEmail;
        }
        
        public String getFolderPath() {
            return mFolderPath;
        }
        
        // returns the leave folder name
        public String getFolderName() {
            String[] fn = mFolderPath.split("/");
            return fn[fn.length - 1];
        }
                
        public String getFolderDefaultView() {
            return MailItem.getNameForType(mFolderDefaultView);
        }
        
        public String getRights() {
            return ACL.rightsToString(mRights);
        }
        
        public short getRightsCode() {
            return mRights;
        }
        
        public String getGranteeType() {
            return ACL.typeToString(mGranteeType);
        }
        
        public String getGranteeId() {
            return mGranteeId;
        }
        
        public String getGranteeName() {
            return mGranteeName;
        }

        
        private String getDigest() {
            return mDigest;
        }
        
        public static void get(Account acct, String granteeType, Account owner, Visitor visitor) 
            throws ServiceException {
            
            Set<String> visited = new HashSet<String>();
            
            byte gt;
            if (granteeType == null)
                gt = 0;
            else {
                gt = ACL.stringToType(granteeType);
                if (gt != ACL.GRANTEE_USER && gt != ACL.GRANTEE_GROUP)
                    throw ServiceException.INVALID_REQUEST("unsupported grantee type", null);
            }
            
            Provisioning prov = Provisioning.getInstance();
    
            if (gt == 0) {
                // no grantee type specified, return both folders shared with the 
                // account and all groups this account belongs to.
                getShares(visitor, acct, owner, visited);
                
                // call prov.getAclGroups instead of prov.getDistributionLists to be 
                // consistant with get() for DL
                AclGroups aclGroups = prov.getAclGroups(acct, false); 
                getSharesGrantedToGroups(prov, visitor, aclGroups, owner, visited);
                
            } else if (gt == ACL.GRANTEE_USER) {
                getShares(visitor, acct, owner, visited);
                
            } else if (gt == ACL.GRANTEE_GROUP) {
                AclGroups aclGroups = prov.getAclGroups(acct, false); 
                getSharesGrantedToGroups(prov, visitor, aclGroups, owner, visited);
                
            } else {
                throw ServiceException.INVALID_REQUEST("unsupported grantee type", null);
            }
        }
        
        // for admin only
        public static void get(Account acct, boolean directOnly, Account owner, Visitor visitor) 
            throws ServiceException {
            String granteeType = directOnly?"usr":null;
            get(acct, granteeType, owner, visitor);
        }
        
        // for admin only
        public static void get(DistributionList dl, boolean directOnly, Account owner, Visitor visitor) 
            throws ServiceException {
            
            Set<String> visited = new HashSet<String>();
            
            Provisioning prov = Provisioning.getInstance();

            if (directOnly) {
                getShares(visitor, dl, owner, visited);
                
            } else {
                getShares(visitor, dl, owner, visited);
                
                // call prov.getAclGroups instead of prov.getDistributionLists
                // because getAclGroups returns cached data, while getDistributionLists
                // does LDAP searches each time
                
                if (!dl.isAclGroup())
                    dl = prov.getAclGroup(DistributionListBy.id, dl.getId());
                AclGroups aclGroups = prov.getAclGroups(dl); 
                getSharesGrantedToGroups(prov, visitor, aclGroups, owner, visited);
                
            } 
        }
    
        private static void getSharesGrantedToGroups(Provisioning prov, Visitor visitor, AclGroups aclGroups, Account owner,
                Set<String> visited) 
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
                         * again on a sub group, and again on an account.  We return only 
                         * one instance of all the identical published shares.
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
     *          Published
     * ===========================
     */
    public static class NotificationSender {
        
        private static class MailSenderVisitor implements Published.Visitor {
            
            List<Published> mShares = new ArrayList<Published>();
            
            public void visit(Published shareInfo) throws ServiceException {
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
            
            private String getRightsText(Published si, Locale locale) {
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
            
            private String formatFolderDesc(Locale locale, Published si) {
                return "(" + L10nUtil.getMessage(MsgKey.shareInfoBodyFolder, locale, si.getFolderDefaultView()) + ")";
            }
            
            private String formatTextShareInfo(MsgKey key, String value, Locale locale, String extra) {
                return L10nUtil.getMessage(key, locale) + ": " + value + (extra==null?"":" "+extra) + "\n";
            }
            
            private void formatTextShare(StringBuilder sb, Locale locale, Published si) {
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
                    for (Published si : mShares) {
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
            
            private void formatHtmlShare(StringBuilder sb, Locale locale, Published si) {
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
                    for (Published si : mShares) {
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
            private void formatXmlShare(StringBuilder sb, Locale locale, Published si) {
                
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
                    for (Published si : mShares) {
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
                Published.get(dl, false, null, visitor);
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



