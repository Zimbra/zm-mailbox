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

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.formatter.CsvFormatter;
import com.zimbra.cs.service.formatter.Formatter;
import com.zimbra.cs.service.formatter.IcsFormatter;
import com.zimbra.cs.service.formatter.RssFormatter;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.ZimbraLog;

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
        int type = (item instanceof Folder) ? ((Folder)item).getDefaultView() : (item != null ? item.getType() : MailItem.TYPE_UNKNOWN);
        switch (type) {
        case MailItem.TYPE_APPOINTMENT: 
            return "ics";
        case MailItem.TYPE_CONTACT:
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

        if (item == null && context.getQueryString() == null) {
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
}
