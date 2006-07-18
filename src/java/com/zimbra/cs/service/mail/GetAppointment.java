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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author tim
 * 
 * This class exists only for testing / debugging.  It is not a published API and may change
 * without notice.
 *
 */
public class GetAppointment extends MailDocumentHandler {
    private static Log sLog = LogFactory.getLog(GetAppointment.class);

    private static final String[] TARGET_ITEM_PATH = new String[] { MailService.A_ID };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_ITEM_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return false; }
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();

        boolean sync = request.getAttributeBool(MailService.A_SYNC, false);
        ItemId iid = new ItemId(request.getAttribute("id"), lc);
        sLog.info("<GetAppointment id=" + iid.getId() + "> " + lc);

        // want to return modified date only on sync-related requests
        int fields = ToXML.NOTIFY_FIELDS;
        if (sync)
            fields |= Change.MODIFIED_CONFLICT;

        Element response = lc.createElement(MailService.GET_APPOINTMENT_RESPONSE);
        synchronized(mbox) {
            Appointment appointment = mbox.getAppointmentById(octxt, iid.getId());
            ToXML.encodeApptSummary(response, lc, appointment, fields);
        }

        return response;
    }
}
