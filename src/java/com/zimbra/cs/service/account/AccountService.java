/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.DocumentDispatcher;
import com.zimbra.soap.DocumentService;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.KeyValuePair;

/**
 * 
 * @zm-service-description		The Account Service includes commands for retrieving,
 * storing and managing information user account information.
 * 
 * @author schemers
 */
public class AccountService implements DocumentService {

    public void registerHandlers(DocumentDispatcher dispatcher) {

        // auth
        dispatcher.registerHandler(AccountConstants.AUTH_REQUEST, new Auth());
        dispatcher.registerHandler(AccountConstants.CHANGE_PASSWORD_REQUEST, new ChangePassword());
        dispatcher.registerHandler(AccountConstants.END_SESSION_REQUEST, new EndSession());

        // prefs
        dispatcher.registerHandler(AccountConstants.GET_PREFS_REQUEST, new GetPrefs());
        dispatcher.registerHandler(AccountConstants.MODIFY_PREFS_REQUEST, new ModifyPrefs());

        dispatcher.registerHandler(AccountConstants.GET_INFO_REQUEST, new GetInfo());
        dispatcher.registerHandler(AccountConstants.GET_ACCOUNT_INFO_REQUEST, new GetAccountInfo());

        dispatcher.registerHandler(AccountConstants.SEARCH_GAL_REQUEST, new SearchGal());
        dispatcher.registerHandler(AccountConstants.AUTO_COMPLETE_GAL_REQUEST, new AutoCompleteGal());
        dispatcher.registerHandler(AccountConstants.SYNC_GAL_REQUEST, new SyncGal());
        dispatcher.registerHandler(AccountConstants.SEARCH_CALENDAR_RESOURCES_REQUEST, new SearchCalendarResources());

        dispatcher.registerHandler(AccountConstants.MODIFY_PROPERTIES_REQUEST, new ModifyProperties());
        dispatcher.registerHandler(AccountConstants.MODIFY_ZIMLET_PREFS_REQUEST, new ModifyZimletPrefs());

        dispatcher.registerHandler(AccountConstants.GET_ALL_LOCALES_REQUEST, new GetAllLocales());
        dispatcher.registerHandler(AccountConstants.GET_AVAILABLE_LOCALES_REQUEST, new GetAvailableLocales());
        dispatcher.registerHandler(AccountConstants.GET_AVAILABLE_SKINS_REQUEST, new GetAvailableSkins());
        dispatcher.registerHandler(AccountConstants.GET_AVAILABLE_CSV_FORMATS_REQUEST, new GetAvailableCsvFormats());

        // identity
        dispatcher.registerHandler(AccountConstants.CREATE_IDENTITY_REQUEST, new CreateIdentity());
        dispatcher.registerHandler(AccountConstants.MODIFY_IDENTITY_REQUEST, new ModifyIdentity());
        dispatcher.registerHandler(AccountConstants.DELETE_IDENTITY_REQUEST, new DeleteIdentity());
        dispatcher.registerHandler(AccountConstants.GET_IDENTITIES_REQUEST, new GetIdentities());
        
        // signature
        dispatcher.registerHandler(AccountConstants.CREATE_SIGNATURE_REQUEST, new CreateSignature());
        dispatcher.registerHandler(AccountConstants.MODIFY_SIGNATURE_REQUEST, new ModifySignature());
        dispatcher.registerHandler(AccountConstants.DELETE_SIGNATURE_REQUEST, new DeleteSignature());
        dispatcher.registerHandler(AccountConstants.GET_SIGNATURES_REQUEST, new GetSignatures());

        // share info
        dispatcher.registerHandler(AccountConstants.GET_SHARE_INFO_REQUEST, new GetShareInfo());

        // white/black list
        dispatcher.registerHandler(AccountConstants.GET_WHITE_BLACK_LIST_REQUEST, new GetWhiteBlackList());
        dispatcher.registerHandler(AccountConstants.MODIFY_WHITE_BLACK_LIST_REQUEST, new ModifyWhiteBlackList());
        
        // distribution list
        dispatcher.registerHandler(AccountConstants.GET_DISTRIBUTION_LIST_MEMBERS_REQUEST, new GetDistributionListMembers());
        
        // misc
        dispatcher.registerHandler(AccountConstants.GET_VERSION_INFO_REQUEST, new GetVersionInfo());

    }

    /**
     * @param request
     * @return
     * @throws ServiceException
     */
    public static Map<String, Object> getAttrs(Element request, String nameAttr) throws ServiceException {
        return getAttrs(request, false, nameAttr);
    }
    
    /**
     * @param request
     * @return
     * @throws ServiceException
     */
    public static Map<String, Object> getAttrs(Element request, boolean ignoreEmptyValues, String nameAttr) throws ServiceException {
        Map<String, Object> result = new HashMap<String, Object>();
        for (KeyValuePair pair : request.listKeyValuePairs(AdminConstants.E_A, nameAttr)) {
            String name = pair.getKey();
            String value = pair.getValue();
            if (!ignoreEmptyValues || (value != null && value.length() > 0))
                StringUtil.addToMultiMap(result, name, value);
        }
        return result;
    } 
}
