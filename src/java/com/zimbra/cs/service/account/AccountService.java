/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import org.dom4j.Namespace;
import org.dom4j.QName;

import com.zimbra.soap.DocumentDispatcher;
import com.zimbra.soap.DocumentService;

/**
 * @author schemers
 */
public class AccountService implements DocumentService {

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
    
    public static final QName SYNC_GAL_REQUEST = QName.get("SyncGalRequest", NAMESPACE);
    public static final QName SYNC_GAL_RESPONSE = QName.get("SyncGalResponse", NAMESPACE);  

    public static final QName SEARCH_CALENDAR_RESOURCES_REQUEST = QName.get("SearchCalendarResourcesRequest", NAMESPACE);
    public static final QName SEARCH_CALENDAR_RESOURCES_RESPONSE = QName.get("SearchCalendarResourcesResponse", NAMESPACE);

    public static final QName MODIFY_PROPERTIES_REQUEST = QName.get("ModifyPropertiesRequest", NAMESPACE);
    public static final QName MODIFY_PROPERTIES_RESPONSE = QName.get("ModifyPropertiesResponse", NAMESPACE);

    public static final String E_ACTION = "action";
    public static final String E_AUTH_TOKEN = "authToken";
    public static final String E_REFERRAL = "refer";
    public static final String E_LIFETIME = "lifetime";
    public static final String E_ACCOUNT = "account";
    public static final String E_CALENDAR_RESOURCE = "calresource";
    public static final String E_NAME = "name";
    public static final String E_PASSWORD = "password";
    public static final String E_OLD_PASSWORD = "oldPassword";
    public static final String E_PREF = "pref";
    public static final String E_PREFS = "prefs";
    public static final String E_ATTR = "attr";
    public static final String E_ATTRS = "attrs";
//    public static final String E_QUOTA = "quota";
    public static final String E_QUOTA_USED = "used";
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

    public static final String A_N = "n";
    public static final String A_NAME = "name";
    public static final String A_ID = "id";
    public static final String A_BY = "by";
    public static final String A_ZIMLET = "zimlet";
    public static final String A_ZIMLET_BASE_URL = "baseUrl";
    public static final String A_ZIMLET_PRIORITY = "priority";
    public static final String A_TIMESTAMP = "timestamp";
    public static final String A_EXPIRES = "expires";

    public static final String A_ATTRS = "attrs";  
    public static final String A_SORT_BY = "sortBy";
    public static final String A_SORT_ASCENDING = "sortAscending";

    // calendar resource search
    public static final String A_ENTRY_SEARCH_FILTER_OR = "or";
    public static final String A_ENTRY_SEARCH_FILTER_NEGATION = "not";
    public static final String A_ENTRY_SEARCH_FILTER_ATTR = "attr";
    public static final String A_ENTRY_SEARCH_FILTER_OP = "op";
    public static final String A_ENTRY_SEARCH_FILTER_VALUE = "value";

	public void registerHandlers(DocumentDispatcher dispatcher) {

		// auth
		dispatcher.registerHandler(AUTH_REQUEST, new Auth());
		dispatcher.registerHandler(CHANGE_PASSWORD_REQUEST, new ChangePassword());

        // prefs
		dispatcher.registerHandler(GET_PREFS_REQUEST, new GetPrefs());
		dispatcher.registerHandler(MODIFY_PREFS_REQUEST, new ModifyPrefs());
		
        dispatcher.registerHandler(GET_INFO_REQUEST, new GetInfo());
        dispatcher.registerHandler(GET_ACCOUNT_INFO_REQUEST, new GetAccountInfo());        
        
        dispatcher.registerHandler(SEARCH_GAL_REQUEST, new SearchGal());
        dispatcher.registerHandler(SYNC_GAL_REQUEST, new SyncGal());        
        dispatcher.registerHandler(SEARCH_CALENDAR_RESOURCES_REQUEST, new SearchCalendarResources());

        dispatcher.registerHandler(MODIFY_PROPERTIES_REQUEST, new ModifyProperties());
	}

}
