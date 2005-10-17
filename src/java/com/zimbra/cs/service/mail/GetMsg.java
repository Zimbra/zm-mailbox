/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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

import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author schemers
 */
public class GetMsg extends WriteOpDocumentHandler {

    private static StopWatch sWatch = StopWatch.getInstance("GetMsg");

    private static final String[] TARGET_MSG_PATH = new String[] { MailService.E_MSG, MailService.A_ID };
    protected String[] getProxiedIdPath()     { return TARGET_MSG_PATH; }
    protected boolean checkMountpointProxy()  { return false; }

    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();
        try {
            ZimbraContext lc = getZimbraContext(context);
            Mailbox mbox = getRequestedMailbox(lc);
            OperationContext octxt = lc.getOperationContext();

            Element eMsg = request.getElement(MailService.E_MSG);
            ItemId iid = new ItemId(eMsg.getAttribute(MailService.A_ID), lc);
            boolean raw = eMsg.getAttributeBool(MailService.A_RAW, false);
            boolean read = eMsg.getAttributeBool(MailService.A_MARK_READ, false);
            String part = eMsg.getAttribute(MailService.A_PART, null);

            Message msg = null;
            Appointment appt = null;

            if (!iid.hasSubpart()) {
                msg = mbox.getMessageById(octxt, iid.getId());
                
                if (read && msg.isUnread() && !RedoLogProvider.getInstance().isSlave())
                    mbox.alterTag(octxt, iid.getId(), MailItem.TYPE_MESSAGE, Flag.ID_FLAG_UNREAD, false);
            } else {
                appt = mbox.getAppointmentById(octxt, iid.getId());
            }
            
            Element response = lc.createElement(MailService.GET_MSG_RESPONSE);
            if (raw) {
                ToXML.encodeMessageAsMIME(response, lc, msg, part);
            } else {
                boolean wantHTML = eMsg.getAttributeBool(MailService.A_WANT_HTML, false);
                if (msg != null)
                    ToXML.encodeMessageAsMP(response, lc, msg, wantHTML, part);
                else if (appt != null)
                    ToXML.encodeApptAsMP(response, lc, appt, iid.getSubpartId(), wantHTML, part);
            }
            return response;
            
        } finally {
            sWatch.stop(startTime);
        }
    }

    public boolean isReadOnly() {
        return RedoLogProvider.getInstance().isSlave();
    }
}
