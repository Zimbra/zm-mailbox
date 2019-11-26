/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.soap.ZimbraSoapContext;

public class GetPop3UID extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        Account acct = mbox.getAccount();
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        String ids = request.getElement(MailConstants.E_MSG).getAttribute(MailConstants.A_IDS);
        ArrayList<Integer> local = new ArrayList<Integer>();
        HashMap<String, StringBuilder> remote = new HashMap<String, StringBuilder>();
        ItemAction.partitionItems(zsc, ids, local, remote);

        Element response = zsc.createElement(MailConstants.GET_POP3_UID_RESPONSE);

        if (remote.size() > 0) {
            List<Element> responses = proxyRemote(request, remote, context);
            for (Element e : responses) {
                response.addElement(e);
            }
        }

        if (local.size() > 0) {
            List<Message> msgs = GetMsg.getMsgs(octxt, mbox, local, false);
            for (Message msg : msgs) {
                if (msg != null) {
                    ToXML.encodeGetPop3UID(response, ifmt, octxt, msg, null);
                }
            }
        }

        return response;
    }

    List<Element> proxyRemote(Element request, Map<String, StringBuilder> remote, Map<String,Object> context)
    throws ServiceException {
        List<Element> responses = new ArrayList<Element>();

        Element eMsg = request.getElement(MailConstants.E_MSG);
        for (Map.Entry<String, StringBuilder> entry : remote.entrySet()) {
            eMsg.addAttribute(MailConstants.A_IDS, entry.getValue().toString());

            Element response = proxyRequest(request, context, entry.getKey());
            for (Element e : response.listElements()) {
                responses.add(e.detach());
            }
        }

        return responses;
    }
}
