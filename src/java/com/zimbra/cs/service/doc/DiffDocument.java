/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.doc.Diff;
import com.zimbra.cs.doc.Diff.Chunk;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class DiffDocument extends DocDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        Element doc = request.getElement(MailConstants.E_DOC);
        String idStr = doc.getAttribute(MailConstants.A_ID, null);
        int v1 = (int) doc.getAttributeLong(MailConstants.A_V1, -1);
        int v2 = (int) doc.getAttributeLong(MailConstants.A_V2, -1);

        ItemId id = new ItemId(idStr, zsc);
        Document r1 = (Document) mbox.getItemRevision(octxt, id.getId(), MailItem.Type.UNKNOWN, v1);
        Document r2 = (Document) mbox.getItemRevision(octxt, id.getId(), MailItem.Type.UNKNOWN, v2);

        Element response = zsc.createElement(MailConstants.DIFF_DOCUMENT_RESPONSE);

        try {
            Collection<Chunk> diffResult = Diff.getResult(r1.getContentStream(), r2.getContentStream());
            for (Chunk c : diffResult) {
                Element chunk = response.addElement(MailConstants.E_CHUNK);
                chunk.addAttribute(MailConstants.A_DISP, c.disposition.toString());
                chunk.setText(StringUtil.join("\n", c.content));
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("can't diff documents", e);
        }

        return response;
    }
}
