package com.zimbra.cs.service.mail;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.cs.service.util.ItemId;


public class CreateAppointmentException extends CreateAppointment {
    
    private static Log sLog = LogFactory.getLog(CreateAppointmentException.class);
    private static StopWatch sWatch = StopWatch.getInstance("CreateAppointmentException");

    private static final String[] TARGET_APPT_PATH = new String[] { MailService.A_ID };
    protected String[] getProxiedIdPath()     { return TARGET_APPT_PATH; }
    protected boolean checkMountpointProxy()  { return false; }

    protected static class CreateApptExceptionInviteParser extends ParseMimeMessage.InviteParser
    {
        private String mUid;
        private TimeZoneMap mTzMap;
        
        CreateApptExceptionInviteParser(String uid, TimeZoneMap tzMap) {
            mUid = uid;
            mTzMap = tzMap;
        }
        
        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraContext lc, Account account, Element inviteElem)
        throws ServiceException {
            return CalendarUtils.parseInviteForCreate(account, inviteElem, mTzMap, mUid, true);
        }
    };
    
    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();
        try {
            ZimbraContext lc = getZimbraContext(context);
            Account acct = getRequestedAccount(lc);
            Mailbox mbox = getRequestedMailbox(lc);
            OperationContext octxt = lc.getOperationContext();

            ItemId iid = new ItemId(request.getAttribute(MailService.A_ID));
            int compNum = (int) request.getAttributeLong(MailService.E_INVITE_COMPONENT);
            sLog.info("<CreateAppointmentException id=" + iid.toString(lc) + " comp=" + compNum + "> " + lc.toString());

            // <M>
            Element msgElem = request.getElement(MailService.E_MSG);
            
            if (msgElem.getAttribute(MailService.A_FOLDER, null) != null) {
                throw ServiceException.FAILURE("You may not specify a target Folder when creating an Exception for an existing appointment", null);
            }

            Element response = lc.createElement(MailService.CREATE_APPOINTMENT_EXCEPTION_RESPONSE);
            synchronized(mbox) {
                Appointment appt = mbox.getAppointmentById(octxt, iid.getId()); 
                Invite inv = appt.getInvite(iid.getSubpartId(), compNum);

                if (inv.hasRecurId()) {
                    throw ServiceException.FAILURE("Invite id=" + iid.toString(lc) + " comp=" + compNum + " is not the a default invite", null);
                }

                if (appt == null)
                    throw ServiceException.FAILURE("Could not find Appointment for id=" + iid.getId() + " comp=" + compNum + ">", null);
                else if (!appt.isRecurring())
                    throw ServiceException.FAILURE("Appointment " + appt.getId() + " is not a recurring appointment", null);

                CreateApptExceptionInviteParser parser = new CreateApptExceptionInviteParser(appt.getUid(), inv.getTimeZoneMap());                
                CalSendData dat = handleMsgElement(lc, msgElem, acct, mbox, parser);

                return sendCalendarMessage(lc, appt.getFolderId(), acct, mbox, dat, response);
            }
        } finally {
            sWatch.stop(startTime);
        }
    }

}
