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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Feb 22, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

/**
 * @author tim
 */
public class CreateAppointment extends CalendarRequest {
    
    private static Log sLog = LogFactory.getLog(CreateAppointment.class);
    private static StopWatch sWatch = StopWatch.getInstance("CreateAppointment");

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailService.E_MSG, MailService.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return true; }
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    static final String DEFAULT_FOLDER = "" + Mailbox.ID_FOLDER_CALENDAR;

    // very simple: generate a new UID and send a REQUEST
    protected static class CreateAppointmentInviteParser extends ParseMimeMessage.InviteParser { 
        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraContext lc, Account account, Element inviteElem) throws ServiceException 
        {
            return CalendarUtils.parseInviteForCreate(account, inviteElem, null, null, false, CalendarUtils.RECUR_ALLOWED);
        }
    };

    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();
        try {
            ZimbraContext lc = getZimbraContext(context);
            Account acct = getRequestedAccount(lc);
            Mailbox mbox = getRequestedMailbox(lc);

            // <M>
            Element msgElem = request.getElement(MailService.E_MSG);

            // no existing Appt referenced -- this is a new create!
            String folderIdStr = msgElem.getAttribute(MailService.A_FOLDER, DEFAULT_FOLDER);
            ItemId iidFolder = new ItemId(folderIdStr, lc);
            sLog.info("<CreateAppointment folder=" + iidFolder.getId() + "> " + lc.toString());

            CreateAppointmentInviteParser parser = new CreateAppointmentInviteParser();
            CalSendData dat = handleMsgElement(lc, msgElem, acct, mbox, parser);

            Element response = lc.createElement(MailService.CREATE_APPOINTMENT_RESPONSE);
            return sendCalendarMessage(lc, iidFolder.getId(), acct, mbox, dat, response, false);
        } finally {
            sWatch.stop(startTime);
        }
    }
}
