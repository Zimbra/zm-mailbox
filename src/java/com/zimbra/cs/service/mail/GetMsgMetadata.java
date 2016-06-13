/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.soap.ZimbraSoapContext;

public class GetMsgMetadata extends MailDocumentHandler {

    static final int SUMMARY_FIELDS = Change.SIZE | Change.PARENT | Change.FOLDER | Change.TAGS | Change.FLAGS |
                                      Change.UNREAD | Change.DATE | Change.CONFLICT;

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        String ids = request.getElement(MailConstants.E_MSG).getAttribute(MailConstants.A_IDS);
        ArrayList<Integer> local = new ArrayList<Integer>();
        HashMap<String, StringBuilder> remote = new HashMap<String, StringBuilder>();
        ItemAction.partitionItems(zsc, ids, local, remote);

        Element response = zsc.createElement(MailConstants.GET_MSG_METADATA_RESPONSE);

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
                    ToXML.encodeMessageSummary(response, ifmt, octxt, msg, null, SUMMARY_FIELDS);
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
