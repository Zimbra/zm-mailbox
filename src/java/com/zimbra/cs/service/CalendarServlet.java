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

package com.zimbra.cs.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ValidationException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.InviteInfo;
import com.zimbra.cs.service.mail.CalendarUtils;
import com.zimbra.cs.util.Constants;
import com.zimbra.soap.Element;

/**
 * simple iCal servlet on a mailbox. URL is:
 * 
 *  http://server/service/ical/cal.ics
 *  
 *
 */

public class CalendarServlet extends ZimbraBasicAuthServlet {

    public CalendarServlet() {
        mAllowCookieAuth = true;
    }

    protected String getRealmHeader()  { return "BASIC realm=\"Zimbra iCal\""; }

    public void doAuthGet(HttpServletRequest req, HttpServletResponse resp, Account acct, Mailbox mailbox)
    throws ServiceException, IOException {
        String pathInfo = req.getPathInfo().toLowerCase();

        if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("")) pathInfo = "/calendar.ics";
        
        //ZimbraLog.calendar.info("pathInfo = "+pathInfo);

        String folderPath = getFolderPath(pathInfo);
        
        //ZimbraLog.calendar.info("folderPath = "+folderPath);
        
        Folder folder = mailbox.getFolderByPath(null, folderPath);

        boolean isRss = pathInfo != null && pathInfo.endsWith("rss");
        
        if (pathInfo.endsWith(".rss")) {
            long start = System.currentTimeMillis() - (7 * Constants.MILLIS_PER_DAY);
            long end = start + (14 * Constants.MILLIS_PER_DAY);            
            doRss(req, resp, acct, mailbox, start, end, folder.getId());
        } else {
            long start = 0;
            //long start = System.currentTimeMillis() - (7 * Constants.MILLIS_PER_DAY);
            long end = System.currentTimeMillis() + (365 * 100 * Constants.MILLIS_PER_DAY);
            doIcal(req, resp, acct, mailbox, start, end, folder.getId());            
        }
    }

    private String getFolderPath(String pathInfo) {
        if (pathInfo.endsWith(".ics")) return pathInfo.substring(0, pathInfo.length()-4);        
        else if (pathInfo.endsWith(".rss")) return pathInfo.substring(0, pathInfo.length()-4);
        else return pathInfo;
    }

    private void doIcal(HttpServletRequest req, HttpServletResponse resp, Account acct, Mailbox mailbox, long start, long end, int folderId)
    throws ServiceException, IOException {
        resp.setContentType("text/calendar");

        try {
            Calendar cal = mailbox.getCalendarForRange(null, start, end, folderId);
            
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            CalendarOutputter calOut = new CalendarOutputter();
            calOut.output(cal, buf);            
            resp.getOutputStream().write(buf.toByteArray());
        } catch (ValidationException e) {
            throw ServiceException.FAILURE("For account:"+acct.getName()+" mbox:"+mailbox.getId()+" unable to get calendar "+e, e);
        }
    }

    private void doRss(HttpServletRequest req, HttpServletResponse resp, Account acct, Mailbox mailbox, long start, long end, int folderId)
    throws ServiceException, IOException
    {
        resp.setContentType("application/rss+xml");
            
        StringBuffer sb = new StringBuffer();

        sb.append("<?xml version=\"1.0\"?>");
            
        Element.XMLElement rss = new Element.XMLElement("rss");
        rss.addAttribute("version", "2.0");

        Element channel = rss.addElement("channel");
        channel.addElement("title").setText("Zimbra Mail: " + acct.getName());
            
        channel.addElement("generator").setText("Zimbra RSS Feed Servlet");

        OperationContext octxt = new OperationContext(acct);            
        Collection appts = mailbox.getAppointmentsForRange(octxt, start, end, folderId, null);
                
        //channel.addElement("description").setText(query);

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
//        MailDateFormat mdf = new MailDateFormat();
        for (Iterator apptIt = appts.iterator(); apptIt.hasNext(); ) {            
            Appointment appt = (Appointment) apptIt.next();

            Collection instances = appt.expandInstances(start, end);
            for (Iterator instIt = instances.iterator(); instIt.hasNext(); ) {
                Appointment.Instance inst = (Appointment.Instance) instIt.next();
                InviteInfo invId = inst.getInviteInfo();
                Invite inv = appt.getInvite(invId.getMsgId(), invId.getComponentId());
                Element item = channel.addElement("item");
                item.addElement("title").setText(inv.getName());
                item.addElement("pubDate").setText(sdf.format(new Date(inst.getStart())));
                /*                
                StringBuffer desc = new StringBuffer();
                sb.append("Start: ").append(sdf.format(new Date(inst.getStart()))).append("\n");
                sb.append("End: ").append(sdf.format(new Date(inst.getEnd()))).append("\n");
                sb.append("Location: ").append(inv.getLocation()).append("\n");
                sb.append("Notes: ").append(inv.getFragment()).append("\n");
                item.addElement("description").setText(sb.toString());
                */
                item.addElement("description").setText(inv.getFragment());
                item.addElement("author").setText(CalendarUtils.paramVal(inv.getOrganizer(), Parameter.CN));
                /* TODO: guid, links, etc */
                //Element guid = item.addElement("guid");
                //guid.setText(appt.getUid()+"-"+inv.getStartTime().getUtcTime());
                //guid.addAttribute("isPermaLink", "false");
            }                    
        }
        sb.append(rss.toString());
        resp.getOutputStream().write(sb.toString().getBytes());
    }
}
