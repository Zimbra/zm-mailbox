/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeDetect;
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
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.store.BlobInputStream;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.Zimbra;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

public class FileUploadServlet extends ZimbraServlet {
    private static final long serialVersionUID = -3156986245375108467L;
    
    // bug 27610
    // We now limit file upload size for messages by zimbraMtaMaxMessageSize
    // If this query param is present in the URI, upload size is limited by zimbraFileUploadMaxSize,
    // This allows customer to allow larger documents/briefcase files than messages sent via SMTP.
    protected static final String PARAM_LIMIT_BY_FILE_UPLOAD_MAX_SIZE = "lbfums";

    /** The character separating upload IDs in a list */
    public static final String UPLOAD_DELIMITER = ",";
    /** The character separating server ID from upload ID */
    private static final String UPLOAD_PART_DELIMITER = ":";
    
    private static String sUploadDir; 

    public static final class Upload {
        final String   accountId;
        String         contentType;
        final String   uuid;
        final String   name;
        final FileItem file;
        long time;
        
        Upload(String acctId, FileItem attachment) throws ServiceException {
            this(acctId, attachment, attachment.getName());
        }

        Upload(String acctId, FileItem attachment, String filename) throws ServiceException {
            String localServer = Provisioning.getInstance().getLocalServer().getId();
            accountId = acctId;
            time      = System.currentTimeMillis();
            uuid      = localServer + UPLOAD_PART_DELIMITER + LdapUtil.generateUUID();
            name      = FileUtil.trimFilename(filename);
            file      = attachment;
            if (file == null) {
                contentType = MimeConstants.CT_TEXT_PLAIN;
            } else {
                // use content based detection.  we can't use magic based
                // detection alone because it defaults to application/xml
                // when it sees xml magic <?xml.  that's incompatible
                // with WebDAV handlers as the content type needs to be
                // text/xml instead.
                
                // 1. detect by file extension
                contentType = MimeDetect.getMimeDetect().detect(name);
                
                // 2. special-case text/xml to avoid detection
                if (contentType == null && file.getContentType() != null) {
                    if (file.getContentType().equals("text/xml"))
                        contentType = file.getContentType();
                }
                
                // 3. detect by magic
                if (contentType == null) {
                    try {
                        contentType = MimeDetect.getMimeDetect().detect(file.getInputStream());
                    } catch (Exception e) {
                        contentType = null;
                    }
                }
                
                // 4. try the browser-specified content type 
                if (contentType == null || contentType.equals(MimeConstants.CT_APPLICATION_OCTET_STREAM)) {
                    contentType = file.getContentType();
                }
                
                // 5. when all else fails, use application/octet-stream
                if (contentType == null)
                    contentType = file.getContentType();
                if (contentType == null)
                    contentType = MimeConstants.CT_APPLICATION_OCTET_STREAM;
            }
        }

        public String getName()         { return name; }
        public String getId()           { return uuid; }
        public String getContentType()  { return contentType; }
        public long getSize()           { return file == null ? 0 : file.getSize(); }

        public InputStream getInputStream() throws IOException {
            if (file == null)
                return new ByteArrayInputStream(new byte[0]);
            if (!file.isInMemory() && (file instanceof DiskFileItem)) {
                // If it's backed by a File, return a BlobInputStream so that any use by JavaMail
                // will avoid loading the whole thing in memory.
                File f = ((DiskFileItem) file).getStoreLocation();
                return new BlobInputStream(f, f.length());
            } else {
                return file.getInputStream();
            }
        }

        boolean accessedAfter(long checkpoint)  { return time > checkpoint; }

        void purge()  { if (file != null)  file.delete(); }

        @Override public String toString() {
            return "Upload: { accountId=" + accountId + ", time=" + new Date(time) +
                   ", uploadId=" + uuid + ", " + (file == null ? "no file" : name) + "}";
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
            return up;
        }
    }

    private static Upload fetchRemoteUpload(String accountId, String uploadId, AuthToken authtoken) throws ServiceException {
        // check if we have fetched the Upload from the remote server previously
        String localUploadId = mProxiedUploadIds.get(uploadId);
        if (localUploadId != null) {
            synchronized (mPending) {
                Upload up = mPending.get(localUploadId);
                if (up != null)
                    return up;
            }
        }
        // the first half of the upload id is the server id where it lives
        Server server = Provisioning.getInstance().get(ServerBy.id, getUploadServerId(uploadId));
        String url = AccountUtil.getBaseUri(server);
        if (url == null)
            return null;
        String hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);
        url += ContentServlet.SERVLET_PATH + ContentServlet.PREFIX_PROXY + '?' +
               ContentServlet.PARAM_UPLOAD_ID + '=' + uploadId + '&' +
               ContentServlet.PARAM_EXPUNGE + "=true";

