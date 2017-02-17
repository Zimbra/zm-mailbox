/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.Comment;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;

public class AddComment extends MailDocumentHandler {

    private static final String[] PARENT_ID_PATH = new String[] { MailConstants.E_COMMENT, MailConstants.A_PARENT_ID };
    
    @Override
    protected String[] getProxiedIdPath(Element request)     { return PARENT_ID_PATH; }
    
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        String creator = zsc.getAuthtokenAccountId();

        Element c = request.getElement(MailConstants.E_COMMENT);
        String itemId = c.getAttribute(MailConstants.A_PARENT_ID);
        String text = c.getAttribute(MailConstants.A_TEXT);

        ItemId iid = new ItemId(itemId, zsc);
        Comment comment = mbox.createComment(octxt, iid.getId(), text, creator);

        Element response = zsc.createElement(MailConstants.ADD_COMMENT_RESPONSE);
        response.addElement(MailConstants.E_COMMENT).addAttribute(MailConstants.A_ID, comment.getId());
        return response;
    }
}
