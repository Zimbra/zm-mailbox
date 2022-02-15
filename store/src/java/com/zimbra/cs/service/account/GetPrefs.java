/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.HashSet;
import java.util.Map;

import com.zimbra.common.calendar.TZIDMapper;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetPrefs extends AccountDocumentHandler  {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");

        Element response = zsc.createElement(AccountConstants.GET_PREFS_RESPONSE);
        handle(request, response, account);
        return response;
    }

    /**
     * Pass in a request that optional has &lt;pref&gt; items as a filter, and
     * fills in the response document with gathered prefs.
     * 
     * @param request 
     * @param acct
     * @param response
     * @throws ServiceException
     */
    public static void handle(Element request, Element response, Account acct) throws ServiceException {
        HashSet<String> specificPrefs = null;
        for (Element epref : request.listElements(AccountConstants.E_PREF)) {
            if (specificPrefs == null)
                specificPrefs = new HashSet<String>();
            specificPrefs.add(epref.getAttribute(AccountConstants.A_NAME));
        }

        Map<String, Object> map = acct.getUnicodeAttrs();
        if (map != null) {
            doPrefs(acct, response, map, specificPrefs);
        }
    }
    
    public static void doPrefs(Account acct, Element prefs, Map<String, Object> attrsMap, HashSet<String> specificPrefs) {
        for (Map.Entry<String, Object> entry : attrsMap.entrySet()) {
            String key = entry.getKey();

            if (specificPrefs != null && !specificPrefs.contains(key))
                continue;
            if (!key.startsWith("zimbraPref"))
                continue;

            Object value = entry.getValue();
            if (value instanceof String[]) {
                String sa[] = (String[]) value;
                for (int i = 0; i < sa.length; i++)
                    prefs.addKeyValuePair(key, sa[i], AccountConstants.E_PREF, AccountConstants.A_NAME);
            } else {
                // Fixup for time zone id.  Always use canonical (Olson ZoneInfo) ID.
                if (key.equals(Provisioning.A_zimbraPrefTimeZoneId))
                    value = TZIDMapper.canonicalize((String) value);
                // ZCS-10113
                if (key.equals(Provisioning.A_zimbraPrefImapEnabled) && !acct.isImapEnabled())
                    continue;
                if (key.equals(Provisioning.A_zimbraPrefPop3Enabled) && !acct.isPop3Enabled())
                    continue;

                prefs.addKeyValuePair(key, (String) value, AccountConstants.E_PREF, AccountConstants.A_NAME);
            }
        }
    }

}
