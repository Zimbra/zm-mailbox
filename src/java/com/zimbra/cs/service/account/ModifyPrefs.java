/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.KeyValuePair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeInfo;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class ModifyPrefs extends AccountDocumentHandler {

    public static final String PREF_PREFIX = "zimbraPref";

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canModifyOptions(zsc, account))
            throw ServiceException.PERM_DENIED("can not modify options");

        HashMap<String, Object> prefs = new HashMap<String, Object>();
        Map<String, Set<String>> name2uniqueAttrValues = new HashMap<String, Set<String>>();
        for (KeyValuePair kvp : request.listKeyValuePairs(AccountConstants.E_PREF, AccountConstants.A_NAME)) {
            String name = kvp.getKey(), value = kvp.getValue();
            char ch = name.length() > 0 ? name.charAt(0) : 0;
            int offset = ch == '+' || ch == '-' ? 1 : 0;
            if (!name.startsWith(PREF_PREFIX, offset))
                throw ServiceException.INVALID_REQUEST("pref name must start with " + PREF_PREFIX, null);

            AttributeInfo attrInfo = AttributeManager.getInstance().getAttributeInfo(name.substring(offset));
            if (attrInfo == null) {
                throw ServiceException.INVALID_REQUEST("no such attribute: " + name, null);
            }
            if (attrInfo.isCaseInsensitive()) {
                String valueLowerCase = Strings.nullToEmpty(value).toLowerCase();
                if (name2uniqueAttrValues.get(name) == null) {
                    Set<String> set = new HashSet<String>();
                    set.add(valueLowerCase);
                    name2uniqueAttrValues.put(name, set);
                    StringUtil.addToMultiMap(prefs, name, value);
                } else {
                    Set<String> set = name2uniqueAttrValues.get(name);
                    if (set.add(valueLowerCase)) {
                        StringUtil.addToMultiMap(prefs, name, value);
                    }
                }
            } else {
                StringUtil.addToMultiMap(prefs, name, value);
            }
        }

        if (prefs.containsKey(Provisioning.A_zimbraPrefMailForwardingAddress)) {
            if (!account.getBooleanAttr(Provisioning.A_zimbraFeatureMailForwardingEnabled, false))
                throw ServiceException.PERM_DENIED("forwarding not enabled");
        }

        // call modifyAttrs and pass true to checkImmutable
        Provisioning.getInstance().modifyAttrs(account, prefs, true, zsc.getAuthToken());

        Element response = zsc.createElement(AccountConstants.MODIFY_PREFS_RESPONSE);
        return response;
    }
}