        // create an HTTP client with auth cookie to fetch the file from the remote ContentServlet
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        GetMethod get = new GetMethod(url);
        authtoken.encode(client, get, false, hostname);
        try {
            // fetch the remote item
            int statusCode = HttpClientUtil.executeMethod(client, get);
            if (statusCode != HttpStatus.SC_OK)
                return null;

            // metadata is encoded in the response's HTTP headers
            Header ctHeader = get.getResponseHeader("Content-Type");
            String contentType = ctHeader == null ? "text/plain" : ctHeader.getValue();
            Header cdispHeader = get.getResponseHeader("Content-Disposition");
            String filename = cdispHeader == null ? "unknown" : new ContentDisposition(cdispHeader.getValue()).getParameter("filename");

            // store the fetched upload along with original uploadId
            Upload up = saveUpload(get.getResponseBodyAsStream(), filename, contentType, accountId);
            mProxiedUploadIds.put(uploadId, up.uuid);
            return up;
        } catch (HttpException e) {
            throw ServiceException.PROXY_ERROR(e, url);
        } catch (IOException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("can't fetch remote upload", e, new InternalArgument(ServiceException.URL, url, Argument.Type.STR));
        } finally {
            get.releaseConnection();
        }
    }

    public static Upload saveUpload(InputStream is, String filename, String contentType, String accountId) throws ServiceException, IOException {
        FileItem fi = null;
        boolean success = false;
        try {
            // store the fetched file as a normal upload
            ServletFileUpload upload = getUploader(false);
            fi = upload.getFileItemFactory().createItem("upload", contentType, false, filename);
            long size = ByteUtil.copy(is, true, fi.getOutputStream(), true, upload.getSizeMax() * 3);
            if (size > upload.getSizeMax())
                throw MailServiceException.UPLOAD_REJECTED(filename, "upload too large");

            Upload up = new Upload(accountId, fi);
            mLog.info("Received file: name=%s, size=%d, id=%s", up.getName(), up.getSize(), up.getId());
            synchronized (mPending) {
                mPending.put(up.uuid, up);
            }
            success = true;
            return up;
        } finally {
            if (!success && fi != null)
                fi.delete();
        }
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
        Upload up = null;
        synchronized (mPending) {
            up = mPending.remove(upload.uuid);
        }
        if (up == upload)
            up.purge();
    }

    private static String getUploadDir() {
        if (sUploadDir == null) {
            sUploadDir = LC.zimbra_tmp_directory.value() + "/upload";
        }
        return sUploadDir;
    }

    private static class TempFileFilter implements FileFilter {
        private long mNow = System.currentTimeMillis();

        TempFileFilter()  { }
        
        /** Returns <code>true</code> if the specified <code>File</code>
         *  follows the {@link DefaultFileItem} naming convention
         *  (<code>upload_*.tmp</code>) and is older than
         *  {@link FileUploadServlet#UPLOAD_TIMEOUT_MSEC}. */
    	public boolean accept(File pathname) {
            // upload_ XYZ .tmp
            if (pathname == null)
                return false;
            String name = pathname.getName();
            // file naming convention used by DefaultFileItem class
            return name.startsWith("upload_") && name.endsWith(".tmp") &&
                   (mNow - pathname.lastModified() > UPLOAD_TIMEOUT_MSEC);
        }
    }

    private static void cleanupLeftoverTempFiles() {
        File files[] = new File(getUploadDir()).listFiles(new TempFileFilter());
        if (files == null || files.length < 1) return;

        mLog.info("deleting " + files.length + " temporary upload files left over from last time");
        for (int i = 0; i < files.length; i++) {
            String path = files[i].getAbsolutePath();
            if (files[i].delete())
                mLog.info("deleted leftover upload file " + path);
            else
                mLog.error("unable to delete leftover upload file " + path);
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

        AuthToken at = isAdminRequest ? getAdminAuthTokenFromCookie(req, resp, false) : getAuthTokenFromCookie(req, resp, false);
        if (at == null) {
            drainRequestStream(req);
            sendResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, fmt, null, null, null);
            return;
        }

        try {
            if (!isAdminRequest) {
                Provisioning prov = Provisioning.getInstance();
                Account acct = AuthProvider.validateAuthToken(prov, at, true);
                
                // fetching the mailbox will except if it's in maintenance mode
                if (Provisioning.onLocalServer(acct)) {
                    Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct, false);
                    if (mbox != null)
                        ZimbraLog.addMboxToContext(mbox.getId());
                }
            }

            boolean limitByFileUploadMaxSize = (req.getParameter(FileUploadServlet.PARAM_LIMIT_BY_FILE_UPLOAD_MAX_SIZE) != null);

            // file upload requires multipart enctype
            if (ServletFileUpload.isMultipartContent(req))
                handleMultipartUpload(req, resp, fmt, at.getAccountId(), limitByFileUploadMaxSize);
            else
                handlePlainUpload(req, resp, fmt, at.getAccountId(), limitByFileUploadMaxSize);
        } catch (ServiceException e) {
            mLog.info("File upload failed", e);
            drainRequestStream(req);
            returnError(resp, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMultipartUpload(HttpServletRequest req, HttpServletResponse resp, String fmt, String accountId, boolean limitByFileUploadMaxSize)
    throws IOException, ServiceException {
        List<FileItem> items = null;
        String reqId = null;

        ServletFileUpload upload = getUploader(limitByFileUploadMaxSize);
        try {
            items = upload.parseRequest(req);
        } catch (FileUploadBase.SizeLimitExceededException e) {
            // at least one file was over max allowed size
            mLog.info("Exceeded maximum upload size of " + upload.getSizeMax() + " bytes: " + e);
            drainRequestStream(req);
            sendResponse(resp, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, fmt, reqId, null, items);
            return;
        } catch (FileUploadBase.InvalidContentTypeException e) {
            // at least one file was of a type not allowed
            mLog.info("File upload failed", e);
            drainRequestStream(req);
            sendResponse(resp, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, fmt, reqId, null, items);
            return;
        } catch (FileUploadException e) {
            // parse of request failed for some other reason
            mLog.info("File upload failed", e);
            drainRequestStream(req);
            sendResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, fmt, reqId, null, items);
            return;
        }

        String lastName = null, charset = "utf-8";
        HashMap<FileItem, String> filenames = new HashMap<FileItem, String>();
        if (items != null) {
            for (Iterator<FileItem> it = items.iterator(); it.hasNext(); ) {
                FileItem fi = it.next();
                if (fi == null)
                    continue;
                if (fi.isFormField()) {
                    // correlate this file upload session's request and response
                    if (fi.getFieldName().equals("requestId"))
                        reqId = fi.getString();
                    // get the form value charset, if specified
                    if (fi.getFieldName().equals("_charset_") && !fi.getString().equals(""))
                        charset = fi.getString();
                    // allow a client to explicitly provide filenames for the uploads
                    if (fi.getFieldName().startsWith("filename"))
                        lastName = fi.getString(charset);
                    // strip form fields out of the list of uploads
                    it.remove();
                } else {
                    if (fi.getName() == null || fi.getName().trim().equals("")) {
                        it.remove();
                    } else {
                        filenames.put(fi, lastName);
                        lastName = null;
                    }
                }
            }
        }

        // empty upload is not a "success"
        if (items == null || items.isEmpty()) {
            sendResponse(resp, HttpServletResponse.SC_NO_CONTENT, fmt, reqId, null, items);
            return;
        }

        // cache the uploaded files in the hash and construct the list of upload IDs
        List<Upload> uploads = new ArrayList<Upload>(items.size());
        for (FileItem fi : items) {
        	String name = filenames.get(fi);
        	if (name == null || name.trim().equals(""))
        		name = fi.getName();
        	Upload up = new Upload(accountId, fi, name);

        	ZimbraLog.mailbox.info("FileUploadServlet received %s", up);
        	synchronized (mPending) {
        		mPending.put(up.uuid, up);
        	}
        	uploads.add(up);
        }

        sendResponse(resp, HttpServletResponse.SC_OK, fmt, reqId, uploads, items);
    }

    private void handlePlainUpload(HttpServletRequest req, HttpServletResponse resp, String fmt, String accountId, boolean limitByFileUploadMaxSize) throws IOException, ServiceException {
        // metadata is encoded in the response's HTTP headers
        ContentType ctype = new ContentType(req.getContentType());
        String contentType = ctype.getContentType(), filename = ctype.getParameter("name");
        if (filename == null)
            filename = new ContentDisposition(req.getHeader("Content-Disposition")).getParameter("filename");

        if (filename == null || filename.trim().equals("")) {
            mLog.info("Rejecting upload with no name.");
            drainRequestStream(req);
            sendResponse(resp, HttpServletResponse.SC_NO_CONTENT, fmt, null, null, null);
            return;
        }

        // store the fetched file as a normal upload
        ServletFileUpload upload = getUploader(limitByFileUploadMaxSize);
        FileItem fi = upload.getFileItemFactory().createItem("upload", contentType, false, filename);
        try {
            // write the upload to disk, but make sure not to exceed the permitted max upload size
            long size = ByteUtil.copy(req.getInputStream(), false, fi.getOutputStream(), true, upload.getSizeMax() * 3);
            if (size > upload.getSizeMax()) {
                fi.delete();
                mLog.info("Exceeded maximum upload size of " + upload.getSizeMax() + " bytes: " + accountId);
                drainRequestStream(req);
                sendResponse(resp, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, fmt, null, null, null);
                return;
            }
        } catch (IOException ioe) {
            fi.delete();
            drainRequestStream(req);
            sendResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, fmt, null, null, null);
            return;
        }
        List<FileItem> items = new ArrayList<FileItem>(1);
        items.add(fi);

        Upload up = new Upload(accountId, fi, filename);
        ZimbraLog.mailbox.info("FileUploadServlet received " + up);
        synchronized (mPending) {
        	mPending.put(up.uuid, up);
        }

        sendResponse(resp, HttpServletResponse.SC_OK, fmt, null, Arrays.asList(up), items);
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
            for (FileItem fi : items)
                fi.delete();
        }
    }
    
    /**
     * Reads the end of the client request when an error occurs, to avoid cases where
     * the client blocks when writing the HTTP request.  
     */
    private static void drainRequestStream(HttpServletRequest req) {
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

    private static ServletFileUpload getUploader(boolean limitByFileUploadMaxSize) {
        // look up the maximum file size for uploads
        long maxSize = DEFAULT_MAX_SIZE;
        DiskFileItemFactory dfif = new DiskFileItemFactory();
        ServletFileUpload upload;
        
        try {
            if (limitByFileUploadMaxSize) {
                maxSize = Provisioning.getInstance().getLocalServer().getLongAttr(Provisioning.A_zimbraFileUploadMaxSize, DEFAULT_MAX_SIZE);
            } else {
                maxSize = Provisioning.getInstance().getConfig().getLongAttr(Provisioning.A_zimbraMtaMaxMessageSize, DEFAULT_MAX_SIZE);
            }
        } catch (ServiceException e) {
            mLog.error("Unable to read " + 
                      ((limitByFileUploadMaxSize)?Provisioning.A_zimbraFileUploadMaxSize:Provisioning.A_zimbraMtaMaxMessageSize) + 
                      " attribute", e);
        }
        dfif.setSizeThreshold(32 * 1024);
        dfif.setRepository(new File(getUploadDir()));
        upload = new ServletFileUpload(dfif);
        upload.setSizeMax(maxSize);
        return upload;
    }

    /** Uploads time out after 15 minutes. */
    static final long UPLOAD_TIMEOUT_MSEC = 15 * Constants.MILLIS_PER_MINUTE;
    /** Purge uploads once every minute. */
    private static final long REAPER_INTERVAL_MSEC = 1 * Constants.MILLIS_PER_MINUTE;

    public void init() throws ServletException {
        String name = getServletName();
        mLog.info("Servlet " + name + " starting up");
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

    public void destroy() {
        String name = getServletName();
        mLog.info("Servlet " + name + " shutting down");
        super.destroy();
    }

    private final class MapReaperTask extends TimerTask {
        MapReaperTask()  { }

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
                            mLog.debug("Purging cached upload: " + up);
                            it.remove();
                            reaped.add(up);
                            assert(mPending.get(up.uuid) == null);
                        }
                    }
                    sizeAfter = mPending.size();
                }
                if (mLog.isInfoEnabled()) {
                    int removed = sizeBefore - sizeAfter;
                    if (removed > 0)
                        mLog.info("Removed " + removed + " expired file uploads; " +
                            sizeAfter + " pending file uploads");
                    else if (sizeAfter > 0)
                        mLog.info(sizeAfter + " pending file uploads");
                }
                for (Upload up : reaped)
                    up.purge();
            } catch (Throwable e) { //don't let exceptions kill the timer
                if (e instanceof OutOfMemoryError)
                    Zimbra.halt("Caught out of memory error", e);
                ZimbraLog.system.warn("Caught exception in FileUploadServlet timer", e);
            }
        }
    }
}
