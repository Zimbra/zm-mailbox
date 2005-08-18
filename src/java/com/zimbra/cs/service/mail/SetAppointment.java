package com.zimbra.cs.service.mail;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Invite;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.ZimbraContext;

public class SetAppointment extends CalendarRequest {
    private static Log sLog = LogFactory.getLog(SetAppointment.class);
    private static StopWatch sWatch = StopWatch.getInstance("SetAppointment");
    
    // very simple: generate a new UID and send a REQUEST
    protected static class SetAppointmentInviteParser implements ParseMimeMessage.InviteParser { 
        private String mUid;
        SetAppointmentInviteParser(String uid) { mUid = uid; };

        public ParseMimeMessage.InviteParserResult parseInviteElement(OperationContext octxt, Account account, Element inviteElem) throws ServiceException 
        {
            return CalendarUtils.parseInviteForCreate(account, inviteElem, null, mUid, false);
        }
    };
    
    
    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();
        try {
            ZimbraContext lc = getZimbraContext(context);
            Account acct = getRequestedAccount(lc);
            Mailbox mbox = getRequestedMailbox(lc);
            OperationContext octxt = lc.getOperationContext();
            
            String uid = request.getAttribute(MailService.A_UID);
            
            sLog.info("<SetAppointment uid="+uid+"> " + lc.toString());
            
            synchronized (mbox) {
                
                Appointment appt;
                
                // First, the <default>
                {
                    Element e = request.getElement(MailService.A_DEFAULT);
                    
                    boolean needsReply = e.getAttributeBool(MailService.A_APPT_NEEDS_REPLY, true);
                    String partStatStr = e.getAttribute(MailService.A_APPT_PARTSTAT, "TE");
                                            
                    // <M>
                    Element msgElem = e.getElement(MailService.E_MSG);
                    CalSendData dat = handleMsgElement(octxt, msgElem, acct, mbox, new SetAppointmentInviteParser(uid));
                    
                    int invMsgId = sendDeleteCalendarMessage(octxt, acct, mbox, dat);
                    appt = mbox.getAppointmentByUid(octxt, uid);
                    
                    mbox.modifyInvitePartStat(octxt, appt.getId(), invMsgId, 0, needsReply, partStatStr);
                }
                
                // for each <exception>
                for (Iterator iter = request.elementIterator(MailService.A_EXCEPT); iter.hasNext();) {
                    Element e = (Element)iter.next();
                    
                    Invite inv = appt.getDefaultInvite();
                    
                    boolean needsReply = e.getAttributeBool(MailService.A_APPT_NEEDS_REPLY, true);
                    String partStatStr = e.getAttribute(MailService.A_APPT_PARTSTAT, "TE");
                    
                    if (inv.hasRecurId()) {
                        throw ServiceException.FAILURE("Invite id="+appt.getId()+"-"+inv.getMailItemId()+" comp="+inv.getComponentNum()+" is not the a default invite", null);
                    }
                    
                    if (appt == null) {
                        throw ServiceException.FAILURE("Could not find Appointment for id="+appt.getId()+"-"+inv.getMailItemId()+" comp="+inv.getComponentNum()+">", null);
                    } else if (!appt.isRecurring()) {
                        throw ServiceException.FAILURE("Appointment "+appt.getId()+" is not a recurring appointment", null);
                    }

                    // <M>
                    Element msgElem = e.getElement(MailService.E_MSG);
                    CalSendData dat = handleMsgElement(octxt, msgElem, acct, mbox, 
                            new CreateAppointmentException.CreateApptExceptionInviteParser(appt.getUid(), inv.getTimeZoneMap()));
                    
                    int invMsgId = sendDeleteCalendarMessage(octxt, acct, mbox, dat);
                    
                    mbox.modifyInvitePartStat(octxt, appt.getId(), invMsgId, 0, needsReply, partStatStr);
                    
                }
                
                Element response = lc.createElement(MailService.SET_APPOINTMENT_RESPONSE);
                response.addAttribute(MailService.A_APPT_ID, appt.getId());
                return response;
            } // synchronized(mbox)
        } finally {
            sWatch.stop(startTime);
        }
    }
    
    protected static int sendDeleteCalendarMessage(OperationContext octxt, Account acct, Mailbox mbox, CalSendData dat) throws ServiceException
    {
        int[] folderId = { Mailbox.ID_FOLDER_CALENDAR };
        
        int msgId = sendMimeMessage(octxt, mbox, acct, folderId, dat, dat.mMm, dat.mOrigId, dat.mReplyType);
        
        mbox.delete(octxt, msgId, MailItem.TYPE_MESSAGE);
        
        return msgId;
    }
    
}
