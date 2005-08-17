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

    public static final String NAMESPACE_STR = "urn:liquidAccount";
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
    
    public static final QName SEARCH_GAL_REQUEST = QName.get("SearchGalRequest", NAMESPACE);
    public static final QName SEARCH_GAL_RESPONSE = QName.get("SearchGalResponse", NAMESPACE);	

    public static final String E_ACTION = "action";
    public static final String E_AUTH_TOKEN = "authToken";
    public static final String E_REFERRAL = "refer";
    public static final String E_LIFETIME = "lifetime";
    public static final String E_ACCOUNT = "account";
    public static final String E_NAME = "name";	
    public static final String E_PASSWORD = "password";
    public static final String E_OLD_PASSWORD = "oldPassword";
    public static final String E_PREF = "pref";
    public static final String E_PREFS = "prefs";
    public static final String E_ATTR = "attr";
    public static final String E_ATTRS = "attrs";
//    public static final String E_QUOTA = "quota";
    public static final String E_QUOTA_USED = "used";

    public static final String A_NAME = "name";

	public void registerHandlers(DocumentDispatcher dispatcher) {

		// auth
		dispatcher.registerHandler(AUTH_REQUEST, new Auth());
		dispatcher.registerHandler(CHANGE_PASSWORD_REQUEST, new ChangePassword());

        // prefs
		dispatcher.registerHandler(GET_PREFS_REQUEST, new GetPrefs());
		dispatcher.registerHandler(MODIFY_PREFS_REQUEST, new ModifyPrefs());
		
        dispatcher.registerHandler(GET_INFO_REQUEST, new GetInfo());
        
		dispatcher.registerHandler(SEARCH_GAL_REQUEST, new SearchGal());
	}

}
