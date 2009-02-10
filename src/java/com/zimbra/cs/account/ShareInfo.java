package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.AclGroups;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mailbox.ACL.Grant;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;


public class ShareInfo {
    
    private static String S_DELIMITER = ";";
    
    private static final String MD_OWNER_NAME   = "n";
    private static final String MD_FOLDER_PATH  = "f";
    private static final String MD_GRANTEE_NAME = "g";
    
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
        private String mFolderIdOrPath; // folder id or path as input by soap/zmprov
        private Boolean mIsFolderId;    // if mFolderIdOrPath is a folder id or path as input by soap/zmprov, null means unknown yet
        public Publishing(Action action, String ownerAcctId, String folderIdOrPath, Boolean isFolderId) {
            setOwnerAcctId(ownerAcctId);
            
            mAction = action;
            mFolderIdOrPath = folderIdOrPath;
            mIsFolderId = isFolderId;
        }
            
        public Action getAction() {
            return mAction;
        }
            
        public String getFolderIdOrPath() {
            return mFolderIdOrPath;
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
        public boolean validateAndDiscoverGrants(OperationContext octxt, NamedEntry publishingOnEntry) 
            throws ServiceException {
            
            // validate
            String ownerAcctId = getOwnerAcctId();
            Account ownerAcct = Provisioning.getInstance().get(AccountBy.id, ownerAcctId);
            
            // sanity check, should not have come to here if owner does not exist
            // note: to defned against harvest attak, do not throw NO_SUCH_ACCOUNT here 
            if (ownerAcct == null)
                throw ServiceException.FAILURE("internal error", null); 
            
            String ownerAcctName = ownerAcct.getName();
            
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(ownerAcctId, false);
            if (mbox == null)
                throw ServiceException.FAILURE("mailbox not found for account " + ownerAcctId, null);
            
            Folder folder = getFolder(octxt, mbox);
            setFolderId(folder.getId());
            
            // done validating, owner OK, folder OK
            // now, discover grants
            discoverGrants(ownerAcctName, folder, publishingOnEntry);
            
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
        private void discoverGrants(String ownerAcctName, Folder folder, NamedEntry publishingOnEntry) throws ServiceException {
            Provisioning prov = Provisioning.getInstance();
            
            ACL acl = folder.getEffectiveACL();
            
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
                        md.put(MD_OWNER_NAME, ownerAcctName);
                        
                        // We record the folder path of the folder we are discovering shars for.
                        // It would be nice if we can know the folder id/path on the folder of 
                        // the grant (matters when the folder arg came to this method is a sub folder
                        // of the folder of the grant), but there is no convenient way to get it.
                        md.put(MD_FOLDER_PATH, folder.getPath());
                        
                        // yuck, ACL.Grant does *never* set the grantee name even there is an API.
                        // we do our own here
                        md.put(MD_GRANTEE_NAME, getGranteeName(prov, grant));
                        mGrants.add(md);
                    }
                    Metadata metadata = grant.encode();
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
    
        // Folder returned is guaranteed to be not null
        private Folder getFolder(OperationContext octxt, Mailbox mbox) throws ServiceException {
            
            Folder folder = null;
            if (mIsFolderId == Boolean.TRUE) {
                return getFolderById(octxt, mbox);
            } else if (mIsFolderId == Boolean.FALSE)
                return getFolderByPath(octxt, mbox);
            else {
                // getFolderById is null
                // try to get by path first
                try {
                    return getFolderByPath(octxt, mbox);
                } catch (MailServiceException e) {
                    if (MailServiceException.NO_SUCH_FOLDER.equals(e.getCode())) {
                        // folder not found by path, try getting it by id
                        return getFolderById(octxt, mbox);
                    } else
                        throw e;
                }
            }
        }
    
        private Folder getFolderById(OperationContext octxt, Mailbox mbox) throws ServiceException {
            String folderIdOrPath = getFolderIdOrPath();
            int folderId = Integer.parseInt(folderIdOrPath);
            Folder folder = mbox.getFolderById(octxt, folderId);
            
            if (folder == null)
                throw MailServiceException.NO_SUCH_FOLDER(folderId);
            
            return folder;
        }
        
        private Folder getFolderByPath(OperationContext octxt, Mailbox mbox) throws ServiceException {
            String folderPath = getFolderIdOrPath();
            Folder folder = mbox.getFolderByPath(octxt, folderPath);
            
            if (folder == null)
                throw MailServiceException.NO_SUCH_FOLDER(folderPath);
            
            return folder;
        }
    
        /**
         * persists shareInfo in LDAP on the publishingOnEntry entry
         * 
         * @param prov
         * @param publishingOnEntry
         * @param shareInfo
         * @throws ServiceException
         */
        public static void persist(Provisioning prov, NamedEntry publishingOnEntry, List<Publishing> shareInfo) 
            throws ServiceException {
            
            Set<String> curShareInfo = publishingOnEntry.getMultiAttrSet(Provisioning.A_zimbraShareInfo);
            String addKey    = "+" + Provisioning.A_zimbraShareInfo;
            String removeKey = "-" + Provisioning.A_zimbraShareInfo;
            
            Map<String, Object> attrs = new HashMap<String, Object>();
            for (Publishing si : shareInfo) {
                if (si.getAction() == Publishing.Action.add) {
                    String value = si.serialize();
                    attrs.put(addKey, value);
                }
                
                /*
                 * if adding, replace existing share info for the same owner:folder
                 * if removing, delete all(there should be only one) share info for the same owner:folder
                 * 
                 * for both case, we remove any value that start with the same owner:folder
                 */
                String ownerAndFoler = si.serializeOwnerAndFolder();
                for (String curSi : curShareInfo) {
                    if (curSi.startsWith(ownerAndFoler)) {
                        attrs.put(removeKey, curSi);
                    }
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
        
        private String mOwnerAcctName;
        private String mFolderPath;
        private short mRights;
        private byte mGranteeType;
        private String mGranteeId;
        private String mGranteeName;
        
        public static interface Visitor {
            public void visit(Published shareInfo) throws ServiceException;
        }
        
        public static Published fromXML(Element eShare) throws ServiceException {
            Published si = new Published();
            
            si.setOwnerAcctId(eShare.getAttribute(AccountConstants.A_OWNER_ID, null));
            si.mOwnerAcctName = eShare.getAttribute(AccountConstants.A_OWNER_NAME, null);
            si.setFolderId(Integer.valueOf(eShare.getAttribute(AccountConstants.A_FOLDER_ID)));
            si.mFolderPath = eShare.getAttribute(AccountConstants.A_FOLDER_PATH, null);
            si.mRights = ACL.stringToRights(eShare.getAttribute(AccountConstants.A_RIGHTS));
            si.mGranteeType = ACL.stringToType(eShare.getAttribute(AccountConstants.A_GRANTEE_TYPE));
            si.mGranteeId = eShare.getAttribute(AccountConstants.A_GRANTEE_ID, null);
            si.mGranteeName = eShare.getAttribute(AccountConstants.A_GRANTEE_NAME, null);
            
            return si;
        }
        
        public void toXML(Element eShare) {
            eShare.addAttribute(AccountConstants.A_OWNER_ID,     getOwnerAcctId());
            eShare.addAttribute(AccountConstants.A_OWNER_NAME,   getOwnerAcctName());
            eShare.addAttribute(AccountConstants.A_FOLDER_ID,    getFolderId());
            eShare.addAttribute(AccountConstants.A_FOLDER_PATH,  getFolderPath());
            eShare.addAttribute(AccountConstants.A_RIGHTS,       getRights());
            eShare.addAttribute(AccountConstants.A_GRANTEE_TYPE, getGranteeType());
            eShare.addAttribute(AccountConstants.A_GRANTEE_ID,   getGranteeId());
            eShare.addAttribute(AccountConstants.A_GRANTEE_NAME, getGranteeName());
        }
        
        private Published() {
        }
        
        private Published(String encodedShareInfo) throws ServiceException {
            deserialize(encodedShareInfo);
            extractMeatdata();
        }
        
        public String getOwnerAcctName() {
            return mOwnerAcctName;
        }
        
        public String getFolderPath() {
            return mFolderPath;
        }
        
        public String getRights() {
            return ACL.rightsToString(mRights);
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
        private void extractMeatdata() throws ServiceException {
            // the first one is folder path
            Metadata metadata = mGrants.getMap(0);
            mOwnerAcctName = metadata.get(MD_OWNER_NAME);
            mFolderPath = metadata.get(MD_FOLDER_PATH);
            mGranteeName = metadata.get(MD_GRANTEE_NAME);
            
            // then extract our grants
            for (int i = 1; i < mGrants.size(); i++) { 
                metadata = mGrants.getMap(i);
                extractMeatdata(metadata);
            }
        }
        
        private void extractMeatdata(Metadata metadata) throws ServiceException {
            Grant grant = new Grant(metadata);
            
            mRights = grant.getGrantedRights();
            mGranteeType = grant.getGranteeType();
            mGranteeId = grant.getGranteeId();
            
            // Mailbox.ACL never sets it
            // mGranteeName = grant.getGranteeName();
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
                getShares(visitor, dl, owner,visited);
                
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
                    Published si = new Published(psi);
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
                    if (visited.contains(psi))
                        continue;
                    
                    visitor.visit(si);
                    visited.add(psi);
                    
                } catch (ServiceException e) {
                    // probably encountered malformed share info, log and ignore
                    // should remove the value from LDAP?
                    ZimbraLog.account.warn("unable to process share info", e);
                }
            }
            
        }
        
    }
}
