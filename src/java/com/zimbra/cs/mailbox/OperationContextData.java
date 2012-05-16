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
package com.zimbra.cs.mailbox;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox.FolderNode;

public abstract class OperationContextData {
    
    protected OperationContext octxt;

    private static final ThreadLocal<GranteeNames> granteeNames = new ThreadLocal<GranteeNames>();

    protected OperationContextData(OperationContext octxt) {
        this.octxt = octxt;
    }
    
    
    /**
     * 
     * bug 35079, 39804: 
     *     avoid potential excessive LDAP searches for grantee names
     *     during XML encoding folder ACLs 
     * 
     * - collect grantee ids on all folder user shares of the mailbox
     * - resolve all ids into names (LDAP search if necessary)
     * - set the ids-to-names map in the OperationContext
     *     
     * 
     * @param octxt
     * @param node
     */
    public static void addGranteeNames(OperationContext octxt, Mailbox.FolderNode node) {
        if (octxt == null || node == null)
            return;
        
        GranteeNames data = getOrInitGranteeNames(octxt);
        data.addRootNode(node);
    }
    
    public static GranteeNames getGranteeNames() {
        return granteeNames.get();
    }
    
    public static void setNeedGranteeName(OperationContext octxt, boolean needGranteeName) {
        if (octxt == null)
            return;
        
        GranteeNames data = getOrInitGranteeNames(octxt);
        data.setNeedGranteeName(needGranteeName);
    }
    
    public static boolean getNeedGranteeName(OperationContext octxt) {
        if (octxt == null) {
            return true;
        }
        
        GranteeNames data = getGranteeNames();
        if (data == null) {
            return true;
        }
        
        return data.needGranteeName();
    }
        
    private static GranteeNames getOrInitGranteeNames(OperationContext octxt) {
        GranteeNames data = getGranteeNames();
        if (data == null) {
            data = new GranteeNames(octxt);
            granteeNames.set(data);
        } 
        return data;
    }
    
    /**
     * 
     * GranteeNames
     *
     */
    public static class GranteeNames extends OperationContextData {
        private static final int USR_GRANTEES = 0;
        private static final int GRP_GRANTEES = 1;
        private static final int COS_GRANTEES = 2;
        private static final int DOM_GRANTEES = 3;
        private static final int NUM_GRANTEE_TYPES = 4;
        
        private boolean encounteredLDAPFailure = false;
        private boolean needGranteeName = true;
        
        private Set<Mailbox.FolderNode> unresolvedRootNodes; // unresolved root nodes
        private Set<Mailbox.FolderNode> resolvedRootNodes;   // resolved root nodes
        
        // id-to-name map
        private Map<String, String>[] idsToNamesMap = new Map[NUM_GRANTEE_TYPES];
       
        GranteeNames(OperationContext octxt) {
            super(octxt);
        }
        
        void setNeedGranteeName(boolean needGranteeName) {
            this.needGranteeName = needGranteeName;
        }
        
        boolean needGranteeName() {
            return needGranteeName;
        }
        
        void addRootNode(Mailbox.FolderNode node) {
            if (unresolvedRootNodes == null)
                unresolvedRootNodes = new HashSet<Mailbox.FolderNode>();
            
            /*
             * We resolve the hierarchy lazily.  
             * When a root node is added, it is put in the unresolved set.
             * When a root node is resolved, move it to the resolved set.
             */
            
            boolean alreadyResolved = false;
            if (resolvedRootNodes != null) {
                for (Mailbox.FolderNode resolvedNode : resolvedRootNodes) {
                    if (resolvedNode.mId == node.mId) {
                        alreadyResolved = true;
                        break;
                    }
                }
            }
            
            // root node already added but not yet resolved
            boolean alreadyAdded = false;
            for (Mailbox.FolderNode unresolvedNode : unresolvedRootNodes) {
                if (unresolvedNode.mId == node.mId) {
                    alreadyAdded = true;
                    break;
                }
            }
            
            // add it to the unresolved set if it had not been added before
            if (!alreadyResolved && !alreadyAdded) {
                unresolvedRootNodes.add(node);
            }
        }
        
        private void resolveIfNecessary() {
            if (unresolvedRootNodes == null || unresolvedRootNodes.isEmpty())
                return;
            
            for (Mailbox.FolderNode unresolvedNode : unresolvedRootNodes) {
                // get all grantees of this folder and all sub-folders
                Set[] idHolders = new Set[NUM_GRANTEE_TYPES];
                collectGranteeIds(unresolvedNode, idHolders);
                    
                // minus the ids already in our map
                for (int bucket = 0; bucket < NUM_GRANTEE_TYPES; bucket++) {
                    if (idHolders[bucket] != null && idsToNamesMap[bucket] != null) {
                        idHolders[bucket] = SetUtil.subtract(idHolders[bucket], idsToNamesMap[bucket].keySet());
                    }
                }
                populateIdToNameMaps(idHolders);
            }
            
            // move nodes to resolved set
            if (resolvedRootNodes == null) {
                resolvedRootNodes = new HashSet<Mailbox.FolderNode>();
            }
            resolvedRootNodes.addAll(unresolvedRootNodes);
            unresolvedRootNodes.clear();
        }
        
