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
 * Created on Feb 22, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.ZimbraContext;

/**
 * @author tim
 */
public class CreateAppointment extends CalendarRequest {
    
    private static Log sLog = LogFactory.getLog(CreateAppointment.class);
    private static StopWatch sWatch = StopWatch.getInstance("CreateAppointment");

    // very simple: generate a new UID and send a REQUEST
    protected static class CreateAppointmentInviteParser extends ParseMimeMessage.InviteParser { 
        public ParseMimeMessage.InviteParserResult parseInviteElement(OperationContext octxt, Account account, Element inviteElem) throws ServiceException 
        {
            return CalendarUtils.parseInviteForCreate(account, inviteElem, null, null, false);
        }
    };
    
    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();
        try {
            ZimbraContext lc = getZimbraContext(context);
            Account acct = getRequestedAccount(lc);
            Mailbox mbx = getRequestedMailbox(lc);
            OperationContext octxt = lc.getOperationContext();

            sLog.info("<CreateAppointment> " + lc.toString());
            
            // <M>
            Element msgElem = request.getElement(MailService.E_MSG);
            CreateAppointmentInviteParser parser = new CreateAppointmentInviteParser();
            CalSendData dat = handleMsgElement(octxt, msgElem, acct, mbx, parser);

            Element response = lc.createElement(MailService.CREATE_APPOINTMENT_RESPONSE);            
            return sendCalendarMessage(octxt, acct, mbx, dat, response);
        } finally {
            sWatch.stop(startTime);
        }
    }
}
