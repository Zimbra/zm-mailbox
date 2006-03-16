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
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.formatter.*;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.DateUtil;
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
 *          start={time}
 *          end={time}
 *          sync="1"
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
 *                            
 *            {time} = (time in milliseconds) |
 *                     YYYY/dd/mm |
 *                     mm/dd/YYYY |
 *                     [-]nnnn{minute,hour,day,week,month,year}  // relative
 * </pre>
 */

public class UserServlet extends ZimbraServlet {

    public static final String SERVLET_PATH = "/service/user";

    public static final String QP_FMT = "fmt"; // format query param

    public static final String QP_ID = "id"; // id query param

    public static final String QP_PART = "part"; // part query param

    public static final String QP_QUERY = "query"; // query query param

    public static final String QP_VIEW = "view"; // view query param    

    public static final String QP_TYPES = "types"; // types

    public static final String QP_START = "start"; // start time

    public static final String QP_END = "end"; // end time

    public static final String QP_AUTH = "auth"; // auth types

    public static final String AUTH_COOKIE = "co"; // auth by cookie

    public static final String AUTH_BASIC = "ba"; // basic auth

    public static final String AUTH_NO_SET_COOKIE = "nsc"; // don't set auth token cookie after basic auth

    public static final String AUTH_DEFAULT = "co,ba"; // both

    private HashMap<String, Formatter> mFormatters;

    protected static final String MSGPAGE_BLOCK = "errorpage.attachment.blocked";
    private String mBlockPage = null;

    public UserServlet() {
        mFormatters = new HashMap<String, Formatter>();
        addFormatter(new CsvFormatter());
        addFormatter(new VcfFormatter());
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
        return mFormatters.get(type);
    }

