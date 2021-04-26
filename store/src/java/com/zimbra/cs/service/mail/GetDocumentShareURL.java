/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2021 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.SharedFileServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetDocumentShareURLResponse;

public class GetDocumentShareURL extends MailDocumentHandler {

    protected static final String[] TARGET_ITEM_PATH = new String[] { MailConstants.E_ITEM, MailConstants.A_ID };

    @Override
    protected String[] getProxiedIdPath(Element request) { return TARGET_ITEM_PATH; }

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        Element itemElem = request.getElement(MailConstants.E_ITEM);
        ItemId iid = new ItemId(itemElem.getAttribute(MailConstants.A_ID), zsc);
        Document doc = mbox.getDocumentById(octxt, iid.getId());
        String url = getPublicShareURL(doc);

        GetDocumentShareURLResponse resp = new GetDocumentShareURLResponse();
        resp.setUrl(url);
        return zsc.jaxbToElement(resp);
    }

    private String getPublicShareURL(Document doc) throws ServiceException {
        Account account = doc.getAccount();
        Folder share = doc.getShare();
        String path = "/service" + SharedFileServlet.getSharedFileURLPath(doc.getUuid(), account.getId(), share != null ? account.getId() : null);
        return URLUtil.getPublicURLForDomain(account.getServer(), Provisioning.getInstance().getDomain(account), path, true);
    }
}