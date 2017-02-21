/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
