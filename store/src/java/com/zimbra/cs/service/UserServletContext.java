/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.fb.FreeBusyQuery;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.formatter.Formatter;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.servlet.CsrfFilter;
import com.zimbra.cs.servlet.util.CsrfUtil;

public class UserServletContext {
    public final HttpServletRequest req;
    public final HttpServletResponse resp;
    public final UserServlet servlet;
    public final Map<String, String> params;
    public FormatType format;
    public Formatter formatter;
    public boolean cookieAuthHappened;
    public boolean jwtAuthHappened;
    public boolean basicAuthHappened;
    public boolean qpAuthHappened;
    public String accountPath;
    public AuthToken authToken;
    public String itemPath;
    public String extraPath;
    public ItemId itemId;
    public MailItem target;
    public SortBy sortBy;
    public FakeFolder fakeTarget = null;
    public int[] reqListIds;
    public ArrayList<Item> requestedItems;
    public boolean fromDumpster;
    public boolean wantCustomHeaders = true;
    public int imapId = -1;
    public boolean sync;
    private Account authAccount;
    public Account targetAccount;
    public Mailbox targetMailbox;
    public OperationContext opContext;
    private Locale locale;
    private long mStartTime = -2;
    private long mEndTime = -2;
    private Throwable error;
    private boolean csrfAuthSucceeded;



    /**
     * @return the csrfAuthSucceeded
     */
    public boolean isCsrfAuthSucceeded() {
        return csrfAuthSucceeded;
    }


    /**
     * @param csrfAuthSucceeded the csrfAuthSucceeded to set
     */
    public void setCsrfAuthSucceeded(boolean csrfAuthSucceeded) {
        this.csrfAuthSucceeded = csrfAuthSucceeded;
    }

    public static class Item {
        public int id;
        public String acctId;
        public int ver;
        public boolean versioned;
        public MailItem mailItem;
        public Item(String itemId, Account targetAccount) throws ServiceException {
            String[] vals = itemId.split("\\.");
            ItemId iid = new ItemId((vals.length > 0) ? vals[0] : itemId, targetAccount == null ? (String) null : targetAccount.getId());
            id = iid.getId();
            if (targetAccount != null && !targetAccount.getId().equals(iid.getAccountId())) {
                acctId = iid.getAccountId();
            }
            if (vals.length == 2) {
                versioned = true;
                ver = Integer.parseInt(vals[1]);
            }
        }
    }

    private static class ItemIterator implements Iterator<MailItem> {

        private final Iterator<Item> items;

        public ItemIterator(ArrayList<Item> items) {
            this.items = items.iterator();
        }

        @Override
        public boolean hasNext() {
            return items.hasNext();
        }

        @Override
        public MailItem next() {
            return items.next().mailItem;
        }

        @Override
        public void remove() {
        }
    }

    public static class FakeFolder {
        private final String account;
        private final String path;
        private final String name;
        public FakeFolder(String targetAccount, String calPath, String calName) {
            account = targetAccount;
            path = calPath;
            name =  calName;
        }
        public String getAccount() { return account; }
        public String getPath() { return path; }
        public String getName() { return name; }
    }

    public UserServletContext(HttpServletRequest request, HttpServletResponse response, UserServlet srvlt)
    throws UserServletException, ServiceException {
        this.req = request;
        this.resp = response;
        this.servlet = srvlt;
        this.params = HttpUtil.getURIParams(request);
        this.sortBy = getSortBy();

        //rest url override for locale
        String language = this.params.get(UserServlet.QP_LANGUAGE);
        if (language != null) {
            String country = this.params.get(UserServlet.QP_COUNTRY);
            if (country != null) {
                String variant = this.params.get(UserServlet.QP_VARIANT);
                if (variant != null) {
                    this.locale = new Locale(language, country, variant);
                } else {
                    this.locale = new Locale(language, country);
                }
            } else {
                this.locale = new Locale(language);
            }
        }

        parseParams(request, authToken);
    }

