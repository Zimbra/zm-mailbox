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
import com.zimbra.cs.service.formatter.AtomFormatter;
import com.zimbra.cs.service.formatter.CsvFormatter;
import com.zimbra.cs.service.formatter.Formatter;
import com.zimbra.cs.service.formatter.IcsFormatter;
import com.zimbra.cs.service.formatter.NativeFormatter;
import com.zimbra.cs.service.formatter.RssFormatter;
import com.zimbra.cs.servlet.ZimbraServlet;

/**
 * 
 * <pre>
 *  http://server/service/user/[~][{username}]/[{folder}]?[{query-params}]
 *         fmt={ics, csv, etc}
 *         id={item-id}
 *         part={mime-part}
 *         query={search-query}
 *         types={types} // when searching
 *         auth={auth-types}
 *         
 *            {types}   = comma-separated list.  Legal values are:
 *                        conversation|message|contact|appointment|note
 *                        (default is "conversation")
 *                         
 *            {auth-types} = comma-separated list. Legal values are:
 *                           co     cookie
 *                           ba     basic auth
 *                           (default is "co,ba", i.e. check both)
 *  </pre>
 */

public class UserServlet extends ZimbraServlet {

    public static final String QP_FMT = "fmt"; // format query param
    public static final String QP_ID = "id"; // id query param
    public static final String QP_PART = "part"; // part query param    
    public static final String QP_QUERY = "query"; // query query param    
    public static final String QP_TYPES = "types"; // types 
    public static final String QP_AUTH = "auth"; // auth types
    
    public static final String AUTH_COOKIE = "co"; // auth by cookie
    public static final String AUTH_BASIC = "ba"; // basic auth
    public static final String AUTH_DEFAULT = "co,ba"; // both

    private HashMap mFormatters;

    public UserServlet() {
        mFormatters = new HashMap();
        addFormatter(new CsvFormatter());
        addFormatter(new IcsFormatter());
        addFormatter(new RssFormatter());
        addFormatter(new AtomFormatter());
        addFormatter(new NativeFormatter());
    }

    private void addFormatter(Formatter f) { mFormatters.put(f.getType(), f); }    

    private Account getAccount(HttpServletRequest req, HttpServletResponse resp, Context context) throws IOException, ServletException, UserServletException
    {
        try {        
            Provisioning prov = Provisioning.getInstance();
            Account acct = null;

            // check cookie first
            if (context.cookieAuthAllowed()) {
                acct = cookieAuthRequest(req, resp, true);
                if (acct != null) return acct;
            }

            // fallback to basic auth        
            if (context.basicAuthAllowed()) {
                acct = basicAuthRequest(req, resp);
                // always return
                return acct;
            }
            
            throw new UserServletException(HttpServletResponse.SC_UNAUTHORIZED, "need to authenticate");

        } catch (ServiceException e) {
            throw new ServletException(e);
        }        
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        try {
            Context context = new Context(req, resp);
            Account acct = getAccount(req, resp, context);
            if (acct == null) return;

            context.setAccount(acct);

            if (!context.targetAccount.isCorrectHost()) {
                try {
                    proxyServletRequest(req, resp, context.targetAccount.getServer());
                    return;
                } catch (ServletException e) {
                    throw ServiceException.FAILURE("proxy error", e);
                }
            }
            
            doAuthGet(req, resp, context);
        } catch (NoSuchItemException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "no such item");
        } catch (ServiceException se) {
            throw new ServletException(se);
        } catch (UserServletException e) {
            // add check for ServiceException root cause?
            resp.sendError(e.getHttpStatusCode(), e.getMessage());
        }
    }
    
    private void doAuthGet(HttpServletRequest req, HttpServletResponse resp, Context context)
        throws ServletException, IOException, ServiceException, UserServletException 
    {
        context.targetMailbox = Mailbox.getMailboxByAccount(context.targetAccount);
        if (context.targetMailbox == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "mailbox not found");
            return;             
        }
        context.opContext = new OperationContext(context.acct);

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
        
        public Context(HttpServletRequest req, HttpServletResponse resp) throws IOException, UserServletException {
            this.req = req;
            this.resp = resp;

            String pathInfo = req.getPathInfo().toLowerCase();        
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("") || !pathInfo.startsWith("/")) {
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "invalid path");
            }
                
            int pos = pathInfo.indexOf('/', 1);
            if (pos == -1) {
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "invalid path");
            }
            this.accountPath = pathInfo.substring(1, pos);

            this.itemPath = pathInfo.substring(pos+1);
            this.format = req.getParameter(QP_FMT);
            String id = req.getParameter(QP_ID);
            this.itemId = id == null ? -1 : Integer.parseInt(id);
            if (this.format != null) this.format = this.format.toLowerCase();
        }            

        public void setAccount(Account acct) throws IOException, ServiceException, UserServletException {
            this.acct = acct;
            Provisioning prov = Provisioning.getInstance();
            if (accountPath.equals("~")) {
                accountPath = acct.getName();
            } else if (accountPath.startsWith("~")) {
                accountPath = accountPath.substring(1);
            }
            
            targetAccount = prov.getAccountByName(accountPath);

            if (targetAccount == null)
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "target account not found");
        }

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

        public boolean cookieAuthAllowed() {
            return getAuth().indexOf(AUTH_COOKIE) != -1;
        }

        public boolean basicAuthAllowed() {
            return getAuth().indexOf(AUTH_BASIC) != -1;
        }

        public String getAuth() {
            String a = req.getParameter(QP_AUTH);
            return (a == null || a.length() == 0) ? AUTH_DEFAULT : a;
        }

        public boolean hasPart() {
            String p = getPart();
            return p != null && p.length() > 0;
        }

        public String getPart() {
            return req.getParameter(QP_PART);
        }

        public String getTypesString() {
            return req.getParameter(QP_TYPES);
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
    
    private String defaultFormat(MailItem item) {
        int type = (item instanceof Folder) ? ((Folder)item).getDefaultView() : (item != null ? item.getType() : MailItem.TYPE_UNKNOWN);
        switch (type) {
        case MailItem.TYPE_APPOINTMENT: 
            return "ics";
        case MailItem.TYPE_CONTACT:
            return "csv";
        default : 
            return "native";
        }
    }

}
