/*
 * Created on Feb 22, 2005
 */
package com.liquidsys.coco.service.mail;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Mailbox.OperationContext;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.stats.StopWatch;
import com.zimbra.soap.LiquidContext;

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
            LiquidContext lc = getLiquidContext(context);
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
