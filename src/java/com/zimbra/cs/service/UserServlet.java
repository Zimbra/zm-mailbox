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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;

import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.formatter.*;
import com.zimbra.cs.service.mail.GetItem;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.NetUtil;

/**
 * 
 * <pre>
 *   http://server/service/user/[&tilde;][{username}]/[{folder}]?[{query-params}]
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
    private static final long serialVersionUID = -5313094316561384586L;

    public static final String SERVLET_PATH = "/home";

    public static final String QP_AUTHTOKEN = "authToken";
    
    public static final String QP_FMT = "fmt"; // format query param
    
    public static final String QP_ZLV = "zlv"; // zip level query param
    
    public static final String QP_ID = "id"; // id query param
    
    public static final String QP_LIST = "list"; // list query param

    public static final String QP_IMAP_ID = "imap_id"; // IMAP id query param

    public static final String QP_PART = "part"; // part query param
    
    public static final String QP_BODY = "body"; // body query param
    
    public static final String BODY_TEXT = "text"; // return text body

    public static final String BODY_HTML = "html"; // return html body if possible

    public static final String QP_QUERY = "query"; // query query param

    public static final String QP_VIEW = "view"; // view query param    

    public static final String QP_TYPES = "types"; // types

    public static final String QP_START = "start"; // start time

    public static final String QP_END = "end"; // end time

    public static final String QP_IGNORE_ERROR = "ignore";  // ignore and continue on error during ics import

    public static final String QP_OFFSET = "offset"; // offset into results
    
    public static final String QP_LIMIT = "limit"; // offset into results

    public static final String QP_AUTH = "auth"; // auth types

    public static final String QP_DISP = "disp"; // disposition (a = attachment, i = inline)

    public static final String QP_NAME = "name"; // filename/path segments, added to pathInfo
    
    public static final String QP_CSVFORMAT = "csvfmt"; // csv type (outlook-2003-csv, yahoo-csv, ...)

    public static final String QP_VERSION = "ver";  // version for WikiItem and Document

    public static final String QP_HISTORY = "history";  // history for WikiItem
    
    public static final String QP_LANGUAGE = "language"; // all three
    
    public static final String QP_COUNTRY = "country"; // all three

    public static final String QP_VARIANT = "variant"; // all three
    
    public static final String AUTH_COOKIE = "co"; // auth by cookie

    public static final String AUTH_BASIC = "ba"; // basic auth

    public static final String AUTH_QUERYPARAM = "qp"; // query parameter

    public static final String AUTH_NO_SET_COOKIE = "nsc"; // don't set auth token cookie after basic auth

    public static final String AUTH_DEFAULT = "co,ba,qp"; // all three

    private HashMap<String, Formatter> mFormatters;
    private HashMap<String, Formatter> mDefaultFormatters;

    protected static final String MSGPAGE_BLOCK = "errorpage.attachment.blocked";
    private String mBlockPage = null;

    /** Default maximum upload size for PUT/POST write ops: 10MB. */
    private static final long DEFAULT_MAX_SIZE = 10 * 1024 * 1024;

    /** Returns the REST URL for the account. */
    public static String getRestUrl(Account acct) throws ServiceException {
    	return getServiceUrl(acct, UserServlet.SERVLET_PATH);
    }
    
    /** Returns the REST URL for the mail item. */
    public static String getRestUrl(MailItem item) throws ServiceException, IOException {
    	Account acct = item.getMailbox().getAccount();
        String url = getRestUrl(acct) + item.getPath();

        if (url.startsWith("https"))
        	url = new HttpsURL(url).toString();
        else
            url = new HttpURL(url).toString();    		

    	return url;
    }
    	    
    

    public UserServlet() {
        mFormatters = new HashMap<String, Formatter>();
        addFormatter(new CsvFormatter());
        addFormatter(new VcfFormatter());
        addFormatter(new IcsFormatter());
        addFormatter(new RssFormatter());
        addFormatter(new AtomFormatter());
        addFormatter(new NativeFormatter());
        addFormatter(new ZipFormatter());
        addFormatter(new FreeBusyFormatter());
        addFormatter(new IfbFormatter());
        addFormatter(new SyncFormatter());
        addFormatter(new WikiFormatter());
        addFormatter(new XmlFormatter());
        addFormatter(new JsonFormatter());
        addFormatter(new HtmlFormatter());

        mDefaultFormatters = new HashMap<String, Formatter>();
        for (Formatter fmt : mFormatters.values())
            for (String mimeType : fmt.getDefaultMimeTypes())
                mDefaultFormatters.put(mimeType, fmt);
    }

    private void addFormatter(Formatter f) {
        f.setServlet(this);
        mFormatters.put(f.getType(), f);
    }

    public Formatter getFormatter(String type) {
        return mFormatters.get(type);
    }

    private Mailbox getTargetMailbox(Context context) throws ServiceException {

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
    
    private void getAccount(Context context) throws IOException, ServletException {
        try {
            boolean isAdminRequest = isAdminRequest(context);
            // check cookie first
            if (context.cookieAuthAllowed()) {
                context.authAccount = cookieAuthRequest(context.req, context.resp, true);
                if (context.authAccount != null) {
                    context.cookieAuthHappened = true;
                    try {
                        AuthToken at = isAdminRequest ? getAdminAuthTokenFromCookie(context.req, context.resp, true) : getAuthTokenFromCookie(context.req, context.resp, true);
                        context.authTokenCookie = at.getEncoded();
                    } catch (AuthTokenException e) {
                    }
                    return;
                }
            }

            // check query string
            if (context.queryParamAuthAllowed()) {
                String auth = context.params.get(QP_AUTHTOKEN);
                if (auth != null) {
                    try {
                        AuthToken at = AuthToken.getAuthToken(auth);
                        if (!at.isExpired()) {
                            context.qpAuthHappened = true;
                            context.authTokenCookie = at.getEncoded();
                            context.authAccount = Provisioning.getInstance().get(AccountBy.id, at.getAccountId());
                        }
                    } catch (AuthTokenException e) {
                    }
                    return;
                }
            }
            
            // fallback to basic auth
            if (context.basicAuthAllowed()) {
                context.authAccount = basicAuthRequest(context.req, context.resp, false);
                if (context.authAccount != null) {
                    context.basicAuthHappened = true;
                    // send cookie back if need be. 
                    if (!context.noSetCookie()) {
                        try {
                            context.authTokenCookie = new AuthToken(context.authAccount, isAdminRequest).getEncoded();
                            context.resp.addCookie(new Cookie(isAdminRequest ? COOKIE_ZM_ADMIN_AUTH_TOKEN : COOKIE_ZM_AUTH_TOKEN, context.authTokenCookie));
                        } catch (AuthTokenException e) {
                        }
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

    private boolean isAdminRequest(Context context) throws ServiceException {
        int adminPort = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, -1);
        return (context.req.getLocalPort() == adminPort);
    }

    private void sendError(Context ctxt, HttpServletRequest req, HttpServletResponse resp, String message) throws IOException {
        if (ctxt == null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, message);
        } else if (!ctxt.cookieAuthHappened && ctxt.basicAuthAllowed() && !ctxt.basicAuthHappened) {
            resp.addHeader(WWW_AUTHENTICATE_HEADER, getRealmHeader());
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, message);
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        Context context = null;
        ZimbraLog.clearContext();
        try {
            context = new Context(req, resp, this);
            if (!checkAuthentication(req, resp, context)) {
                sendError(context, req, resp, "must authenticate");
                return;
            }

            if (isProxyRequest(req, resp, context))
                return;

            // at this point context.authAccount is set either from the Cookie,
            // or from basic auth.  if there was no credential in either the Cookie
            // or basic auth, authAccount is set to anonymous account.
            if (context.authAccount != null) {
                ZimbraLog.addAccountNameToContext(context.authAccount.getName());
                if(context.authAccount.getLocale() != null && context.locale == null){
                	context.locale = context.authAccount.getLocale();
                }
            }
            addRemoteIpToLoggingContext(req);

            doAuthGet(req, resp, context);

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

    private boolean checkAuthentication(HttpServletRequest req, HttpServletResponse resp, Context context) throws IOException, ServletException {
        // if they specify /~/, we must auth
        if (context.targetAccount == null && context.accountPath.equals("~")) {
            getAccount(context);
            if (context.authAccount == null)
                return false;
            context.targetAccount = context.authAccount;
        }

        // auth is required if we haven't yet authed and they've specified a formatter that requires auth (including write ops).
        boolean authRequired = context.authAccount == null &&
                (context.formatter == null || context.formatter.requiresAuth() || req.getMethod().equalsIgnoreCase("POST"));

        // need this before proxy if we want to support sending cookie from a basic-auth
        if (authRequired) {
            getAccount(context);
            if (context.authAccount == null) {
                context.setAnonymousRequest();
            }
        }

        return true;
    }

    private boolean isProxyRequest(HttpServletRequest req, HttpServletResponse resp, Context context) throws IOException, ServiceException {
        // this should handle both explicit /user/user-on-other-server/ and
        // /user/~/?id={account-id-on-other-server}:id            

        if (context.targetAccount != null && !Provisioning.onLocalServer(context.targetAccount)) {
            try {
                if (context.basicAuthHappened && context.authTokenCookie == null)
                    context.authTokenCookie = new AuthToken(context.authAccount, isAdminRequest(context)).getEncoded();
                Provisioning prov = Provisioning.getInstance();                
                proxyServletRequest(req, resp, prov.getServer(context.targetAccount), context.basicAuthHappened ? context.authTokenCookie : null);
                return true;
            } catch (AuthTokenException e) {
                throw ServiceException.FAILURE("proxy error", e);
            }
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

        context.opContext = new OperationContext(context.authAccount, isAdminRequest(context));
        Mailbox mbox = getTargetMailbox(context);
        if (mbox != null) {
            ZimbraLog.addMboxToContext(mbox.getId());
            if (context.reqListIds != null) {
            	resolveItems(context);
            } else {
    	        MailItem item = resolveItem(context, true);
    	        if (item instanceof Mountpoint) {
    	            // if the target is a mountpoint, proxy the request on to the resolved target
    	            proxyOnMountpoint(req, resp, context, (Mountpoint) item);
    	            return;
    	        }
            }
        }
        
        resolveFormatter(context);

        // Prevent harvest attacks.  If mailbox doesn't exist for a request requiring authentication,
        // return auth error instead of "no such mailbox".  If request/formatter doesn't require
        // authentication, call the formatter and let it deal with preventing harvest attacks.
        if (mbox == null && context.formatter.requiresAuth())
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions");

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
    public void doPut(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        doPost(req, resp);
    }

    /** Adds an item to a folder specified in the URI.  The item content is
     *  provided in the POST request's body. */
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        Context context = null;
        ZimbraLog.clearContext();
        try {
            context = new Context(req, resp, this);
            if (!checkAuthentication(req, resp, context)) {
                sendError(context, req, resp, "must authenticate");
                return;
            }

            if (isProxyRequest(req, resp, context))
                return;

            if (context.authAccount != null)
                ZimbraLog.addAccountNameToContext(context.authAccount.getName());
            addRemoteIpToLoggingContext(req);

            Folder folder = null;
            String filename = null;
            Mailbox mbox = getTargetMailbox(context);
            if (mbox != null) {
                ZimbraLog.addMboxToContext(mbox.getId());
    
                ZimbraLog.mailbox.info("UserServlet (POST): " + context.req.getRequestURL().toString());
    
                context.opContext = new OperationContext(context.authAccount, isAdminRequest(context));
    
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
                }
    
                folder = (context.target instanceof Folder ? (Folder) context.target : mbox.getFolderById(context.opContext, context.target.getFolderId()));
    
                if (context.target != folder) {
                    if (filename == null)
                        filename = context.target.getName();
                    else
                        // need to fail on POST to "Notebook/existing-file/random-cruft"
                        throw MailServiceException.NO_SUCH_FOLDER(context.itemPath);
                }
    
                if (folder instanceof Mountpoint) {
                    // if the target is a mountpoint, proxy the request on to the resolved target
                    context.extraPath = filename;
                    proxyOnMountpoint(req, resp, context, (Mountpoint) folder);
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

            // get the POST body content
            long sizeLimit = Provisioning.getInstance().getLocalServer().getLongAttr(Provisioning.A_zimbraFileUploadMaxSize, DEFAULT_MAX_SIZE);
            byte[] body = ByteUtil.getContent(req.getInputStream(), req.getContentLength(), sizeLimit);

            context.target = folder;
            resolveFormatter(context);
            if (!context.formatter.supportsSave())
                sendError(context, req, resp, "format not supported for save");

            // Prevent harvest attacks.  If mailbox doesn't exist for a request requiring authentication,
            // return auth error instead of "no such mailbox".  If request/formatter doesn't require
            // authentication, call the formatter and let it deal with preventing harvest attacks.
            if (mbox == null && context.formatter.requiresAuth())
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");

            context.formatter.save(body, context, ctype, folder, filename);
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
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "unsupported format");
        }

        if (context.formatter == null)
            context.formatter = mFormatters.get(context.format);
        if (context.formatter == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "unsupported format");
    }
    
    private void resolveItems(Context context) throws ServiceException {
    	context.respListItems = new ArrayList<MailItem>();
    	
    	for (int id : context.reqListIds) {
    		try {
    			context.respListItems.add(GetItem.getItemById(context.opContext, context.targetMailbox, id, MailItem.TYPE_UNKNOWN));
    		} catch (NoSuchItemException x) {
    			ZimbraLog.misc.info(x.getMessage());
    		} catch (ServiceException x) {
                if (x.getCode() == ServiceException.PERM_DENIED) {
                	ZimbraLog.misc.info(x.getMessage());
                } else {
                	throw x;
                }
    		}
    	}
    	
    	//We consider partial success OK.  Let the client figure out which item is missing
    	if (context.respListItems.size() == 0) {
    		throw MailServiceException.NO_SUCH_ITEM(context.reqListIds.toString());
    	}
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
    private MailItem resolveItem(Context context, boolean checkExtension) throws ServiceException, UserServletException {
        if (context.formatter != null && !context.formatter.requiresAuth())
            return null;

        // special-case the fetch-by-IMAP-id option
        if (context.imapId > 0) {
            // fetch the folder from the path
            Folder folder = context.targetMailbox.getFolderByPath(context.opContext, context.itemPath);
            
            // and then fetch the item from the "imap_id" query parameter
            return GetItem.getItemByImapId(context.opContext, context.targetMailbox, context.imapId, folder.getId());
        }

        if (context.itemId != null) {
            context.target = GetItem.getItemById(context.opContext, context.targetMailbox, context.itemId.getId(), MailItem.TYPE_UNKNOWN);

            context.itemPath = context.target.getPath();
            if (context.target instanceof Mountpoint || context.extraPath == null || context.extraPath.equals(""))
                return context.target;
            if (context.itemPath == null)
                throw MailServiceException.NO_SUCH_ITEM("?id=" + context.itemId + "&name=" + context.extraPath);
            context.itemId = null;
        }

        if (context.extraPath != null && !context.extraPath.equals("")) {
            context.itemPath = (context.itemPath + '/' + context.extraPath).replaceAll("//+", "/");
            context.extraPath = null;
        }

        try {
            // first, try the full path
            context.target = GetItem.getItemByPath(context.opContext, context.targetMailbox, context.itemPath, MailItem.TYPE_UNKNOWN);
        } catch (ServiceException e) {
            if (!(e instanceof NoSuchItemException) && e.getCode() != ServiceException.PERM_DENIED)
                throw e;

            // no joy.  if they asked for something like "calendar.csv" (where "calendar" was the folder name), try again minus the extension
            if (!checkExtension || context.format != null)
                throw e;

            /* if path == /foo/bar/baz.html, then
             *      format -> html
             *      path   -> /foo/bar/baz  */
            int dot = context.itemPath.lastIndexOf('.');
            if (dot == -1)
                throw e;

            String unsuffixedPath = context.itemPath.substring(0, dot);

            // try again, w/ the extension removed...
            context.target = GetItem.getItemByPath(context.opContext, context.targetMailbox, unsuffixedPath, MailItem.TYPE_UNKNOWN);

            context.format = context.itemPath.substring(dot + 1);
            context.itemPath = unsuffixedPath;
        }

        // don't think this code can ever get called because <tt>item</tt> can't be null at this point
        if (context.target == null && context.getQueryString() == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "item not found");

        return context.target;
    }

    private void proxyOnMountpoint(HttpServletRequest req, HttpServletResponse resp, Context context, Mountpoint mpt)
    throws IOException, ServiceException, UserServletException {
        String uri = SERVLET_PATH + "/~/?" + QP_ID + '=' + mpt.getOwnerId() + "%3A" + mpt.getRemoteId();
        if (context.format != null)
            uri += '&' + QP_FMT + '=' + URLEncoder.encode(context.format, "UTF-8");
        if (context.extraPath != null)
            uri += '&' + QP_NAME + '=' + URLEncoder.encode(context.extraPath, "UTF-8");
        for (Map.Entry<String, String> entry : HttpUtil.getURIParams(req).entrySet()) {
            String qp = entry.getKey();
            if (!qp.equals(QP_ID) && !qp.equals(QP_FMT))
                uri += '&' + URLEncoder.encode(qp, "UTF-8") + '=' + URLEncoder.encode(entry.getValue(), "UTF-8");
        }

        try {
            if (context.basicAuthHappened && context.authTokenCookie == null) 
                context.authTokenCookie = new AuthToken(context.authAccount, isAdminRequest(context)).getEncoded();
        } catch (AuthTokenException e) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "cannot generate auth token");
        }

        Provisioning prov = Provisioning.getInstance();
        Account targetAccount = prov.get(AccountBy.id, mpt.getOwnerId());
        if (targetAccount == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "referenced account not found");

        proxyServletRequest(req, resp, prov.getServer(targetAccount), uri, context.basicAuthHappened ? context.authTokenCookie : null);
    }

    public static class Context {
        public HttpServletRequest req;
        public HttpServletResponse resp;
        public Map<String, String> params;
        public String format;
        public Formatter formatter;        
        public boolean cookieAuthHappened;
        public boolean basicAuthHappened;
        public boolean qpAuthHappened;
        public String accountPath;
        public String authTokenCookie;
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

        Context(HttpServletRequest request, HttpServletResponse response, UserServlet servlet)
        throws UserServletException, ServiceException {
            Provisioning prov = Provisioning.getInstance();

            this.req = request;
            this.resp = response;
            this.params = HttpUtil.getURIParams(request);
            
            //rest url override for locale
            String language = this.params.get(QP_LANGUAGE);
            if (language != null) {
                String country =  this.params.get(QP_COUNTRY);
                if (country != null) {
                    String variant = this.params.get(QP_VARIANT);
                    if (variant != null) {
                        this.locale = new Locale(language, country, variant);
                    }
                    this.locale =  new Locale(language, country);
                }
                this.locale =  new Locale(language);
            }else{
            	this.locale =  req.getLocale();
            }            
            
            String pathInfo = request.getPathInfo().toLowerCase();
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("") || !pathInfo.startsWith("/"))
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "invalid path");
            int pos = pathInfo.indexOf('/', 1);
            if (pos == -1)
                pos = pathInfo.length();
            if (pos < 1)
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "invalid path");

            String checkInternalDispatch = (String) request.getAttribute("zimbra-internal-dispatch");
            if (checkInternalDispatch != null && checkInternalDispatch.equals("yes")) {
                // XXX extra decoding is necessary if the HttpRequest was
                // re-dispatched internally from SetHeaderFilter class in
                // zimbra app.  it appears org.apache.catalina.core.ApplicationHttpRequest
                // does not perform URL decoding on pathInfo.  refer to
                // bug 8159.
                try {
                    pathInfo = URLDecoder.decode(pathInfo, "UTF-8");
                } catch (UnsupportedEncodingException uee) {
                    ZimbraLog.misc.info("cannot decode pathInfo " + pathInfo);
                }
            }
            this.accountPath = pathInfo.substring(1, pos);

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
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "invalid id requested");
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
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "invalid imap id requested");
            }

            if (this.format != null) {
                this.format = this.format.toLowerCase();
                this.formatter = servlet.getFormatter(this.format);
                if (this.formatter == null)
                    throw new UserServletException(HttpServletResponse.SC_NOT_IMPLEMENTED, "not implemented yet");
            }                

            // see if we can get target account or not
            if (itemId != null && itemId.getAccountId() != null) {
                targetAccount = prov.get(AccountBy.id, itemId.getAccountId());                                
                return;
            } else if (accountPath.equals("~")) {
                // can't resolve this yet
                return;
            } else if (accountPath.startsWith("~")) {
                accountPath = accountPath.substring(1);
            }
            targetAccount = prov.get(AccountBy.name, accountPath);                
        }

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

        public String getQueryString() {
            return params.get(QP_QUERY);
        }

        public boolean cookieAuthAllowed() {
            return getAuth().indexOf(AUTH_COOKIE) != -1;
        }

        public boolean noSetCookie() {
            return authAccount != null && authAccount instanceof ACL.GuestAccount
            || getAuth().indexOf(AUTH_NO_SET_COOKIE) != -1;
        }

        public boolean basicAuthAllowed() {
            return getAuth().indexOf(AUTH_BASIC) != -1;
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
        

        public void setAnonymousRequest() {
            authAccount = ACL.ANONYMOUS_ACCT;
        }

        public boolean isAnonymousRequest() {
            return authAccount.equals(ACL.ANONYMOUS_ACCT);
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
            //case MailItem.TYPE_DOCUMENT:   // use native formatter for Document
                return "wiki";
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


    public static byte[] getRemoteContent(String authToken, ItemId iid, Map<String, String> params) throws ServiceException {
        return getRemoteResource(authToken, iid, params).getSecond();
    }

    public static byte[] getRemoteContent(String authToken, Account target, String folder, Map<String,String> params) throws ServiceException {
        return getRemoteResource(authToken, target, folder, params).getSecond();
    }

    public static byte[] getRemoteContent(String authToken, String hostname, String url) throws ServiceException {
        return getRemoteResource(authToken, hostname, url).getSecond();
    }

    public static Pair<Header[], byte[]> getRemoteResource(String authToken, ItemId iid, Map<String, String> params) throws ServiceException {
        Account target = Provisioning.getInstance().get(AccountBy.id, iid.getAccountId());
        Map<String, String> pcopy = new HashMap<String, String>(params);
        pcopy.put(QP_ID, iid.toString());
        return getRemoteResource(authToken, target, null, pcopy);
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

        StringBuffer url = new StringBuffer(UserServlet.getRestUrl(target));
        url.append("/").append(folder).append("/?");
        url.append(QP_AUTH).append('=').append(AUTH_COOKIE);
        if (params != null) {
            for (Map.Entry<String, String> param : params.entrySet())
                url.append('&').append(param.getKey()).append('=').append(param.getValue());
        }
        return url.toString();
    }

    public static Pair<Header[], byte[]> getRemoteResource(String authToken, Account target, String folder, Map<String,String> params) throws ServiceException {
        // fetch from remote store
        Provisioning prov = Provisioning.getInstance();
        Server server = (target == null ? prov.getLocalServer() : prov.getServer(target));
        String hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);
        String url = getRemoteUrl(target, folder, params);

        return getRemoteResource(authToken, hostname, url);
    }

    public static Pair<Header[], byte[]> getRemoteResource(String authToken, String hostname, String url) throws ServiceException {
        return getRemoteResource(authToken, hostname, url, null, 0, null, null);
    }

    public static Pair<Header[], byte[]> getRemoteResource(String authToken, String hostname, String url,
            String proxyHost, int proxyPort, String proxyUser, String proxyPass) throws ServiceException {
        HttpMethod get = null;
        try {
            Pair<Header[], HttpMethod> pair = getRemoteResourceInternal(authToken, hostname, url, proxyHost, proxyPort, proxyUser, proxyPass);
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
    throws ServiceException, IOException, AuthTokenException {
        Map<String, String> pcopy = new HashMap<String, String>(params);
        pcopy.put(QP_ID, iid.toString());

        // fetch from remote store
        Provisioning prov = Provisioning.getInstance();
        Account target = prov.get(AccountBy.id, iid.getAccountId());
        String url = getRemoteUrl(target, null, pcopy);
        Server server = (target == null ? prov.getLocalServer() : prov.getServer(target));
        String hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);

        Pair<Header[], HttpInputStream> response = getRemoteResourceAsStream(at.getEncoded(), hostname, url);

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


    //Helper class so that we can close connection upon stream close
    public static class HttpInputStream extends FilterInputStream {
		private HttpMethod method;

    	HttpInputStream(HttpMethod m) throws IOException {
    		super(m.getResponseBodyAsStream());
    		this.method = m;
    	}

    	@Override public void close() {
    		method.releaseConnection();
    	}
    }
    
    public static Pair<Header[], HttpInputStream> getRemoteResourceAsStream(String authToken, String hostname, String url) throws ServiceException, IOException {
    	return getRemoteResourceAsStream(authToken, hostname, url, null, 0, null, null);
    }
    
    public static Pair<Header[], HttpInputStream> getRemoteResourceAsStream(String authToken, String hostname, String url,
    		String proxyHost, int proxyPort, String proxyUser, String proxyPass) throws ServiceException, IOException {
    	Pair<Header[], HttpMethod> pair = getRemoteResourceInternal(authToken, hostname, url, proxyHost, proxyPort, proxyUser, proxyPass);
    	return new Pair<Header[], HttpInputStream>(pair.getFirst(), new HttpInputStream(pair.getSecond()));
    }


    public static Pair<Header[], HttpInputStream> putRemoteResource(String authToken, String url, Account target,
            byte[] req, Header[] headers) throws ServiceException, IOException {
        Provisioning prov = Provisioning.getInstance();
        Server server = (target == null ? prov.getLocalServer() : prov.getServer(target));
        String hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);

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
        method.setRequestEntity(new ByteArrayRequestEntity(req, contentType));
        Pair<Header[], HttpMethod> pair = doHttpOp(authToken, hostname, null, 0, null, null, method);
        return new Pair<Header[], HttpInputStream>(pair.getFirst(), new HttpInputStream(pair.getSecond()));
    }


    private static Pair<Header[], HttpMethod> getRemoteResourceInternal(String authToken, String hostname, String url,
    		String proxyHost, int proxyPort, String proxyUser, String proxyPass) throws ServiceException {
        return doHttpOp(authToken, hostname, proxyHost, proxyPort, proxyUser, proxyPass, new GetMethod(url));
    }

    private static Pair<Header[], HttpMethod> doHttpOp(String authToken, String hostname,
            String proxyHost, int proxyPort, String proxyUser, String proxyPass, HttpMethod method) throws ServiceException {
        // create an HTTP client with the same cookies
        String url = "";
        try {
            url = method.getURI().toString();
        } catch (IOException e) {
        }
        HttpState state = new HttpState();
        state.addCookie(new org.apache.commons.httpclient.Cookie(hostname, COOKIE_ZM_AUTH_TOKEN, authToken, "/", null, false));
        HttpClient client = new HttpClient();
        client.setState(state);
    	if (proxyHost != null && proxyPort > 0) {
    		client.getHostConfiguration().setProxy(proxyHost, proxyPort);
    		if (proxyUser != null && proxyPass != null) {
    			client.getState().setProxyCredentials(new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials(proxyUser, proxyPass));
    		}
    	} else {
    	    NetUtil.configureProxy(client);
    	}
        
        try {
            int statusCode = client.executeMethod(method);
            if (statusCode == HttpStatus.SC_NOT_FOUND)
                throw MailServiceException.NO_SUCH_ITEM(-1);
            else if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_CREATED)
                throw ServiceException.RESOURCE_UNREACHABLE(method.getStatusText(), null);

            Header[] headers = method.getResponseHeaders();
            return new Pair<Header[], HttpMethod>(headers, method);
        } catch (HttpException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("HttpException while fetching " + url, e);
        } catch (IOException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("IOException while fetching " + url, e);
        }
    }
}
