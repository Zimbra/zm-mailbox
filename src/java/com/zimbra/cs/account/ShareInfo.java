package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;


public class ShareInfo {
    
    private static String S_DELIMITER = ":";
    
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
    private String mOwnerAcctId;
    private String mFolderIdOrPath; // folder id or path as input by soap/zmprov
    private Boolean mIsFolderId;    // if mFolderIdOrPath is a folder id or path as input by soap/zmprov, null means unknown yet
    private String mDesc;           // TODO: probably dont 'need this
    
    //
    // following data are filled in the discoverGrants() method
    //
    // It is a list(MetadataList) instead of a single object(Metadata) because the entry we are 
    // publishing share info for could belong to multiple groups, and each group can have different 
    // rights(e.g. r, w, a, ...) on the folder.  The effective folder rights is union of all grants. 
    // But when we publish share info we probably need to state where is right from, thus we keep 
    // a list of all grants that apply to the entry we are publishing share info for.   In the future 
    // when we support share info from cos/domain/all authed users, they will be added in the list too.
    //
    private int mFolderId;     // folder id, set in validateAndDiscoverGrants after the folder with mFolderIdOrPath is validated 
    private MetadataList mMatchedGrants;
    
    public ShareInfo(Action action, String ownerAcctId, String folderIdOrPath, Boolean isFolderId, String desc) {
        mAction = action;
        mOwnerAcctId = ownerAcctId;
        mFolderIdOrPath = folderIdOrPath;
        mIsFolderId = isFolderId;
        mDesc = desc;
        
        mMatchedGrants = null;
    }
        
    public Action getAction() {
        return mAction;
    }
    
    public String getOwnerAcctId() {
        return mOwnerAcctId;
    }
        
    public String getFolderIdOrPath() {
        return mFolderIdOrPath;
    }
        
    public String getDesc() {
        return mDesc;
    }
    
    /**
     * serialize this ShareInfo into String persisted in LDAP
     * 
     * The format is:
     * owner-zimbraId:itemId:btencoded-metadata
     * 
     * @return
     */
    private String serialize() throws ServiceException {
        // callsites should *not* call this if validateAndDiscoverGrants return false.
        if (mMatchedGrants == null)
            throw ServiceException.FAILURE("internal error, no matching grants", null);
        
        StringBuilder sb = new StringBuilder();
        sb.append(serializeOwnerAndFolder());
        sb.append(S_DELIMITER);
        sb.append(mMatchedGrants.toString());
        
        return sb.toString();
    }
    
    private String serializeOwnerAndFolder() throws ServiceException {
        if (mMatchedGrants == null)
            throw ServiceException.FAILURE("internal error, no matching grants", null);
        
        StringBuilder sb = new StringBuilder();
        sb.append(mOwnerAcctId);
        sb.append(S_DELIMITER);
        sb.append(mFolderId);
        
        return sb.toString();
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
    public boolean validateAndDiscoverGrants(OperationContext octxt, NamedEntry publishingOnEntry) throws ServiceException {
        
        // validate
        String ownerAcctId = getOwnerAcctId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(ownerAcctId, false);
        if (mbox == null)
            throw ServiceException.FAILURE("mailbox not found for account " + ownerAcctId, null);
        
        Folder folder = getFolder(octxt, mbox);
        mFolderId = folder.getId();
        
        // done validating, owner OK, folder OK
        // now, discover grants
        discoverGrants(folder, publishingOnEntry);
        
        return (mMatchedGrants != null);
    }
    
    /**
     * discover all grants on the folder that match to the publishingOnEntry.
     * for each matched grant, add it to the MetadataList 
     * 
     * @param folder
     * @param publishingOnEntry
     * @throws ServiceException
     */
    private void discoverGrants(Folder folder, NamedEntry publishingOnEntry) throws ServiceException {
        ACL acl = folder.getEffectiveACL();
        
        for (ACL.Grant grant : acl.getGrants()) {
            if (grantMatchesEntry(grant, publishingOnEntry)) {
                if (mMatchedGrants == null)
                    mMatchedGrants = new MetadataList();
                mMatchedGrants.add(grant.encode());
            }
        }
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
    private boolean grantMatchesEntry(ACL.Grant grant, NamedEntry publishingOnEntry) throws ServiceException {
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
    public static void persist(Provisioning prov, NamedEntry publishingOnEntry, List<ShareInfo> shareInfo) throws ServiceException{
        
        Set<String> curShareInfo = publishingOnEntry.getMultiAttrSet(Provisioning.A_zimbraShareInfo);
        String addKey    = "+" + Provisioning.A_zimbraShareInfo;
        String removeKey = "-" + Provisioning.A_zimbraShareInfo;
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        for (ShareInfo si : shareInfo) {
            if (si.getAction() == ShareInfo.Action.add) {
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
