/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

public final class LDAPUtilsConstants {
    public static final String NAMESPACE_STR = AdminConstants.NAMESPACE_STR;
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    public static final String E_GET_LDAP_ENTRIES_REQUEST = "GetLDAPEntriesRequest";
    public static final String E_GET_LDAP_ENTRIES_RESPONSE = "GetLDAPEntriesResponse";

    public static final String E_CREATE_LDAP_ENTRIY_REQUEST = "CreateLDAPEntryRequest";
    public static final String E_CREATE_LDAP_ENTRY_RESPONSE = "CreateLDAPEntryResponse";

    public static final String E_MODIFY_LDAP_ENTRIY_REQUEST = "ModifyLDAPEntryRequest";
    public static final String E_MODIFY_LDAP_ENTRY_RESPONSE = "ModifyLDAPEntryResponse";

    public static final String E_RENAME_LDAP_ENTRIY_REQUEST = "RenameLDAPEntryRequest";
    public static final String E_RENAME_LDAP_ENTRY_RESPONSE = "RenameLDAPEntryResponse";

    public static final String E_DELETE_LDAP_ENTRIY_REQUEST = "DeleteLDAPEntryRequest";
    public static final String E_DELETE_LDAP_ENTRY_RESPONSE = "DeleteLDAPEntryResponse";

    public static final QName GET_LDAP_ENTRIES_REQUEST = QName.get(E_GET_LDAP_ENTRIES_REQUEST, NAMESPACE);
    public static final QName GET_LDAP_ENTRIES_RESPONSE = QName.get(E_GET_LDAP_ENTRIES_RESPONSE, NAMESPACE);

    public static final QName CREATE_LDAP_ENTRIY_REQUEST = QName.get(E_CREATE_LDAP_ENTRIY_REQUEST, NAMESPACE);
    public static final QName CREATE_LDAP_ENTRY_RESPONSE = QName.get(E_CREATE_LDAP_ENTRY_RESPONSE, NAMESPACE);

    public static final QName MODIFY_LDAP_ENTRIY_REQUEST = QName.get(E_MODIFY_LDAP_ENTRIY_REQUEST, NAMESPACE);
    public static final QName MODIFY_LDAP_ENTRY_RESPONSE = QName.get(E_MODIFY_LDAP_ENTRY_RESPONSE, NAMESPACE);

    public static final QName RENAME_LDAP_ENTRIY_REQUEST = QName.get(E_RENAME_LDAP_ENTRIY_REQUEST, NAMESPACE);
    public static final QName RENAME_LDAP_ENTRY_RESPONSE = QName.get(E_RENAME_LDAP_ENTRY_RESPONSE, NAMESPACE);

    public static final QName DELETE_LDAP_ENTRIY_REQUEST = QName.get(E_DELETE_LDAP_ENTRIY_REQUEST, NAMESPACE);
    public static final QName DELETE_LDAP_ENTRY_RESPONSE = QName.get(E_DELETE_LDAP_ENTRY_RESPONSE, NAMESPACE);

    public static final String E_LDAPEntry = "LDAPEntry";
    public static final String E_DN = "dn";
    public static final String E_NEW_DN = "new_dn";    
    public static final String E_LDAPSEARCHBASE = "ldapSearchBase";    
}
