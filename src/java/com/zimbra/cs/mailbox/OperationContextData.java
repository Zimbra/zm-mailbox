package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Mailbox.FolderNode;

public abstract class OperationContextData {
    
    /**
     * 
     * bug 35079: avoid potential excessive LDAP searches while XML
     *            encoding folder ACLs 
     * 
     * - collect grantee ids on all folder user shares of the mailbox
     * - resolve all ids into names (LDAP search if necessary)
     * - set the ids-to-names map in the OperationContext
     *     
     * 
     * @param octxt
     * @param mbox
     */
    public static void setGranteeNames(OperationContext octxt, Mailbox mbox) {
        GranteeNames data = new GranteeNames(octxt, mbox);
        octxt.SetCtxtData(GranteeNames.getKey(), data);
    }
    
    public static void addGranteeNames(OperationContext octxt, Mailbox.FolderNode node) {
        GranteeNames data = getGranteeNames(octxt);
        if (data == null) {
            data = new GranteeNames(node);
            octxt.SetCtxtData(GranteeNames.getKey(), data);
        } else {
            data.add(node);
        }
    }
    
    public static GranteeNames getGranteeNames(OperationContext octxt) {
        if (octxt == null)
            return null;
        else
            return (GranteeNames)octxt.getCtxtData(GranteeNames.getKey());
    }
    
    
    /**
     * 
     * GranteeNames
     *
     */
    public static class GranteeNames extends OperationContextData {
        private static String getKey() {
            return "GranteeNames";
        }
        
        // id-to-name map
        private Map<String, String> idsToNamesMap;
        
        private GranteeNames(OperationContext octxt, Mailbox mbox) {
            try {
                Set<String> granteeIds = getAllUserGranteeIds(octxt, mbox);
                idsToNamesMap = Provisioning.getInstance().getAccountNamesForIds(granteeIds);
            } catch (ServiceException e) {
                // log a warning, return an empty map, and let the flow continue
                ZimbraLog.mailbox.warn("cannot lookup grantee names", e);
                idsToNamesMap = new HashMap<String, String>();
            }
        }
        
        private GranteeNames(Mailbox.FolderNode node) {
            try {
                Set<String> granteeIds = getAllUserGranteeIds(node);
                idsToNamesMap = Provisioning.getInstance().getAccountNamesForIds(granteeIds);
            } catch (ServiceException e) {
                // log a warning, return an empty map, and let the flow continue
                ZimbraLog.mailbox.warn("cannot lookup grantee names", e);
                idsToNamesMap = new HashMap<String, String>();
            }
        }
        
        private void add(Mailbox.FolderNode node) {
            try {
                // get all grantees of this folder and all sub-folders
                Set<String> granteeIds = getAllUserGranteeIds(node);
                // minus the ids already in our map
                granteeIds = SetUtil.subtract(granteeIds, idsToNamesMap.keySet());
                // all Provisioning/LDAP results to our map 
                idsToNamesMap.putAll(Provisioning.getInstance().getAccountNamesForIds(granteeIds));
            } catch (ServiceException e) {
                // log a warning, don't touch the original map, and let the flow continue
                ZimbraLog.mailbox.warn("cannot lookup grantee names", e);
           }
        }
        
        private Set<String> getAllUserGranteeIds(OperationContext octxt, Mailbox mbox) throws ServiceException {
            Set<String> granteeIds = new HashSet<String>();
            
            List<Folder> folders = mbox.getFolderList(octxt, SortBy.NONE);
            for (Folder folder : folders) {
                ACL acl = folder.getACL();  // no need to getEffectiveACL, since we are going through all folders of the mailbox 
                addUserGranteeIds(acl, granteeIds);
            }
            return granteeIds;
        }
        
        private Set<String> getAllUserGranteeIds(Mailbox.FolderNode node) {
            Set<String> granteeIds = new HashSet<String>();
            getAllUserGranteeIds(node, granteeIds);
            return granteeIds;
        }
        
        private void getAllUserGranteeIds(FolderNode node, Set<String> granteeIds) {
            if (node.mFolder != null) {
                ACL acl = node.mFolder.getEffectiveACL();
                addUserGranteeIds(acl, granteeIds);
            }
            
            for (FolderNode subNode : node.mSubfolders)
                getAllUserGranteeIds(subNode, granteeIds);
        }
        
        private void addUserGranteeIds(ACL acl, Set<String> granteeIds) {
            if (acl != null) {
                for (ACL.Grant grant : acl.getGrants()) {
                    if (grant.getGranteeType() == ACL.GRANTEE_USER)
                        granteeIds.add(grant.getGranteeId());
                }
            }
        }
        
        public String getNameById(String id) {
            return idsToNamesMap.get(id);
        }
    }
}
