/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;


public class AccountConstants {

    public static final String NAMESPACE_STR = "urn:zimbraAccount";
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    // auth
    public static final QName AUTH_REQUEST = QName.get("AuthRequest", NAMESPACE);
    public static final QName AUTH_RESPONSE = QName.get("AuthResponse", NAMESPACE);
    public static final QName CHANGE_PASSWORD_REQUEST = QName.get("ChangePasswordRequest", NAMESPACE);
    public static final QName CHANGE_PASSWORD_RESPONSE = QName.get("ChangePasswordResponse", NAMESPACE);

    // prefs
    public static final QName GET_PREFS_REQUEST = QName.get("GetPrefsRequest", NAMESPACE);
    public static final QName GET_PREFS_RESPONSE = QName.get("GetPrefsResponse", NAMESPACE);
    public static final QName MODIFY_PREFS_REQUEST = QName.get("ModifyPrefsRequest", NAMESPACE);
    public static final QName MODIFY_PREFS_RESPONSE = QName.get("ModifyPrefsResponse", NAMESPACE);
    public static final QName GET_INFO_REQUEST = QName.get("GetInfoRequest", NAMESPACE);
    public static final QName GET_INFO_RESPONSE = QName.get("GetInfoResponse", NAMESPACE);
    public static final QName GET_ACCOUNT_INFO_REQUEST = QName.get("GetAccountInfoRequest", NAMESPACE);
    public static final QName GET_ACCOUNT_INFO_RESPONSE = QName.get("GetAccountInfoResponse", NAMESPACE);
    public static final QName SEARCH_GAL_REQUEST = QName.get("SearchGalRequest", NAMESPACE);
    public static final QName SEARCH_GAL_RESPONSE = QName.get("SearchGalResponse", NAMESPACE);
    public static final QName AUTO_COMPLETE_GAL_REQUEST = QName.get("AutoCompleteGalRequest", NAMESPACE);
    public static final QName AUTO_COMPLETE_GAL_RESPONSE = QName.get("AutoCompleteGalResponse", NAMESPACE);
    public static final QName SYNC_GAL_REQUEST = QName.get("SyncGalRequest", NAMESPACE);
    public static final QName SYNC_GAL_RESPONSE = QName.get("SyncGalResponse", NAMESPACE);
    public static final QName SEARCH_CALENDAR_RESOURCES_REQUEST = QName.get("SearchCalendarResourcesRequest", NAMESPACE);
    public static final QName SEARCH_CALENDAR_RESOURCES_RESPONSE = QName.get("SearchCalendarResourcesResponse", NAMESPACE);
    public static final QName MODIFY_PROPERTIES_REQUEST = QName.get("ModifyPropertiesRequest", NAMESPACE);
    public static final QName MODIFY_PROPERTIES_RESPONSE = QName.get("ModifyPropertiesResponse", NAMESPACE);
    public static final QName GET_ALL_LOCALES_REQUEST = QName.get("GetAllLocalesRequest", NAMESPACE);
    public static final QName GET_ALL_LOCALES_RESPONSE = QName.get("GetAllLocalesResponse", NAMESPACE);
    public static final QName GET_AVAILABLE_LOCALES_REQUEST = QName.get("GetAvailableLocalesRequest", NAMESPACE);
    public static final QName GET_AVAILABLE_LOCALES_RESPONSE = QName.get("GetAvailableLocalesResponse", NAMESPACE);
    public static final QName GET_AVAILABLE_SKINS_REQUEST = QName.get("GetAvailableSkinsRequest", NAMESPACE);
    public static final QName GET_AVAILABLE_SKINS_RESPONSE = QName.get("GetAvailableSkinsResponse", NAMESPACE);
    public static final QName GET_AVAILABLE_CSV_FORMATS_REQUEST = QName.get("GetAvailableCsvFormatsRequest", NAMESPACE);
    public static final QName GET_AVAILABLE_CSV_FORMATS_RESPONSE = QName.get("GetAvailableCsvFormatsResponse", NAMESPACE);

    // identities
    public static final QName CREATE_IDENTITY_REQUEST = QName.get("CreateIdentityRequest", NAMESPACE);
    public static final QName CREATE_IDENTITY_RESPONSE = QName.get("CreateIdentityResponse", NAMESPACE);
    public static final QName GET_IDENTITIES_REQUEST = QName.get("GetIdentitiesRequest", NAMESPACE);
    public static final QName GET_IDENTITIES_RESPONSE = QName.get("GetIdentitiesResponse", NAMESPACE);
    public static final QName MODIFY_IDENTITY_REQUEST = QName.get("ModifyIdentityRequest", NAMESPACE);
    public static final QName MODIFY_IDENTITY_RESPONSE = QName.get("ModifyIdentityResponse", NAMESPACE);
    public static final QName DELETE_IDENTITY_REQUEST = QName.get("DeleteIdentityRequest", NAMESPACE);
    public static final QName DELETE_IDENTITY_RESPONSE = QName.get("DeleteIdentityResponse", NAMESPACE);
    
