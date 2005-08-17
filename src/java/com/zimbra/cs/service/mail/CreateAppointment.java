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
    protected static ParseMimeMessage.InviteParser CREATE_APPOINTMENT_INVITE_PARSER = new ParseMimeMessage.InviteParser() { 
        public ParseMimeMessage.InviteParserResult parseInviteElement(OperationContext octxt, Account account, Element inviteElem) throws ServiceException 
        {
            return CalendarUtils.parseInviteForCreate(account, inviteElem);
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
            CalSendData dat = handleMsgElement(octxt, msgElem, acct, mbx, CREATE_APPOINTMENT_INVITE_PARSER);

            Element response = lc.createElement(MailService.CREATE_APPOINTMENT_RESPONSE);            
            return sendCalendarMessage(octxt, acct, mbx, dat, response);
        } finally {
            sWatch.stop(startTime);
        }
    }
}