        private void populateIdToNameMaps(Set<String>[] idHolders) {
            Map<String, String> result = null;
            
            for (int bucket = 0; bucket < NUM_GRANTEE_TYPES; bucket++) {
                if (idHolders[bucket] == null || idHolders[bucket].isEmpty())
                    continue;
                
                try {
                    Provisioning.EntryType entryType = null;
                    if (bucket == USR_GRANTEES)
                        entryType = Provisioning.EntryType.account;
                    else if (bucket == GRP_GRANTEES)
                        entryType = Provisioning.EntryType.group;
                    else if (bucket == COS_GRANTEES)
                        entryType = Provisioning.EntryType.cos;
                    else if (bucket == DOM_GRANTEES)
                        entryType = Provisioning.EntryType.domain;
                    
                    if (entryType != null)  // should not
                        result = Provisioning.getInstance().getNamesForIds(idHolders[bucket], entryType);
                } catch (ServiceException e) {
                    // log a warning, return an empty map, and let the flow continue
                    ZimbraLog.mailbox.warn("cannot lookup user grantee names", e);
                    encounteredLDAPFailure = true; // so that we don't mark grants invalid
                }
                
                if (result != null) {
                    if (idsToNamesMap[bucket] == null)
                        idsToNamesMap[bucket] = result;
                    else
                        idsToNamesMap[bucket].putAll(result);
                }
            }
        }
        
        private void collectGranteeIds(FolderNode node, Set<String>[] idHolders) {
            if (node.mFolder != null) {
                ACL acl = node.mFolder.getEffectiveACL();
                collectGranteeIdsOnACL(acl, idHolders);
            }
            
            for (FolderNode subNode : node.mSubfolders)
                collectGranteeIds(subNode, idHolders);
        }
        
        int getGranteeBucket(byte granteeType) {
            switch (granteeType) {
            case ACL.GRANTEE_USER:
                return USR_GRANTEES;
            case ACL.GRANTEE_GROUP:
                return GRP_GRANTEES;
            case ACL.GRANTEE_COS:
                return COS_GRANTEES;
            case ACL.GRANTEE_DOMAIN:
                return DOM_GRANTEES;
            default:
                return -1;
            }
        }
        
        private void collectGranteeIdsOnACL(ACL acl, Set<String>[] idHolders) {
            if (acl != null) {
                for (ACL.Grant grant : acl.getGrants()) {
                    int idx = getGranteeBucket(grant.getGranteeType());
                    if (idx != -1) {
                        if (idHolders[idx] == null) {
                            idHolders[idx] = new HashSet<String>();
                        }
                        idHolders[idx].add(grant.getGranteeId());    
                    }
                }
            }
        }
        
        public static final String EMPTY_NAME = "";
        public static final String INVALID_GRANT = new String("***INVALID***");
        
        public String getNameById(String id, byte granteeType) {
            resolveIfNecessary();
            
            int idx = getGranteeBucket(granteeType);
            if (idx != -1) {
                // it's one of the grantee types we are responsible for (usr, grp, cos, dom)
                // idsToNamesMap[idx] should not be null, but if for whatever reason
                // (some callsite missed calling us to populate?),
                // return null and let caller to look it up.
                if (idsToNamesMap[idx] == null) {
                    return null;
                } else {
                    String name = idsToNamesMap[idx].get(id);
                    // We've searched but didn't find the id, the grantee might have been deleted,
                    // return empty string so caller won't try to search for it again (bug 39804).
                    if (name == null) {
                        // - if encountered temporary LDAP failures when we searched, the grantee
                        //   probably still exists, return empty string.
                        // - if no LDAP failure was encountered, but the name is not found, return 
                        //   a "hint" that this grant could be invalid.  Note, it is only a hint because 
                        //   glitches like an not synced LDAP replica can return us "not found" without 
                        //   throwing a NamingException, which is caught and returned from our LDAP code 
                        //   as a ServiceException.  
                        // See http://bugzilla.zimbra.com/show_bug.cgi?id=39806#c4 
                        if (encounteredLDAPFailure) {
                            return EMPTY_NAME;
                        } else {
                            return INVALID_GRANT;
                        }
                    } else
                        return name;
                }
            } else
                return null;
        }

    }

}
