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
package com.zimbra.qa.unittest.prov.ldap;

import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.qa.unittest.prov.ProvTestUtil;

class LdapProvTestUtil extends ProvTestUtil {

    LdapProvTestUtil() throws Exception {
        super(LdapProv.getInst());
    }
    
    LdapProv getProv() {
        return (LdapProv) prov;
    }
}

