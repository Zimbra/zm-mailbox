/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
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
import com.zimbra.cs.operation.GetMsgOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.session.Session;
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
        Mailbox.OperationContext octxt = zsc.getOperationContext();
        Session session = getSession(context);

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
            GetMsgOperation op = new GetMsgOperation(session, octxt, mbox, Requester.SOAP, local, false);
            op.schedule();
            List<Message> msgs = op.getMsgList();

            for (Message msg : msgs) {
                if (msg != null)
                    ToXML.encodeMessageSummary(response, zsc, msg, null, SUMMARY_FIELDS);
            }
        }

        return response;
    }

    static List<Element> proxyRemote(Element request, Map<String, StringBuffer> remote, Map<String,Object> context)
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
