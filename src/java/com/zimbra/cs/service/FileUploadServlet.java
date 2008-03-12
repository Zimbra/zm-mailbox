/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.DefaultFileItem;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.ContentType;
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
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.Zimbra;

public class FileUploadServlet extends ZimbraServlet {
    private static final long serialVersionUID = -3156986245375108467L;

    /** The character separating upload IDs in a list */
    public static final String UPLOAD_DELIMITER = ",";
    /** The character separating server ID from upload ID */
    private static final String UPLOAD_PART_DELIMITER = ":";

    public static final class Upload {
        final String   accountId;
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
        }

        public String getName()         { return name; }
        public String getId()           { return uuid; }
        public String getContentType()  { return file.getContentType(); }
        public InputStream getInputStream() throws IOException {
            return file.getInputStream();
        }
        
        public long getSize() {
            if (this.file == null) {
                return 0;
            }
            return this.file.getSize();
        }

        boolean accessedAfter(long checkpoint)  { return time > checkpoint; }

        void purge()  { if (file != null)  file.delete(); }

        public String toString() {
            return "Upload: { accountId=" + accountId + ", time=" + new Date(time) +
                   ", uploadId=" + uuid + ", " + (file == null ? "no file" : name) + "}";
        }
    }

    static HashMap<String, Upload> mPending = new HashMap<String, Upload>(100);
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
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(url);
        authtoken.encode(client, get, false, hostname);

        FileItem fi = null;
        boolean success = false;
        try {
            // fetch the remote item
            int statusCode = client.executeMethod(get);
            if (statusCode != HttpStatus.SC_OK)
                return null;

            // metadata is encoded in the response's HTTP headers
            Header ctHeader = get.getResponseHeader("Content-Type");
            String contentType = ctHeader == null ? "text/plain" : ctHeader.getValue();
            Header cdispHeader = get.getResponseHeader("Content-Disposition");
            String filename = cdispHeader == null ? "unknown" : new ContentDisposition(cdispHeader.getValue()).getParameter("filename");

            // store the fetched file as a normal upload
            return saveUpload(get.getResponseBodyAsStream(), filename, contentType, accountId);
        } catch (HttpException e) {
            throw ServiceException.PROXY_ERROR(e, url);
        } catch (IOException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("can't fetch remote upload", e, new InternalArgument(ServiceException.URL, url, Argument.Type.STR));
        } finally {
            if (!success && fi != null)
                fi.delete();
            get.releaseConnection();
        }
    }

    public static Upload saveUpload(InputStream is, String filename, String contentType, String accountId) throws ServiceException, IOException {
        FileItem fi = null;
        boolean success = false;
        try {
            // store the fetched file as a normal upload
            DiskFileUpload upload = getUploader();
            fi = upload.getFileItemFactory().createItem("upload", contentType, false, filename);
            int size = ByteUtil.copy(is, true, fi.getOutputStream(), true, upload.getSizeMax() * 3);
            if (size > upload.getSizeMax())
                throw MailServiceException.UPLOAD_REJECTED(filename, "upload too large");

            Upload up = new Upload(accountId, fi);
            ZimbraLog.mailbox.info("FileUploadServlet received %s", up);
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

    public static void deleteUploads(List<Upload> uploads) {
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

    private static String getTempDirectory() {
    	return System.getProperty("java.io.tmpdir", "/tmp");
    }

    private static class TempFileFilter implements FileFilter {
        private long mNow = System.currentTimeMillis();
        
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
        File files[] = new File(getTempDirectory()).listFiles(new TempFileFilter());
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
        
	    String fmt = req.getParameter(ContentServlet.PARAM_FORMAT);
	    addRemoteIpToLoggingContext(req);
	    ZimbraLog.addUserAgentToContext(req.getHeader("User-Agent"));

        // file upload requires authentication
        int adminPort = -1;
        try {
            adminPort = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, -1);
        } catch (ServiceException e) {
            drainRequestStream(req);
            throw new ServletException(e);
        }
        boolean isAdminRequest = (req.getLocalPort() == adminPort);
        AuthToken at = isAdminRequest ? getAdminAuthTokenFromCookie(req, resp, true) : getAuthTokenFromCookie(req, resp, true);
        if (at == null) {
            drainRequestStream(req);
            sendResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, fmt, null, null, null);
            return;
        }

        try {
        	if (!isAdminRequest) {
        		// make sure we're on the right host; proxy if we're not...
        		Provisioning prov = Provisioning.getInstance();
        		Account acct = prov.get(AccountBy.id, at.getAccountId());
        		if (acct == null)
        			throw AccountServiceException.NO_SUCH_ACCOUNT(at.getAccountId());
        		ZimbraLog.addAccountNameToContext(acct.getName());
        		if (!Provisioning.onLocalServer(acct)) {
        			proxyServletRequest(req, resp, prov.getServer(acct), null);
        			return;
        		}
        		// make sure the authenticated account is active
        		if (acct.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_MAINTENANCE))
        			throw AccountServiceException.MAINTENANCE_MODE();
        		else if (!acct.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_ACTIVE))
        			throw AccountServiceException.ACCOUNT_INACTIVE(acct.getName());
        		// fetching the mailbox will except if it's in maintenance mode
        		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(acct.getId(), false);
        		if (mbox != null)
        			ZimbraLog.addMboxToContext(mbox.getId());
        	}

        	// file upload requires multipart enctype
        	if (FileUploadBase.isMultipartContent(req))
        		handleMultipartUpload(req, resp, fmt, at.getAccountId());
        	else
        		handlePlainUpload(req, resp, fmt, at.getAccountId());
        } catch (ServiceException e) {
            mLog.info("File upload failed", e);
            drainRequestStream(req);
        	returnError(resp, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMultipartUpload(HttpServletRequest req, HttpServletResponse resp, String fmt, String accountId) throws IOException, ServiceException {
        List<FileItem> items = null;
        String reqId = null;

        DiskFileUpload upload = getUploader();
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

    private void handlePlainUpload(HttpServletRequest req, HttpServletResponse resp, String fmt, String accountId) throws IOException, ServiceException {
        // metadata is encoded in the response's HTTP headers
        ContentType ctype = new ContentType(req.getContentType());
        String contentType = ctype.getValue(), filename = ctype.getParameter("name");
        if (filename == null)
            filename = new ContentDisposition(req.getHeader("Content-Disposition")).getParameter("filename");

        if (filename == null || filename.trim().equals("")) {
            drainRequestStream(req);
            sendResponse(resp, HttpServletResponse.SC_NO_CONTENT, fmt, null, null, null);
            return;
        }

        // store the fetched file as a normal upload
        DiskFileUpload upload = getUploader();
        FileItem fi = upload.getFileItemFactory().createItem("upload", contentType, false, filename);
        try {
            // write the upload to disk, but make sure not to exceed the permitted max upload size
            int size = ByteUtil.copy(req.getInputStream(), false, fi.getOutputStream(), true, upload.getSizeMax() * 3);
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
        results.append(status).append(",'").append(reqId != null ? reqId : "null").append('\'');
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
            mLog.info("Draining request input stream");
            while ((numRead = in.read(buf)) >= 0) {
                mLog.info("Drained %d bytes", numRead);
            }
        } catch (IOException e) {
            mLog.info("Ignoring error that occurred while reading the end of the client request: " + e);
        }
    }

    private static DiskFileUpload getUploader() {
        // look up the maximum file size for uploads
        long maxSize = DEFAULT_MAX_SIZE;
        try {
            Server config = Provisioning.getInstance().getLocalServer();
            maxSize = config.getLongAttr(Provisioning.A_zimbraFileUploadMaxSize, DEFAULT_MAX_SIZE);
        } catch (ServiceException e) {
            mLog.error("Unable to read " + Provisioning.A_zimbraFileUploadMaxSize + " attribute", e);
        }

        DiskFileUpload upload = new DiskFileUpload();
        upload.setSizeThreshold(4096);     // in-memory limit
        upload.setSizeMax(maxSize);
        upload.setRepositoryPath(getTempDirectory());
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

        File tempDir = new File(getTempDirectory());
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
