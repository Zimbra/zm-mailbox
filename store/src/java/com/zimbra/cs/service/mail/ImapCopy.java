/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.IMAPCopyRequest;
import com.zimbra.soap.mail.message.IMAPCopyResponse;
import com.zimbra.soap.mail.type.IMAPItemInfo;

public class ImapCopy extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        IMAPCopyRequest req = zsc.elementToJaxb(request);
        String[] stringIds = req.getIds().split(",");
        List<Integer> itemIds = Lists.newArrayList();
        for(String id : stringIds) {
            try {
                itemIds.add(Integer.parseInt(id));
            } catch (Exception e) {
                throw ServiceException.INVALID_REQUEST("Invalid item ids", e);
            }
        }
        String typeString = req.getType();
        List<MailItem> items =
                mbox.imapCopy(octxt, Ints.toArray(itemIds), MailItem.Type.valueOf(typeString), req.getFolder());
        IMAPCopyResponse resp = new IMAPCopyResponse();
        for(MailItem item : items) {
            resp.addItem(new IMAPItemInfo(item.getId(), item.getImapUid()));
        }
        return zsc.jaxbToElement(resp);
    }

}
