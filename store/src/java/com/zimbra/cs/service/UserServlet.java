/*
 * ***** BEGIN LICENSE BLOCK ***** Zimbra Collaboration Suite Server Copyright
 * (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016
 * Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>. *****
 * END LICENSE BLOCK *****
 */

package com.zimbra.cs.service;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.httpclient.InputStreamRequestHttpRetryHandler;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ZimbraAuthTokenEncoded;
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
import com.zimbra.cs.service.admin.AdminAccessControl;
import com.zimbra.cs.service.formatter.Formatter;
import com.zimbra.cs.service.formatter.FormatterFactory;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;
import com.zimbra.cs.service.formatter.IfbFormatter;
import com.zimbra.cs.service.formatter.OctopusPatchFormatter;
import com.zimbra.cs.service.formatter.TarFormatter;
import com.zimbra.cs.service.formatter.ZipFormatter;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.UserServletUtil;
import com.zimbra.cs.servlet.CsrfFilter;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.servlet.util.AuthUtil;
import com.zimbra.cs.servlet.util.CsrfUtil;
import com.zimbra.cs.util.AccountUtil;

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
 *                            jwt    JWT based auth
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

    public static final String QP_NOHIERARCHY = "nohierarchy"; // nohierarchy
                                                               // query param

    public static final String QP_ZLV = "zlv"; // zip level query param

    public static final String QP_ID = "id"; // id query param

    public static final String QP_LIST = "list"; // list query param

    public static final String QP_IMAP_ID = "imap_id"; // IMAP id query param

    public static final String QP_PART = "part"; // part query param

    /**
     * Body query param. Also used by {@link ZipFormatter} and
     * {@link TarFormatter} to specify whether the entire message should be
     * returned (<tt>body=1</tt>), or just the headers (<tt>body=0</tt>). The
     * default is <tt>1</tt>.
     */
    public static final String QP_BODY = "body"; // body query param

    public static final String BODY_TEXT = "text"; // return text body

    public static final String BODY_HTML = "html"; // return html body if
                                                   // possible

    public static final String QP_QUERY = "query"; // query query param

    public static final String QP_SORT = "sort"; // sort query param

    public static final String QP_VIEW = "view"; // view query param

    public static final String QP_TYPES = "types"; // types

    public static final String QP_START = "start"; // start time

    public static final String QP_END = "end"; // end time

    public static final String QP_FREEBUSY_CALENDAR = "fbcal"; // calendar
                                                               // folder to run
                                                               // free/busy
                                                               // search on

    public static final String QP_IGNORE_ERROR = "ignore"; // ignore and
                                                           // continue on error
                                                           // during ics import

    public static final String QP_PRESERVE_ALARMS = "preserveAlarms"; // preserve
                                                                      // existing
                                                                      // alarms
                                                                      // during
                                                                      // ics
                                                                      // import

    public static final String QP_OFFSET = "offset"; // offset into results

    public static final String QP_LIMIT = "limit"; // offset into results

    public static final String QP_AUTH = "auth"; // auth types

    public static final String QP_DISP = "disp"; // disposition (a = attachment,
                                                 // i = inline)

    public static final String QP_NAME = "name"; // filename/path segments,
                                                 // added to pathInfo

    public static final String QP_CSVFORMAT = "csvfmt"; // csv type
                                                        // (outlook-2003-csv,
                                                        // yahoo-csv, ...)

    public static final String QP_CSVLOCALE = "csvlocale"; // refining locale
                                                           // for csvfmt - e.g.
                                                           // zh-CN

    public static final String QP_CSVSEPARATOR = "csvsep"; // separator

    public static final String QP_VERSION = "ver"; // version for WikiItem and
                                                   // Document

    public static final String QP_HISTORY = "history"; // history for WikiItem

    public static final String QP_LANGUAGE = "language"; // all three

    public static final String QP_COUNTRY = "country"; // all three

    public static final String QP_VARIANT = "variant"; // all three

    public static final String UPLOAD_NAME = "uploadName"; // upload filename

    public static final String UPLOAD_TYPE = "uploadType"; // upload content
                                                           // type

    public static final String QP_FBFORMAT = "fbfmt"; // free/busy format - "fb"
                                                      // (default) or "event"

    /**
     * Used by {@link OctopusPatchFormatter}
     */
    public static final String QP_MANIFEST = "manifest"; // selects whether
                                                         // server returns patch
                                                         // manifest or not

    public static final String QP_DUMPSTER = "dumpster"; // whether search in
                                                         // dumpster

    /**
     * Used by {@link TarFormatter} to specify whether the <tt>.meta</tt> files
     * should be added to the tarball (<tt>meta=1</tt>) or not
     * (<tt>meta=0</tt>). The default is <tt>1</tt>.
     */
    public static final String QP_META = "meta";

    /**
     * Used by {@link IfbFormatter} to specify the UID of calendar item to
     * exclude when computing free/busy.
     */
    public static final String QP_EXUID = "exuid";

    public static final String AUTH_COOKIE = "co"; // auth by cookie

    public static final String AUTH_BASIC = "ba"; // basic auth

    public static final String AUTH_QUERYPARAM = "qp"; // query parameter

    public static final String AUTH_NO_SET_COOKIE = "nsc"; // don't set auth
                                                           // token cookie after
                                                           // basic auth
                                                           // same as ba after
                                                           // bug 42782

    public static final String AUTH_JWT = "jwt"; // auth by jwt

    // see https://bugzilla.zimbra.com/show_bug.cgi?id=42782#c11
    public static final String AUTH_SET_COOKIE = "sc"; // set auth token cookie
                                                       // after basic auth

    public static final String AUTH_DEFAULT = "co,jwt,nsc,qp"; // all four

    public static final String HTTP_URL = "http_url";
    public static final String HTTP_STATUS_CODE = "http_code";

    public static final String QP_MAX_WIDTH = "max_width";
    public static final String QP_MAX_HEIGHT = "max_height";

    protected static final String MSGPAGE_BLOCK = "errorpage.attachment.blocked";

    // used in restCalendar
    public static final String QP_ACTION = "action";
    public static final String QP_BODYPART = "bodypart";
    public static final String QP_COLOR = "color";
    public static final String QP_DATE = "date";
    public static final String QP_EX_COMP_NUM = "exCompNum";
    public static final String QP_EX_INV_ID = "exInvId";
    public static final String QP_FOLDER_IDS = "folderIds";
    public static final String QP_IM_ID = "im_id";
    public static final String QP_IM_PART = "im_part";
    public static final String QP_IM_XIM = "im_xim";
    public static final String QP_INST_DURATION = "instDuration";
    public static final String QP_INST_START_TIME = "instStartTime";
    public static final String QP_INV_COMP_NUM = "invCompNum";
    public static final String QP_INV_ID = "invId";
    public static final String QP_NOTOOLBAR = "notoolbar";
    public static final String QP_NUMDAYS = "numdays";
    public static final String QP_PSTAT = "pstat";
    public static final String QP_REFRESH = "refresh";
    public static final String QP_SKIN = "skin";
    public static final String QP_SQ = "sq";
    public static final String QP_TZ = "tz";
    public static final String QP_USE_INSTANCE = "useInstance";
    public static final String QP_XIM = "xim";

    public static final Log log = LogFactory.getLog(UserServlet.class);

    /** Returns the REST URL for the account. */
    public static String getRestUrl(Account acct) throws ServiceException {
        return getServiceUrl(acct, UserServlet.SERVLET_PATH);
    }

    /** Returns the REST URL for the mail item. */
    public static String getRestUrl(MailItem item) throws ServiceException {
        Account acct = item.getMailbox().getAccount();
        return getRestUrl(acct) + HttpUtil.urlEscape(item.getPath());
    }

    public static Formatter getFormatter(FormatType type) {
        return FormatterFactory.mFormatters.get(type);
    }

    private void sendError(UserServletContext ctxt, HttpServletRequest req, HttpServletResponse resp, String message)
            throws IOException {
        if (resp.isCommitted()) {
            log.info("Response already committed. Skipping sending error code for response");
            return;
        }
        if (ctxt != null && !ctxt.cookieAuthHappened && ctxt.basicAuthAllowed() && !ctxt.basicAuthHappened) {
            resp.addHeader(AuthUtil.WWW_AUTHENTICATE_HEADER, getRealmHeader(req, null));
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, L10nUtil.getMessage(MsgKey.errMustAuthenticate, req));
        } else if (ctxt != null && ctxt.cookieAuthHappened && !ctxt.isCsrfAuthSucceeded()
                && (req.getMethod().equalsIgnoreCase("POST") || req.getMethod().equalsIgnoreCase("PUT"))) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, L10nUtil.getMessage(MsgKey.errMustAuthenticate, req));
        } else if (ctxt != null && ctxt.getAuthAccount() instanceof GuestAccount && ctxt.basicAuthAllowed()) {
            resp.addHeader(AuthUtil.WWW_AUTHENTICATE_HEADER, getRealmHeader(req, null));
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, L10nUtil.getMessage(MsgKey.errMustAuthenticate, req));
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, message);
        }
    }

    protected UserServletContext createContext(HttpServletRequest req, HttpServletResponse resp, UserServlet servlet)
            throws UserServletException, ServiceException {
        return new UserServletContext(req, resp, servlet);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserServletContext context = null;
        ZimbraLog.clearContext();
        addRemoteIpToLoggingContext(req);
        try {
            context = createContext(req, resp, this);
            if (!checkAuthentication(context)) {
                sendError(context, req, resp, L10nUtil.getMessage(MsgKey.errMustAuthenticate, req));
                return;
            }

            checkTargetAccountStatus(context);

            if (proxyIfRemoteTargetAccount(req, resp, context)) {
                return;
            }
            // at this point context.authAccount is set either from the Cookie,
            // or from basic auth. if there was no credential in either the
            // Cookie
            // or basic auth, authAccount is set to anonymous account.
            if (context.getAuthAccount() != null) {
                ZimbraLog.addAccountNameToContext(context.getAuthAccount().getName());
            }

            doAuthGet(req, resp, context);

        } catch (ServiceException se) {
            if (se.getCode() == ServiceException.PERM_DENIED || se instanceof NoSuchItemException)
                sendError(context, req, resp, L10nUtil.getMessage(MsgKey.errNoSuchItem, req));
            else if (se.getCode() == AccountServiceException.MAINTENANCE_MODE
                    || se.getCode() == AccountServiceException.ACCOUNT_INACTIVE)
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

    private boolean checkAuthentication(UserServletContext context)
            throws IOException, ServletException, UserServletException {

        // if they specify /~/, we must auth
        if (context.targetAccount == null && context.accountPath != null && context.accountPath.equals("~")) {
            UserServletUtil.getAccount(context);
            if (context.getAuthAccount() == null) {
                return false;
            }
            context.targetAccount = context.getAuthAccount();
        }

        // need this before proxy if we want to support sending cookie from a
        // basic-auth
        UserServletUtil.getAccount(context);
        if (context.getAuthAccount() == null) {
            context.setAnonymousRequest();
        }
        return true;
    }

    private void checkTargetAccountStatus(UserServletContext context) throws ServiceException {
        if (context.targetAccount != null) {
            String acctStatus = context.targetAccount.getAccountStatus(Provisioning.getInstance());

            // no one can touch an account if it in maintenance mode
            if (Provisioning.ACCOUNT_STATUS_MAINTENANCE.equals(acctStatus))
                throw AccountServiceException.MAINTENANCE_MODE();

            // allow only admin access if the account is not active
            if (!Provisioning.ACCOUNT_STATUS_ACTIVE.equals(acctStatus)
                    && !(context.authToken != null && (context.authToken.isDelegatedAuth()
                            || AdminAccessControl.isAdequateAdminAccount(context.getAuthAccount())))) {
                throw AccountServiceException.ACCOUNT_INACTIVE(context.targetAccount.getName());
            }
        }
    }

    protected static AuthToken getProxyAuthToken(UserServletContext context) throws ServiceException {
        String encoded = Provisioning.getInstance().getProxyAuthToken(context.targetAccount.getId(), null);
        if (encoded != null) {
            return new ZimbraAuthTokenEncoded(encoded);
        } else if (context.basicAuthHappened) {
            return context.authToken;
        } else {
            return null;
        }
    }

    protected boolean proxyIfRemoteTargetAccount(HttpServletRequest req, HttpServletResponse resp,
            UserServletContext context) throws IOException, ServiceException {
        // this should handle both explicit /user/user-on-other-server/ and
        // /user/~/?id={account-id-on-other-server}:id

        if (context.targetAccount != null && !Provisioning.onLocalServer(context.targetAccount)) {
            try {
                proxyServletRequest(req, resp, Provisioning.getInstance().getServer(context.targetAccount),
                        getProxyAuthToken(context));
            } catch (HttpException e) {
                throw new IOException("Unknown error", e);
            }
            return true;
        }

        return false;
    }

    /**
     * Constructs the exteral url for a mount point. This gets the link back to
     * the correct server without need for proxying it
     * 
     * @param authToken
     * @param mpt
     *            The mount point to create the url for
     * @return The url for the mountpoint/share that goes back to the original
     *         user/share/server
     * @throws ServiceException
     */
    public static String getExternalRestUrl(OperationContext octxt, Mountpoint mpt) throws ServiceException {
        AuthToken authToken = AuthToken.getCsrfUnsecuredAuthToken(octxt.getAuthToken());
        // check to see if it is a local mount point, if it is there's
        // no need to do anything
        if (mpt.isLocal()) {
            return null;
        }

        String folderPath = null;

        // Figure out the target server from the target user's account.
        // This will let us get the correct server/port
        Provisioning prov = Provisioning.getInstance();
        Account targetAccount = prov.get(AccountBy.id, mpt.getOwnerId());
        if (targetAccount == null) {
            // Remote owner account has been deleted.
            return null;
        }
        Server targetServer = prov.getServer(targetAccount);

        // Avoid the soap call if its a local mailbox
        if (Provisioning.onLocalServer(targetAccount)) {
            Mailbox mailbox = MailboxManager.getInstance().getMailboxByAccountId(targetAccount.getId());
            if (mailbox == null) {
                // no mailbox (shouldn't happen normally)
                return null;
            }
            // Get the folder from the mailbox
            Folder folder = mailbox.getFolderById(octxt, mpt.getRemoteId());
            if (folder == null) {
                return null;
            }
            folderPath = folder.getPath();
        } else {
            // The remote server case
            // Get the target user's mailbox..
            ZMailbox.Options zoptions = new ZMailbox.Options(authToken.toZAuthToken(),
                    AccountUtil.getSoapUri(targetAccount));
            zoptions.setTargetAccount(mpt.getOwnerId());
            zoptions.setTargetAccountBy(AccountBy.id);
            zoptions.setNoSession(true);
            ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
            if (zmbx == null) {
                // we didn't manage to get a mailbox
                return null;
            }

            // Get an instance of their folder so we can build the path
            // correctly
            ZFolder folder = zmbx.getFolderById(mpt.getTarget().toString(authToken.getAccount().getId()));
            // if for some reason we can't find the folder, return null
            if (folder == null) {
                return null;
            }
            folderPath = folder.getPath();
        }
        // For now we'll always use SSL
        return URLUtil.getServiceURL(targetServer,
                SERVLET_PATH + HttpUtil.urlEscape(getAccountPath(targetAccount) + folderPath), true);
    }

    /**
     * Constructs the exteral url for a mount point. This gets the link back to
     * the correct server without need for proxying it
     * 
     * @param authToken
     * @param mpt
     *            The mount point to create the url for
     * @return The url for the mountpoint/share that goes back to the original
     *         user/share/server
     * @throws ServiceException
     */
    public static String getExternalRestUrl(Folder folder) throws ServiceException {
        // Figure out the target server from the target user's account.
        // This will let us get the correct server/port
        Provisioning prov = Provisioning.getInstance();
        Account targetAccount = folder.getAccount();

        Server targetServer = prov.getServer(targetAccount);

        // For now we'll always use SSL
        return URLUtil.getServiceURL(targetServer,
                SERVLET_PATH + HttpUtil.urlEscape(getAccountPath(targetAccount) + folder.getPath()), true);
    }

    protected void resolveItems(UserServletContext context) throws ServiceException, IOException {
        UserServletUtil.resolveItems(context);
    }

    protected MailItem resolveItem(UserServletContext context) throws ServiceException, IOException {
        return UserServletUtil.resolveItem(context, true);
    }

    private void doAuthGet(HttpServletRequest req, HttpServletResponse resp, UserServletContext context)
            throws ServletException, IOException, ServiceException, UserServletException {
        if (log.isDebugEnabled()) {
            StringBuffer reqURL = context.req.getRequestURL();
            String queryParam = context.req.getQueryString();
            if (queryParam != null)
                reqURL.append('?').append(queryParam);
            log.debug("UserServlet: " + reqURL.toString());
        }

        context.opContext = new OperationContext(context.getAuthAccount(), isAdminRequest(req));
        Mailbox mbox = UserServletUtil.getTargetMailbox(context);
        if (mbox != null) {
            ZimbraLog.addMboxToContext(mbox.getId());
            if (context.reqListIds != null) {
                resolveItems(context);
            } else {
                MailItem item = resolveItem(context);
                if (proxyIfMountpoint(req, resp, context, item)) {
                    // if the target is a mountpoint, the request was already
                    // proxied to the resolved target
                    return;
                }
                context.target = item; /* imap_id resolution needs this. */
            }
        }

        if (FormatType.FREE_BUSY.equals(context.format)) {
            /*
             * Always reference Calendar. This is the fallback for non-existent
             * paths, thus, could harvest emails by asking for freebusy against,
             * say the Drafts folder. Without this change, the returned HTML
             * would reference Drafts for valid emails and Calendar for invalid
             * ones.
             */
            context.fakeTarget = new UserServletContext.FakeFolder(context.accountPath, "/Calendar", "Calendar");
        }

        resolveFormatter(context);

        // Prevent harvest attacks. If mailbox doesn't exist for a request
        // requiring authentication,
        // return auth error instead of "no such mailbox". If request/formatter
        // doesn't require
        // authentication, call the formatter and let it deal with preventing
        // harvest attacks.
        if (mbox == null && context.formatter.requiresAuth())
            throw ServiceException.PERM_DENIED(L10nUtil.getMessage(MsgKey.errPermissionDenied, req));

        String cacheControlValue = LC.rest_response_cache_control_value.value();
        if (!StringUtil.isNullOrEmpty(cacheControlValue)) {
            resp.addHeader("Cache-Control", cacheControlValue);
        }
        FormatType formatType = context.formatter.getType();
        if (formatType != null) {
            context.resp.setContentType(formatType.getContentType());
        }

        context.formatter.format(context);
    }

    /**
     * Adds an item to a folder specified in the URI. The item content is
     * provided in the PUT request's body.
     */
    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    /**
     * Adds an item to a folder specified in the URI. The item content is
     * provided in the POST request's body.
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserServletContext context = null;
        ZimbraLog.clearContext();
        addRemoteIpToLoggingContext(req);
        try {
            context = new UserServletContext(req, resp, this);
            if (!checkAuthentication(context)) {
                sendError(context, req, resp, L10nUtil.getMessage(MsgKey.errMustAuthenticate, req));
                return;
            }

            checkTargetAccountStatus(context);
            if (proxyIfRemoteTargetAccount(req, resp, context))
                return;

            if (context.getAuthAccount() != null) {
                ZimbraLog.addAccountNameToContext(context.getAuthAccount().getName());
            }

            boolean doCsrfCheck = false;
            if (req.getAttribute(CsrfFilter.CSRF_TOKEN_CHECK) != null) {
                doCsrfCheck = (Boolean) req.getAttribute(CsrfFilter.CSRF_TOKEN_CHECK);
            }

            if (doCsrfCheck) {
                String csrfToken = req.getHeader(Constants.CSRF_TOKEN);
                if (log.isDebugEnabled()) {
                    String paramValue = req.getParameter(QP_AUTH);
                    log.debug("CSRF check is: %s, CSRF token is: %s, Authentication recd with request is: %s",
                            doCsrfCheck, csrfToken, paramValue);
                }

                if (!StringUtil.isNullOrEmpty(csrfToken)) {
                    if (!CsrfUtil.isValidCsrfToken(csrfToken, context.authToken)) {
                        context.setCsrfAuthSucceeded(Boolean.FALSE);
                        log.debug(
                                "CSRF token validation failed for account: %s" + ", Auth token is CSRF enabled:  %s"
                                        + "CSRF token is: %s",
                                context.authToken, context.authToken.isCsrfTokenEnabled(), csrfToken);
                        sendError(context, req, resp, L10nUtil.getMessage(MsgKey.errMustAuthenticate, req));
                        return;
                    } else {
                        context.setCsrfAuthSucceeded(Boolean.TRUE);
                    }
                }
            }
            Folder folder = null;
            String filename = null;
            Mailbox mbox = UserServletUtil.getTargetMailbox(context);
            if (mbox != null) {
                ZimbraLog.addMboxToContext(mbox.getId());

                log.info("POST: " + context.req.getRequestURL().toString());

                context.opContext = new OperationContext(context.getAuthAccount(), isAdminRequest(req));

                try {
                    context.target = UserServletUtil.resolveItem(context, false);
                } catch (NoSuchItemException nsie) {
                    // perhaps it's a POST to "Notebook/new-file-name" -- find
                    // the parent folder and proceed from there
                    if (context.itemPath == null)
                        throw nsie;
                    int separator = context.itemPath.lastIndexOf('/');
                    if (separator <= 0)
                        throw nsie;
                    filename = context.itemPath.substring(separator + 1);
                    context.itemPath = context.itemPath.substring(0, separator);
                    context.target = UserServletUtil.resolveItem(context, false);
                    context.extraPath = filename;
                }

                folder = (context.target instanceof Folder ? (Folder) context.target
                        : mbox.getFolderById(context.opContext, context.target.getFolderId()));

                if (context.target != folder) {
                    if (filename == null)
                        filename = context.target.getName();
                    else
                        // need to fail on POST to
                        // "Notebook/existing-file/random-cruft"
                        throw MailServiceException.NO_SUCH_FOLDER(context.itemPath);
                }

                if (proxyIfMountpoint(req, resp, context, folder)) {
                    // if the target is a mountpoint, the request was already
                    // proxied to the resolved target
                    return;
                }
            }

            // if they specified a filename, default to the native formatter
            if (context.format == null && filename != null)
                context.format = FormatType.HTML_CONVERTED;

            String ctype = context.req.getContentType();

            // if no format explicitly specified, try to guess it from the
            // Content-Type header
            if (context.format == null && ctype != null) {
                String normalizedType = new com.zimbra.common.mime.ContentType(ctype).getContentType();
                Formatter fmt = FormatterFactory.mDefaultFormatters.get(normalizedType);
                if (fmt != null)
                    context.format = fmt.getType();
            }

            context.target = folder;
            resolveFormatter(context);
            if (!context.formatter.supportsSave())
                sendError(context, req, resp, L10nUtil.getMessage(MsgKey.errUnsupportedFormat, req));

            // Prevent harvest attacks. If mailbox doesn't exist for a request
            // requiring authentication,
            // return auth error instead of "no such mailbox". If
            // request/formatter doesn't require
            // authentication, call the formatter and let it deal with
            // preventing harvest attacks.
            if (mbox == null && context.formatter.requiresAuth())
                throw ServiceException.PERM_DENIED(L10nUtil.getMessage(MsgKey.errPermissionDenied, req));

            context.formatter.save(context, ctype, folder, filename);
        } catch (ServiceException se) {
            if (se.getCode() == ServiceException.PERM_DENIED || se instanceof NoSuchItemException) {
                sendError(context, req, resp, L10nUtil.getMessage(MsgKey.errNoSuchItem, req));
            } else if (se.getCode() == AccountServiceException.MAINTENANCE_MODE
                    || se.getCode() == AccountServiceException.ACCOUNT_INACTIVE) {
                sendError(context, req, resp, se.getMessage());
            } else if (se.getCode() == ServiceException.INVALID_REQUEST) {
                if (log.isDebugEnabled()) {
                    log.debug("Invalid POST Request", se);
                } else {
                    log.info("Invalid POST Request - %s", se.getMessage());
                }
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, se.getMessage());
            } else {
                if (log.isDebugEnabled()) {
                    log.info("Service Exception caught whilst processing POST", se);
                } else {
                    log.info("Service Exception caught whilst processing POST - %s", se.getMessage());
                }
                throw new ServletException(se);
            }
        } catch (UserServletException e) {
            // add check for ServiceException root cause?
            if (e.getHttpStatusCode() == HttpServletResponse.SC_UNAUTHORIZED) {
                sendError(context, req, resp, L10nUtil.getMessage(MsgKey.errMustAuthenticate, req));
            } else {
                resp.sendError(e.getHttpStatusCode(), e.getMessage());
            }
        } catch (HttpException e) {
            throw new ServletException(e);
        } finally {
            ZimbraLog.clearContext();
        }
    }

    /**
     * Determines the <code>format</code> and <code>formatter<code> for the
     * request, if not already set.
     */
    private void resolveFormatter(UserServletContext context) throws UserServletException {
        if (context.format == null) {
            context.format = defaultFormat(context);
            if (context.format == null) {
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                        L10nUtil.getMessage(MsgKey.errUnsupportedFormat, context.req));
            }
        }

        if (context.formatter == null) {
            context.formatter = FormatterFactory.mFormatters.get(context.format);
            if (context.formatter == null) {
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                        L10nUtil.getMessage(MsgKey.errUnsupportedFormat, context.req));
            }
        }
        context.formatter.validateParams(context);
    }

    protected boolean proxyIfMountpoint(HttpServletRequest req, HttpServletResponse resp, UserServletContext context,
            MailItem item) throws IOException, ServiceException, UserServletException {
        if (!(item instanceof Mountpoint))
            return false;
        if (context.format != null && context.format.equals("html"))
            return false;
        Mountpoint mpt = (Mountpoint) item;

        String uri = SERVLET_PATH + "/~/?" + QP_ID + '=' + HttpUtil.urlEscape(mpt.getOwnerId()) + "%3A"
                + mpt.getRemoteId();
        if (context.format != null)
            uri += '&' + QP_FMT + '=' + HttpUtil.urlEscape(context.format.toString());
        if (context.extraPath != null)
            uri += '&' + QP_NAME + '=' + HttpUtil.urlEscape(context.extraPath);
        for (Map.Entry<String, String> entry : HttpUtil.getURIParams(req).entrySet()) {
            String qp = entry.getKey();
            if (!qp.equals(QP_ID) && !qp.equals(QP_FMT))
                uri += '&' + HttpUtil.urlEscape(qp) + '=' + HttpUtil.urlEscape(entry.getValue());
        }

        Provisioning prov = Provisioning.getInstance();
        Account targetAccount = prov.get(AccountBy.id, mpt.getOwnerId());
        if (targetAccount == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST,
                    L10nUtil.getMessage(MsgKey.errNoSuchAccount, req));
        try {
            proxyServletRequest(req, resp, prov.getServer(targetAccount), uri, getProxyAuthToken(context));
        } catch (HttpException e) {
            throw new IOException("Unknown error", e);
        }
        return true;
    }

    private static HashSet<String> ZIMBRA_DOC_CONTENT_TYPE = new HashSet<String>();
    static {
        ZIMBRA_DOC_CONTENT_TYPE.add("application/x-zimbra-doc");
        ZIMBRA_DOC_CONTENT_TYPE.add("application/x-zimbra-slides");
        ZIMBRA_DOC_CONTENT_TYPE.add("application/x-zimbra-xls");
    }

    private FormatType defaultFormat(UserServletContext context) {
        if (context.hasPart()) {
            return FormatType.HTML_CONVERTED;
        }
        MailItem.Type type = MailItem.Type.UNKNOWN;
        if (context.target instanceof Folder)
            type = ((Folder) context.target).getDefaultView();
        else if (context.target != null)
            type = context.target.getType();

        switch (type) {
            case APPOINTMENT:
            case TASK:
                return FormatType.ICS;
            case CONTACT:
                return context.target instanceof Folder ? FormatType.CSV : FormatType.VCF;
            case DOCUMENT:
                // Zimbra docs and folder rendering should use html formatter.
                if (context.target instanceof Folder)
                    return FormatType.HTML;
                String contentType = ((Document) context.target).getContentType();
                if (contentType != null && contentType.indexOf(';') > 0)
                    contentType = contentType.substring(0, contentType.indexOf(';')).toLowerCase();
                if (ZIMBRA_DOC_CONTENT_TYPE.contains(contentType))
                    return FormatType.HTML;
                return FormatType.HTML_CONVERTED;
            default:
                return FormatType.HTML_CONVERTED;
        }
    }

    @Override
    public void init() throws ServletException {
        log.info("Starting up");
        super.init();
    }

    @Override
    public void destroy() {
        log.info("Shutting down");
        super.destroy();
    }

    public static byte[] getRemoteContent(AuthToken authToken, ItemId iid, Map<String, String> params)
            throws ServiceException {
        Account target = Provisioning.getInstance().get(AccountBy.id, iid.getAccountId(), authToken);
        Map<String, String> pcopy = new HashMap<String, String>(params);
        pcopy.put(QP_ID, iid.toString());
        return getRemoteContent(authToken, target, (String) null, pcopy);
    }

    public static byte[] getRemoteContent(AuthToken authToken, Account target, String folder,
            Map<String, String> params) throws ServiceException {
        return getRemoteContent(authToken.toZAuthToken(), getRemoteUrl(target, folder, params));
    }

    public static byte[] getRemoteContent(ZAuthToken authToken, String url) throws ServiceException {
        return getRemoteResource(authToken, url).getSecond();
    }

    public static HttpInputStream getRemoteContentAsStream(AuthToken authToken, Account target, String folder,
            Map<String, String> params) throws ServiceException, IOException {
        String url = getRemoteUrl(target, folder, params);
        return getRemoteResourceAsStream(authToken.toZAuthToken(), url).getSecond();
    }

    private static String getRemoteUrl(Account target, String folder, Map<String, String> params)
            throws ServiceException {
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
                url.append('&').append(HttpUtil.urlEscape(param.getKey())).append('=')
                        .append(HttpUtil.urlEscape(param.getValue()));
        }
        return url.toString();
    }

    public static Pair<Header[], byte[]> getRemoteResource(ZAuthToken authToken, String url) throws ServiceException {
        HttpResponse response = null;
        try {
            Pair<Header[], HttpResponse> pair = doHttpOp(authToken, new HttpGet(url));
            response = pair.getSecond();
            return new Pair<Header[], byte[]>(pair.getFirst(), EntityUtils.toByteArray(response.getEntity()));
        } catch (IOException x) {
            throw ServiceException.FAILURE("Can't read response body " + url, x);
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        }
    }

    public static FileUploadServlet.Upload getRemoteResourceAsUpload(AuthToken at, ItemId iid,
            Map<String, String> params) throws ServiceException, IOException {
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
            filename = new com.zimbra.common.mime.ContentType(ctype).getParameter("name");
        if (filename == null || filename.equals(""))
            filename = "unknown";
        return FileUploadServlet.saveUpload(response.getSecond(), filename, ctype, at.getAccountId());
    }

    /** Helper class so that we can close connection upon stream close */
    public static class HttpInputStream extends FilterInputStream {
        private final HttpResponse response;

        public HttpInputStream(HttpResponse r) throws IOException {
            super(r.getEntity().getContent());
            this.response = r;
        }

        public int getContentLength() {
            String cl = getHeader("Content-Length");
            if (cl != null)
                return Integer.parseInt(cl);
            return -1;
        }

        public String getHeader(String headerName) {
            Header cl = response.getFirstHeader(headerName);
            if (cl != null)
                return cl.getValue();
            return null;
        }

        public int getStatusCode() {
            return response.getStatusLine().getStatusCode();
        }

        @Override
        public void close() {
            EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    public static Pair<Header[], HttpInputStream> getRemoteResourceAsStream(ZAuthToken authToken, ItemId iid,
            String extraPath) throws ServiceException, IOException {
        Map<String, String> params = new HashMap<String, String>();
        params.put(QP_ID, iid.toString());
        if (extraPath != null)
            params.put(QP_NAME, extraPath);
        Account target = Provisioning.getInstance().getAccountById(iid.getAccountId());
        String url = getRemoteUrl(target, null, params);
        return getRemoteResourceAsStream(authToken, url);
    }

    public static Pair<Integer, InputStream> getRemoteResourceAsStreamWithLength(ZAuthToken authToken, String url)
            throws ServiceException, IOException {
        HttpInputStream his = getRemoteResourceAsStream(authToken, url).getSecond();
        return new Pair<Integer, InputStream>(his.getContentLength(), his);
    }

    public static Pair<Header[], HttpInputStream> getRemoteResourceAsStream(ZAuthToken authToken, String url)
            throws ServiceException, IOException {
        Pair<Header[], HttpResponse> pair = doHttpOp(authToken, new HttpGet(url));
        return new Pair<Header[], HttpInputStream>(pair.getFirst(), new HttpInputStream(pair.getSecond()));
    }

    public static Pair<Header[], HttpInputStream> putMailItem(ZAuthToken authToken, String url, MailItem item)
            throws ServiceException, IOException {
        if (item instanceof Document) {
            Document doc = (Document) item;
            StringBuilder u = new StringBuilder(url);
            u.append("?").append(QP_AUTH).append('=').append(AUTH_COOKIE);
            if (doc.getType() == MailItem.Type.WIKI) {
                u.append("&fmt=wiki");
            }
            HttpPut method = new HttpPut(u.toString());
            String contentType = doc.getContentType();
            method.addHeader("Content-Type", contentType);
            method.setEntity(
                    new InputStreamEntity(doc.getContentStream(), doc.getSize(), ContentType.create(contentType)));

            method.addHeader("X-Zimbra-Description", doc.getDescription());
            method.setEntity(
                    new InputStreamEntity(doc.getContentStream(), doc.getSize(), ContentType.create(contentType)));
            Pair<Header[], HttpResponse> pair = doHttpOp(authToken, method);
            return new Pair<Header[], HttpInputStream>(pair.getFirst(), new HttpInputStream(pair.getSecond()));
        }
        return putRemoteResource(authToken, url, item.getContentStream(), null);
    }

    public static Pair<Header[], HttpInputStream> putRemoteResource(AuthToken authToken, String url, InputStream req,
            Header[] headers) throws ServiceException, IOException {
        return putRemoteResource(authToken.toZAuthToken(), url, req, headers);
    }

    public static Pair<Header[], HttpInputStream> putRemoteResource(ZAuthToken authToken, String url, InputStream req,
            Header[] headers) throws ServiceException, IOException {
        StringBuilder u = new StringBuilder(url);
        u.append("?").append(QP_AUTH).append('=').append(AUTH_COOKIE);
        HttpPut method = new HttpPut(u.toString());
        String contentType = "application/octet-stream";
        if (headers != null) {
            for (Header hdr : headers) {
                String name = hdr.getName();
                method.addHeader(hdr);
                if (name.equals("Content-Type"))
                    contentType = hdr.getValue();
            }
        }
        method.setEntity(new InputStreamEntity(req, ContentType.create(contentType)));
        Pair<Header[], HttpResponse> pair = doHttpOp(authToken, method);
        return new Pair<Header[], HttpInputStream>(pair.getFirst(), new HttpInputStream(pair.getSecond()));
    }

    private static Pair<Header[], HttpResponse> doHttpOp(ZAuthToken authToken, HttpRequestBase method)
            throws ServiceException {
        // create an HTTP client with the same cookies
        String url = "";
        String hostname = "";
        url = method.getURI().toString();
        hostname = method.getURI().getHost();

        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        Map<String, String> cookieMap = authToken.cookieMap(false);
        if (cookieMap != null) {
            BasicCookieStore cookieStore = new BasicCookieStore();
            for (Map.Entry<String, String> ck : cookieMap.entrySet()) {

                BasicClientCookie cookie = new BasicClientCookie(ck.getKey(), ck.getValue());
                cookie.setDomain(hostname);
                cookie.setPath("/");
                cookie.setSecure(false);
                cookieStore.addCookie(cookie);
            }
            clientBuilder.setDefaultCookieStore(cookieStore);
            RequestConfig reqConfig = RequestConfig
                    .copy(ZimbraHttpConnectionManager.getInternalHttpConnMgr().getZimbraConnMgrParams().getReqConfig())
                    .setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();

            clientBuilder.setDefaultRequestConfig(reqConfig);
            if (method.getMethod().equalsIgnoreCase("PUT")) {
                clientBuilder.setRetryHandler(new InputStreamRequestHttpRetryHandler());
            }

        }

        if (method instanceof HttpPut) {
            long contentLength = ((HttpPut) method).getEntity().getContentLength();
            if (contentLength > 0) {
                int timeEstimate = Math.max(10000, (int) (contentLength / 100)); // 100kbps
                                                                                 // in
                                                                                 // millis
                // cannot set connection time using our
                // ZimbrahttpConnectionManager,
                // see comments in ZimbrahttpConnectionManager.
                // actually, length of the content to Put should not be a factor
                // for
                // establishing a connection, only read time out matter, which
                // we set
                // client.getHttpConnectionManager().getParams().setConnectionTimeout(timeEstimate);
                SocketConfig config = SocketConfig.custom().setSoTimeout(timeEstimate).build();
                clientBuilder.setDefaultSocketConfig(config);
            }
        }

        HttpClient client = clientBuilder.build();
        try {
            HttpResponse response = HttpClientUtil.executeMethod(client, method);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_NOT_FOUND || statusCode == HttpStatus.SC_FORBIDDEN)
                throw MailServiceException.NO_SUCH_ITEM(-1);
            else if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_CREATED
                    && statusCode != HttpStatus.SC_NO_CONTENT)
                throw ServiceException.RESOURCE_UNREACHABLE(response.getStatusLine().getReasonPhrase(), null,
                        new ServiceException.InternalArgument(HTTP_URL, url, ServiceException.Argument.Type.STR),
                        new ServiceException.InternalArgument(HTTP_STATUS_CODE, statusCode,
                                ServiceException.Argument.Type.NUM));

            List<Header> headers = new ArrayList<Header>(Arrays.asList(response.getAllHeaders()));
            headers.add(new BasicHeader("X-Zimbra-Http-Status", "" + statusCode));
            return new Pair<Header[], HttpResponse>(headers.toArray(new Header[0]), response);
        } catch (HttpException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("HttpException while fetching " + url, e);
        } catch (IOException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("IOException while fetching " + url, e);
        }
    }
}
