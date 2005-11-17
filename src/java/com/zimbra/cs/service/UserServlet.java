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
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
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
import com.zimbra.cs.service.formatter.IfbFormatter;
import com.zimbra.cs.service.formatter.NativeFormatter;
import com.zimbra.cs.service.formatter.RssFormatter;
import com.zimbra.cs.service.formatter.SyncFormatter;
import com.zimbra.cs.service.formatter.ZipFormatter;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.ZimbraLog;

/**
 * 
 * <pre>
 *   http://server/service/user/[&tilde;][{username}]/[{folder}]?[{query-params}]
 *          fmt={ics, csv, etc}
 *          id={item-id}
 *          part={mime-part}
 *          query={search-query}
 *          types={types} // when searching
 *          auth={auth-types}
 *          start={start-time}  // milli for now
 *          end={end-time}  // milli for now *          
 *          
 *             {types}   = comma-separated list.  Legal values are:
 *                         conversation|message|contact|appointment|note
 *                         (default is &quot;conversation&quot;)
 *                          
 *             {auth-types} = comma-separated list. Legal values are:
 *                            co     cookie
 *                            ba     basic auth
 *                            nsc    do not set a cookie when using basic auth
 *                            (default is &quot;co,ba&quot;, i.e. check both)
 * </pre>
 */

public class UserServlet extends ZimbraServlet {

    public static final String QP_FMT = "fmt"; // format query param

    public static final String QP_ID = "id"; // id query param

    public static final String QP_PART = "part"; // part query param

    public static final String QP_QUERY = "query"; // query query param

    public static final String QP_TYPES = "types"; // types
    
    public static final String QP_START = "start"; // start time
    
    public static final String QP_END = "end"; // end time

    public static final String QP_AUTH = "auth"; // auth types

    public static final String AUTH_COOKIE = "co"; // auth by cookie

    public static final String AUTH_BASIC = "ba"; // basic auth

    public static final String AUTH_NO_SET_COOKIE = "nsc"; // don't set auth token cookie after basic auth
    
    public static final String AUTH_DEFAULT = "co,ba"; // both

    private HashMap mFormatters;

    public UserServlet() {
        mFormatters = new HashMap();
        addFormatter(new CsvFormatter());
        addFormatter(new IcsFormatter());
        addFormatter(new RssFormatter());
        addFormatter(new AtomFormatter());
        addFormatter(new NativeFormatter());
        addFormatter(new ZipFormatter());
        addFormatter(new IfbFormatter());
        addFormatter(new SyncFormatter());
    }

    private void addFormatter(Formatter f) {
        mFormatters.put(f.getType(), f);
    }

    public Formatter getFormatter(String type) {
        return (Formatter) mFormatters.get(type);
    }

