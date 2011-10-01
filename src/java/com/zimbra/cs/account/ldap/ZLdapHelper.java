/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.account.ldap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.ldap.ILdapContext;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapException.LdapEntryNotFoundException;
import com.zimbra.cs.ldap.LdapException.LdapMultipleEntriesMatchedException;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapTODO.*;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZModificationList;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.ldap.ZSearchScope;

/**
 * An SDK-neutral LdapHelper.  
 * Based on Z* classes and LdapUtil in the com.zimbra.cs.ldap package.
 * 
 * @author pshao
 *
 */
public class ZLdapHelper extends LdapHelper {
    
    ZLdapHelper(LdapProv ldapProv) {
        super(ldapProv);
    }

    @Override
    public void searchLdap(ILdapContext ldapContext, SearchLdapOptions searchOptions) 
    throws ServiceException {
        
        ZLdapContext zlc = LdapClient.toZLdapContext(getProv(), ldapContext);
        zlc.searchPaged(searchOptions);
    }

    @Override
    public void deleteEntry(String dn, LdapUsage ldapUsage) throws ServiceException {
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, ldapUsage);
            zlc.deleteEntry(dn);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    private ZModificationList getModList(ZLdapContext zlc, String dn, 
            Map<String, ? extends Object> attrs, Entry entry) throws ServiceException {
        ZModificationList modList = zlc.createModificationList();
        
        AttributeManager attrMgr = AttributeManager.getInst();
        
        for (Map.Entry<String, ? extends Object> attr : attrs.entrySet()) {    
            Object v= attr.getValue();
            String key = attr.getKey();
            boolean doAdd = key.charAt(0) == '+';
            boolean doRemove = key.charAt(0) == '-';
            
            if (doAdd || doRemove) {
                // make sure there aren't other changes without +/- going on at the same time 
                key = key.substring(1);
                if (attrs.containsKey(key)) 
                    throw ServiceException.INVALID_REQUEST("can't mix +attrName/-attrName with attrName", null);
            }
             
            boolean containsBinaryData = attrMgr == null ? false : attrMgr.containsBinaryData(key);
            boolean isBinaryTransfer = attrMgr == null ? false : attrMgr.isBinaryTransfer(key);
            
            // Convert array to List so it can be treated as a Collection
            if (v instanceof Object[]) {
                // Note: Object[] cast is required, so that asList() knows to create a List
                // that contains the contents of the object array, as opposed to a List with one
                // element, which is the entire Object[].  Ick.
                v = Arrays.asList((Object[]) v);
            }
            
            if (v instanceof Collection) {
                Collection c = (Collection) v;
                if (c.size() == 0) {
                    // make sure it exists
                    if (entry.getAttr(key, false) != null) {
                        modList.removeAttr(key, isBinaryTransfer);
                    }
                } else {
                    // Convert values Collection to a String array
                    String[] sa = new String[c.size()];
                    int i = 0;
                    for (Object o : c) {
                        sa[i++] = (o == null ? null : o.toString());
                    }
                    
                    // Add attrs
                    if (doAdd) {
                        modList.addAttr(key, sa, entry, containsBinaryData, isBinaryTransfer);
                    } else if (doRemove) {
                        modList.removeAttr(key, sa, entry, containsBinaryData, isBinaryTransfer);
                    } else {
                        modList.modifyAttr(key, sa, containsBinaryData, isBinaryTransfer);
                    }
                }
            } else if (v instanceof Map) {
                throw ServiceException.FAILURE("Map is not a supported value type", null);
            } else {
                String s = (v == null ? null : v.toString());
                if (doAdd) {
                    modList.addAttr(key, s, entry, containsBinaryData, isBinaryTransfer);
                }
                else if (doRemove) {
                    modList.removeAttr(key, s, entry, containsBinaryData, isBinaryTransfer);
                }
                else {
                    modList.modifyAttr(key, s, entry, containsBinaryData, isBinaryTransfer);
                }
            }
        }
        
        return modList;
    }
    
