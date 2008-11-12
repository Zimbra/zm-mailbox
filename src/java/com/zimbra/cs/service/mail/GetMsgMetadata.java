/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.soap.ZimbraSoapContext;

public class GetMsgMetadata extends MailDocumentHandler {

    static final int SUMMARY_FIELDS = Change.MODIFIED_SIZE | Change.MODIFIED_PARENT | Change.MODIFIED_FOLDER |
                                      Change.MODIFIED_TAGS | Change.MODIFIED_FLAGS  | Change.MODIFIED_UNREAD |
                                      Change.MODIFIED_DATE | Change.MODIFIED_CONFLICT;

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        Mailbox.OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        String ids = request.getElement(MailConstants.E_MSG).getAttribute(MailConstants.A_IDS);
        ArrayList<Integer> local = new ArrayList<Integer>();
        HashMap<String, StringBuffer> remote = new HashMap<String, StringBuffer>();
        ItemAction.partitionItems(zsc, ids, local, remote);

        Element response = zsc.createElement(MailConstants.GET_MSG_METADATA_RESPONSE);

        if (remote.size() > 0) {
            List<Element> responses = proxyRemote(request, remote, context);
            for (Element e : responses)
                response.addElement(e);
        }
        
        if (local.size() > 0) {
            List<Message> msgs = GetMsg.getMsgs(octxt, mbox, local, false);
            for (Message msg : msgs) {
                if (msg != null)
                    ToXML.encodeMessageSummary(response, ifmt, octxt, msg, null, SUMMARY_FIELDS);
            }
        }

        return response;
    }

    List<Element> proxyRemote(Element request, Map<String, StringBuffer> remote, Map<String,Object> context)
    throws ServiceException {
        List<Element> responses = new ArrayList<Element>();

        Element eMsg = request.getElement(MailConstants.E_MSG);
        for (Map.Entry<String, StringBuffer> entry : remote.entrySet()) {
            eMsg.addAttribute(MailConstants.A_IDS, entry.getValue().toString());

            Element response = proxyRequest(request, context, entry.getKey());
            for (Element e : response.listElements())
                responses.add(e.detach());
        }

        return responses;
    }
}
