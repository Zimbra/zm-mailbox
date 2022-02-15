/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.util.SharedByteArrayInputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.DefaultFileItem;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.CompositeDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.mime.MimeTypesFactory;

import com.google.common.base.Strings;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.service.ServiceException.Argument;
import com.zimbra.common.service.ServiceException.InternalArgument;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.MapUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.util.JWTUtil;
import com.zimbra.cs.servlet.CsrfFilter;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.servlet.util.CsrfUtil;
import com.zimbra.cs.store.BlobInputStream;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.Zimbra;

public class FileUploadServlet extends ZimbraServlet {
    private static final long serialVersionUID = -3156986245375108467L;

    // bug 27610
    // We now limit file upload size for messages by zimbraMtaMaxMessageSize
    // If this query param is present in the URI, upload size is limited by zimbraFileUploadMaxSize,
    // This allows customer to allow larger documents/briefcase files than messages sent via SMTP.
    protected static final String PARAM_LIMIT_BY_FILE_UPLOAD_MAX_SIZE = "lbfums";

    protected static final String PARAM_CSRF_TOKEN = "csrfToken";
    private final Pattern ALLOWED_REQUESTID_CHARS = Pattern.compile("^[a-zA-Z0-9_.-]+$");

    /** The character separating upload IDs in a list */
    public static final String UPLOAD_DELIMITER = ",";
    /** The character separating server ID from upload ID */
    private static final String UPLOAD_PART_DELIMITER = ":";

    private static String sUploadDir;

    public static final class Upload {
        final String   accountId;
        String         contentType;
        String         extension;
        final String   uuid;
        final String   name;
        final FileItem file;
        long time;
        boolean deleted = false;
        BlobInputStream blobInputStream;
        final static TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
        final static Detector detector = tikaConfig.getDetector();

        Upload(String acctId, FileItem attachment) throws ServiceException {
            this(acctId, attachment, attachment.getName());
        }

        Upload(String acctId, FileItem attachment, String filename) throws ServiceException {
            assert(attachment != null); // TODO: Remove null checks in mainline.


            String localServer = Provisioning.getInstance().getLocalServer().getId();
            accountId = acctId;
            time      = System.currentTimeMillis();
            uuid      = localServer + UPLOAD_PART_DELIMITER + LdapUtil.generateUUID();
            name      = FileUtil.trimFilename(filename);
            file      = attachment;
            extension = filename == null ? "" : FilenameUtils.getExtension(filename).trim();
            MimeType mimeType = null;
            if (file == null) {
                contentType = MimeConstants.CT_TEXT_PLAIN;
            } else {
                mimeType = getMimeType(file);
                contentType = mimeType.toString();
            }

            Account acct = Provisioning.getInstance().getAccount(acctId);
            if (acct.isFeatureFileTypeUploadRestrictionsEnabled()) {
                String [] blockedFileTypes = null;
                blockedFileTypes = acct.getFileUploadBlockedFileTypes();
                List<String> blockedExtensionList = new ArrayList<>(Arrays.asList(blockedFileTypes));
                if (blockedExtensionList.size() > 0) {
                    if (blockedExtensionList.stream().anyMatch(extension::equalsIgnoreCase)) {
                        throw ServiceException.BLOCKED_FILE_TYPE_UPLOAD(
                                String.format("Blocked attachment during uploading %s filetype ", extension), null);
                    }

                    if (blockedExtensionList.stream().anyMatch((blockedContentType) -> {
                        if (blockedContentType.contains("/")) {
                            Pattern p = Pattern.compile(blockedContentType);
                            Matcher m = p.matcher(contentType);
                            return m.find();
                        } else {
                            return false;
                        }
                    })) {
                        throw ServiceException.BLOCKED_FILE_TYPE_UPLOAD(
                                String.format("Blocked attachment during uploading %s content-type ", contentType), null);
                    } else {
                        mLog.debug("Start - Using Tika library for retrieving the extension.");
                        String fileExtension = getExtension(mimeType);
                        mLog.debug("End - Using Tika library for retrieving the extension.");
                        if (blockedExtensionList.stream().anyMatch(fileExtension::equalsIgnoreCase)) {
                            throw ServiceException.BLOCKED_FILE_TYPE_UPLOAD(
                                    String.format("Blocked attachment during uploading %s filetype ", fileExtension), null);
                        }
                    }
                    mLog.debug(String.format("End - Using Tika library, time taken for [ %s ] - [ %d ] milliseconds.",
                            filename, (System.currentTimeMillis() - time)));
                }
            }
        }

