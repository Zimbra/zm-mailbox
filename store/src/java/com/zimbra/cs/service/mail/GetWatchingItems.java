/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.google.common.collect.Multimap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.OctopusXmlConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.cache.WatchCache;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

public class GetWatchingItems extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Element response = zsc.createElement(OctopusXmlConstants.GET_WATCHING_ITEMS_RESPONSE);
        Provisioning prov = Provisioning.getInstance();
        ItemIdFormatter fmt = new ItemIdFormatter(zsc);
        Account account = prov.getAccountById(zsc.getRequestedAccountId());
        Multimap<String,Integer> cache = WatchCache.get(account).getMap();
        for (String key : cache.keySet()) {
            Element target = response.addNonUniqueElement(MailConstants.E_TARGET);
            Account a = prov.getAccountById(key);
            target.addAttribute(MailConstants.A_ID, a.getId());
            target.addAttribute(MailConstants.A_EMAIL, a.getName());
            target.addAttribute(MailConstants.A_NAME, a.getDisplayName());
            for (Integer itemId : cache.get(key)) {
                Element i = target.addNonUniqueElement(MailConstants.E_ITEM);
                ItemId iid = new ItemId(key, itemId);
                i.addAttribute(MailConstants.A_ID, fmt.formatItemId(iid));
            }
        }
        return response;
    }

}
