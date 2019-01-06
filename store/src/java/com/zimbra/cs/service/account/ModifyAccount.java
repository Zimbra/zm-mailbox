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
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.account;

import java.util.Map;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.ModifyAccountRequest;
import com.zimbra.common.soap.Element.KeyValuePair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.AttributeInfo;
import com.google.common.base.Strings;

/**
 * @author schemers
 */
public class ModifyAccount extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getAuthenticatedAccount(zsc);
        Provisioning prov = Provisioning.getInstance();
        ModifyAccountRequest req = zsc.elementToJaxb(request);

        HashMap<String, Object> attrs = new HashMap<String, Object>();
        Map<String, Set<String>> name2uniqueAttrValues = new HashMap<String, Set<String>>();
        for (KeyValuePair kvp : request.listKeyValuePairs("a", "n")) {
            String name = kvp.getKey(), value = kvp.getValue();
            char ch = name.length() > 0 ? name.charAt(0) : 0;
            int offset = ch == '+' || ch == '-' ? 1 : 0;

            AttributeInfo attrInfo = AttributeManager.getInstance().getAttributeInfo(name.substring(offset));
            //TODO: throw ServiceException if name var doesn't match one of the approved attributes for the user to edit (need to create list)
            if (attrInfo == null) {
                throw ServiceException.INVALID_REQUEST("no such attribute: " + name, null);
            }
            if (attrInfo.isCaseInsensitive()) {
                String valueLowerCase = Strings.nullToEmpty(value).toLowerCase();
                if (name2uniqueAttrValues.get(name) == null) {
                    Set<String> set = new HashSet<String>();
                    set.add(valueLowerCase);
                    name2uniqueAttrValues.put(name, set);
                    StringUtil.addToMultiMap(attrs, name, value);
                } else {
                    Set<String> set = name2uniqueAttrValues.get(name);
                    if (set.add(valueLowerCase)) {
                        StringUtil.addToMultiMap(attrs, name, value);
                    }
                }
            } else {
                StringUtil.addToMultiMap(attrs, name, value);
            }
        }

        // pass in true to checkImmutable
        prov.modifyAttrs(account, attrs, true, zsc.getAuthToken());
        // get account again, in the case when zimbraCOSId or zimbraForeignPrincipal
        // is changed, the cache object(he one we are holding on to) would'd been
        // flushed out from cache. Get the account again to get the fresh one.
        account = getAuthenticatedAccount(zsc);

        ZimbraLog.security
                .info(ZimbraLog.encodeAttrs(new String[] { "cmd", "ModifyAccount", "name", account.getName() }, attrs));

        Element response = zsc.createElement(AccountConstants.MODIFY_ACCOUNT_RESPONSE);
        ToXML.encodeAccount(response, account);
        return response;
    }
}