        public String getName()         { return name; }
        public String getId()           { return uuid; }
        public String getContentType()  { return contentType; }
        public long getSize()           { return file == null ? 0 : file.getSize(); }
        public BlobInputStream getBlobInputStream()        { return blobInputStream; }

        public static MimeType getMimeType(FileItem fileItem) {
            MimeType mimeType = null;
            Metadata metadata = new Metadata();
            metadata.add(Metadata.RESOURCE_NAME_KEY, fileItem.getName());
            String customMimeTypesPath = LC.custom_mimetypes.value();
            MediaType mediaType = null;
            try {
                TikaInputStream stream = TikaInputStream.get(fileItem.getInputStream());
                if (new File(customMimeTypesPath).isFile()) {
                    MimeTypes customMimeTypes = MimeTypesFactory.create(new URL("file://" + customMimeTypesPath));
                    mediaType = new CompositeDetector(customMimeTypes, detector).detect(stream, metadata);
                } else {
                    mediaType = detector.detect(stream, metadata);
                }
                mimeType = tikaConfig.getMimeRepository().forName(mediaType.toString());
                mLog.debug("Content type detected by tika: %s.", mimeType.toString());
            } catch (MimeTypeException mexp) {
                mLog.warn("Failed to detect file content type", mexp);
            } catch (IOException exp) {
                mLog.warn("Cannot get content for upload", exp);
            }
            return mimeType;
        }

        public static String getExtension(MimeType mimeType) {
            String fileExtension = null;
            fileExtension = mimeType == null ? "" : StringUtils.strip(mimeType.getExtension(), ".");
            mLog.debug("File extension detected by tika: %s.", fileExtension);
            return fileExtension;
        }

        public InputStream getInputStream() throws IOException {
            if (wasDeleted()) {
                throw new IOException("Cannot get content for upload " + uuid + " because it was deleted.");
            }
            if (file == null) {
                return new SharedByteArrayInputStream(new byte[0]);
            }
            if (!file.isInMemory() && file instanceof DiskFileItem) {
                // If it's backed by a File, return a BlobInputStream so that any use by JavaMail
                // will avoid loading the whole thing in memory.
                File f = ((DiskFileItem) file).getStoreLocation();
                blobInputStream = new BlobInputStream(f, f.length());
                return blobInputStream;
            } else {
                return file.getInputStream();
            }
        }

        boolean accessedAfter(long checkpoint)  { return time > checkpoint; }

        void purge() {
            if (file != null) {
                mLog.debug("Deleting from disk: id=%s, %s", uuid, file);
                file.delete();
            }
            if (blobInputStream != null) {
                blobInputStream.closeFile();
            }
        }

        synchronized void markDeleted() {
            deleted = true;
        }

        public synchronized boolean wasDeleted() {
            return deleted;
        }

        @Override public String toString() {
            return "Upload: { accountId=" + accountId + ", time=" + new Date(time) +
                   ", size=" + getSize() + ", uploadId=" + uuid + ", name=" + name + ", path=" + getStoreLocation(file) + " }";
        }
    }

    static HashMap<String, Upload> mPending = new HashMap<String, Upload>(100);
    static Map<String, String> mProxiedUploadIds = MapUtil.newLruMap(100);
    static Log mLog = LogFactory.getLog(FileUploadServlet.class);

    static final long DEFAULT_MAX_SIZE = 10 * 1024 * 1024;

    /** Returns the zimbra id of the server the specified upload resides on.
     *
     * @param uploadId  The id of the upload.
     * @throws ServiceException if the upload id is malformed. */
    static String getUploadServerId(String uploadId) throws ServiceException {
        // uploadId is in the format of {serverId}:{uuid of the upload}
        String[] parts = null;
        if (uploadId == null || (parts = uploadId.split(UPLOAD_PART_DELIMITER)).length != 2)
            throw ServiceException.INVALID_REQUEST("invalid upload ID: " + uploadId, null);
        return parts[0];
    }

    /** Returns whether the specified upload resides on this server.
     *
     * @param uploadId  The id of the upload.
     * @throws ServiceException if the upload id is malformed or if there is
     *         an error accessing LDAP. */
    static boolean isLocalUpload(String uploadId) throws ServiceException {
        String serverId = getUploadServerId(uploadId);
        return Provisioning.getInstance().getLocalServer().getId().equals(serverId);
    }