    // signatures
    public static final QName CREATE_SIGNATURE_REQUEST = QName.get("CreateSignatureRequest", NAMESPACE);
    public static final QName CREATE_SIGNATURE_RESPONSE = QName.get("CreateSignatureResponse", NAMESPACE);
    public static final QName GET_SIGNATURES_REQUEST = QName.get("GetSignaturesRequest", NAMESPACE);
    public static final QName GET_SIGNATURES_RESPONSE = QName.get("GetSignaturesResponse", NAMESPACE);
    public static final QName MODIFY_SIGNATURE_REQUEST = QName.get("ModifySignatureRequest", NAMESPACE);
    public static final QName MODIFY_SIGNATURE_RESPONSE = QName.get("ModifySignatureResponse", NAMESPACE);
    public static final QName DELETE_SIGNATURE_REQUEST = QName.get("DeleteSignatureRequest", NAMESPACE);
    public static final QName DELETE_SIGNATURE_RESPONSE = QName.get("DeleteSignatureResponse", NAMESPACE);
     
    
    
    public static final String E_ACTION = "action";
    public static final String E_AUTH_TOKEN = "authToken";
    public static final String E_CRUMB = "crumb";
    public static final String E_REFERRAL = "refer";
    public static final String E_LIFETIME = "lifetime";
    public static final String E_ACCOUNT = "account";
    public static final String E_CALENDAR_RESOURCE = "calresource";
    public static final String E_VERSION = "version";
    public static final String E_NAME = "name";
    public static final String E_ID = "id";
    public static final String E_PASSWORD = "password";
    public static final String E_OLD_PASSWORD = "oldPassword";
    public static final String A_SECTIONS = "sections";
    public static final String E_PREF = "pref";
    public static final String E_PREFS = "prefs";
    public static final String E_ATTR = "attr";
    public static final String E_ATTRS = "attrs";
    public static final String E_QUOTA_USED = "used";
    public static final String E_PREVIOUS_SESSION = "prevSession";
    public static final String E_LAST_ACCESS = "accessed";
    public static final String E_RECENT_MSGS = "recent";
    public static final String E_ZIMLET = "zimlet";
    public static final String E_ZIMLETS = "zimlets";
    public static final String E_ZIMLET_CONTEXT = "zimletContext";
    public static final String E_PROPERTY = "prop";
    public static final String E_PROPERTIES = "props";
    public static final String E_SOAP_URL = "soapURL";
    public static final String E_PREAUTH = "preauth";
    public static final String E_A = "a";
    public static final String E_ENTRY_SEARCH_FILTER = "searchFilter";
    public static final String E_ENTRY_SEARCH_FILTER_MULTICOND = "conds";
    public static final String E_ENTRY_SEARCH_FILTER_SINGLECOND = "cond";
    public static final String E_LOCALE = "locale";
    public static final String E_VIRTUAL_HOST = "virtualHost";
    public static final String E_SKIN = "skin";
    public static final String E_LICENSE = "license";
    public static final String E_IDENTITIES = "identities";
    public static final String E_SIGNATURES = "signatures";
    public static final String E_IDENTITY = "identity";
    public static final String E_SIGNATURE = "signature";
    public static final String E_DATA_SOURCES = "dataSources";
    public static final String E_DATA_SOURCE = "dataSource";
    public static final String E_CHILD_ACCOUNTS = "childAccounts";
    public static final String E_CHILD_ACCOUNT = "childAccount";
    public static final String E_CONTENT = "content";
    public static final String E_REQUESTED_SKIN = "requestedSkin";
    public static final String E_REST = "rest";
    public static final String E_CSV = "csv";
    public static final String A_N = "n";
    public static final String A_NAME = "name";
    public static final String A_ID = "id";
    public static final String A_BY = "by";
    public static final String A_TYPE = "type";
    public static final String A_LIMIT = "limit";
    public static final String A_MORE = "more";
    public static final String A_ZIMLET = "zimlet";
    public static final String A_ZIMLET_BASE_URL = "baseUrl";
    public static final String A_ZIMLET_PRIORITY = "priority";
    public static final String A_TIMESTAMP = "timestamp";
    public static final String A_EXPIRES = "expires";
    public static final String A_STATUS = "status";
    public static final String A_ATTRS = "attrs";
    public static final String A_SORT_BY = "sortBy";
    public static final String A_SORT_ASCENDING = "sortAscending";
    public static final String A_UTF8 = "utf8";
    public static final String A_VISIBLE = "visible";

    // calendar resource search
    public static final String A_ENTRY_SEARCH_FILTER_OR = "or";
    public static final String A_ENTRY_SEARCH_FILTER_NEGATION = "not";
    public static final String A_ENTRY_SEARCH_FILTER_ATTR = "attr";
    public static final String A_ENTRY_SEARCH_FILTER_OP = "op";
    public static final String A_ENTRY_SEARCH_FILTER_VALUE = "value";
}
