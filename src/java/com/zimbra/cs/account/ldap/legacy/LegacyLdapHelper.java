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
package com.zimbra.cs.account.ldap.legacy;

import java.util.Map;

import javax.naming.NamingException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.ldap.LdapHelper;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.ldap.ILdapContext;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapException.LdapEntryNotFoundException;
import com.zimbra.cs.ldap.LdapException.LdapMultipleEntriesMatchedException;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.ldap.jndi.JNDISearchScope;

/**
 * An LdapHelper tied to ZimbraLdapContext and the legacy ldapUtil methods.
 * 
 * @author pshao
 *
 */
public class LegacyLdapHelper extends LdapHelper {

    public LegacyLdapHelper(LdapProv ldapProv) {
        super(ldapProv);
    }

    @Override
    public void searchLdap(ILdapContext ldapContext, SearchLdapOptions searchOptions) 
    throws ServiceException {
        LegacyZimbraLdapContext zlc = LdapClient.toLegacyZimbraLdapContext(getProv(), ldapContext);
        LegacyLdapUtil.searchLdap(zlc, 
                searchOptions.getSearchBase(), 
                searchOptions.getFilter().toFilterString(),
                searchOptions.getReturnAttrs(), 
                searchOptions.getBinaryAttrs(), 
                ((JNDISearchScope) searchOptions.getSearchScope()).getNative(),
                searchOptions.getVisitor());
    }
    
    @Override
    public void deleteEntry(String dn, LdapUsage ldapUsage) throws ServiceException {
        LegacyZimbraLdapContext zlc = null;
        try {
            zlc = new LegacyZimbraLdapContext(true);
            zlc.unbindEntry(dn);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to modify attrs: "
                    + e.getMessage(), e);
        } finally {
            LegacyZimbraLdapContext.closeContext(zlc);
        }
    }
    
    @Override
    public void modifyAttrs(ZLdapContext zlc, String dn, Map<String, ? extends Object> attrs, 
            Entry entry)
    throws ServiceException {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void modifyEntry(String dn, Map<String, ? extends Object> attrs, 
            Entry entry, LdapUsage ldapUsage) 
    throws ServiceException {
        
        LegacyZimbraLdapContext zlc = null;
        try {
            zlc = new LegacyZimbraLdapContext(true);
            LegacyLdapUtil.modifyAttrs(zlc, dn, attrs, entry);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to modify attrs: "
                    + e.getMessage(), e);
        } finally {
            LegacyZimbraLdapContext.closeContext(zlc);
        }
    }
    

    @Override
    public boolean testAndModifyEntry(ZLdapContext zlc, String dn,
            ZLdapFilter testFilter, Map<String, ? extends Object> attrs,
            Entry entry) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ZSearchResultEntry searchForEntry(String base, ZLdapFilter filter,
            ZLdapContext initZlc, boolean useMaster, String[] returnAttrs)
    throws LdapMultipleEntriesMatchedException, ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ZAttributes getAttributes(ZLdapContext initZlc, 
            LdapServerType ldapServerType, LdapUsage usage,
            String dn, String[] returnAttrs) 
    throws LdapEntryNotFoundException, ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ZSearchResultEnumeration searchDir(String baseDN, ZLdapFilter filter,
            ZSearchControls searchControls, ZLdapContext initZlc, LdapServerType ldapServerType) 
    throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long countEntries(String baseDN, ZLdapFilter filter,
            ZSearchControls searchControls, ZLdapContext initZlc,
            LdapServerType ldapServerType) throws ServiceException {
        throw new UnsupportedOperationException();
    }
}
