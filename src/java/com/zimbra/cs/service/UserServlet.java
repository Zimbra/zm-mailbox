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

package com.zimbra.cs.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ValidationException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.InviteInfo;
import com.zimbra.cs.service.formatter.CsvFormatter;
import com.zimbra.cs.service.formatter.Formatter;
import com.zimbra.cs.service.formatter.IcsFormatter;
import com.zimbra.cs.service.formatter.RssFormatter;
import com.zimbra.cs.service.mail.CalendarUtils;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.Constants;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.Element;

/**
 * simple iCal servlet on a mailbox. URL is:
 * 
 *  http://server/service/ical/cal.ics
 *  
 *
 */

public class UserServlet extends ZimbraServlet {

    public static final String QP_FMT = "fmt"; // format query param
    public static final String QP_ID = "id"; // id query param
    public static final String QP_QUERY = "query"; // id query param    

    private HashMap mFormatters;

    public UserServlet() {
        mFormatters = new HashMap();
        addFormatter(new CsvFormatter());
        addFormatter(new IcsFormatter());
        addFormatter(new RssFormatter());
    }

    private void addFormatter(Formatter f) { mFormatters.put(f.getType(), f); }    
    
    public static class Context {
        public HttpServletRequest req;
        public HttpServletResponse resp;
        public String format;
        public String accountPath;
        public String itemPath;
        public int itemId;        
        public Account acct;
        public Account targetAccount;
        public Mailbox targetMailbox;
        public OperationContext opContext;
        private long mStartTime = -1;
        private long mEndTime = -1;
        
        // eventually get this from query param ?start=long|YYYYMMMDDHHMMSS
        public long getStartTime() {
            if (mStartTime == -1) {
                //
            }
            return mStartTime;
        }

        // eventually get this from query param ?end=long|YYYYMMMDDHHMMSS
        public long getEndTime() {
            if (mEndTime == -1) {
                // query param
            }
            return mEndTime;
        }

        public String getQueryString() {
            return req.getParameter(QP_QUERY);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("account("+accountPath+")\n");
            sb.append("itemPath("+itemPath+")\n");
            sb.append("foramt("+format+")\n");            
            return sb.toString();
        }
    }
    
    protected String getRealmHeader()  { return "BASIC realm=\"Zimbra\""; }
    
    private Context initContext(HttpServletRequest req, HttpServletResponse resp, Account acct) throws IOException, ServiceException {
        Context c = new Context();
        c.req = req;
        c.resp = resp;

        String pathInfo = req.getPathInfo().toLowerCase();        
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("") || !pathInfo.startsWith("/")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid path");
            return null;
        }
        
        int pos = pathInfo.indexOf('/', 1);
        if (pos == -1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid path");
            return null;
        }
        c.accountPath = pathInfo.substring(1, pos);

        c.itemPath = pathInfo.substring(pos+1);
        c.format = req.getParameter(QP_FMT);
        String id = req.getParameter(QP_ID);
        c.itemId = id == null ? -1 : Integer.parseInt(id);
        if (c.format != null) c.format = c.format.toLowerCase();

        Provisioning prov = Provisioning.getInstance();
        if (c.accountPath.equals("~")) {
            c.accountPath = acct.getName();
        } else if (c.accountPath.startsWith("~")) {
            c.accountPath = c.accountPath.substring(1);
        }
        
        c.targetAccount = prov.getAccountByName(c.accountPath);

        if (c.targetAccount == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "mailbox not found");
            return null;
        }

        if (!c.targetAccount.isCorrectHost()) {
            try {
                proxyServletRequest(req, resp, c.targetAccount.getServer());
                return null;
            } catch (ServletException e) {
                throw ServiceException.FAILURE("proxy error", e);
            }
        }
        return c;
    }
    
    private String defaultFormat(MailItem item) {
        int type = (item instanceof Folder) ? ((Folder)item).getDefaultView() : item.getType();
        switch (type) {
        case Folder.TYPE_APPOINTMENT: 
            return "ics";
        case Folder.TYPE_CONTACT:
            return "csv";
        default : 
            return null;
        }
    }

    private Account getAccount(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException
    {
        try {        
            Provisioning prov = Provisioning.getInstance();
            Account acct = null;

            // check cookie first
            acct = cookieAuthRequest(req, resp, true);
            // fallback to basic auth        
            if (acct == null) acct = basicAuthRequest(req, resp);
            return acct;            
        } catch (ServiceException e) {
            throw new ServletException(e);
        }        
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        try {
            Account acct = getAccount(req, resp);
            if (acct == null) return;
            doAuthGet(req, resp, acct);
        } catch (ServiceException se) {
            throw new ServletException(se);
        }
    }
    
    private void doAuthGet(HttpServletRequest req, HttpServletResponse resp, Account acct)
        throws ServletException, IOException, ServiceException 
    {
        Context context = initContext(req, resp, acct);
        if (context == null) return;

        ZimbraLog.calendar.info("context = "+context);

        context.targetMailbox = Mailbox.getMailboxByAccount(context.targetAccount);
        if (context.targetMailbox == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "mailbox not found");
            return;             
        }

        context.opContext = new OperationContext(acct);

        MailItem item = null;
        
        if (context.itemId != -1) {
            item = context.targetMailbox.getItemById(context.opContext, context.itemId, MailItem.TYPE_UNKNOWN);
        } else {
            Folder folder = null;
            try {
                folder = context.targetMailbox.getFolderByPath(context.opContext, context.itemPath);
            } catch (NoSuchItemException nse) {
                if (context.format == null) {
                    int pos = context.itemPath.lastIndexOf('.');
                    if (pos != -1) {
                        context.format = context.itemPath.substring(pos+1);                  
                        context.itemPath = context.itemPath.substring(0, pos);
                        folder = context.targetMailbox.getFolderByPath(context.opContext, context.itemPath);                  
                    }
                }
                if (folder == null) throw nse;
            }
            item = folder;
        }

        if (item == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "item not found");
            return;
        }
        if (context.format == null) context.format = defaultFormat(item);

        if (context.format == null) {
            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "not implemented yet");
            return;
        }

        Formatter formatter = (Formatter) mFormatters.get(context.format);
        if (formatter == null) {
            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "not implemented yet");
            return;
        }
        


        formatter.format(context, item);
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