    private void getAccount(Context context) throws IOException,
            ServletException, UserServletException {
        try {
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
            if (!checkAuthentication(req, resp, context))
                return;

            if (context.authAccount != null)
                ZimbraLog.addAccountNameToContext(context.authAccount.getName());
            ZimbraLog.addIpToContext(context.req.getRemoteAddr());

            if (context.authAccount == null)
                doUnAuthGet(req, resp, context);
            else
                doAuthGet(req, resp, context);
        } catch (NoSuchItemException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "no such item");
        } catch (ServiceException se) {
            if (se.getCode() == ServiceException.PERM_DENIED)
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, se.getMessage());
            else
                throw new ServletException(se);
        } catch (UserServletException e) {
            // add check for ServiceException root cause?
            resp.sendError(e.getHttpStatusCode(), e.getMessage());
        } finally {
            ZimbraLog.clearContext();
        }
    }

    private boolean checkAuthentication(HttpServletRequest req, HttpServletResponse resp, Context context) throws IOException, ServletException, ServiceException, UserServletException {
        // if they specify /~/, we must auth
        if (context.targetAccount == null && context.accountPath.equals("~")) {
            getAccount(context);
            if (context.authAccount == null)
                return false;
            context.targetAccount = context.authAccount;
        }

        // at this point we must have a target account
        if (context.targetAccount == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "target account not found");

        // auth is required if we haven't yet authed and they've specified a formatter that requires auth (including write ops).
        boolean authRequired = context.authAccount == null &&
                        (context.formatter == null || context.formatter.requiresAuth() || req.getMethod().equalsIgnoreCase("POST"));

        // need this before proxy if we want to support sending cookie from a basic-auth
        if (authRequired) {
            getAccount(context);
            if (context.authAccount == null)
                return false;                
        }

        // this should handle both explicit /user/user-on-other-server/ and
        // /user/~/?id={account-id-on-other-server}:id            
        if (!context.targetAccount.isCorrectHost()) {
            try {
                if (context.basicAuthHappened && context.authTokenCookie == null) 
                    context.authTokenCookie = new AuthToken(context.authAccount).getEncoded();
                proxyServletRequest(req, resp, context.targetAccount.getServer(),
                                    context.basicAuthHappened ? context.authTokenCookie : null);
                return false;
            } catch (ServletException e) {
                throw ServiceException.FAILURE("proxy error", e);
            } catch (AuthTokenException e) {
                throw ServiceException.FAILURE("proxy error", e);
            }
        }

        return true;
    }

    private void doAuthGet(HttpServletRequest req, HttpServletResponse resp, Context context)
    throws ServletException, IOException, ServiceException, UserServletException {
        Mailbox mbox = context.targetMailbox = Mailbox.getMailboxByAccount(context.targetAccount);
        if (mbox == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "mailbox not found");
        ZimbraLog.addToContext(mbox);

        ZimbraLog.mailbox.info("UserServlet: " + context.req.getRequestURL().toString());
        
        context.opContext = new OperationContext(context.authAccount);

        MailItem item = null;
        if (context.itemId != null) {
            item = mbox.getItemById(context.opContext, context.itemId.getId(), MailItem.TYPE_UNKNOWN);
        } else {
            try {
                item = mbox.getFolderByPath(context.opContext, context.itemPath);
            } catch (NoSuchItemException nse) {
                if (context.format == null) {
                    int pos = context.itemPath.lastIndexOf('.');
                    if (pos != -1) {
                        context.format = context.itemPath.substring(pos + 1);
                        context.itemPath = context.itemPath.substring(0, pos);
                        item = mbox.getFolderByPath(context.opContext, context.itemPath);
                    }
                }
                if (item == null)
                    throw nse;
            }
        }

        if (item == null && context.getQueryString() == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "item not found");

        if (context.format == null) {
            context.format = defaultFormat(item, context);
            if (context.format == null)
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "unsupported format");
        }            

        if (item instanceof Mountpoint) {
            // if the target is a mountpoint, proxy the request on to the resolved target
            proxyOnMountpoint(req, resp, context, (Mountpoint) item);
            return;
        }

        if (context.formatter == null)
            context.formatter = mFormatters.get(context.format);
        if (context.formatter == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "unsupported format");

        if (context.formatter.canBeBlocked()) {
            if (Formatter.checkGlobalOverride(Provisioning.A_zimbraAttachmentsBlocked, context.authAccount)) {
                sendbackBlockMessage(context.req, context.resp);
                return;
            }
        }
        context.formatter.format(context, item);
    }

    private void doUnAuthGet(HttpServletRequest req, HttpServletResponse resp, Context context)
    throws ServletException, IOException, ServiceException, UserServletException {
        context.targetMailbox = Mailbox.getMailboxByAccount(context.targetAccount);
        if (context.targetMailbox == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "mailbox not found");

        ZimbraLog.addToContext(context.targetMailbox);
        
        ZimbraLog.mailbox.info("UserServlet: " + context.req.getRequestURL());
        
        // this SUCKS! Need an OperationContext that is for "public/unauth'd" access
        context.opContext = null; // new OperationContext(context.authAccount);

        MailItem item = null;

        if (context.itemId != null) {
            item = context.targetMailbox.getItemById(context.opContext, context.itemId.getId(), MailItem.TYPE_UNKNOWN);
        } else {
            item = context.targetMailbox.getFolderByPath(context.opContext, context.itemPath);
        }
        
        if (item == null && context.getQueryString() == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "item not found");

        if (item instanceof Mountpoint) {
            // if the target is a mountpoint, proxy the request on to the resolved target
            proxyOnMountpoint(req, resp, context, (Mountpoint) item);
            return;
        }

        // UnAuth'd get *MUST* already have formatter set!
        if (context.formatter == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "unsupported format");
        context.formatter.format(context, item);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        try {
            byte[] body = ByteUtil.getContent(req.getInputStream(), req.getContentLength());

            Context context = new Context(req, resp, this);
            if (!checkAuthentication(req, resp, context))
                return;

            if (context.authAccount != null)
                ZimbraLog.addAccountNameToContext(context.authAccount.getName());
            ZimbraLog.addIpToContext(context.req.getRemoteAddr());

            Mailbox mbox = context.targetMailbox = Mailbox.getMailboxByAccount(context.targetAccount);
            if (mbox == null)
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "mailbox not found");
            ZimbraLog.addToContext(mbox);

            ZimbraLog.mailbox.info("UserServlet (POST): " + context.req.getRequestURL().toString());

            context.opContext = new OperationContext(context.authAccount);

            Folder folder = null;
            if (context.itemId != null)
                folder = mbox.getFolderById(context.opContext, context.itemId.getId());
            else
                folder = mbox.getFolderByPath(context.opContext, context.itemPath);

            if (context.format == null) {
                context.format = defaultFormat(folder, context);
                if (context.format == null)
                    throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "unsupported format");
            }

            if (folder instanceof Mountpoint) {
                // if the target is a mountpoint, proxy the request on to the resolved target
                proxyOnMountpoint(req, resp, context, (Mountpoint) folder);
                return;
            }
            
            if (context.formatter == null)
                context.formatter = mFormatters.get(context.format);
            if (context.formatter == null)
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "unsupported format");

            context.formatter.save(body, context, folder);
        } catch (NoSuchItemException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "no such item");
        } catch (ServiceException se) {
            if (se.getCode() == ServiceException.PERM_DENIED)
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, se.getMessage());
            else
                throw new ServletException(se);
        } catch (UserServletException e) {
            // add check for ServiceException root cause?
            resp.sendError(e.getHttpStatusCode(), e.getMessage());
        } finally {
            ZimbraLog.clearContext();
        }
    }

    private void proxyOnMountpoint(HttpServletRequest req, HttpServletResponse resp, Context context, Mountpoint mpt)
    throws IOException, ServletException, ServiceException, UserServletException {
        String uri = SERVLET_PATH + "/~/?" + QP_ID + '=' + mpt.getOwnerId() + "%3A" + mpt.getRemoteId();
        if (context.format != null)
            uri += '&' + QP_FMT + '=' + URLEncoder.encode(context.format, "UTF-8");
        for (Map.Entry entry : (Set<Map.Entry>) req.getParameterMap().entrySet()) {
            String qp = (String) entry.getKey();
            if (!qp.equals(QP_ID) && !qp.equals(QP_FMT))
                uri += '&' + URLEncoder.encode(qp, "UTF-8") + '=' + URLEncoder.encode((String) entry.getValue(), "UTF-8");
        }

        try {
            if (context.basicAuthHappened && context.authTokenCookie == null) 
                context.authTokenCookie = new AuthToken(context.authAccount).getEncoded();
        } catch (AuthTokenException e) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "cannot generate auth token");
        }

        Account targetAccount = Provisioning.getInstance().getAccountById(mpt.getOwnerId());
        if (targetAccount == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "referenced account not found");

        proxyServletRequest(req, resp, targetAccount.getServer(), uri,
                            context.basicAuthHappened ? context.authTokenCookie : null);
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
        public boolean sync;
        public Account authAccount;
        public Account targetAccount;
        public Mailbox targetMailbox;
        public OperationContext opContext;
        private long mStartTime = -2;
        private long mEndTime = -2;

        Context(HttpServletRequest request, HttpServletResponse response, UserServlet servlet)
        throws UserServletException, ServiceException {
            Provisioning prov = Provisioning.getInstance();

            this.req = request;
            this.resp = response;

            String pathInfo = request.getPathInfo().toLowerCase();
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
            this.format = request.getParameter(QP_FMT);
            String id = request.getParameter(QP_ID);
            try {
                this.itemId = id == null ? null : new ItemId(id, null);
            } catch (ServiceException e) {
                throw new UserServletException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "invalid id requested");
            }

            if (this.format != null) {
                this.format = this.format.toLowerCase();
                this.formatter = servlet.getFormatter(this.format);
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

        public long getStartTime() {
            if (mStartTime == -2) {
                String st = req.getParameter(QP_START);
                long defaultStartTime = formatter.getDefaultStartTime();
                mStartTime = (st != null) ? DateUtil.parseDateSpecifier(st, defaultStartTime) : defaultStartTime;
            }
            return mStartTime;
        }

        public long getEndTime() {
            if (mEndTime == -2) {
                String et = req.getParameter(QP_END);
                long defaultEndTime = formatter.getDefaultEndTime();                
                mEndTime = (et != null) ? DateUtil.parseDateSpecifier(et, defaultEndTime) : defaultEndTime;
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

        public boolean hasView() {
            String v = getView();
            return v != null && v.length() > 0;
        }

        public String getView() {
            return req.getParameter(QP_VIEW);
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

    private String defaultFormat(MailItem item, Context context) {
        if (context.hasPart())
            return "native";
        
        byte type = MailItem.TYPE_UNKNOWN;
        if (item instanceof Folder)
            type = ((Folder) item).getDefaultView();
        else if (item != null)
            type = item.getType();

        switch (type) {
            case MailItem.TYPE_APPOINTMENT:
                return "ics";
            case MailItem.TYPE_CONTACT:
                return "csv";
            default:
                return "native";
        }
    }
    
    /**
     * @param req
     * @param resp
     * @throws IOException
     * @throws ServletException
     */
    private void sendbackBlockMessage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(mBlockPage);
        if (dispatcher != null) {
            dispatcher.forward(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "The attachment download has been disabled per security policy.");
    }
    
    public void init() throws ServletException {
        String name = getServletName();
        ZimbraLog.mailbox.info("Servlet " + name + " starting up");
        super.init();
        mBlockPage = getInitParameter(MSGPAGE_BLOCK);
    }

    public void destroy() {
        String name = getServletName();
        ZimbraLog.mailbox.info("Servlet " + name + " shutting down");
        super.destroy();
    }

    public static InputStream getResourceAsStream(AuthToken auth, ItemId iid, Map<String,String> params) throws ServiceException {
        // fetch from remote store
        Server server = Provisioning.getInstance().getAccountById(iid.getAccountId()).getServer();
        int port = server.getIntAttr(Provisioning.A_zimbraMailPort, 0);
        boolean useHTTP = port > 0;
        if (!useHTTP)
            port = server.getIntAttr(Provisioning.A_zimbraMailSSLPort, 0);
        if (port <= 0)
            throw ServiceException.FAILURE("remote server " + server.getName() + " has neither http nor https port enabled", null);
        String hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);

        StringBuffer url = new StringBuffer(useHTTP ? "http" : "https");
        url.append("://").append(hostname).append(':').append(port);
        url.append(SERVLET_PATH).append("/~/?");
        url.append(QP_ID).append('=').append(iid.toString());
        url.append('&').append(QP_AUTH).append('=').append(AUTH_COOKIE);
        if (params != null)
            for (Map.Entry<String, String> param : params.entrySet())
                url.append('&').append(param.getKey()).append('=').append(param.getValue());

        // create an HTTP client with the same cookies
        HttpState state = new HttpState();
        try {
            state.addCookie(new org.apache.commons.httpclient.Cookie(hostname, COOKIE_ZM_AUTH_TOKEN, auth.getEncoded(), "/", null, false));
        } catch (AuthTokenException ate) {
            throw ServiceException.PROXY_ERROR(ate, url.toString());
        }
        HttpClient client = new HttpClient();
        client.setState(state);
        GetMethod get = new GetMethod(url.toString());
        try {
            int statusCode = client.executeMethod(get);
            if (statusCode != HttpStatus.SC_OK)
                throw ServiceException.RESOURCE_UNREACHABLE(get.getStatusText(), null);
            return get.getResponseBodyAsStream();
        } catch (HttpException e) {
            throw ServiceException.RESOURCE_UNREACHABLE(get.getStatusText(), e);
        } catch (IOException e) {
            throw ServiceException.RESOURCE_UNREACHABLE(get.getStatusText(), e);
        }
    }
}
