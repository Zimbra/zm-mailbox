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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.DocumentDispatcher;
import com.zimbra.soap.DocumentService;
import com.zimbra.common.soap.Element;

/**
 * @author schemers
 */
public class AccountService implements DocumentService {

    public void registerHandlers(DocumentDispatcher dispatcher) {

        // auth
        dispatcher.registerHandler(AccountConstants.AUTH_REQUEST, new Auth());
        dispatcher.registerHandler(AccountConstants.CHANGE_PASSWORD_REQUEST, new ChangePassword());

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

        dispatcher.registerHandler(AccountConstants.GET_ALL_LOCALES_REQUEST, new GetAllLocales());
        dispatcher.registerHandler(AccountConstants.GET_AVAILABLE_SKINS_REQUEST, new GetAvailableSkins());

        // identity
        dispatcher.registerHandler(AccountConstants.CREATE_IDENTITY_REQUEST, new CreateIdentity());
        dispatcher.registerHandler(AccountConstants.MODIFY_IDENTITY_REQUEST, new ModifyIdentity());
        dispatcher.registerHandler(AccountConstants.DELETE_IDENTITY_REQUEST, new DeleteIdentity());
        dispatcher.registerHandler(AccountConstants.GET_IDENTITIES_REQUEST, new GetIdentities());

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
        for (Element a : request.listElements(AdminConstants.E_A)) {
            String name = a.getAttribute(nameAttr);
            String value = a.getText();
            if (!ignoreEmptyValues || (value != null && value.length() > 0))
                StringUtil.addToMultiMap(result, name, value);
        }
        return result;
    } 
}
