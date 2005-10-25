package com.zimbra.cs.service;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.property.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.mail.SendInviteReply;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.AccountUtil;


public class PublicICalServlet extends ZimbraServlet 
{
    private static Log sLog = LogFactory.getLog(PublicICalServlet.class);
    
    private static final String PARAM_ORG = "org";
    private static final String PARAM_UID = "uid";
    private static final String PARAM_AT = "at"; 
    private static final String PARAM_RECUR = "r";
    private static final String PARAM_VERB = "v";
    private static final String PARAM_INVID = "invId";

    
    public final void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String pathInfo = req.getPathInfo().toLowerCase();
        boolean isReply = pathInfo != null && pathInfo.endsWith("reply");
        
        if (isReply) {
            doReply(req, resp);
        }
    }
    
    public final void doReply(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String org= req.getParameter(PARAM_ORG);
        String uid = req.getParameter(PARAM_UID);
        String at = req.getParameter(PARAM_AT);
        String recur = req.getParameter(PARAM_RECUR);
        String verbStr = req.getParameter(PARAM_VERB);
        String oldInvId = req.getParameter(PARAM_INVID);
        
        if (checkBlankOrNull(resp, PARAM_ORG, org)
                || checkBlankOrNull(resp, PARAM_UID, uid)
                || checkBlankOrNull(resp, PARAM_AT, at) 
                || checkBlankOrNull(resp, PARAM_VERB, verbStr) 
                || checkBlankOrNull(resp, PARAM_INVID, oldInvId)) 
            return;
        
        try {

            Account acct = Provisioning.getInstance().getAccountByName(org);
            if (acct == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Account "+org+" not found");
                return;             
            }
            
            Mailbox mbox = Mailbox.getMailboxByAccountId(acct.getId());
            if (mbox == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "mailbox not found");
                return;             
            }
            
            int apptId;
            int inviteMsgId;
            Appointment appt;
            Invite oldInv;
            
            ItemId iid = new ItemId(oldInvId, null);
            // the user could be accepting EITHER the original-mail-item (id="nnn") OR the
            // appointment (id="aaaa-nnnn") --- work in both cases
            if (iid.hasSubpart()) {
                // directly accepting the appointment
                apptId = iid.getId();
                inviteMsgId = iid.getSubpartId();
                appt = mbox.getAppointmentById(null, apptId); 
                oldInv = appt.getInvite(inviteMsgId, 0);
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid invId: "+oldInvId);
                return;             
            }
            
            if (!appt.getUid().equals(uid)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Wrong UID");
                return;             
            }
            
            SendInviteReply.ParsedVerb verb = SendInviteReply.parseVerb(verbStr);
            String replySubject = SendInviteReply.getReplySubject(verb, oldInv);
            Invite reply = new Invite(Method.REPLY, new TimeZoneMap(oldInv.getTimeZoneMap().getLocalTimeZone()));
            reply.getTimeZoneMap().add(oldInv.getTimeZoneMap());
            
            // ATTENDEE -- send back this attendee with the proper status
            ZAttendee meReply = null;
            ZAttendee me = oldInv.getMatchingAttendee(at);
            if (me != null) {
                meReply = new ZAttendee(me.getAddress());
                meReply.setPartStat(verb.getXmlPartStat());
                meReply.setRole(me.getRole());
                meReply.setCn(me.getCn());
                reply.addAttendee(meReply);
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, at+" not on Attendee list");
                return;             
            }
            
            // DTSTART (outlook seems to require this, even though it shouldn't)
            reply.setDtStart(oldInv.getStartTime());
            
            // ORGANIZER
            reply.setOrganizer(oldInv.getOrganizer());
            
            // UID
            reply.setUid(oldInv.getUid());
                
            // RECURRENCE-ID (if necessary)
            if (recur != null && !recur.equals("")) {
                ParsedDateTime exceptDt = ParsedDateTime.parse(recur, reply.getTimeZoneMap());
                reply .setRecurId(new RecurId(exceptDt, RecurId.RANGE_NONE));
            } else if (oldInv.hasRecurId()) {
                reply.setRecurId(oldInv.getRecurId());
            }
            
            // SEQUENCE        
            reply.setSeqNo(oldInv.getSeqNo());
            
            // DTSTAMP
            // we should pick "now" -- but the dtstamp MUST be >= the one sent by the organizer,
            // so we'll use theirs if it is after "now"...
            Date now = new Date();
            Date dtStampDate = new Date(oldInv.getDTStamp());
            if (now.after(dtStampDate)) {
                dtStampDate = now;
            }
            reply.setDtStamp(dtStampDate.getTime());
            
            
            // SUMMARY
            reply.setName(replySubject);
            
            Calendar iCal = reply.toICalendar();
            
            // send the message via SMTP
            try {
                MimeMessage mm = SendInviteReply.createDefaultReply(at, oldInv, replySubject, verb, iCal);
                
                String replyTo = acct.getAttr(Provisioning.A_zimbraPrefReplyToAddress);
                mm.setFrom(AccountUtil.getOutgoingFromAddress(acct));
                mm.setSentDate(new Date());
                if (replyTo != null && !replyTo.trim().equals(""))
                    mm.setHeader("Reply-To", replyTo);
                mm.saveChanges();
                mm.addRecipient(Message.RecipientType.TO, new InternetAddress(at));
                
                if (mm.getAllRecipients() != null) {
                    Transport.send(mm);
                }
            } catch (MessagingException e) {
                e.printStackTrace();
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Caught exception: "+e);
            } catch (RuntimeException e) {
                e.printStackTrace();
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Caught exception: "+e);
            }
            
            resp.setContentType(Mime.CT_TEXT_PLAIN);

            StringBuffer body = new StringBuffer();
            body.append('\n');
            body.append("Attendee: ").append(at).append(" has replied as ");
            if (verb.equals(SendInviteReply.VERB_ACCEPT)) {
                body.append("ACCEPTED ");
            } else if (verb.equals(SendInviteReply.VERB_DECLINE)) {
                body.append("DECLINED");
            } if (verb.equals(SendInviteReply.VERB_TENTATIVE)) {
                body.append("TENTATIVE");                
            }
            body.append('\n');
            
            resp.getOutputStream().write(body.toString().getBytes());
            
        } catch (ServiceException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Caught exception: "+e);
        } catch (ParseException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Caught exception: "+e);
        }
    }
    
    static boolean checkBlankOrNull(HttpServletResponse resp, String field, String value) throws IOException
    {
        if (value == null || value.equals("")) { 
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, field+" required");
            return true;
        } else {
            return false;
        }
        
    }
    
    public void init() throws ServletException {
        String name = getServletName();
        sLog.info("Servlet " + name + " starting up");
        super.init();
  }

  public void destroy() {
        String name = getServletName();
        sLog.info("Servlet " + name + " shutting down");
        super.destroy();
  }
    
    
    
}