    private void getAccount(Context context) throws IOException,
            ServletException, UserServletException {
        try {
            Provisioning prov = Provisioning.getInstance();

            // check cookie first
            if (context.cookieAuthAllowed()) {
                context.authAccount = cookieAuthRequest(context.req, context.resp, true);
                if (context.authAccount != null)
                    return;
            }

            // fallback to basic auth
            if (context.basicAuthAllowed()) {
                context.authAccount = basicAuthRequest(context.req, context.resp);
                if (context.authAccount != null) {
                    context.basicAuthHappened = true;
                    // send cookie back if need be. 
                    if (!context.noSetCookie()) {
                        try {
                            AuthToken authToken = new AuthToken(context.authAccount);
                            context.authTokenCookie = authToken.getEncoded();
                            context.resp.addCookie(new Cookie(COOKIE_ZM_AUTH_TOKEN, context.authTokenCookie));
                        } catch (AuthTokenException e) {
                        }
                    }
                }
                // always return
                return;
            }

            // when we support unauth'd/public things, change this to create the
            // special "public" account
            throw new UserServletException(HttpServletResponse.SC_UNAUTHORIZED,
                    "need to authenticate");
        } catch (ServiceException e) {
            throw new ServletException(e);
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            Context context = new Context(req, resp, this);

            // if they specify /~/, we must auth
            if (context.targetAccount == null && context.accountPath.equals("~")) {
                getAccount(context);
                if (context.authAccount == null) return;
                context.targetAccount = context.authAccount;
            }

            // at this point we must have a target account
            if (context.targetAccount == null)
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "target account not found");

            // auth is required if we haven't, or if they haven't specified a formatter that doesn't need auth.
            boolean authRequired = context.authAccount == null &&
                            (context.formatter == null || context.formatter.requiresAuth());

            // need this before proxy if we want to support sending cookie from a basic-auth
            if (authRequired) {
                getAccount(context);
                if (context.authAccount == null) return;                
            }

            if (context.authAccount != null)
                ZimbraLog.addAccountNameToContext(context.authAccount.getName());
            ZimbraLog.addIpToContext(context.req.getRemoteAddr());
            
            // this should handle both explicit /user/user-on-other-server/ and
            // /user/~/?id={account-id-on-other-server}:id            
            if (!context.targetAccount.isCorrectHost()) {
                try {
                    if (context.basicAuthHappened && context.authTokenCookie == null) 
                        context.authTokenCookie = new AuthToken(context.authAccount).getEncoded();
                    proxyServletRequest(req, resp, context.targetAccount.getServer(),
                                                context.basicAuthHappened ? context.authTokenCookie : null);
                    return;
                } catch (ServletException e) {
                    throw ServiceException.FAILURE("proxy error", e);
                } catch (AuthTokenException e) {
                    throw ServiceException.FAILURE("proxy error", e);
                }
            }
            if (context.authAccount == null)
                doUnAuthGet(req, resp, context);
            else
                doAuthGet(req, resp, context);
        } catch (NoSuchItemException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "no such item");
        } catch (ServiceException se) {
            throw new ServletException(se);
        } catch (UserServletException e) {
            // add check for ServiceException root cause?
            resp.sendError(e.getHttpStatusCode(), e.getMessage());
        } finally {
            ZimbraLog.clearContext();
        }
    }

    private void doAuthGet(HttpServletRequest req, HttpServletResponse resp,
            Context context) throws ServletException, IOException, ServiceException, UserServletException 
    {
        context.targetMailbox = Mailbox.getMailboxByAccount(context.targetAccount);

        if (context.targetMailbox == null) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                    "mailbox not found");
        }

        ZimbraLog.addToContext(context.targetMailbox);

        ZimbraLog.mailbox.info("UserServlet: "+context.req.getRequestURL().toString());
        
        context.opContext = new OperationContext(context.authAccount);

        MailItem item = null;

        if (context.itemId != null) {
            item = context.targetMailbox.getItemById(context.opContext,
                    context.itemId.getId(), MailItem.TYPE_UNKNOWN);
        } else {
            Folder folder = null;
            try {
                folder = context.targetMailbox.getFolderByPath(
                        context.opContext, context.itemPath);
            } catch (NoSuchItemException nse) {
                if (context.format == null) {
                    int pos = context.itemPath.lastIndexOf('.');
                    if (pos != -1) {
                        context.format = context.itemPath.substring(pos + 1);
                        context.itemPath = context.itemPath.substring(0, pos);
                        folder = context.targetMailbox.getFolderByPath(
                                context.opContext, context.itemPath);
                    }
                }
                if (folder == null)
                    throw nse;
            }
            item = folder;
        }

        if (item == null && context.getQueryString() == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                    "item not found");

        if (context.format == null) {
            context.format = defaultFormat(item);
            if (context.format == null)
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "unsupported format");
        }            

        if (context.formatter == null)
            context.formatter = (Formatter) mFormatters.get(context.format);

        if (context.formatter == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "unsupported format");

        context.formatter.format(context, item);
    }

    private void doUnAuthGet(HttpServletRequest req, HttpServletResponse resp,
            Context context) throws ServletException, IOException,
            ServiceException, UserServletException {
        context.targetMailbox = Mailbox
                .getMailboxByAccount(context.targetAccount);
        if (context.targetMailbox == null) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                    "mailbox not found");
        }

        ZimbraLog.addToContext(context.targetMailbox);
        
        ZimbraLog.mailbox.info("UserServlet: "+context.req.getRequestURL().toString());
        
        // this SUCKS! Need an OperationContext that is for "public/unauth'd" access
        context.opContext = null; // new OperationContext(context.authAccount);

        MailItem item = null;

        if (context.itemId != null) {
            item = context.targetMailbox.getItemById(context.opContext, context.itemId.getId(), MailItem.TYPE_UNKNOWN);
        } else {
            Folder folder = context.targetMailbox.getFolderByPath(context.opContext, context.itemPath);
            item = folder;
        }

        if (item == null && context.getQueryString() == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                    "item not found");

        // UnAuth'd get *MUST* already have formatter set!
        if (context.formatter == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "unsupported format");
        context.formatter.format(context, item);
    }

    public static class Context {
        public HttpServletRequest req;
        public HttpServletResponse resp;
        public String format;
        public Formatter formatter;        
        public boolean basicAuthHappened;
        public String accountPath;
        public String authTokenCookie;
        public String itemPath;
        public ItemId itemId;
        public Account authAccount;
        public Account targetAccount;
        public Mailbox targetMailbox;
        public OperationContext opContext;
        private long mStartTime = -2;
        private long mEndTime = -2;

        Context(HttpServletRequest req, HttpServletResponse resp, UserServlet servlet)
                throws IOException, UserServletException, ServiceException {
            
            Provisioning prov = Provisioning.getInstance();
            
            this.req = req;
            this.resp = resp;

            String pathInfo = req.getPathInfo().toLowerCase();
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("")
                    || !pathInfo.startsWith("/")) {
                throw new UserServletException(
                        HttpServletResponse.SC_BAD_REQUEST, "invalid path");
            }

            int pos = pathInfo.indexOf('/', 1);
            if (pos == -1) {
                throw new UserServletException(
                        HttpServletResponse.SC_BAD_REQUEST, "invalid path");
            }
            this.accountPath = pathInfo.substring(1, pos);

            this.itemPath = pathInfo.substring(pos + 1);
            this.format = req.getParameter(QP_FMT);
            String id = req.getParameter(QP_ID);
            try {
                this.itemId = id == null ? null : new ItemId(id, null);
            } catch (ServiceException e) {
                throw new UserServletException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "invalid id requested");
            }

            if (this.format != null) {
                this.format = this.format.toLowerCase();
                this.formatter = (Formatter) servlet.getFormatter(this.format);
                if (this.formatter == null)
                    throw new UserServletException(
                            HttpServletResponse.SC_NOT_IMPLEMENTED,
                            "not implemented yet");
            }                

            // see if we can get target account or not
            if (itemId != null && itemId.getAccountId() != null) {
                targetAccount = prov.getAccountById(itemId.getAccountId());
                return;
            } else if (accountPath.equals("~")) {
                // can't resolve this yet
                return;
            } else if (accountPath.startsWith("~")) {
                accountPath = accountPath.substring(1);
            }
            targetAccount = prov.getAccountByName(accountPath);                
        }

        // eventually get this from query param ?start=long|YYYYMMMDDHHMMSS
        public long getStartTime() {
            if (mStartTime == -2) {
                String st = req.getParameter(QP_START);
                mStartTime = (st != null) ? Long.parseLong(st) : -1;
            }
            return mStartTime;
        }

        // eventually get this from query param ?end=long|YYYYMMMDDHHMMSS
        public long getEndTime() {
            if (mEndTime == -2) {
                String et = req.getParameter(QP_END);
                mEndTime = (et != null) ? Long.parseLong(et) : -1;                
            }
            return mEndTime;
        }

        public String getQueryString() {
            return req.getParameter(QP_QUERY);
        }

        public boolean cookieAuthAllowed() {
            return getAuth().indexOf(AUTH_COOKIE) != -1;
        }

        public boolean noSetCookie() {
            return getAuth().indexOf(AUTH_NO_SET_COOKIE) != -1;
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
            sb.append("account(" + accountPath + ")\n");
            sb.append("itemPath(" + itemPath + ")\n");
            sb.append("foramt(" + format + ")\n");
            return sb.toString();
        }
    }

    protected String getRealmHeader() {
        return "BASIC realm=\"Zimbra\"";
    }

    private String defaultFormat(MailItem item) {
        int type = (item instanceof Folder) ? ((Folder) item).getDefaultView()
                : (item != null ? item.getType() : MailItem.TYPE_UNKNOWN);
        switch (type) {
        case MailItem.TYPE_APPOINTMENT:
            return "ics";
        case MailItem.TYPE_CONTACT:
            return "csv";
        default:
            return "native";
        }
    }

}
