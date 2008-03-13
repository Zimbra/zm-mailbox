/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.fb.FreeBusy;
import com.zimbra.cs.fb.FreeBusyProvider;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.servlet.ZimbraServlet;


public class PublicICalServlet extends ZimbraServlet {
    private static final long serialVersionUID = -7350146465570984660L;

    private static Log sLog = LogFactory.getLog(PublicICalServlet.class);

    private static final long MAX_PERIOD_SIZE_IN_DAYS = 200;


    public final void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        ZimbraLog.clearContext();
        String pathInfo = req.getPathInfo().toLowerCase();
        boolean isReply = pathInfo != null && pathInfo.endsWith("reply");
        boolean isFreeBusy = pathInfo != null && pathInfo.endsWith("freebusy.ifb");

        if (isReply) {
            doReply(req, resp);
        } else if (isFreeBusy) {
            doGetFreeBusy(req, resp);
        }
    }

    /**
     * 
     * http://localhost:7070/service/pubcal/freebusy.ifb?acct=user@host.com
     * 
     * @param req
     * @param resp
     * @throws IOException
     * @throws ServletException
     */
    public final void doGetFreeBusy(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String acctName = req.getParameter("acct");
        String startStr = req.getParameter("s");
        String endStr = req.getParameter("e");

        resp.setContentType(Mime.CT_TEXT_CALENDAR);

        if (checkBlankOrNull(resp, "acct", acctName))
            return;

        long now = new Date().getTime();

        long rangeStart = now - Constants.MILLIS_PER_MONTH;
        long rangeEnd = now + (2 * Constants.MILLIS_PER_MONTH); 

        if (startStr != null)
            rangeStart = Long.parseLong(startStr);

        if (endStr != null)
            rangeEnd = Long.parseLong(endStr);

        if (rangeEnd < rangeStart) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "End time must be after Start time");
            return;
        }

        long days = (rangeEnd - rangeStart) / Constants.MILLIS_PER_DAY;
        if (days > MAX_PERIOD_SIZE_IN_DAYS) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Requested range is too large (max " + MAX_PERIOD_SIZE_IN_DAYS + " days)");
            return;
        }

        try {
            Account acct = Provisioning.getInstance().get(AccountBy.name, acctName);
            FreeBusy fb = null;
            String email = null;
            if (acct == null) {
            	ArrayList<String> ids = new ArrayList<String>();
            	ids.add(acctName);
            	rangeStart = now - Constants.MILLIS_PER_WEEK;  // exchange doesn't like start date being now - 1 month.
        		Calendar cal = GregorianCalendar.getInstance();
        		cal.setTimeInMillis(rangeStart);
        		cal.set(Calendar.MINUTE, 0);
        		cal.set(Calendar.SECOND, 0);
        		rangeStart = cal.getTimeInMillis();
        		cal.setTimeInMillis(rangeEnd);
        		cal.set(Calendar.MINUTE, 0);
        		cal.set(Calendar.SECOND, 0);
        		rangeEnd = cal.getTimeInMillis();
            	List<FreeBusy> fblist = FreeBusyProvider.getRemoteFreeBusy(null, ids, rangeStart, rangeEnd);
            	fb = fblist.get(0);
            	email = acctName;
            } else if (!Provisioning.onLocalServer(acct)) {
                // request was sent to incorrect server, so proxy to the right one
                proxyServletRequest(req, resp, Provisioning.getInstance().getServer(acct), null);
                return;
            } else {
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(acct.getId());
                if (mbox == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "mailbox not found");
                    return;             
                }

                fb = mbox.getFreeBusy(rangeStart, rangeEnd);
                email = mbox.getAccount().getName();
            }

            String url = req.getRequestURL() + "?" + req.getQueryString();
            String fbMsg = fb.toVCalendar(FreeBusy.Method.PUBLISH, email, null, url);
            resp.getOutputStream().write(fbMsg.getBytes());

        } catch (ServiceException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Caught exception: "+e);
        }
    }

    public final void doReply(HttpServletRequest req, HttpServletResponse resp) {
//        String org= req.getParameter(PARAM_ORG);
//        String uid = req.getParameter(PARAM_UID);
//        String at = req.getParameter(PARAM_AT);
//        String recur = req.getParameter(PARAM_RECUR);
//        String verbStr = req.getParameter(PARAM_VERB);
//        String oldInvId = req.getParameter(PARAM_INVID);
//        
//        if (checkBlankOrNull(resp, PARAM_ORG, org)
//                || checkBlankOrNull(resp, PARAM_UID, uid)
//                || checkBlankOrNull(resp, PARAM_AT, at) 
//                || checkBlankOrNull(resp, PARAM_VERB, verbStr) 
//                || checkBlankOrNull(resp, PARAM_INVID, oldInvId)) 
//            return;
//        
//        try {
//
//            Account acct = Provisioning.getInstance().getAccountByName(org);
//            if (acct == null) {
//                 resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Account "+org+" not found");
//                return;             
//            }
//            
//            Mailbox mbox = Mailbox.getMailboxByAccountId(acct.getId());
//            if (mbox == null) {
//                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "mailbox not found");
//                return;             
//            }
//            
//            int calItemId;
//            int inviteMsgId;
//            CalendarItem calItem;
//            Invite oldInv;
//            
//            ItemId iid = new ItemId(oldInvId, null);
//            // the user could be accepting EITHER the original-mail-item (id="nnn") OR the
//            // calendar item (id="aaaa-nnnn") --- work in both cases
//            if (iid.hasSubpart()) {
//                // directly accepting the calendar item
//                calItemId = iid.getId();
//                inviteMsgId = iid.getSubpartId();
//                calItem = mbox.getCalendarItemById(null, calItemId);
//                oldInv = calItem.getInvite(inviteMsgId, 0);
//            } else {
//                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid invId: "+oldInvId);
//                return;             
//            }
//            
//            if (!calItem.getUid().equals(uid)) {
//                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Wrong UID");
//                return;             
//            }
//            
//            SendInviteReply.ParsedVerb verb = SendInviteReply.parseVerb(verbStr);
//            String replySubject = SendInviteReply.getReplySubject(verb, oldInv);
//            Invite reply = new Invite(Method.REPLY, new TimeZoneMap(oldInv.getTimeZoneMap().getLocalTimeZone()));
//            reply.getTimeZoneMap().add(oldInv.getTimeZoneMap());
//            
//            // ATTENDEE -- send back this attendee with the proper status
//            ZAttendee meReply = null;
//            ZAttendee me = oldInv.getMatchingAttendee(at);
//            if (me != null) {
//                meReply = new ZAttendee(me.getAddress());
//                meReply.setPartStat(verb.getXmlPartStat());
//                meReply.setRole(me.getRole());
//                meReply.setCn(me.getCn());
//                reply.addAttendee(meReply);
//            } else {
//                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, at+" not on Attendee list");
//                return;             
//            }
//            
//            // DTSTART (outlook seems to require this, even though it shouldn't)
//            reply.setDtStart(oldInv.getStartTime());
//            
//            // ORGANIZER
//            reply.setOrganizer(oldInv.getOrganizer());
//            
//            // UID
//            reply.setUid(oldInv.getUid());
//                
//            // RECURRENCE-ID (if necessary)
//            if (recur != null && !recur.equals("")) {
//                ParsedDateTime exceptDt = ParsedDateTime.parse(recur, reply.getTimeZoneMap());
//                reply .setRecurId(new RecurId(exceptDt, RecurId.RANGE_NONE));
//            } else if (oldInv.hasRecurId()) {
//                reply.setRecurId(oldInv.getRecurId());
//            }
//            
//            // SEQUENCE        
//            reply.setSeqNo(oldInv.getSeqNo());
//            
//            // DTSTAMP
//            // we should pick "now" -- but the dtstamp MUST be >= the one sent by the organizer,
//            // so we'll use theirs if it is after "now"...
//            Date now = new Date();
//            Date dtStampDate = new Date(oldInv.getDTStamp());
//            if (now.after(dtStampDate)) {
//                dtStampDate = now;
//            }
//            reply.setDtStamp(dtStampDate.getTime());
//            
//            
//            // SUMMARY
//            reply.setName(replySubject);
//            
//            Calendar iCal = reply.toICalendar();
//            
//            // send the message via SMTP
//            try {
//                MimeMessage mm = SendInviteReply.createDefaultReply(at, oldInv, replySubject, verb, iCal);
//                
//                String replyTo = acct.getAttr(Provisioning.A_zimbraPrefReplyToAddress);
//                mm.setFrom(AccountUtil.getOutgoingFromAddress(acct));
//                mm.setSentDate(new Date());
//                if (replyTo != null && !replyTo.trim().equals(""))
//                    mm.setHeader("Reply-To", replyTo);
//                mm.saveChanges();
//                mm.addRecipient(Message.RecipientType.TO, new InternetAddress(at));
//                
//                if (mm.getAllRecipients() != null) {
//                    Transport.send(mm);
//                }
//            } catch (MessagingException e) {
//                e.printStackTrace();
//                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Caught exception: "+e);
//            } catch (RuntimeException e) {
//                e.printStackTrace();
//                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Caught exception: "+e);
//            }
//            
//            resp.setContentType(Mime.CT_TEXT_PLAIN);
//
//            StringBuffer body = new StringBuffer();
//            body.append('\n');
//            body.append("Attendee: ").append(at).append(" has replied as ");
//            if (verb.equals(SendInviteReply.VERB_ACCEPT)) {
//                body.append("ACCEPTED ");
//            } else if (verb.equals(SendInviteReply.VERB_DECLINE)) {
//                body.append("DECLINED");
//            } if (verb.equals(SendInviteReply.VERB_TENTATIVE)) {
//                body.append("TENTATIVE");                
//            }
//            body.append('\n');
//            
//            resp.getOutputStream().write(body.toString().getBytes());
//            
//        } catch (ServiceException e) {
//            e.printStackTrace();
//            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Caught exception: "+e);
//        } catch (ParseException e) {
//            e.printStackTrace();
//            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Caught exception: "+e);
//        }
    }

    static boolean checkBlankOrNull(HttpServletResponse resp, String field, String value) throws IOException {
        if (value == null || value.equals("")) { 
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, field + " required");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void init() throws ServletException {
        String name = getServletName();
        sLog.info("Servlet " + name + " starting up");
        super.init();
    }

    @Override
    public void destroy() {
        String name = getServletName();
        sLog.info("Servlet " + name + " shutting down");
        super.destroy();
    }
}
