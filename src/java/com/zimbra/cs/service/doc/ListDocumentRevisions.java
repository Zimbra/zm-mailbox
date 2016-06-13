/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.service.doc;

import java.util.HashSet;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class ListDocumentRevisions extends DocDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        Element doc = request.getElement(MailConstants.E_DOC);
        String id = doc.getAttribute(MailConstants.A_ID);
        int version = (int) doc.getAttributeLong(MailConstants.A_VERSION, -1);
        int count = (int) doc.getAttributeLong(MailConstants.A_COUNT, 1);

        Element response = zsc.createElement(MailConstants.LIST_DOCUMENT_REVISIONS_RESPONSE);

        Document item;

        ItemId iid = new ItemId(id, zsc);
        item = mbox.getDocumentById(octxt, iid.getId());

        if (version < 0) {
            version = item.getVersion();
        }
        MailItem.Type type = item.getType();
        HashSet<Account> accounts = new HashSet<Account>();
        Provisioning prov = Provisioning.getInstance();
        while (version > 0 && count > 0) {
            item = (Document) mbox.getItemRevision(octxt, iid.getId(), type, version);
            if (item != null) {
                ToXML.encodeDocument(response, ifmt, octxt, item);
                Account a = prov.getAccountByName(item.getCreator());
                if (a != null)
                    accounts.add(a);
            }
            version--; count--;
        }
        for (Account a : accounts) {
            Element user = response.addElement(MailConstants.A_USER);
            user.addAttribute(MailConstants.A_ID, a.getId());
            user.addAttribute(MailConstants.A_EMAIL, a.getName());
            user.addAttribute(MailConstants.A_NAME, a.getDisplayName());
        }

        return response;
    }
}