    protected void parseParams(HttpServletRequest request, AuthToken authToken)
            throws UserServletException, ServiceException {
        Provisioning prov = Provisioning.getInstance();

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
        this.extraPath = this.params.get(UserServlet.QP_NAME);
        this.format = FormatType.fromString(this.params.get(UserServlet.QP_FMT));
        String id = this.params.get(UserServlet.QP_ID);
        try {
            this.itemId = id == null ? null : new ItemId(id, (String) null);
        } catch (ServiceException e) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, L10nUtil.getMessage(MsgKey.errInvalidId, request));
        }

        String imap = this.params.get(UserServlet.QP_IMAP_ID);
        try {
            this.imapId = imap == null ? -1 : Integer.parseInt(imap);
        } catch (NumberFormatException nfe) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, L10nUtil.getMessage(MsgKey.errInvalidImapId, request));
        }

        if (this.format != null) {
            this.formatter = UserServlet.getFormatter(this.format);
            if (this.formatter == null) {
                throw new UserServletException(HttpServletResponse.SC_NOT_IMPLEMENTED, L10nUtil.getMessage(MsgKey.errNotImplemented, request));
            } else {
                formatter.validateParams(this);
            }
        }

        // see if we can get target account or not
        if (itemId != null && itemId.getAccountId() != null) {
            targetAccount = prov.get(AccountBy.id, itemId.getAccountId(), authToken);
        } else if (accountPath.equals("~")) {
            // can't resolve this yet
        } else {
            if (accountPath.startsWith("~")) {
                accountPath = accountPath.substring(1);
            }
            targetAccount = prov.get(AccountBy.name, accountPath, authToken);
        }

        String listParam = this.params.get(UserServlet.QP_LIST);
        if (listParam != null && listParam.length() > 0) {
            String[] ids = listParam.split(",");
            requestedItems = new ArrayList<Item>();
            reqListIds = new int[ids.length];
            String proxyAcct = null;
            for (int i = 0; i < ids.length; ++i) {
                Item item = new Item(ids[i], targetAccount);
                requestedItems.add(item);
                reqListIds[i] = item.id;
                if (targetAccount != null && !targetAccount.getId().equals(item.acctId)) {
                    if (proxyAcct != null && !proxyAcct.equals(item.acctId)) {
                        throw ServiceException.INVALID_REQUEST("Cross account multi list is not supported. already requested item from "+proxyAcct+" also found "+item.acctId+":"+item.id, null);
                    } else if (proxyAcct == null) {
                        proxyAcct = item.acctId;
                    }
                }
            }
            if (proxyAcct != null) {
                targetAccount = prov.get(AccountBy.id, proxyAcct, authToken);
            }
        }

        String dumpsterParam = params.get(UserServlet.QP_DUMPSTER);
        fromDumpster = (dumpsterParam != null && !dumpsterParam.equals("0") && !dumpsterParam.equalsIgnoreCase("false"));
    }

    public Locale getLocale() {
        return locale != null ? locale : req.getLocale();
    }

    public void setAuthAccount(Account value) throws ServiceException {
        authAccount = value;
        if (locale == null && authAccount != null && authAccount.getLocale() != null) {
            locale = authAccount.getLocale();
        }
    }

    public Account getAuthAccount() {
        return authAccount;
    }

    public Iterator<MailItem> getRequestedItems() {
        return new ItemIterator(requestedItems);
    }

    public boolean isUsingAdminPrivileges() {
        return authToken != null && AuthToken.isAnyAdmin(authToken);
    }

    public UserServlet getServlet() {
        return servlet;
    }

    public long getStartTime() {
        if (mStartTime == -2) {
            String st = params.get(UserServlet.QP_START);
            long defaultStartTime = formatter.getDefaultStartTime();
            mStartTime = (st != null) ? DateUtil.parseDateSpecifier(st, defaultStartTime) : defaultStartTime;
        }
        return mStartTime;
    }

    public long getEndTime() {
        if (mEndTime == -2) {
            String et = params.get(UserServlet.QP_END);
            long defaultEndTime = formatter.getDefaultEndTime();
            mEndTime = (et != null) ? DateUtil.parseDateSpecifier(et, defaultEndTime) : defaultEndTime;
        }
        return mEndTime;
    }

    public int getFreeBusyCalendar() {
        int folder = FreeBusyQuery.CALENDAR_FOLDER_ALL;
        String str = params.get(UserServlet.QP_FREEBUSY_CALENDAR);
        if (str != null) {
            try {
                folder = Integer.parseInt(str);
            } catch (NumberFormatException e) {}
        }
        return folder;
    }

    public boolean ignoreAndContinueOnError() {
        String val = params.get(UserServlet.QP_IGNORE_ERROR);
        if (val != null) {
            try {
                int n = Integer.parseInt(val);
                return n != 0;
            } catch (NumberFormatException e) {}
        }
        return false;
    }

    public boolean preserveAlarms() {
        String val = params.get(UserServlet.QP_PRESERVE_ALARMS);
        if (val != null) {
            try {
                int n = Integer.parseInt(val);
                return n != 0;
            } catch (NumberFormatException e) {}
        }
        return false;
    }

    public boolean noHierarchy() {
        String val = params.get(UserServlet.QP_NOHIERARCHY);
        if (val != null) {
            try {
                int n = Integer.parseInt(val);
                return n != 0;
            } catch (NumberFormatException e) {}
        }
        return false;
    }

    public String getQueryString() {
        return params.get(UserServlet.QP_QUERY);
    }

    private SortBy getSortBy() throws UserServletException {
        String sort = params.get(UserServlet.QP_SORT);
        if (sort == null) {
            return SortBy.DATE_DESC;
        } else {
            SortBy sortBy = SortBy.of(sort);
            if (sortBy == null) {
                throw UserServletException.badRequest(sort + " is not a valid sort order");
            } else {
                return sortBy;
            }
        }
    }

    /**
     * Shortcut to {@code params.get("charset")}.
     *
     * @return value of charset parameter, or UTF-8 if null
     * @throws ServiceException if the charset name is invalid
     */
    public Charset getCharset() throws ServiceException {
        String charset = params.get("charset");
        if (charset != null) {
            try {
                return Charset.forName(charset);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid charset: " + charset, e);
            }
        } else {
            return Charsets.UTF_8;
        }
    }

    public boolean cookieAuthAllowed() {
        return getAuth().indexOf(UserServlet.AUTH_COOKIE) != -1;
    }

    public boolean jwtAuthAllowed() {
        return getAuth().indexOf(UserServlet.AUTH_JWT) != -1;
    }

    public boolean isAuthedAcctGuest() {
        return authAccount != null && authAccount instanceof GuestAccount;
    }

    // bug 42782
    public boolean setCookie() {
        return (!isAuthedAcctGuest() &&
                getAuth().indexOf(UserServlet.AUTH_SET_COOKIE) != -1 &&
                getAuth().indexOf(UserServlet.AUTH_NO_SET_COOKIE) == -1);
    }

    public boolean basicAuthAllowed() {
        String auth = getAuth();
        return auth.indexOf(UserServlet.AUTH_NO_SET_COOKIE) != -1 ||
               auth.indexOf(UserServlet.AUTH_BASIC) != -1 ||
               auth.indexOf(UserServlet.AUTH_SET_COOKIE) != -1;
    }

    public boolean queryParamAuthAllowed() {
        return getAuth().indexOf(UserServlet.AUTH_QUERYPARAM) != -1;
    }

    public String getAuth() {
        String a = params.get(UserServlet.QP_AUTH);
        return (a == null || a.length() == 0) ? UserServlet.AUTH_DEFAULT : a;
    }

    public boolean hasPart() {
        String p = getPart();
        return p != null && p.length() > 0;
    }

    public String getPart() {
        return params.get(UserServlet.QP_PART);
    }

    public boolean hasBody() {
        String p = getBody();
        return p != null;
    }

    public String getBody() {
        return params.get(UserServlet.QP_BODY);
    }

    public boolean hasView() {
        String v = getView();
        return v != null && v.length() > 0;
    }

    public String getView() {
        return params.get(UserServlet.QP_VIEW);
    }

    public int getOffset() {
        String s = params.get(UserServlet.QP_OFFSET);
        if (s != null) {
            int offset = Integer.parseInt(s);
            if (offset > 0)
                return offset;
        }
        return 0;
    }

    public int getLimit() {
        String s = params.get(UserServlet.QP_LIMIT);
        if (s != null) {
            int limit = Integer.parseInt(s);
            if (limit > 0)
                return limit;
        }
        return 50;
    }

    public String getTypesString() {
        return params.get(UserServlet.QP_TYPES);
    }

    public String getAction() {
        return params.get(UserServlet.QP_ACTION);
    }

    public String getBodypart() {
        return params.get(UserServlet.QP_BODYPART);
    }

    public String getColor() {
        return params.get(UserServlet.QP_COLOR);
    }

    public String getDate() {
        return params.get(UserServlet.QP_DATE);
    }

    public String getExCompNum() {
        return params.get(UserServlet.QP_EX_COMP_NUM);
    }

    public String getExInvId() {
        return params.get(UserServlet.QP_EX_INV_ID);
    }

    public String getFmt() {
        return params.get(UserServlet.QP_FMT);
    }

    public String getFolderIds() {
        return params.get(UserServlet.QP_FOLDER_IDS);
    }

    public String getImId() {
        return params.get(UserServlet.QP_IM_ID);
    }

    public String getImPart() {
        return params.get(UserServlet.QP_IM_PART);
    }

    public String getImXim() {
        return params.get(UserServlet.QP_IM_XIM);
    }

    public String getInstDuration() {
        return params.get(UserServlet.QP_INST_DURATION);
    }

    public String getInstStartTime() {
        return params.get(UserServlet.QP_INST_START_TIME);
    }

    public String getInvCompNum() {
        return params.get(UserServlet.QP_INV_COMP_NUM);
    }

    public String getInvId() {
        return params.get(UserServlet.QP_INV_ID);
    }

    public String getNotoolbar() {
        return params.get(UserServlet.QP_NOTOOLBAR);
    }

    public String getNumdays() {
        return params.get(UserServlet.QP_NUMDAYS);
    }

    public String getPstat() {
        return params.get(UserServlet.QP_PSTAT);
    }

    public String getRefresh() {
        return params.get(UserServlet.QP_REFRESH);
    }

    public String getSkin() {
        return params.get(UserServlet.QP_SKIN);
    }

    public String getSq() {
        return params.get(UserServlet.QP_SQ);
    }

    public String getTz() {
        return params.get(UserServlet.QP_TZ);
    }

    public String getUseInstance() {
        return params.get(UserServlet.QP_USE_INSTANCE);
    }

    public String getXim() {
        return params.get(UserServlet.QP_XIM);
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

    public boolean hasMaxWidth() {
        return getMaxWidth() != null;
    }

    /**
     * Returns the maximum width of the image returned by this request, or
     * {@code null} if the max with is not specified or invalid.
     */
    public Integer getMaxWidth() {
        String s = params.get(UserServlet.QP_MAX_WIDTH);
        if (StringUtil.isNullOrEmpty(s)) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            UserServlet.log.warn("Ignoring invalid maxWidth value: " + s);
            return null;
        }
    }

    public boolean hasMaxHeight() {
        return getMaxHeight() != null;
    }

    /**
     * Returns the maximum height of the image returned by this request, or
     * {@code null} if the max height is not specified or invalid.
     */
    public Integer getMaxHeight() {
        String s = params.get(UserServlet.QP_MAX_HEIGHT);
        if (StringUtil.isNullOrEmpty(s)) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            UserServlet.log.warn("Ignoring invalid maxHeight value: " + s);
            return null;
        }
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

    private static final class UploadInputStream extends InputStream {
        private FileItem fi = null;
        private final InputStream is;
        private long curSize = 0;
        private final long maxSize;
        private long markSize = 0;

        UploadInputStream(InputStream is, long maxSize) {
            this.is = is;
            this.maxSize = maxSize;
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

        @Override public int read() throws IOException
        {
            int value = is.read();
            if (value != -1)
                check(1);
            return value;
        }

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
            if (req.getParameter("lbfums") != null) {
                limit = Provisioning.getInstance().getLocalServer().getLongAttr(
                        Provisioning.A_zimbraFileUploadMaxSize, DEFAULT_MAX_SIZE);
            } else {
                limit = Provisioning.getInstance().getConfig().getLongAttr(
                        Provisioning.A_zimbraMtaMaxMessageSize, DEFAULT_MAX_SIZE);
            }
        }

        boolean doCsrfCheck = false;
        if (req.getAttribute(CsrfFilter.CSRF_TOKEN_CHECK) != null) {
            doCsrfCheck =  (Boolean) req.getAttribute(CsrfFilter.CSRF_TOKEN_CHECK);
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
                        if (doCsrfCheck && !this.csrfAuthSucceeded) {
                            String csrfToken = params.get(FileUploadServlet.PARAM_CSRF_TOKEN);
                            if (UserServlet.log.isDebugEnabled()) {
                                String paramValue = req.getParameter(UserServlet.QP_AUTH);
                                UserServlet.log.debug(
                                    "CSRF check is: %s, CSRF token is: %s, Authentication recd with request is: %s",
                                    doCsrfCheck, csrfToken, paramValue);
                            }

                            if (!CsrfUtil.isValidCsrfToken(csrfToken, authToken)) {
                                setCsrfAuthSucceeded(Boolean.FALSE);
                                UserServlet.log.debug("CSRF token validation failed for account: %s"
                                    + ", Auth token is CSRF enabled:  %s" + "CSRF token is: %s",
                                    authToken, authToken.isCsrfTokenEnabled(), csrfToken);
                                throw new UserServletException(HttpServletResponse.SC_UNAUTHORIZED,
                                    L10nUtil.getMessage(MsgKey.errMustAuthenticate));
                            } else {
                                setCsrfAuthSucceeded(Boolean.TRUE);
                            }

                        }

                        is.close();
                        is = null;
                    } else {
                        is = new UploadInputStream(fis.openStream(), limit);
                        break;
                    }
                }
            } catch (UserServletException e) {
                throw new UserServletException(e.getHttpStatusCode(), e.getMessage(), e);
            } catch (Exception e) {
                throw new UserServletException(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, e.toString());
            }
            if (is == null)
                throw new UserServletException(HttpServletResponse.SC_NO_CONTENT, "No file content");
        } else {
            ContentType ctype = new ContentType(req.getContentType());
            String contentEncoding = req.getHeader("Content-Encoding");

            contentType = ctype.getContentType();
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
            params.put(UserServlet.UPLOAD_NAME, filename);
        params.put(UserServlet.UPLOAD_TYPE, contentType);
        ZimbraLog.mailbox.info("UserServlet received file %s - %d request bytes",
            filename, req.getContentLength());
        return is;
    }

    public void logError(Throwable e) {
        error = e;
    }

    public Throwable getLoggedError() {
        return error;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("account", accountPath)
            .add("item", itemPath)
            .add("format", format)
            .add("locale", locale)
            .add("extraPath", extraPath)
            .add("itemId", itemId)
            .add("target", target)
            .add("authAccount", authAccount)
            .add("targetAccount", targetAccount)
            .add("targetMailbox", targetMailbox)
            .add("params", params)
            .toString();
    }
}
