/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.KeyValuePair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.soap.DocumentDispatcher;
import com.zimbra.soap.DocumentService;

/**
 *
 * @zm-service-description		The Account Service includes commands for retrieving,
 * storing and managing information user account information.
 *
 * @author schemers
 */
public class AccountService implements DocumentService {

    @Override
    public void registerHandlers(DocumentDispatcher dispatcher) {

        // auth
        dispatcher.registerHandler(AccountConstants.AUTH_REQUEST, new Auth());
        dispatcher.registerHandler(AccountConstants.CHANGE_PASSWORD_REQUEST, new ChangePassword());
        dispatcher.registerHandler(AccountConstants.END_SESSION_REQUEST, new EndSession());

        // oauth
        dispatcher.registerHandler(AccountConstants.GET_OAUTH_CONSUMERS_REQUEST, new GetOAuthConsumers());
        dispatcher.registerHandler(AccountConstants.REVOKE_OAUTH_CONSUMER_REQUEST, new RevokeOAuthConsumer());

        // prefs
        dispatcher.registerHandler(AccountConstants.GET_PREFS_REQUEST, new GetPrefs());
        dispatcher.registerHandler(AccountConstants.MODIFY_PREFS_REQUEST, new ModifyPrefs());

        dispatcher.registerHandler(AccountConstants.GET_INFO_REQUEST, new GetInfo());
        dispatcher.registerHandler(AccountConstants.GET_ACCOUNT_INFO_REQUEST, new GetAccountInfo());

        dispatcher.registerHandler(AccountConstants.AUTO_COMPLETE_GAL_REQUEST, new AutoCompleteGal());
        dispatcher.registerHandler(AccountConstants.SEARCH_CALENDAR_RESOURCES_REQUEST, new SearchCalendarResources());
        dispatcher.registerHandler(AccountConstants.SEARCH_GAL_REQUEST, new SearchGal());
        dispatcher.registerHandler(AccountConstants.SYNC_GAL_REQUEST, new SyncGal());

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
        dispatcher.registerHandler(AccountConstants.CREATE_DISTRIBUTION_LIST_REQUEST, new CreateDistributionList());
        dispatcher.registerHandler(AccountConstants.DISTRIBUTION_LIST_ACTION_REQUEST, new DistributionListAction());
        dispatcher.registerHandler(AccountConstants.GET_ACCOUNT_DISTRIBUTION_LISTS_REQUEST, new GetAccountDistributionLists());
        dispatcher.registerHandler(AccountConstants.GET_DISTRIBUTION_LIST_REQUEST, new GetDistributionList());
        dispatcher.registerHandler(AccountConstants.GET_DISTRIBUTION_LIST_MEMBERS_REQUEST, new GetDistributionListMembers());
        dispatcher.registerHandler(AccountConstants.SUBSCRIBE_DISTRIBUTION_LIST_REQUEST, new SubscribeDistributionList());

        // rights
        dispatcher.registerHandler(AccountConstants.CHECK_RIGHTS_REQUEST, new CheckRights());
        dispatcher.registerHandler(AccountConstants.DISCOVER_RIGHTS_REQUEST, new DiscoverRights());
        dispatcher.registerHandler(AccountConstants.GET_RIGHTS_REQUEST, new GetRights());
        dispatcher.registerHandler(AccountConstants.GRANT_RIGHTS_REQUEST, new GrantRights());
        dispatcher.registerHandler(AccountConstants.REVOKE_RIGHTS_REQUEST, new RevokeRights());

        // misc
        dispatcher.registerHandler(AccountConstants.GET_VERSION_INFO_REQUEST, new GetVersionInfo());

        // reset password
        dispatcher.registerHandler(AccountConstants.RESET_PASSWORD_REQUEST, new ResetPassword());

        // attrs
        dispatcher.registerHandler(AccountConstants.MODIFY_ACCOUNT_REQUEST, new ModifyAccount());
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


    /**
     * parse key values pairs in the form of:
     *     <{elemName} {attrName}="{key}">{value}</{elemName}>
     *
     *     e.g.
     *     <a n="boo">bar</a>
     *
     * @param parent
     * @param elemName
     * @param attrName
     * @return
     * @throws ServiceException
     */
    public static Map<String, Object> getKeyValuePairs(Element parent, String elemName, String attrName)
    throws ServiceException {
        Map<String, Object> result = new HashMap<String, Object>();
        for (Element eKV : parent.listElements(elemName)) {
            String key = eKV.getAttribute(attrName);
            String value = eKV.getText();
            StringUtil.addToMultiMap(result, key, value);
        }
        return result;
    }
}
