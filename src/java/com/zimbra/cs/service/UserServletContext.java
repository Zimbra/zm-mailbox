/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
import com.google.common.base.Objects;
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
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.formatter.Formatter;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;
import com.zimbra.cs.service.util.ItemId;

public final class UserServletContext {
    public final HttpServletRequest req;
    public final HttpServletResponse resp;
    public final UserServlet servlet;
    public final Map<String, String> params;
    public FormatType format;
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
    public ArrayList<Item> requestedItems;
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

        private Iterator<Item> items;

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

    public UserServletContext(HttpServletRequest request, HttpServletResponse response, UserServlet srvlt)
    throws UserServletException, ServiceException {
        Provisioning prov = Provisioning.getInstance();

        this.req = request;
        this.resp = response;
        this.servlet = srvlt;
        this.params = HttpUtil.getURIParams(request);

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
            if (this.formatter == null)
                throw new UserServletException(HttpServletResponse.SC_NOT_IMPLEMENTED, L10nUtil.getMessage(MsgKey.errNotImplemented, request));
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

    public String getQueryString() {
        return params.get(UserServlet.QP_QUERY);
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
        return Objects.toStringHelper(this)
            .add("account", accountPath)
            .add("item", itemPath)
            .add("format", format)
            .add("locale", locale)
            .toString();
    }
}