    public static Upload fetchUpload(String accountId, String uploadId, AuthToken authtoken) throws ServiceException {
        mLog.debug("Fetching upload %s for account %s", uploadId, accountId);
        String context = "accountId=" + accountId + ", uploadId=" + uploadId;
        if (accountId == null || uploadId == null)
            throw ServiceException.FAILURE("fetchUploads(): missing parameter: " + context, null);

        // if the upload is remote, fetch it from the other server
        if (!isLocalUpload(uploadId))
            return fetchRemoteUpload(accountId, uploadId, authtoken);

        // the upload is local, so get it from the cache
        synchronized (mPending) {
            Upload up = mPending.get(uploadId);
            if (up == null) {
                mLog.warn("upload not found: " + context);
                throw MailServiceException.NO_SUCH_UPLOAD(uploadId);
            }
            if (!accountId.equals(up.accountId)) {
                mLog.warn("mismatched accountId for upload: " + up + "; expected: " + context);
                throw MailServiceException.NO_SUCH_UPLOAD(uploadId);
            }
            up.time = System.currentTimeMillis();
            mLog.debug("fetchUpload() returning %s", up);
            return up;
        }
    }

    private static Upload fetchRemoteUpload(String accountId, String uploadId, AuthToken authtoken) throws ServiceException {
        // check if we have fetched the Upload from the remote server previously
        String localUploadId = null;
        synchronized(mProxiedUploadIds) {
            localUploadId = mProxiedUploadIds.get(uploadId);
        }
        if (localUploadId != null) {
            synchronized (mPending) {
                Upload up = mPending.get(localUploadId);
                if (up != null)
                    return up;
            }
        }
        // the first half of the upload id is the server id where it lives
        Server server = Provisioning.getInstance().get(Key.ServerBy.id, getUploadServerId(uploadId));
        String url = AccountUtil.getBaseUri(server);
        if (url == null)
            return null;
        String hostname = server.getServiceHostname();
        url += ContentServlet.SERVLET_PATH + ContentServlet.PREFIX_PROXY + '?' +
               ContentServlet.PARAM_UPLOAD_ID + '=' + uploadId + '&' +
               ContentServlet.PARAM_EXPUNGE + "=true";

        // create an HTTP client with auth cookie to fetch the file from the remote ContentServlet
        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        HttpGet get = new HttpGet(url);

        authtoken.encode(clientBuilder, get, false, hostname);
        HttpClient client = clientBuilder.build();
        try {
            // fetch the remote item
            HttpResponse httpResp = HttpClientUtil.executeMethod(client, get);
            int statusCode = httpResp.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK)
                return null;

            // metadata is encoded in the response's HTTP headers
            Header ctHeader = httpResp.getFirstHeader("Content-Type");
            String contentType = ctHeader == null ? "text/plain" : ctHeader.getValue();
            Header cdispHeader = httpResp.getFirstHeader("Content-Disposition");
            String filename = cdispHeader == null ? "unknown" : new ContentDisposition(cdispHeader.getValue()).getParameter("filename");

            // store the fetched upload along with original uploadId
            Upload up = saveUpload(httpResp.getEntity().getContent(), filename, contentType, accountId);
            synchronized (mProxiedUploadIds) {
                mProxiedUploadIds.put(uploadId, up.uuid);
            }
            return up;
        } catch (HttpException e) {
            throw ServiceException.PROXY_ERROR(e, url);
        } catch (IOException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("can't fetch remote upload", e, new InternalArgument(ServiceException.URL, url, Argument.Type.STR));
        } finally {
            get.releaseConnection();
        }
    }

    public static Upload saveUpload(InputStream is, String filename, String contentType, String accountId, boolean limitByFileUploadMaxSize) throws ServiceException, IOException {
        return saveUpload(is, filename, contentType, accountId, getFileUploadMaxSize(limitByFileUploadMaxSize));
    }

    public static Upload saveUpload(InputStream is, String filename, String contentType, String accountId, long limit) throws ServiceException, IOException {
        FileItem fi = null;
        boolean success = false;
        try {
            // store the fetched file as a normal upload
            ServletFileUpload upload = getUploader(limit);
            long sizeMax = upload.getSizeMax();
            fi = upload.getFileItemFactory().createItem("upload", contentType, false, filename);
            // sizeMax=-1 means "no limit"
            long size = ByteUtil.copy(is, true, fi.getOutputStream(), true, sizeMax < 0 ? sizeMax : sizeMax + 1);
            if (upload.getSizeMax() >= 0 && size > upload.getSizeMax()) {
                mLog.warn("Exceeded maximum upload size of %s bytes", upload.getSizeMax());
                throw MailServiceException.UPLOAD_TOO_LARGE(filename, "upload too large");
            }

            Upload up = new Upload(accountId, fi);
            mLog.info("saveUpload(): received %s", up);
            synchronized (mPending) {
                mPending.put(up.uuid, up);
            }
            success = true;
            return up;
        } finally {
            if (!success && fi != null) {
                mLog.debug("saveUpload(): unsuccessful attempt.  Deleting %s", fi);
                fi.delete();
            }
        }
    }

    public static Upload saveUpload(InputStream is, String filename, String contentType, String accountId) throws ServiceException, IOException {
        return saveUpload(is, filename, contentType, accountId, false);
    }

    static File getStoreLocation(FileItem fi) {
        if (fi.isInMemory() || !(fi instanceof DiskFileItem)) {
            return null;
        }
        return ((DiskFileItem) fi).getStoreLocation();
    }

    public static void deleteUploads(Collection<Upload> uploads) {
        if (uploads != null && !uploads.isEmpty()) {
            for (Upload up : uploads)
                deleteUpload(up);
        }
    }

    public static void deleteUpload(Upload upload) {
        if (upload == null)
            return;
        Upload up;
        synchronized (mPending) {
            mLog.debug("deleteUpload(): removing %s", upload);
            up = mPending.remove(upload.uuid);
            if (up != null) {
                up.markDeleted();
            }
        }
        if (up == upload) {
            up.purge();
        }
    }

    protected static String getUploadDir() {
        if (sUploadDir == null) {
            sUploadDir = LC.zimbra_tmp_directory.value() + "/upload";
        }
        return sUploadDir;
    }

    private static class TempFileFilter implements FileFilter {
        private final long mNow = System.currentTimeMillis();

        TempFileFilter()  { }

        /** Returns <code>true</code> if the specified <code>File</code>
         *  follows the {@link DefaultFileItem} naming convention
         *  (<code>upload_*.tmp</code>) and is older than
         *  {@link FileUploadServlet#UPLOAD_TIMEOUT_MSEC}. */
        @Override
        public boolean accept(File pathname) {
            // upload_ XYZ .tmp
            if (pathname == null)
                return false;
            String name = pathname.getName();
            // file naming convention used by DefaultFileItem class
            return name.startsWith("upload_") && name.endsWith(".tmp") && mNow - pathname.lastModified() > UPLOAD_TIMEOUT_MSEC;
        }
    }

    private static void cleanupLeftoverTempFiles() {
        File files[] = new File(getUploadDir()).listFiles(new TempFileFilter());
        if (files == null || files.length < 1)
            return;

        mLog.info("deleting %d temporary upload files left over from last time", files.length);
        for (int i = 0; i < files.length; i++) {
            String path = files[i].getAbsolutePath();
            if (files[i].delete()) {
                mLog.info("deleted leftover upload file %s", path);
            } else {
                mLog.error("unable to delete leftover upload file %s", path);
            }
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        ZimbraLog.clearContext();
        addRemoteIpToLoggingContext(req);

        String fmt = req.getParameter(ContentServlet.PARAM_FORMAT);

        ZimbraLog.addUserAgentToContext(req.getHeader("User-Agent"));

        // file upload requires authentication
        boolean isAdminRequest = false;
        try {
            isAdminRequest = isAdminRequest(req);
        } catch (ServiceException e) {
            drainRequestStream(req);
            throw new ServletException(e);
        }

        AuthToken at = isAdminRequest ? getAdminAuthTokenFromCookie(req, resp, true) : getAuthTokenFromCookie(req, resp, true);
        if (at == null) {
            mLog.info("Auth token not present.  Returning %d response.", HttpServletResponse.SC_UNAUTHORIZED);
            drainRequestStream(req);
            sendResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, fmt, null, null, null);
            return;
        }

        boolean doCsrfCheck = false;
        boolean csrfCheckComplete = false;
        if (req.getAttribute(CsrfFilter.CSRF_TOKEN_CHECK) != null) {
            doCsrfCheck =  (Boolean) req.getAttribute(CsrfFilter.CSRF_TOKEN_CHECK);
        }

        if (JWTUtil.isJWT(at)) {
            doCsrfCheck = false;
        }

        if (doCsrfCheck) {
            String csrfToken = req.getHeader(Constants.CSRF_TOKEN);

            // Bug: 96344
            if (!StringUtil.isNullOrEmpty(csrfToken)) {
                if (!CsrfUtil.isValidCsrfToken(csrfToken, at)) {

                    drainRequestStream(req);
                    mLog.info("CSRF token validation failed for account: %s"
                        + ", Auth token is CSRF enabled: %s" + ". CSRF token is: %s", at,
                        at.isCsrfTokenEnabled(), csrfToken);
                    sendResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, fmt, null, null, null);
                    return;
                }
                csrfCheckComplete = true;
            } else {
                if (at.isCsrfTokenEnabled()) {
                    csrfCheckComplete = false;
                    mLog.debug("CSRF token was not found in the header. Auth token is %s, it is CSRF enabled:  %s, will check if sent in"
                        + " form field.", at, at.isCsrfTokenEnabled());
                }
            }
        } else {
            csrfCheckComplete = true;
        }


        try {
            Provisioning prov = Provisioning.getInstance();
            Account acct = AuthProvider.validateAuthToken(prov, at, true);
            if (!isAdminRequest) {
                // fetching the mailbox will except if it's in maintenance mode
                if (Provisioning.onLocalServer(acct)) {
                    Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct, false);
                    if (mbox != null) {
                        ZimbraLog.addMboxToContext(mbox.getId());
                    }
                }
            }

            boolean limitByFileUploadMaxSize = req.getParameter(PARAM_LIMIT_BY_FILE_UPLOAD_MAX_SIZE) != null;

            // file upload requires multipart enctype
            if (ServletFileUpload.isMultipartContent(req)) {
                handleMultipartUpload(req, resp, fmt, acct, limitByFileUploadMaxSize, at, csrfCheckComplete);
            } else {
                if (!csrfCheckComplete) {
                    drainRequestStream(req);
                    mLog.info("CSRF token validation failed for account: %s.No csrf token recd.", acct);
                    sendResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, fmt, null, null, null);
                } else {
                    handlePlainUpload(req, resp, fmt, acct, limitByFileUploadMaxSize);
                }
            }
        } catch (ServiceException e) {
            mLog.info("File upload failed", e);
            drainRequestStream(req);
            returnError(resp, e);
        }
    }

    @SuppressWarnings("unchecked")
    List<Upload> handleMultipartUpload(HttpServletRequest req, HttpServletResponse resp, String fmt, Account acct,
        boolean limitByFileUploadMaxSize, AuthToken at, boolean csrfCheckComplete)
    throws IOException, ServiceException {
        List<FileItem> items = null;
        String reqId = null;

        ServletFileUpload upload = getUploader2(limitByFileUploadMaxSize, acct);
        try {
            items = upload.parseRequest(req);

            if (!csrfCheckComplete && !CsrfUtil.checkCsrfInMultipartFileUpload(items, at)) {
                drainRequestStream(req);
                mLog.info("CSRF token validation failed for account: %s, Auth token is CSRF enabled", acct.getName());
                sendResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, fmt, null, null, items);
                return Collections.emptyList();
            }
        } catch (FileUploadBase.SizeLimitExceededException e) {
            // at least one file was over max allowed size
            mLog.info("Exceeded maximum upload size of " + upload.getSizeMax() + " bytes: " + e);
            drainRequestStream(req);
            sendResponse(resp, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, fmt, reqId, null, items);
            return Collections.emptyList();
        } catch (FileUploadBase.InvalidContentTypeException e) {
            // at least one file was of a type not allowed
            mLog.info("File upload failed", e);
            drainRequestStream(req);
            sendResponse(resp, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, fmt, reqId, null, items);
            return Collections.emptyList();
        } catch (FileUploadException e) {
            // parse of request failed for some other reason
            mLog.info("File upload failed", e);
            drainRequestStream(req);
            sendResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, fmt, reqId, null, items);
            return Collections.emptyList();
        }

        String charset = "utf-8";
        LinkedList<String> names = new LinkedList<String>();
        HashMap<FileItem, String> filenames = new HashMap<FileItem, String>();
        if (items != null) {
            for (Iterator<FileItem> it = items.iterator(); it.hasNext(); ) {
                FileItem fi = it.next();
                if (fi == null)
                    continue;

                if (fi.isFormField()) {
                    if (fi.getFieldName().equals("requestId")) {
                        // correlate this file upload session's request and response
                        reqId = fi.getString();
                    } else if (fi.getFieldName().equals("_charset_") && !fi.getString().equals("")) {
                        // get the form value charset, if specified
                        charset = fi.getString();
                    } else if (fi.getFieldName().startsWith("filename")) {
                        // allow a client to explicitly provide filenames for the uploads
                        names.clear();
                        String value = fi.getString(charset);
                        if (!Strings.isNullOrEmpty(value)) {
                            for (String name : value.split("\n")) {
                                names.add(name.trim());
                            }
                        }
                    }
                    // strip form fields out of the list of uploads
                    it.remove();
                } else {
                    if (fi.getName() == null || fi.getName().trim().equals("")) {
                        it.remove();
                    } else {
                        filenames.put(fi, names.isEmpty() ? null : names.remove());
                    }
                }
            }
        }

        // restrict requestId value for safety due to later use in javascript
        if (reqId != null && reqId.length() != 0) {
            if (!ALLOWED_REQUESTID_CHARS.matcher(reqId).matches()) {
                mLog.info("Rejecting upload with invalid chars in reqId: %s", reqId);
                sendResponse(resp, HttpServletResponse.SC_BAD_REQUEST, fmt, null, null, items);
                return Collections.emptyList();
            }
        }

        // empty upload is not a "success"
        if (items == null || items.isEmpty()) {
            mLog.info("No data in upload for reqId: %s", reqId);
            sendResponse(resp, HttpServletResponse.SC_NO_CONTENT, fmt, reqId, null, items);
            return Collections.emptyList();
        }

        // cache the uploaded files in the hash and construct the list of upload IDs
        List<Upload> uploads = new ArrayList<Upload>(items.size());
        for (FileItem fi : items) {
            String name = filenames.get(fi);
            if (name == null || name.trim().equals(""))
                name = fi.getName();
            Upload up = new Upload(acct.getId(), fi, name);

            mLog.info("Received multipart: %s", up);
            synchronized (mPending) {
                mPending.put(up.uuid, up);
            }
            uploads.add(up);
        }

        sendResponse(resp, HttpServletResponse.SC_OK, fmt, reqId, uploads, items);
        return uploads;
    }

    /**
     * This is used when handling a POST request generated by {@link ZMailbox#uploadContentAsStream}
     *
     * @param req
     * @param resp
     * @param fmt
     * @param acct
     * @param limitByFileUploadMaxSize
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    List<Upload> handlePlainUpload(HttpServletRequest req, HttpServletResponse resp, String fmt, Account acct,
            boolean limitByFileUploadMaxSize) throws IOException, ServiceException {
        // metadata is encoded in the response's HTTP headers
        ContentType ctype = new ContentType(req.getContentType());
        String contentType = ctype.getContentType(), filename = ctype.getParameter("name");
        if (filename == null) {
            filename = new ContentDisposition(req.getHeader("Content-Disposition")).getParameter("filename");
        }

        if (filename == null || filename.trim().equals("")) {
            mLog.info("Rejecting upload with no name.");
            drainRequestStream(req);
            sendResponse(resp, HttpServletResponse.SC_NO_CONTENT, fmt, null, null, null);
            return Collections.emptyList();
        }

        // Unescape the filename so it actually displays correctly
        filename = StringEscapeUtils.unescapeHtml(filename);

        // store the fetched file as a normal upload
        ServletFileUpload upload = getUploader2(limitByFileUploadMaxSize, acct);
        FileItem fi = upload.getFileItemFactory().createItem("upload", contentType, false, filename);
        try {
            // write the upload to disk, but make sure not to exceed the permitted max upload size
            long size = ByteUtil.copy(req.getInputStream(), false, fi.getOutputStream(), true, upload.getSizeMax() * 3);
            if ((upload.getSizeMax() >= 0 /* -1 would mean "no limit" */) && (size > upload.getSizeMax())) {
                mLog.debug("handlePlainUpload(): deleting %s", fi);
                fi.delete();
                mLog.info("Exceeded maximum upload size of " + upload.getSizeMax() + " bytes: " + acct.getId());
                drainRequestStream(req);
                sendResponse(resp, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, fmt, null, null, null);
                return Collections.emptyList();
            }
        } catch (IOException ioe) {
            mLog.warn("Unable to store upload.  Deleting %s", fi, ioe);
            fi.delete();
            drainRequestStream(req);
            sendResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, fmt, null, null, null);
            return Collections.emptyList();
        }
        List<FileItem> items = new ArrayList<FileItem>(1);
        items.add(fi);
        Upload up = new Upload(acct.getId(), fi, filename);

        mLog.info("Received plain: %s", up);
        synchronized (mPending) {
            mPending.put(up.uuid, up);
        }

        List<Upload> uploads = Arrays.asList(up);
        sendResponse(resp, HttpServletResponse.SC_OK, fmt, null, uploads, items);
        return uploads;
    }

    public static void sendResponse(HttpServletResponse resp, int status, String fmt, String reqId, List<Upload> uploads, List<FileItem> items)
    throws IOException {
        boolean raw = false, extended = false;
        if (fmt != null && !fmt.trim().equals("")) {
            // parse out the comma-separated "fmt" options
            for (String foption : fmt.toLowerCase().split(",")) {
                raw      |= ContentServlet.FORMAT_RAW.equals(foption);
                extended |= "extended".equals(foption);
            }
        }

        StringBuffer results = new StringBuffer();
        results.append(status).append(",'").append(reqId != null ? StringUtil.jsEncode(reqId) : "null").append('\'');
        if (status == HttpServletResponse.SC_OK) {
            boolean first = true;
            if (extended) {
                // serialize as a list of JSON objects, one per upload
                results.append(",[");
                for (Upload up : uploads) {
                    Element.JSONElement elt = new Element.JSONElement("ignored");
                    elt.addAttribute(MailConstants.A_ATTACHMENT_ID, up.uuid);
                    elt.addAttribute(MailConstants.A_CONTENT_TYPE, up.getContentType());
                    elt.addAttribute(MailConstants.A_CONTENT_FILENAME, up.name);
                    elt.addAttribute(MailConstants.A_SIZE, up.getSize());
                    results.append(first ? "" : ",").append(elt.toString());
                    first = false;
                }
                results.append(']');
            } else {
                // serialize as a string containing the comma-separated upload IDs
                results.append(",'");
                for (Upload up : uploads) {
                    results.append(first ? "" : UPLOAD_DELIMITER).append(up.uuid);
                    first = false;
                }
                results.append('\'');
            }
        }

        resp.setContentType("text/html; charset=utf-8");
        PrintWriter out = resp.getWriter();

        if (raw) {
            out.println(results);
        } else {
            out.println("<html><head>" +
                    "<script language='javascript'>\nfunction doit() { window.parent._uploadManager.loaded("+ results + "); }\n</script>" +
                    "</head><body onload='doit()'></body></html>\n");
        }
        out.close();

        // handle failure by cleaning up the failed upload
        if (status != HttpServletResponse.SC_OK && items != null && items.size() > 0) {
            for (FileItem fi : items) {
                mLog.debug("sendResponse(): deleting %s", fi);
                fi.delete();
            }
        }
    }

    /**
     * Reads the end of the client request when an error occurs, to avoid cases where
     * the client blocks when writing the HTTP request.
     */
    public static void drainRequestStream(HttpServletRequest req) {
        try {
            InputStream in = req.getInputStream();
            byte[] buf = new byte[1024];
            int numRead = 0;
            int totalRead = 0;
            mLog.debug("Draining request input stream");
            while ((numRead = in.read(buf)) >= 0) {
                totalRead += numRead;
            }
            mLog.debug("Drained %d bytes", totalRead);
        } catch (IOException e) {
            mLog.info("Ignoring error that occurred while reading the end of the client request: " + e);
        }
    }

    private static long getFileUploadMaxSize(boolean limitByFileUploadMaxSize) {
        // look up the maximum file size for uploads
        long maxSize = DEFAULT_MAX_SIZE;
        try {
            if (limitByFileUploadMaxSize) {
                maxSize = Provisioning.getInstance().getLocalServer().getLongAttr(
                        Provisioning.A_zimbraFileUploadMaxSize, DEFAULT_MAX_SIZE);
            } else {
                maxSize = Provisioning.getInstance().getConfig().getLongAttr(
                        Provisioning.A_zimbraMtaMaxMessageSize, DEFAULT_MAX_SIZE);
                if (maxSize == 0) {
                    /* zimbraMtaMaxMessageSize=0 means "no limit".  The return value from this function gets used
                     * by FileUploadBase "sizeMax" where "-1" means "no limit"
                     */
                    maxSize = -1;
                }
            }
        } catch (ServiceException e) {
            mLog.error("Unable to read " +
                      ((limitByFileUploadMaxSize) ? Provisioning.A_zimbraFileUploadMaxSize : Provisioning.A_zimbraMtaMaxMessageSize) +
                      " attribute", e);
        }
        return maxSize;
    }

    private static long getFileUploadMaxSize(boolean limitByFileUploadMaxSize, Account acct) {
        long maxSize = DEFAULT_MAX_SIZE;
        String attributeUsed = null;
        try {
            if (limitByFileUploadMaxSize && acct.getFileUploadMaxSizePerFile() > 0) {
                long fileUploadSizePerFile = acct.getFileUploadMaxSizePerFile();
                long fileUploadSize = Provisioning.getInstance().getLocalServer().getLongAttr(
                        Provisioning.A_zimbraFileUploadMaxSize, DEFAULT_MAX_SIZE);
                if (fileUploadSizePerFile >= fileUploadSize) {
                    maxSize = fileUploadSize;
                    attributeUsed = Provisioning.A_zimbraFileUploadMaxSize;
                } else {
                    maxSize = fileUploadSizePerFile;
                    attributeUsed = Provisioning.A_zimbraFileUploadMaxSizePerFile;
                }
            } else if (!limitByFileUploadMaxSize && acct.getMailAttachmentMaxSize() > 0) {
                long mailAttachmentMaxSize = acct.getMailAttachmentMaxSize();
                long mtaMaxMsgSize = Provisioning.getInstance().getConfig().getLongAttr(
                        Provisioning.A_zimbraMtaMaxMessageSize, DEFAULT_MAX_SIZE);
                if (mailAttachmentMaxSize > mtaMaxMsgSize) {
                    maxSize = mtaMaxMsgSize;
                    attributeUsed = Provisioning.A_zimbraMtaMaxMessageSize;
                } else {
                    maxSize = mailAttachmentMaxSize;
                    attributeUsed = Provisioning.A_zimbraMailAttachmentMaxSize;
                }
                if (maxSize == 0) {
                    /* 0 means "no limit". The return value from this function gets used
                     * by FileUploadBase "sizeMax" where "-1" means "no limit"
                     */
                    maxSize = -1;
                }
            } else {
                maxSize = getFileUploadMaxSize(limitByFileUploadMaxSize);
            }
        } catch (ServiceException exp) {
            mLog.error(String.format("Unable to read %s attribute.", attributeUsed), exp);
        }
        return maxSize;
    }

    public static ServletFileUpload getUploader2(boolean limitByFileUploadMaxSize) {
        return getUploader(getFileUploadMaxSize(limitByFileUploadMaxSize));
    }

    public static ServletFileUpload getUploader2(boolean limitByFileUploadMaxSize, Account acct) {
        return getUploader(getFileUploadMaxSize(limitByFileUploadMaxSize, acct));
    }

    public static ServletFileUpload getUploader(long maxSize) {
        DiskFileItemFactory dfif = new DiskFileItemFactory();
        dfif.setSizeThreshold(32 * 1024);
        dfif.setRepository(new File(getUploadDir()));
        ServletFileUpload upload = new ServletFileUpload(dfif);
        upload.setSizeMax(maxSize);
        upload.setHeaderEncoding("utf-8");
        return upload;
    }

    /** Uploads time out after 15 minutes. */
    static final long UPLOAD_TIMEOUT_MSEC = 15 * Constants.MILLIS_PER_MINUTE;
    /** Purge uploads once every minute. */
    private static final long REAPER_INTERVAL_MSEC = 1 * Constants.MILLIS_PER_MINUTE;

    @Override
    public void init() throws ServletException {
        String name = getServletName();
        mLog.info("Servlet %s starting up", name);
        super.init();

        File tempDir = new File(getUploadDir());
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs()) {
                String msg = "Unable to create temporary upload directory " + tempDir;
                mLog.error(msg);
                throw new ServletException(msg);
            }
        }
        cleanupLeftoverTempFiles();

        Zimbra.sTimer.schedule(new MapReaperTask(), REAPER_INTERVAL_MSEC, REAPER_INTERVAL_MSEC);
    }

    @Override
    public void destroy() {
        String name = getServletName();
        mLog.info("Servlet %s shutting down", name);
        super.destroy();
    }

    private final class MapReaperTask extends TimerTask {
        MapReaperTask()  { }

        @Override
        public void run() {
            try {
                ArrayList<Upload> reaped = new ArrayList<Upload>();
                int sizeBefore;
                int sizeAfter;
                synchronized(mPending) {
                    sizeBefore = mPending.size();
                    long cutoffTime = System.currentTimeMillis() - UPLOAD_TIMEOUT_MSEC;
                    for (Iterator<Upload> it = mPending.values().iterator(); it.hasNext(); ) {
                        Upload up = it.next();
                        if (!up.accessedAfter(cutoffTime)) {
                            mLog.debug("Purging cached upload: %s", up);
                            it.remove();
                            reaped.add(up);
                            up.markDeleted();
                            assert(mPending.get(up.uuid) == null);
                        }
                    }
                    sizeAfter = mPending.size();
                }

                int removed = sizeBefore - sizeAfter;
                if (removed > 0) {
                    mLog.info("Removed %d expired file uploads; %d pending file uploads", removed, sizeAfter);
                } else if (sizeAfter > 0) {
                    mLog.info("%d pending file uploads", sizeAfter);
                }

                for (Upload up : reaped) {
                    up.purge();
                }
            } catch (Throwable e) { //don't let exceptions kill the timer
                if (e instanceof OutOfMemoryError) {
                    Zimbra.halt("Caught out of memory error", e);
                }
                ZimbraLog.system.warn("Caught exception in FileUploadServlet timer", e);
            }
        }
    }
}
