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

import java.util.Map;

import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetIMAPRecentCutoffRequest;
import com.zimbra.soap.mail.message.GetIMAPRecentCutoffResponse;

public class GetIMAPRecentCutoff extends MailDocumentHandler {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.A_ID };
    @Override
    protected String[] getProxiedIdPath(Element request) {
        return TARGET_FOLDER_PATH;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        GetIMAPRecentCutoffRequest req = zsc.elementToJaxb(request);
        ItemIdentifier itemIdent = ItemIdentifier.fromOwnerAndRemoteId(mbox.getAccountId(), req.getId());
        if (!mbox.getAccountId().equals(itemIdent.accountId)) {
            throw MailServiceException.NO_SUCH_FOLDER(req.getId());
        }
        return zsc.jaxbToElement(new GetIMAPRecentCutoffResponse(
                mbox.getImapRecentCutoff(octxt, itemIdent.id)));
    }
}
