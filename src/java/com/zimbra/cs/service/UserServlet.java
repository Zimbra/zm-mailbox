/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ZimbraAuthTokenEncoded;
import com.zimbra.cs.fb.FreeBusyQuery;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.formatter.AtomFormatter;
import com.zimbra.cs.service.formatter.ContactFolderFormatter;
import com.zimbra.cs.service.formatter.CsvFormatter;
import com.zimbra.cs.service.formatter.Formatter;
import com.zimbra.cs.service.formatter.FreeBusyFormatter;
import com.zimbra.cs.service.formatter.HtmlFormatter;
import com.zimbra.cs.service.formatter.IcsFormatter;
import com.zimbra.cs.service.formatter.IfbFormatter;
import com.zimbra.cs.service.formatter.JsonFormatter;
import com.zimbra.cs.service.formatter.NativeFormatter;
import com.zimbra.cs.service.formatter.RssFormatter;
import com.zimbra.cs.service.formatter.SyncFormatter;
import com.zimbra.cs.service.formatter.TarFormatter;
import com.zimbra.cs.service.formatter.TgzFormatter;
import com.zimbra.cs.service.formatter.VcfFormatter;
import com.zimbra.cs.service.formatter.WikiFormatter;
import com.zimbra.cs.service.formatter.XmlFormatter;
import com.zimbra.cs.service.formatter.ZipFormatter;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.servlet.ZimbraServlet;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * 
 * <pre>
 *   http://server/service/home/[&tilde;][{username}]/[{folder}]?[{query-params}]
 *          fmt={ics, csv, etc}
 *          id={item-id}
 *          imap_id={item-imap-id}
 *          part={mime-part}
 *          query={search-query}
 *          types={types} // when searching
 *          auth={auth-types}
 *          start={time}
 *          end={time}
 *          sync="1"
 *          
 *             {types}   = comma-separated list.  Legal values are:
 *                         appointment|chat|contact|conversation|document|
 *                         message|note|tag|task|wiki
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
    private static final long serialVersionUID = -5313094316561384586L;

    public static final String SERVLET_PATH = "/home";

    public static final String QP_ZAUTHTOKEN = "zauthtoken";
    public static final String QP_AUTHTOKEN = "authToken";
    
    public static final String QP_FMT = "fmt"; // format query param
    
    public static final String QP_ZLV = "zlv"; // zip level query param
    
    public static final String QP_ID = "id"; // id query param
    
    public static final String QP_LIST = "list"; // list query param

    public static final String QP_IMAP_ID = "imap_id"; // IMAP id query param

    public static final String QP_PART = "part"; // part query param
    
    /**
     * Body query param.  Also used by {@link #ZipFormatter} and {@link #TarFormatter} to specify whether
     * the entire message should be returned (<tt>body=1</tt>), or just the headers (<tt>body=0</tt>).
     * The default is <tt>1</tt>.
     */
    public static final String QP_BODY = "body"; // body query param
    
    public static final String BODY_TEXT = "text"; // return text body

    public static final String BODY_HTML = "html"; // return html body if possible

    public static final String QP_QUERY = "query"; // query query param

    public static final String QP_VIEW = "view"; // view query param    

    public static final String QP_TYPES = "types"; // types

    public static final String QP_START = "start"; // start time

    public static final String QP_END = "end"; // end time

    public static final String QP_FREEBUSY_CALENDAR = "fbcal";  // calendar folder to run free/busy search on

    public static final String QP_IGNORE_ERROR = "ignore";  // ignore and continue on error during ics import

    public static final String QP_PRESERVE_ALARMS = "preserveAlarms";  // preserve existing alarms during ics import

    public static final String QP_OFFSET = "offset"; // offset into results
    
    public static final String QP_LIMIT = "limit"; // offset into results

    public static final String QP_AUTH = "auth"; // auth types

    public static final String QP_DISP = "disp"; // disposition (a = attachment, i = inline)

    public static final String QP_NAME = "name"; // filename/path segments, added to pathInfo
    
    public static final String QP_CSVFORMAT = "csvfmt"; // csv type (outlook-2003-csv, yahoo-csv, ...)

    public static final String QP_CSVLOCALE = "csvlocale"; // refining locale for csvfmt - e.g. zh-CN

    public static final String QP_VERSION = "ver";  // version for WikiItem and Document

    public static final String QP_HISTORY = "history";  // history for WikiItem
    
    public static final String QP_LANGUAGE = "language"; // all three
    
    public static final String QP_COUNTRY = "country"; // all three

    public static final String QP_VARIANT = "variant"; // all three

    public static final String UPLOAD_NAME = "uploadName"; // upload filename

    public static final String UPLOAD_TYPE = "uploadType"; // upload content type

    /**
     * Used by {@link #TarFormatter} to specify whether the <tt>.meta</tt>
     * files should be added to the tarball (<tt>meta=1</tt>) or not (<tt>meta=0</tt>).
     * The default is <tt>1</tt>.
     */
    public static final String QP_META = "meta";

    /**
     * Used by {@link #IfbFormatter} to specify the UID of calendar item to exclude when computing free/busy.
     */
    public static final String QP_EXUID = "exuid";

    public static final String AUTH_COOKIE = "co"; // auth by cookie

    public static final String AUTH_BASIC = "ba"; // basic auth

    public static final String AUTH_QUERYPARAM = "qp"; // query parameter

    public static final String AUTH_NO_SET_COOKIE = "nsc"; // don't set auth token cookie after basic auth
                                                           // same as ba after bug 42782

    // see https://bugzilla.zimbra.com/show_bug.cgi?id=42782#c11
    public static final String AUTH_SET_COOKIE = "sc"; // set auth token cookie after basic auth
    
    public static final String AUTH_DEFAULT = "co,nsc,qp"; // all three

    public static final String HTTP_URL = "http_url";
    public static final String HTTP_STATUS_CODE = "http_code";
    
    private static HashMap<String, Formatter> mFormatters;
    private static HashMap<String, Formatter> mDefaultFormatters;

    protected static final String MSGPAGE_BLOCK = "errorpage.attachment.blocked";
    private String mBlockPage = null;

    static {
        mFormatters = new HashMap<String, Formatter>();
        mDefaultFormatters = new HashMap<String, Formatter>();
        addFormatter(new CsvFormatter());
        addFormatter(new VcfFormatter());
        addFormatter(new IcsFormatter());
        addFormatter(new RssFormatter());
        addFormatter(new AtomFormatter());
        addFormatter(new NativeFormatter());
        addFormatter(new FreeBusyFormatter());
        addFormatter(new IfbFormatter());
        addFormatter(new SyncFormatter());
        addFormatter(new WikiFormatter());
        addFormatter(new XmlFormatter());
        addFormatter(new JsonFormatter());
        addFormatter(new HtmlFormatter());
        addFormatter(new TarFormatter());
        addFormatter(new TgzFormatter());
        addFormatter(new ZipFormatter());
        addFormatter(new ContactFolderFormatter());
    }

    /** Returns the REST URL for the account. */
    public static String getRestUrl(Account acct) throws ServiceException {
    	return getServiceUrl(acct, UserServlet.SERVLET_PATH);
    }
    
    /** Returns the REST URL for the mail item. */
    public static String getRestUrl(MailItem item) throws ServiceException {
    	Account acct = item.getMailbox().getAccount();
        return getRestUrl(acct) + URLUtil.urlEscape(item.getPath());
    }

    public synchronized static void addFormatter(Formatter f) {
        mFormatters.put(f.getType(), f);
        for (String mimeType : f.getDefaultMimeTypes())
            mDefaultFormatters.put(mimeType, f);
    }

    public Formatter getFormatter(String type) {
        return mFormatters.get(type);
    }

    private Mailbox getTargetMailbox(Context context) {
        // treat the non-existing target account the same as insufficient permission
        // to access existing item in order to prevent account harvesting.
        Mailbox mbox = null;
        try {
            mbox = context.targetMailbox = MailboxManager.getInstance().getMailboxByAccount(context.targetAccount);
        } catch (Exception e) {
            // ignore IllegalArgumentException or ServiceException being thrown.
        }
        return mbox;
    }
    
    private void getAccount(Context context) throws IOException, ServletException, UserServletException {
        try {
            boolean isAdminRequest = isAdminRequest(context.req);
            
            // check cookie or access key
            if (context.cookieAuthAllowed() || AuthProvider.allowAccessKeyAuth(context.req, this)) {
                try {
                    AuthToken at = AuthProvider.getAuthToken(context.req, isAdminRequest);
                    if (at != null) {
                        
                        if (at.isZimbraUser()) {
                            try {
                                context.authAccount = AuthProvider.validateAuthToken(Provisioning.getInstance(), at, false);
                            } catch (ServiceException e) {
                                throw new UserServletException(HttpServletResponse.SC_UNAUTHORIZED, L10nUtil.getMessage(MsgKey.errMustAuthenticate, context.req));
                            }
                            context.cookieAuthHappened = true;
                            context.authToken = at;
                            return;
                        } else {
                            if (at.isExpired())
                                throw new UserServletException(HttpServletResponse.SC_UNAUTHORIZED, L10nUtil.getMessage(MsgKey.errMustAuthenticate, context.req));
                            context.authAccount = new GuestAccount(at);
                            context.basicAuthHappened = true; // pretend that we basic authed
                            context.authToken = at;
                            return;
                        }
                    }
                } catch (AuthTokenException e) {
                    // bug 35917: malformed auth token means auth failure
                    throw new UserServletException(HttpServletResponse.SC_UNAUTHORIZED, L10nUtil.getMessage(MsgKey.errMustAuthenticate, context.req));
                }
            }

            // check query string
            if (context.queryParamAuthAllowed()) {
                String auth = context.params.get(QP_ZAUTHTOKEN);
                if (auth == null)
                    auth = context.params.get(QP_AUTHTOKEN);  // not sure who uses this parameter; zauthtoken is preferred
                if (auth != null) {
                    try {
                        // Only supported by ZimbraAuthProvider
                        AuthToken at = AuthProvider.getAuthToken(auth);
                        
                        try {
                            context.authAccount = AuthProvider.validateAuthToken(Provisioning.getInstance(), at, false);
                            context.qpAuthHappened = true;
                            context.authToken = at;
                            return;
                        } catch (ServiceException e) {
                            throw new UserServletException(HttpServletResponse.SC_UNAUTHORIZED, L10nUtil.getMessage(MsgKey.errMustAuthenticate, context.req));
                        }

                    } catch (AuthTokenException e) {
                        // bug 35917: malformed auth token means auth failure
                        throw new UserServletException(HttpServletResponse.SC_UNAUTHORIZED, L10nUtil.getMessage(MsgKey.errMustAuthenticate, context.req));
                    }
                }
            }
            
            /* AP-TODO-3: 
             *    http auth currently does not work for non-Zimbra auth provider,
             *    for Yahoo Y&T, will probably need to retrieve Y&T cookies from a 
             *    site in the basicAuthRequest after authenticating using user/pass.
             */
            // fallback to basic auth
            if (context.basicAuthAllowed()) {
                context.authAccount = basicAuthRequest(context.req, context.resp, false);
                if (context.authAccount != null) {
                    context.basicAuthHappened = true;
                    context.authToken = AuthProvider.getAuthToken(context.authAccount, isAdminRequest);  
                    
                    // send cookie back if need be. 
                    if (context.setCookie()) {
                        boolean secureCookie = context.req.getScheme().equals("https");
                        context.authToken.encode(context.resp, isAdminRequest, secureCookie);
                    }
                }
                // always return
                return;
            }

            // there is no credential at this point.  assume anonymous public access and continue.
        } catch (ServiceException e) {
            throw new ServletException(e);
        }
    }

    private void sendError(Context ctxt, HttpServletRequest req, HttpServletResponse resp, String message) throws IOException {
        if (ctxt == null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, message);
        } else if (!ctxt.cookieAuthHappened && ctxt.basicAuthAllowed() && !ctxt.basicAuthHappened) {
            resp.addHeader(WWW_AUTHENTICATE_HEADER, getRealmHeader(req, null));
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, message);
        }
    }

    @Override public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        Context context = null;
        ZimbraLog.clearContext();
        addRemoteIpToLoggingContext(req);
        try {
            context = new Context(req, resp, this);
            if (!checkAuthentication(req, resp, context)) {
                sendError(context, req, resp, L10nUtil.getMessage(MsgKey.errMustAuthenticate, req));
                return;
            }

            if (proxyIfNecessary(req, resp, context))
                return;

            // at this point context.authAccount is set either from the Cookie,
            // or from basic auth.  if there was no credential in either the Cookie
            // or basic auth, authAccount is set to anonymous account.
            if (context.authAccount != null) {
                ZimbraLog.addAccountNameToContext(context.authAccount.getName());
                if (context.authAccount.getLocale() != null && context.locale == null)
                	context.locale = context.authAccount.getLocale();
            }

            doAuthGet(req, resp, context);

        } catch (NoSuchItemException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, L10nUtil.getMessage(MsgKey.errNoSuchItem, req));
        } catch (ServiceException se) {
            if (se.getCode() == ServiceException.PERM_DENIED)
                sendError(context, req, resp, se.getMessage());
            else
                throw new ServletException(se);
        } catch (UserServletException e) {
            // add check for ServiceException root cause?
            resp.sendError(e.getHttpStatusCode(), e.getMessage());
        } finally {
            ZimbraLog.clearContext();
        }
    }

    private boolean checkAuthentication(HttpServletRequest req, HttpServletResponse resp, Context context)
    throws IOException, ServletException, UserServletException {
        // if they specify /~/, we must auth
        if (context.targetAccount == null && context.accountPath.equals("~")) {
            getAccount(context);
            if (context.authAccount == null)
                return false;
            context.targetAccount = context.authAccount;
        }

        // need this before proxy if we want to support sending cookie from a basic-auth
        getAccount(context);
        if (context.authAccount == null)
            context.setAnonymousRequest();

        return true;
    }

    private AuthToken getProxyAuthToken(Context context) throws ServiceException {            
        String encoded = Provisioning.getInstance().getProxyAuthToken(context.targetAccount.getId());
        if (encoded != null) {
            return new ZimbraAuthTokenEncoded(encoded);
        } else if (context.basicAuthHappened) {
            return context.authToken;
        } else {
            return null;
        }
    }
    
    private boolean proxyIfNecessary(HttpServletRequest req, HttpServletResponse resp, Context context) throws IOException, ServiceException {
        // this should handle both explicit /user/user-on-other-server/ and
        // /user/~/?id={account-id-on-other-server}:id            

        if (context.targetAccount != null && !Provisioning.onLocalServer(context.targetAccount)) {                                                           
            proxyServletRequest(req, resp, Provisioning.getInstance().getServer(context.targetAccount), getProxyAuthToken(context));
            return true;
        }

        return false;
    }

    private void doAuthGet(HttpServletRequest req, HttpServletResponse resp, Context context)
    throws ServletException, IOException, ServiceException, UserServletException {
        if (ZimbraLog.mailbox.isDebugEnabled()) {
            StringBuffer reqURL = context.req.getRequestURL();
            String queryParam = context.req.getQueryString();
            if (queryParam != null) reqURL.append('?').append(queryParam);
            ZimbraLog.mailbox.debug("UserServlet: " + reqURL.toString());
        }

        context.opContext = new OperationContext(context.authAccount, isAdminRequest(req));
        Mailbox mbox = getTargetMailbox(context);
        if (mbox != null) {
            ZimbraLog.addMboxToContext(mbox.getId());
            if (context.reqListIds != null) {
            	resolveItems(context);
            } else {
    	        MailItem item = resolveItem(context, true);
    	        if (isProxyRequest(req, resp, context, item)) {
    	            // if the target is a mountpoint, proxy the request on to the resolved target
    	            return;
    	        }
            }
        }
        
        resolveFormatter(context);

        // Prevent harvest attacks.  If mailbox doesn't exist for a request requiring authentication,
        // return auth error instead of "no such mailbox".  If request/formatter doesn't require
        // authentication, call the formatter and let it deal with preventing harvest attacks.
        if (mbox == null && context.formatter.requiresAuth())
            throw ServiceException.PERM_DENIED(L10nUtil.getMessage(MsgKey.errPermissionDenied, req));

        if (context.formatter.canBeBlocked()) {
            if (Formatter.checkGlobalOverride(Provisioning.A_zimbraAttachmentsBlocked, context.authAccount)) {
                sendbackBlockMessage(context.req, context.resp);
                return;
            }
        }

        context.formatter.format(context);
    }

    /** Adds an item to a folder specified in the URI.  The item content is
     *  provided in the PUT request's body.
     * @see #doPost(HttpServletRequest, HttpServletResponse) */
    @Override public void doPut(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        doPost(req, resp);
    }

    /** Adds an item to a folder specified in the URI.  The item content is
     *  provided in the POST request's body. */
    @Override public void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        Context context = null;
        ZimbraLog.clearContext();
        addRemoteIpToLoggingContext(req);
        try {
            context = new Context(req, resp, this);
            if (!checkAuthentication(req, resp, context)) {
                sendError(context, req, resp, L10nUtil.getMessage(MsgKey.errMustAuthenticate, req));
                return;
            }

            if (proxyIfNecessary(req, resp, context))
                return;

            if (context.authAccount != null)
                ZimbraLog.addAccountNameToContext(context.authAccount.getName());

            Folder folder = null;
            String filename = null;
            Mailbox mbox = getTargetMailbox(context);
            if (mbox != null) {
                ZimbraLog.addMboxToContext(mbox.getId());
    
                ZimbraLog.mailbox.info("UserServlet (POST): " + context.req.getRequestURL().toString());
    
                context.opContext = new OperationContext(context.authAccount, isAdminRequest(req));

                try {
                    context.target = resolveItem(context, false);
                } catch (NoSuchItemException nsie) {
                    // perhaps it's a POST to "Notebook/new-file-name" -- find the parent folder and proceed from there
                    if (context.itemPath == null)
                        throw nsie;
                    int separator = context.itemPath.lastIndexOf('/');
                    if (separator <= 0)
                        throw nsie;
                    filename = context.itemPath.substring(separator + 1);
                    context.itemPath = context.itemPath.substring(0, separator);
                    context.target = resolveItem(context, false);
                    context.extraPath = filename;
                }

                folder = (context.target instanceof Folder ? (Folder) context.target : mbox.getFolderById(context.opContext, context.target.getFolderId()));

                if (context.target != folder) {
                    if (filename == null)
                        filename = context.target.getName();
                    else
                        // need to fail on POST to "Notebook/existing-file/random-cruft"
                        throw MailServiceException.NO_SUCH_FOLDER(context.itemPath);
                }

    	        if (isProxyRequest(req, resp, context, folder)) {
    	            // if the target is a mountpoint, proxy the request on to the resolved target
    	            return;
    	        }
            }

            // if they specified a filename, default to the native formatter
            if (context.format == null && filename != null)
                context.format = NativeFormatter.FMT_NATIVE;

            String ctype = context.req.getContentType();

            // if no format explicitly specified, try to guess it from the Content-Type header
            if (context.format == null && ctype != null) {
                String normalizedType = new ContentType(ctype).getValue();
                Formatter fmt = mDefaultFormatters.get(normalizedType);
                if (fmt != null)
                    context.format = fmt.getType();
            }

            context.target = folder;
            resolveFormatter(context);
            if (!context.formatter.supportsSave())
                sendError(context, req, resp, L10nUtil.getMessage(MsgKey.errUnsupportedFormat, req));

            // Prevent harvest attacks.  If mailbox doesn't exist for a request requiring authentication,
            // return auth error instead of "no such mailbox".  If request/formatter doesn't require
            // authentication, call the formatter and let it deal with preventing harvest attacks.
            if (mbox == null && context.formatter.requiresAuth())
                throw ServiceException.PERM_DENIED(L10nUtil.getMessage(MsgKey.errPermissionDenied, req));

            context.formatter.save(context, ctype, folder, filename);
        } catch (NoSuchItemException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "no such item");
        } catch (ServiceException se) {
            if (se.getCode() == ServiceException.PERM_DENIED)
                sendError(context, req, resp, se.getMessage());
            else
                throw new ServletException(se);
        } catch (UserServletException e) {
            // add check for ServiceException root cause?
            resp.sendError(e.getHttpStatusCode(), e.getMessage());
        } finally {
            ZimbraLog.clearContext();
        }
    }

    /** Determines the <code>format</code> and <code>formatter<code> for the
     *  request, if not already set. */
    private void resolveFormatter(Context context) throws UserServletException {
        if (context.format == null) {
            context.format = defaultFormat(context);
            if (context.format == null)
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, L10nUtil.getMessage(MsgKey.errUnsupportedFormat, context.req));
        }

        if (context.formatter == null)
            context.formatter = mFormatters.get(context.format);
        if (context.formatter == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, L10nUtil.getMessage(MsgKey.errUnsupportedFormat, context.req));
    }
    
    private void resolveItems(Context context) throws ServiceException {
        context.respListItems = new ArrayList<MailItem>();

    	for (int id : context.reqListIds) {
    	    try {
    	        context.respListItems.add(context.targetMailbox.getItemById(context.opContext, id, MailItem.TYPE_UNKNOWN));
    	    } catch (NoSuchItemException x) {
    	        ZimbraLog.misc.info(x.getMessage());
    	    } catch (ServiceException x) {
    	        if (x.getCode().equals(ServiceException.PERM_DENIED)) {
    	            ZimbraLog.misc.info(x.getMessage());
    	        } else {
    	            throw x;
    	        }
    	    }
    	}

    	// we consider partial success OK -- let the client figure out which item is missing
    	if (context.respListItems.isEmpty())
    	    throw MailServiceException.NO_SUCH_ITEM(context.reqListIds.toString());
    }

    /*
     * Parses the pathInfo, then returns MailItem corresponding to the resource in pathInfo.
     * 
     * If the formatter does not require authentication, e.g. IfbFormatter, 
     * then the path resolution is skipped and returns null.  That's because
     * IfbFormatter does not internally use the resource identified in the URL.
     * It gets the ifb information directly from the Mailbox.
     * 
     * If the formatter declares that the authentication is not required, it's
     * the formatter's responsibility to make sure the MailItems returned to
     * the clients has gone through the access checks.
     * 
     */
    private MailItem resolveItem(Context context, boolean checkExtension) throws ServiceException {
        if (context.formatter != null && !context.formatter.requiresAuth())
            return null;

        Mailbox mbox = context.targetMailbox;

        // special-case the fetch-by-IMAP-id option
        if (context.imapId > 0) {
            // fetch the folder from the path
            Folder folder = mbox.getFolderByPath(context.opContext, context.itemPath);

            // and then fetch the item from the "imap_id" query parameter
            return mbox.getItemByImapId(context.opContext, context.imapId, folder.getId());
        }

        if (context.itemId != null) {
            context.target = mbox.getItemById(context.opContext, context.itemId.getId(), MailItem.TYPE_UNKNOWN);

            context.itemPath = context.target.getPath();
            if (context.target instanceof Mountpoint || context.extraPath == null || context.extraPath.equals(""))
                return context.target;
            if (context.itemPath == null)
                throw MailServiceException.NO_SUCH_ITEM("?id=" + context.itemId + "&name=" + context.extraPath);
            context.target = null;
            context.itemId = null;
        }

        if (context.extraPath != null && !context.extraPath.equals("")) {
            context.itemPath = (context.itemPath + '/' + context.extraPath).replaceAll("//+", "/");
            context.extraPath = null;
        }

        if (context.format != null && (context.format.equalsIgnoreCase("freebusy") || context.format.equalsIgnoreCase("ifb"))) {
            try {
                // Do the get as mailbox owner to circumvent ACL system.
                context.target = mbox.getItemByPath(null, context.itemPath);
            } catch (ServiceException e) {
                if (!(e instanceof NoSuchItemException))
                    throw e;
            }
        } else {
            // first, try the full requested path
            ServiceException failure = null;
            try {
                context.target = mbox.getItemByPath(context.opContext, context.itemPath);
            } catch (ServiceException e) {
                if (!(e instanceof NoSuchItemException) && !e.getCode().equals(ServiceException.PERM_DENIED))
                    throw e;
                failure = e;
            }
    
            if (context.target == null) {
                // no joy.  if they asked for something like "calendar.csv" (where "calendar" was the folder name), try again minus the extension
                int dot = context.itemPath.lastIndexOf('.'), slash = context.itemPath.lastIndexOf('/');
                if (checkExtension && context.format == null && dot != -1 && dot > slash) {
                    /* if path == /foo/bar/baz.html, then
                     *      format -> html
                     *      path   -> /foo/bar/baz  */
                    String unsuffixedPath = context.itemPath.substring(0, dot);
                    try {
                        context.target = mbox.getItemByPath(context.opContext, unsuffixedPath);
                        context.format = context.itemPath.substring(dot + 1);
                        context.itemPath = unsuffixedPath;
                    } catch (ServiceException e) { }
                }
            }
    
            if (context.target == null) {
                // still no joy.  the only viable possibility at this point is that there's a mountpoint somewhere higher up in the requested path
                try {
                    Pair<Folder, String> match = mbox.getFolderByPathLongestMatch(context.opContext, Mailbox.ID_FOLDER_USER_ROOT, context.itemPath);
                    Folder reachable = match.getFirst();
                    if (reachable instanceof Mountpoint) {
                        context.target = reachable;
                        context.itemPath = reachable.getPath();
                        context.extraPath = match.getSecond();
                    }
                } catch (ServiceException e) { }
            }
    
            // don't think this code can ever get called because <tt>context.target</tt> can't be null at this point
            if (context.target == null && context.getQueryString() == null)
                throw failure;
        }

        return context.target;
    }

    private boolean isProxyRequest(HttpServletRequest req, HttpServletResponse resp, Context context, MailItem item)
    throws IOException, ServiceException, UserServletException {
        if (!(item instanceof Mountpoint))
            return false;
        if (context.format != null && context.format.equals("html"))
            return false;
        Mountpoint mpt = (Mountpoint) item;

        String uri = SERVLET_PATH + "/~/?" + QP_ID + '=' + URLUtil.urlEscape(mpt.getOwnerId()) + "%3A" + mpt.getRemoteId();
        if (context.format != null)
            uri += '&' + QP_FMT + '=' + URLUtil.urlEscape(context.format);
        if (context.extraPath != null)
            uri += '&' + QP_NAME + '=' + URLUtil.urlEscape(context.extraPath);
        for (Map.Entry<String, String> entry : HttpUtil.getURIParams(req).entrySet()) {
            String qp = entry.getKey();
            if (!qp.equals(QP_ID) && !qp.equals(QP_FMT))
                uri += '&' + URLUtil.urlEscape(qp) + '=' + URLUtil.urlEscape(entry.getValue());
        }

        Provisioning prov = Provisioning.getInstance();
        Account targetAccount = prov.get(AccountBy.id, mpt.getOwnerId());
        if (targetAccount == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, L10nUtil.getMessage(MsgKey.errNoSuchAccount, req));

        proxyServletRequest(req, resp, prov.getServer(targetAccount), uri, getProxyAuthToken(context));
        return true;
    }

    public static class Context {
        public HttpServletRequest req;
        public HttpServletResponse resp;
        public Servlet servlet;
        public Map<String, String> params;
        public String format;
        public Formatter formatter;        
        public boolean cookieAuthHappened;
        public boolean basicAuthHappened;
        public boolean qpAuthHappened;
        public String accountPath;
        public AuthToken authToken;
        public String itemPath;
        public String extraPath;
        public ItemId itemId;
        public MailItem target;
        public int[] reqListIds;
        public List<MailItem> respListItems;
        public int imapId = -1;
        public boolean sync;
        public Account authAccount;
        public Account targetAccount;
        public Mailbox targetMailbox;
        public OperationContext opContext;
        public Locale locale;
        private long mStartTime = -2;
        private long mEndTime = -2;

        Context(HttpServletRequest request, HttpServletResponse response, UserServlet srvlt)
        throws UserServletException, ServiceException {
            Provisioning prov = Provisioning.getInstance();

            this.req = request;
            this.resp = response;
            this.servlet = srvlt;
            this.params = HttpUtil.getURIParams(request);
            
            //rest url override for locale
            String language = this.params.get(QP_LANGUAGE);
            if (language != null) {
                String country = this.params.get(QP_COUNTRY);
                if (country != null) {
                    String variant = this.params.get(QP_VARIANT);
                    if (variant != null) {
                        this.locale = new Locale(language, country, variant);
                    } else {
                        this.locale = new Locale(language, country);
                    }
                } else {
                    this.locale = new Locale(language);
                }
            } else {
            	this.locale = req.getLocale();
            }

            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("") || !pathInfo.startsWith("/"))
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, L10nUtil.getMessage(MsgKey.errInvalidPath, request));
            int pos = pathInfo.indexOf('/', 1);
            if (pos == -1)
                pos = pathInfo.length();
            if (pos < 1)
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, L10nUtil.getMessage(MsgKey.errInvalidPath, request));

            this.accountPath = pathInfo.substring(1, pos).toLowerCase();

            if (pos < pathInfo.length()) {
                this.itemPath = pathInfo.substring(pos + 1);
                if (itemPath.equals(""))
                	itemPath = "/";
            } else {
                itemPath = "/";
            }
            this.extraPath = this.params.get(QP_NAME);
            this.format = this.params.get(QP_FMT);
            String id = this.params.get(QP_ID);
            try {
                this.itemId = id == null ? null : new ItemId(id, (String) null);
            } catch (ServiceException e) {
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, L10nUtil.getMessage(MsgKey.errInvalidId, request));
            }

            String listParam = this.params.get(QP_LIST);
            if (listParam != null && listParam.length() > 0) {
            	String[] ids = listParam.split(",");
            	reqListIds = new int[ids.length];
            	for (int i = 0; i < ids.length; ++i) {
            		reqListIds[i] = Integer.parseInt(ids[i]);
            	}
            }

            String imap = this.params.get(QP_IMAP_ID);
            try {
                this.imapId = imap == null ? -1 : Integer.parseInt(imap);
            } catch (NumberFormatException nfe) {
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, L10nUtil.getMessage(MsgKey.errInvalidImapId, request));
            }

            if (this.format != null) {
                this.format = this.format.toLowerCase();
                this.formatter = srvlt.getFormatter(this.format);
                if (this.formatter == null)
                    throw new UserServletException(HttpServletResponse.SC_NOT_IMPLEMENTED, L10nUtil.getMessage(MsgKey.errNotImplemented, request));
            }

            // see if we can get target account or not
            if (itemId != null && itemId.getAccountId() != null) {
                targetAccount = prov.get(AccountBy.id, itemId.getAccountId(), authToken);                                
                return;
            } else if (accountPath.equals("~")) {
                // can't resolve this yet
                return;
            } else if (accountPath.startsWith("~")) {
                accountPath = accountPath.substring(1);
            }
            targetAccount = prov.get(AccountBy.name, accountPath, authToken);                
        }

        public boolean isUsingAdminPrivileges() {
            return authToken != null && AuthToken.isAnyAdmin(authToken);
        }

        public Servlet getServlet() { return servlet; }

        public long getStartTime() {
            if (mStartTime == -2) {
                String st = params.get(QP_START);
                long defaultStartTime = formatter.getDefaultStartTime();
                mStartTime = (st != null) ? DateUtil.parseDateSpecifier(st, defaultStartTime) : defaultStartTime;
            }
            return mStartTime;
        }

        public long getEndTime() {
            if (mEndTime == -2) {
                String et = params.get(QP_END);
                long defaultEndTime = formatter.getDefaultEndTime();                
                mEndTime = (et != null) ? DateUtil.parseDateSpecifier(et, defaultEndTime) : defaultEndTime;
            }
            return mEndTime;
        }

        public int getFreeBusyCalendar() {
            int folder = FreeBusyQuery.CALENDAR_FOLDER_ALL;
            String str = params.get(QP_FREEBUSY_CALENDAR);
            if (str != null) {
                try {
                    folder = Integer.parseInt(str);
                } catch (NumberFormatException e) {}
            }
            return folder;
        }

        public boolean ignoreAndContinueOnError() {
            String val = params.get(QP_IGNORE_ERROR);
            if (val != null) {
                try {
                    int n = Integer.parseInt(val);
                    return n != 0;
                } catch (NumberFormatException e) {}
            }
            return false;
        }

        public boolean preserveAlarms() {
            String val = params.get(QP_PRESERVE_ALARMS);
            if (val != null) {
                try {
                    int n = Integer.parseInt(val);
                    return n != 0;
                } catch (NumberFormatException e) {}
            }
            return false;
        }

        public String getQueryString() {
            return params.get(QP_QUERY);
        }

        public boolean cookieAuthAllowed() {
            return getAuth().indexOf(AUTH_COOKIE) != -1;
        }
        
        public boolean isAuthedAcctGuest() {
            return authAccount != null && authAccount instanceof GuestAccount;
        }
        
        // bug 42782
        public boolean setCookie() {
            return (!isAuthedAcctGuest() &&
                    getAuth().indexOf(AUTH_SET_COOKIE) != -1 &&
                    getAuth().indexOf(AUTH_NO_SET_COOKIE) == -1);
        }

        public boolean basicAuthAllowed() {
            String auth = getAuth();
            return auth.indexOf(AUTH_NO_SET_COOKIE) != -1 ||
                   auth.indexOf(AUTH_BASIC) != -1 ||
                   auth.indexOf(AUTH_SET_COOKIE) != -1;
        }

        public boolean queryParamAuthAllowed() {
            return getAuth().indexOf(AUTH_QUERYPARAM) != -1;
        }

        public String getAuth() {
            String a = params.get(QP_AUTH);
            return (a == null || a.length() == 0) ? AUTH_DEFAULT : a;
        }

        public boolean hasPart() {
            String p = getPart();
            return p != null && p.length() > 0;
        }

        public String getPart() {
            return params.get(QP_PART);
        }

        public boolean hasBody() {
            String p = getBody();
            return p != null;
        }

        public String getBody() {
            return params.get(QP_BODY);
        }

        public boolean hasView() {
            String v = getView();
            return v != null && v.length() > 0;
        }

        public String getView() {
            return params.get(QP_VIEW);
        }

        public int getOffset() {
            String s = params.get(QP_OFFSET);
            if (s != null) { 
                int offset = Integer.parseInt(s);
                if (offset > 0)
                    return offset;
            }
            return 0;
        }

        public int getLimit() {
            String s = params.get(QP_LIMIT);
            if (s != null) {
                int limit = Integer.parseInt(s);
                if (limit > 0)
                    return limit;
            }
            return 50;
        }

        public String getTypesString() {
            return params.get(QP_TYPES);
        }
        
        /** Returns <tt>true</tt> if {@link UserServlet#QP_BODY} is not
         *  specified or is set to a non-zero value. */
        public boolean shouldReturnBody() {
            String bodyVal = params.get(UserServlet.QP_BODY);
            if (bodyVal != null && bodyVal.equals("0"))
                return false;
            return true;
        }

        public void setAnonymousRequest() {
            authAccount = GuestAccount.ANONYMOUS_ACCT;
        }

        public boolean isAnonymousRequest() {
            return authAccount.equals(GuestAccount.ANONYMOUS_ACCT);
        }

        /** Default maximum upload size for PUT/POST write ops: 10MB. */
        private static final long DEFAULT_MAX_POST_SIZE = 10 * 1024 * 1024;

        // don't use this for a large upload.  use getUpload() instead.
        public byte[] getPostBody() throws ServiceException, IOException, UserServletException {
            long sizeLimit = Provisioning.getInstance().getLocalServer().getLongAttr(
                    Provisioning.A_zimbraFileUploadMaxSize, DEFAULT_MAX_POST_SIZE);
            InputStream is = getRequestInputStream(sizeLimit);
            try {
                return ByteUtil.getContent(is, req.getContentLength(), sizeLimit);
            } finally {
                is.close();
            }
        }

        public FileUploadServlet.Upload getUpload() throws ServiceException, IOException {
            return FileUploadServlet.saveUpload(req.getInputStream(), itemPath, req.getContentType(), authAccount.getId());
        }

        private static final class UploadInputStream extends InputStream {
            private FileItem fi = null;
            private InputStream is;
            private long curSize = 0;
            private long maxSize;
            private long markSize = 0;
            
            UploadInputStream(InputStream is, long maxSize) {
                this.is = is;
                this.maxSize = maxSize;
            }

            UploadInputStream(FileItem fi, long maxSize) throws IOException {
                this(fi.getInputStream(), maxSize);
                this.fi = fi;
            }

            @Override public void close() throws IOException {
                try {
                    is.close();
                } finally {
                    if (fi != null)
                        fi.delete();
                    fi = null;
                }
            }
            
            @Override public int available() throws IOException { return is.available(); }

            @Override public void mark(int where) { is.mark(where); markSize = curSize; }

            @Override public boolean markSupported() { return is.markSupported(); }

            @Override public int read() throws IOException { return (int)check(is.read()); }

            @Override public int read(byte b[]) throws IOException { return (int)check(is.read(b)); }

            @Override public int read(byte b[], int off, int len) throws IOException {
                return (int)check(is.read(b, off, len));
            }

            @Override public void reset() throws IOException { is.reset(); curSize = markSize; }

            @Override public long skip(long n) throws IOException { return check(is.skip(n)); }

            private long check(long in) throws IOException {
                if (in > 0) {
                    curSize += in;
                    if (maxSize > 0 && curSize > maxSize)
                        throw new IOException("upload over " + maxSize + " byte limit");
                }
                return in;
            }
        }
        
        public InputStream getRequestInputStream() 
            throws IOException, ServiceException, UserServletException {
            return getRequestInputStream(0);
        }
        
        public InputStream getRequestInputStream(long limit) 
            throws IOException, ServiceException, UserServletException {
            String contentType = MimeConstants.CT_APPLICATION_OCTET_STREAM;
            String filename = null;
            InputStream is = null;
            final long DEFAULT_MAX_SIZE = 10 * 1024 * 1024;
            
            if (limit == 0) {
                if (req.getParameter("lbfums") != null)
                    limit = Provisioning.getInstance().getLocalServer().getLongAttr(Provisioning.A_zimbraFileUploadMaxSize, DEFAULT_MAX_SIZE);
                else
                    limit = Provisioning.getInstance().getConfig().getLongAttr(Provisioning.A_zimbraMtaMaxMessageSize, DEFAULT_MAX_SIZE);
            }
            if (ServletFileUpload.isMultipartContent(req)) {
                ServletFileUpload sfu = new ServletFileUpload();

                try {
                    FileItemIterator iter = sfu.getItemIterator(req);

                    while (iter.hasNext()) {
                        FileItemStream fis = iter.next();
                        
                        if (fis.isFormField()) {
                            is = fis.openStream();
                            params.put(fis.getFieldName(),
                                new String(ByteUtil.getContent(is, -1), "UTF-8"));
                            is.close();
                            is = null;
                        } else {
                            contentType = fis.getContentType();
                            filename = fis.getName();
                            is = new UploadInputStream(fis.openStream(), limit);
                            break;
                        }
                    }
                } catch (Exception e) {
                    throw new UserServletException(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, e.toString());
                }
                if (is == null)
                    throw new UserServletException(HttpServletResponse.SC_NO_CONTENT, "No file content");
            } else {
                ContentType ctype = new ContentType(req.getContentType());
                String contentEncoding = req.getHeader("Content-Encoding");
                
                contentType = ctype.getValue();
                filename = ctype.getParameter("name");
                if (filename == null || filename.trim().equals(""))
                    filename = new ContentDisposition(req.getHeader("Content-Disposition")).getParameter("filename");
                is = new UploadInputStream(contentEncoding != null &&
                    contentEncoding.indexOf("gzip") != -1 ?
                    new GZIPInputStream(req.getInputStream()) :
                        req.getInputStream(), limit);
            }
            if (filename == null || filename.trim().equals(""))
                filename = "unknown";
            else
                params.put(UPLOAD_NAME, filename);
            params.put(UPLOAD_TYPE, contentType);
            ZimbraLog.mailbox.info("UserServlet received file %s - %d request bytes",
                filename, req.getContentLength());
            return is;
        }
        
        @Override public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("account(" + accountPath + ")\n");
            sb.append("itemPath(" + itemPath + ")\n");
            sb.append("foramt(" + format + ")\n");
            return sb.toString();
        }
    }

    private String defaultFormat(Context context) {
        if (context.hasPart())
            return "native";

        byte type = MailItem.TYPE_UNKNOWN;
        if (context.target instanceof Folder)
            type = ((Folder) context.target).getDefaultView();
        else if (context.target != null)
            type = context.target.getType();

        switch (type) {
            case MailItem.TYPE_APPOINTMENT:
            case MailItem.TYPE_TASK:
                return "ics";
            case MailItem.TYPE_CONTACT:
                return context.target instanceof Folder? "csv" : "vcf";
            case MailItem.TYPE_WIKI:
                return "wiki";
            case MailItem.TYPE_DOCUMENT:   // use native formatter for Document
                return context.target instanceof Folder? "wiki" : "native";
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
        resp.sendError(HttpServletResponse.SC_FORBIDDEN, L10nUtil.getMessage(MsgKey.errAttachmentDownloadDisabled, req));
    }

    @Override public void init() throws ServletException {
        String name = getServletName();
        ZimbraLog.mailbox.info("Servlet " + name + " starting up");
        super.init();
        mBlockPage = getInitParameter(MSGPAGE_BLOCK);
    }

    @Override public void destroy() {
        String name = getServletName();
        ZimbraLog.mailbox.info("Servlet " + name + " shutting down");
        super.destroy();
    }

    public static byte[] getRemoteContent(AuthToken authToken, ItemId iid, Map<String, String> params) throws ServiceException {
        Account target = Provisioning.getInstance().get(AccountBy.id, iid.getAccountId(), authToken);
        Map<String, String> pcopy = new HashMap<String, String>(params);
        pcopy.put(QP_ID, iid.toString());
        return getRemoteContent(authToken, target, (String)null, pcopy);
    }

    public static byte[] getRemoteContent(AuthToken authToken, Account target, String folder, Map<String,String> params) throws ServiceException {
        return getRemoteContent(authToken.toZAuthToken(), getRemoteUrl(target, folder, params));
    }

    public static byte[] getRemoteContent(ZAuthToken authToken, String url) throws ServiceException {
        return getRemoteResource(authToken, url).getSecond();
    }

    public static HttpInputStream getRemoteContentAsStream(AuthToken authToken, Account target, String folder, Map<String,String> params) throws ServiceException, IOException {
        String url = getRemoteUrl(target, folder, params);
        return getRemoteResourceAsStream(authToken.toZAuthToken(), url).getSecond();
    }

    private static String getRemoteUrl(Account target, String folder, Map<String, String> params) throws ServiceException {
        if (folder == null) {
            folder = "";
        } else {
            if (folder.endsWith("/"))
                folder = folder.substring(0, folder.length() - 1);
            if (folder.startsWith("/"))
                folder = folder.substring(1);
        }

        Server server = Provisioning.getInstance().getServer(target);
        StringBuffer url = new StringBuffer(getProxyUrl(null, server, SERVLET_PATH + getAccountPath(target)));
        if (folder.length() > 0)
        	url.append("/").append(folder);
        url.append("/?").append(QP_AUTH).append('=').append(AUTH_COOKIE);
        if (params != null) {
            for (Map.Entry<String, String> param : params.entrySet())
                url.append('&').append(URLUtil.urlEscape(param.getKey())).append('=').append(URLUtil.urlEscape(param.getValue()));
        }
        return url.toString();
    }

    public static Pair<Header[], byte[]> getRemoteResource(ZAuthToken authToken, String url) throws ServiceException {
        HttpMethod get = null;
        try {
            Pair<Header[], HttpMethod> pair = doHttpOp(authToken, new GetMethod(url));
            get = pair.getSecond();
            return new Pair<Header[], byte[]>(pair.getFirst(), get.getResponseBody());
        } catch (IOException x) {
            throw ServiceException.FAILURE("Can't read response body " + url, x);
        } finally {
            if (get != null) {
                get.releaseConnection();
            }
        }
    }

    public static FileUploadServlet.Upload getRemoteResourceAsUpload(AuthToken at, ItemId iid, Map<String,String> params)
    throws ServiceException, IOException {
        Map<String, String> pcopy = new HashMap<String, String>(params);
        pcopy.put(QP_ID, iid.toString());

        // fetch from remote store
        Provisioning prov = Provisioning.getInstance();
        Account target = prov.get(AccountBy.id, iid.getAccountId(), at);
        String url = getRemoteUrl(target, null, pcopy);

        Pair<Header[], HttpInputStream> response = getRemoteResourceAsStream(at.toZAuthToken(), url);

        // and save the result as an upload
        String ctype = "text/plain", filename = null;
        for (Header hdr : response.getFirst()) {
            String hname = hdr.getName().toLowerCase();
            if (hname.equals("content-type"))
                ctype = hdr.getValue();
            else if (hname.equals("content-disposition"))
                filename = new ContentDisposition(hdr.getValue()).getParameter("filename");
        }
        if (filename == null || filename.equals(""))
            filename = new ContentType(ctype).getParameter("name");
        if (filename == null || filename.equals(""))
            filename = "unknown";
        return FileUploadServlet.saveUpload(response.getSecond(), filename, ctype, at.getAccountId());
    }


    /** Helper class so that we can close connection upon stream close */
    public static class HttpInputStream extends FilterInputStream {
        private HttpMethod method;

        public HttpInputStream(HttpMethod m) throws IOException {
            super(m.getResponseBodyAsStream());
            this.method = m;
        }
        public int getContentLength() {
            String cl = getHeader("Content-Length");
            if (cl != null)
                return Integer.parseInt(cl);
            return -1;
        }
        public String getHeader(String headerName) {
            Header cl = method.getResponseHeader(headerName);
            if (cl != null)
                return cl.getValue();
            return null;
        }
        public int getStatusCode() {
            return method.getStatusCode();
        }
        @Override public void close() {
            method.releaseConnection();
        }
    }
    
    public static Pair<Header[], HttpInputStream> getRemoteResourceAsStream(ZAuthToken authToken, ItemId iid, String extraPath)
            throws ServiceException, IOException {
        Map<String,String> params = new HashMap<String,String>();
        params.put(QP_ID, iid.toString());
        if (extraPath != null)
        	params.put(QP_NAME, extraPath);
        Account target = Provisioning.getInstance().getAccountById(iid.getAccountId());
        String url = getRemoteUrl(target, null, params);
        return getRemoteResourceAsStream(authToken, url);
    }
    
    public static Pair<Integer, InputStream> getRemoteResourceAsStreamWithLength(ZAuthToken authToken, String url) throws ServiceException, IOException {
        HttpInputStream his = getRemoteResourceAsStream(authToken, url).getSecond();
        return new Pair<Integer, InputStream>(his.getContentLength(), his);
    }
    
    public static Pair<Header[], HttpInputStream> getRemoteResourceAsStream(ZAuthToken authToken, String url)
    throws ServiceException, IOException {
        Pair<Header[], HttpMethod> pair = doHttpOp(authToken, new GetMethod(url));
        return new Pair<Header[], HttpInputStream>(pair.getFirst(), new HttpInputStream(pair.getSecond()));
    }

    public static Pair<Header[], HttpInputStream> putMailItem(ZAuthToken authToken, String url, MailItem item)
    throws ServiceException, IOException {
        if (item instanceof Document) {
            Document doc = (Document) item;
            StringBuilder u = new StringBuilder(url);
            u.append("?").append(QP_AUTH).append('=').append(AUTH_COOKIE);
            if (doc.getType() == MailItem.TYPE_WIKI)
                u.append("&fmt=wiki");
            PutMethod method = new PutMethod(u.toString());
            String contentType = doc.getContentType();
            method.addRequestHeader("Content-Type", contentType);
            method.setRequestEntity(new InputStreamRequestEntity(doc.getContentStream(), doc.getSize(), contentType));
            method = HttpClientUtil.addInputStreamToHttpMethod(method, doc.getContentStream(), doc.getSize(), contentType);
            method.addRequestHeader("X-Zimbra-Description", doc.getDescription());
            method.setRequestEntity(new InputStreamRequestEntity(doc.getContentStream(), doc.getSize(), contentType));
            Pair<Header[], HttpMethod> pair = doHttpOp(authToken, method);
            return new Pair<Header[], HttpInputStream>(pair.getFirst(), new HttpInputStream(pair.getSecond()));
        }
        return putRemoteResource(authToken, url, item.getContentStream(), null);
    }

    public static Pair<Header[], HttpInputStream> putRemoteResource(AuthToken authToken, String url, Account target,
                InputStream req, Header[] headers)
    throws ServiceException, IOException {
        return putRemoteResource(authToken.toZAuthToken(), url, req, headers);
    }
    
    public static Pair<Header[], HttpInputStream> putRemoteResource(ZAuthToken authToken, String url, InputStream req, Header[] headers)
    throws ServiceException, IOException {
        StringBuilder u = new StringBuilder(url);
        u.append("?").append(QP_AUTH).append('=').append(AUTH_COOKIE);
        PutMethod method = new PutMethod(u.toString());
        String contentType = "application/octet-stream";
        if (headers != null) {
            for (Header hdr : headers) {
                String name = hdr.getName();
                method.addRequestHeader(hdr);
                if (name.equals("Content-Type"))
                    contentType = hdr.getValue();
            }
        }
        method.setRequestEntity(new InputStreamRequestEntity(req, contentType));
        Pair<Header[], HttpMethod> pair = doHttpOp(authToken, method);
        return new Pair<Header[], HttpInputStream>(pair.getFirst(), new HttpInputStream(pair.getSecond()));
    }

    private static Pair<Header[], HttpMethod> doHttpOp(ZAuthToken authToken, HttpMethod method)
    throws ServiceException {
        // create an HTTP client with the same cookies
        String url = "";
        String hostname = "";
        try {
            url = method.getURI().toString();
            hostname = method.getURI().getHost();
        } catch (IOException e) {
            ZimbraLog.mailbox.warn("can't parse target URI", e);
        }
        
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        Map<String, String> cookieMap = authToken.cookieMap(false);
        if (cookieMap != null) {
            HttpState state = new HttpState();
            for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
                state.addCookie(new org.apache.commons.httpclient.Cookie(hostname, ck.getKey(), ck.getValue(), "/", null, false));
            }
            client.setState(state);
            client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        }
        
    	if (method instanceof PutMethod) {
    		long contentLength = ((PutMethod)method).getRequestEntity().getContentLength();
    		if (contentLength > 0) {
    			int timeEstimate = Math.max(10000, (int)(contentLength / 100));  // 100kbps in millis
    			// cannot set connection time using our ZimbrahttpConnectionManager,
    			// see comments in ZimbrahttpConnectionManager.
    			// actually, length of the content to Put should not be a factor for 
    			// establishing a connection, only read time out matter, which we set
    			// client.getHttpConnectionManager().getParams().setConnectionTimeout(timeEstimate);
    			
    			method.getParams().setSoTimeout(timeEstimate);
    		}
    	}
    	
        try {
            int statusCode = HttpClientUtil.executeMethod(client, method);
            if (statusCode == HttpStatus.SC_NOT_FOUND)
                throw MailServiceException.NO_SUCH_ITEM(-1);
            else if (statusCode != HttpStatus.SC_OK && 
            		statusCode != HttpStatus.SC_CREATED &&
            		statusCode != HttpStatus.SC_NO_CONTENT)
                throw ServiceException.RESOURCE_UNREACHABLE(method.getStatusText(), null,
                		new ServiceException.InternalArgument(HTTP_URL, url, ServiceException.Argument.Type.STR),
                		new ServiceException.InternalArgument(HTTP_STATUS_CODE, statusCode, ServiceException.Argument.Type.NUM));

            List<Header> headers = new ArrayList<Header>(Arrays.asList(method.getResponseHeaders()));
            headers.add(new Header("X-Zimbra-Http-Status", ""+statusCode));
            return new Pair<Header[], HttpMethod>(headers.toArray(new Header[0]), method);
        } catch (HttpException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("HttpException while fetching " + url, e);
        } catch (IOException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("IOException while fetching " + url, e);
        }
    }
}
