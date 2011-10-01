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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.ldap.ILdapContext;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.ldap.LdapException.LdapEntryNotFoundException;
import com.zimbra.cs.ldap.LdapException.LdapMultipleEntriesMatchedException;

public abstract class LdapHelper {
    
    private LdapProv ldapProv;
    
    protected LdapHelper(LdapProv ldapProv) {
        this.ldapProv = ldapProv;
    }
    
    protected LdapProv getProv() {
        return ldapProv;
    }
    
    /**
     * === IMPORTANT:  caller is responsible to get and close the ILdapContext. ===
     * 
     * @param ldapContext
     * @param searchOptions
     * @throws ServiceException
     */
    public abstract void searchLdap(ILdapContext ldapContext, SearchLdapOptions searchOptions) 
    throws ServiceException;
    
    public abstract void deleteEntry(String dn, LdapUsage ldapUsage) throws ServiceException;
    
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
    public abstract void modifyAttrs(ZLdapContext zlc, String dn, 
            Map<String, ? extends Object> attrs, Entry entry) 
    throws ServiceException;
    
    /**
     * like modifyAttrs, but ZLdapContext cannot be specified.
     * Implementation should always do the modification against 
     * LDAP master. 
     * 
     * @param dn
     * @param attrs
     * @param entry
     * @throws ServiceException
     */
    public abstract void modifyEntry(String dn, Map<String, ? extends Object> attrs, 
            Entry entry, LdapUsage ldapUsage) 
    throws ServiceException;
    
    /**
     * Modify the entry only if the assertion specified in testFilter is true.
     * @param zlc
     * @param dn
     * @param testFilter
     * @param attrs
     * @param entry
     * @param ldapUsage
     * @returns true if the filter matches the target entry and the entry is 
     *          successfully modified.
     *          false if the filter does not match the target entry
     * @throws ServiceException
     */
    public abstract boolean testAndModifyEntry(ZLdapContext zlc, String dn, ZLdapFilter testFilter,
            Map<String, ? extends Object> attrs,  Entry entry)
    throws ServiceException;
    
    /**
     * Search for an entry by search base and query.
     * At most one entry will be returned from the search.
     * 
     * @param base        search base
     * 
     * @param filter      search filter
     * 
     * @param initZlc     initial ZLdapContext
     *                        - if null, a new one will be created to be used for the search, 
     *                          and then closed
     *                        - if not null, it will be used for the search, this API will 
     *                          *not* close it, it is the responsibility of callsite to close 
     *                          it when it i no longer needed.
     *                          
     * @param useMaster   if initZlc is null, whether to do the search on LDAP master.
     *
     * @return            a ZSearchResultEnumeration is an entry is found
     *                    null if the search does not find any matching entry.
     *                        
     * @throws LdapMultipleEntriesMatchedException  if more than one entries is matched
     * 
     * @throws ServiceException                     all other errors
     */
    public abstract ZSearchResultEntry searchForEntry(String base, ZLdapFilter filter, 
            ZLdapContext initZlc, boolean useMaster, String[] returnAttrs) 
    throws LdapMultipleEntriesMatchedException, ServiceException;
    
    public ZSearchResultEntry searchForEntry(String base, ZLdapFilter filter, 
            ZLdapContext initZlc, boolean useMaster) 
    throws LdapMultipleEntriesMatchedException, ServiceException {
        return searchForEntry(base, filter, initZlc, useMaster, null);
    }
    
    public ZSearchResultEntry searchForEntry(String base, ZLdapFilter filter, 
            ZLdapContext initZlc, String[] returnAttrs) 
    throws LdapMultipleEntriesMatchedException, ServiceException {
        if (initZlc == null) {
            throw ServiceException.FAILURE("internal error", null);
        }
        return searchForEntry(base, filter, initZlc, false, returnAttrs); 
    }
    
    public ZSearchResultEntry searchForEntry(String base, String filter, 
            ZLdapContext initZlc, String[] returnAttrs) 
    throws LdapMultipleEntriesMatchedException, ServiceException {
        ZLdapFilter zFilter = ZLdapFilterFactory.getInstance().fromFilterString(filter);
        return searchForEntry(base, zFilter, initZlc, returnAttrs); 
    }
    
    /**
     * Get all attributes of the LDAP entry at the specified DN.
     * 
     * @param dn
     * @param initZlc
     * @param ldapServerType
     * @param attrs
     * 
     * @return a ZAttributes objects
     *         Note: this API never returns null.  If an entry is not found at the specified 
     *         DN, LdapEntryNotFoundException will be thrown.
     * 
     * @throws LdapEntryNotFoundException  if the entry is not found
     * @throws ServiceException            all other errors
     */
    public abstract ZAttributes getAttributes(ZLdapContext initZlc, 
            LdapServerType ldapServerType, LdapUsage usage,
            String dn, String[] returnAttrs) 
    throws LdapEntryNotFoundException, ServiceException;
    
    public ZAttributes getAttributes(ZLdapContext initZlc, String dn, String[] returnAttrs) 
    throws LdapEntryNotFoundException, ServiceException {
        assert(initZlc != null);
        return getAttributes(initZlc, (LdapServerType) null, (LdapUsage) null, dn, returnAttrs);
    }
    
    public ZAttributes getAttributes(ZLdapContext initZlc, String dn) 
    throws LdapEntryNotFoundException, ServiceException {
        assert(initZlc != null);
        return getAttributes(initZlc, (LdapServerType) null, (LdapUsage) null, dn, (String[]) null);
    }
    
    public ZAttributes getAttributes(LdapUsage usage, String dn) 
    throws LdapEntryNotFoundException, ServiceException {
        return getAttributes((ZLdapContext) null, LdapServerType.REPLICA, usage, 
                dn, (String[]) null);
    }
    
    /**
     * A convenient wrapper for ZldapContext.searchDir.
     * Saves callsites the burden of having to get and close ZldapContext
     * 
     * @param baseDN
     * @param filter
     * @param searchControls
     * @return
     * @throws LdapException
     */
    public abstract ZSearchResultEnumeration searchDir(String baseDN, ZLdapFilter filter, 
            ZSearchControls searchControls, ZLdapContext initZlc, LdapServerType ldapServerType) 
    throws ServiceException;
        
    public ZSearchResultEnumeration searchDir(String baseDN, ZLdapFilter filter, 
            ZSearchControls searchControls) 
    throws ServiceException {
        return searchDir(baseDN, filter, searchControls, null, LdapServerType.REPLICA);
    }
    
}
