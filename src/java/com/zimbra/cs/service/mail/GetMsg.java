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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.GetMsgOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetMsg extends MailDocumentHandler {

    private static final String[] TARGET_MSG_PATH = new String[] { MailConstants.E_MSG, MailConstants.A_ID };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_MSG_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return false; }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();
        Session session = getSession(context);
        
        Element eMsg = request.getElement(MailConstants.E_MSG);
        ItemId iid = new ItemId(eMsg.getAttribute(MailConstants.A_ID), lc);
        boolean raw = eMsg.getAttributeBool(MailConstants.A_RAW, false);
        boolean read = eMsg.getAttributeBool(MailConstants.A_MARK_READ, false);
        boolean neuter = eMsg.getAttributeBool(MailConstants.A_NEUTER, true);
        String part = eMsg.getAttribute(MailConstants.A_PART, null);
        
        GetMsgOperation op = new GetMsgOperation(session, octxt, mbox, Requester.SOAP, iid, read);
        op.schedule();
        
        Element response = lc.createElement(MailConstants.GET_MSG_RESPONSE);
        if (raw) {
            ToXML.encodeMessageAsMIME(response, lc, op.getMsg(), part);
        } else {
            boolean wantHTML = eMsg.getAttributeBool(MailConstants.A_WANT_HTML, false);
            if (op.getMsg() != null)
                ToXML.encodeMessageAsMP(response, lc, op.getMsg(), part, wantHTML, neuter);
            else if (op.getCalendarItem() != null)
                ToXML.encodeInviteAsMP(response, lc, op.getCalendarItem(), iid, part, wantHTML, neuter);
        }
        return response;
    }

    public boolean isReadOnly() {
        return RedoLogProvider.getInstance().isSlave();
    }
}
