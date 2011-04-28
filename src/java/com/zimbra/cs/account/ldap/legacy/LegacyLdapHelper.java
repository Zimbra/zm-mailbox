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

import javax.naming.directory.SearchControls;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.prov.ldap.LdapHelper;
import com.zimbra.cs.prov.ldap.LdapProv;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.ldap.ILdapContext;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.LdapException.LdapMultipleEntriesMatchedException;

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
        ZimbraLdapContext zlc = LdapClient.toZimbraLdapContext(getProv(), ldapContext);
        LegacyLdapUtil.searchLdap(zlc, 
                searchOptions.getSearchBase(), 
                searchOptions.getQuery(),
                searchOptions.getReturnAttrs(), 
                searchOptions.getBinaryAttrs(), 
                SearchControls.SUBTREE_SCOPE,
                searchOptions.getVisitor());
    }

    @Override
    public ZSearchResultEntry searchForEntry(String base, String query,
            ZLdapContext initZlc, boolean useMaster)
            throws LdapMultipleEntriesMatchedException, ServiceException {
        throw new UnsupportedOperationException();
    }

}
