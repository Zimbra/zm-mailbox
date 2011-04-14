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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.prov.ldap.LdapHelper;
import com.zimbra.cs.prov.ldap.LdapProv;

/**
 * An LdapHelper tied to ZimbraLdapContext and the legacy ldapUtil methods.
 * 
 * @author pshao
 *
 */
public class LegacyLdapHelper extends LdapHelper {

    LegacyLdapHelper(LdapProv ldapProv) {
        super(ldapProv);
    }

    @Override
    public void searchLdap(SearchLdapOptions searchOptions) throws ServiceException {
        ZimbraLdapContext zlc = LdapClient.toZimbraLdapContext(getProv(), searchOptions.getILdapContext());
        LdapUtil.searchLdap(zlc, 
                searchOptions.getSearchBase(), 
                searchOptions.getQuery(),
                searchOptions.getReturnAttrs(), 
                searchOptions.getBinaryAttrs(), 
                searchOptions.getVisitor());
    }

}