    /**
     * Modifies the specified entry.  <code>attrs</code> is a <code>Map</code> consisting of
     * keys that are <code>String</code>s, and values that are either
     * <ul>
     *   <li><code>null</code>, in which case the attr is removed</li>
     *   <li>a single <code>Object</code>, in which case the attr is modified
     *     based on the object's <code>toString()</code> value</li>
     *   <li>an <code>Object</code> array or <code>Collection</code>,
     *     in which case a multi-valued attr is updated</li>
     * </ul>
     */
    public void modifyAttrs(ZLdapContext zlc, String dn, Map<String, ? extends Object> attrs, Entry entry) 
    throws ServiceException {
        ZModificationList modList = getModList(zlc, dn, attrs, entry);
        
        if (!modList.isEmpty()) {
            zlc.modifyAttributes(dn, modList);
        }
    }

    @Override
    public void modifyEntry(String dn, Map<String, ? extends Object> attrs, 
            Entry entry, LdapUsage ldapUsage)
    throws ServiceException {
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, ldapUsage);
            modifyAttrs(zlc, dn, attrs, entry);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    @Override
    public boolean testAndModifyEntry(ZLdapContext zlc, String dn,
            ZLdapFilter testFilter, Map<String, ? extends Object> attrs,
            Entry entry) throws ServiceException {
        ZModificationList modList = getModList(zlc, dn, attrs, entry);
        return zlc.testAndModifyAttributes(dn, modList, testFilter);
    }
    

    @Override
    @TODOEXCEPTIONMAPPING
    public ZSearchResultEntry searchForEntry(String base, ZLdapFilter filter, ZLdapContext initZlc, 
            boolean useMaster, String[] returnAttrs) 
    throws LdapMultipleEntriesMatchedException, ServiceException {
        ZLdapContext zlc = initZlc;
        try {
            if (zlc == null) {
                zlc = LdapClient.getContext(LdapServerType.get(useMaster), LdapUsage.SEARCH);
            }
            
            ZSearchControls sc = (returnAttrs == null) ? ZSearchControls.SEARCH_CTLS_SUBTREE() :
                ZSearchControls.createSearchControls(ZSearchScope.SEARCH_SCOPE_SUBTREE,
                        ZSearchControls.SIZE_UNLIMITED, returnAttrs);
            
            ZSearchResultEnumeration ne = zlc.searchDir(base, filter, sc);
            if (ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
                if (ne.hasMore()) {
                    String dups = LdapUtil.formatMultipleMatchedEntries(sr, ne);
                    throw LdapException.MULTIPLE_ENTRIES_MATCHED(base, filter.toFilterString(), dups);
                }
                ne.close();
                return sr;
            }
        /*  all callsites with the following @TODOEXCEPTIONMAPPING pattern can have ease of mind now and remove the 
         * TODOEXCEPTIONMAPPING annotation
         *  
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup account via query: "+query+" message: "+e.getMessage(), e);
        */
        } finally {
            if (initZlc == null)
                LdapClient.closeContext(zlc);
        }
        return null;
    }

    @Override
    public ZAttributes getAttributes(ZLdapContext initZlc, 
            LdapServerType ldapServerType, LdapUsage usage,
            String dn, String[] returnAttrs) 
    throws LdapEntryNotFoundException, ServiceException {
        ZLdapContext zlc = initZlc;
        try {
            if (zlc == null) {
                assert(ldapServerType != null);
                assert(usage != null);
                zlc = LdapClient.getContext(ldapServerType, usage);
            }
            return zlc.getAttributes(dn, returnAttrs);
        /*  all callsites with the following @TODOEXCEPTIONMAPPING pattern can have ease of mind now and remove the 
         * TODOEXCEPTIONMAPPING annotation
         *     
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup COS by name: "+name+" message: "+e.getMessage(), e);
        */    
        } finally {
            if (initZlc == null) {
                LdapClient.closeContext(zlc);
            }
        }
    }

    @Override
    public ZSearchResultEnumeration searchDir(String baseDN, ZLdapFilter filter,
            ZSearchControls searchControls, ZLdapContext initZlc, LdapServerType ldapServerType) 
    throws ServiceException {
        ZLdapContext zlc = initZlc;
        try {
            if (zlc == null) {
                zlc = LdapClient.getContext(ldapServerType, LdapUsage.SEARCH);
            }
            return zlc.searchDir(baseDN, filter, searchControls);
        /*    
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;
        */
        } finally {
            if (initZlc == null) {
                LdapClient.closeContext(zlc);
            }
        }
    }


}
