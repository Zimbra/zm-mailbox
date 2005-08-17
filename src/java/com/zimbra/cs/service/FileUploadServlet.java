package com.zimbra.cs.service;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.servlet.LiquidServlet;
import com.zimbra.cs.util.Constants;
import com.zimbra.cs.util.Liquid;

public class FileUploadServlet extends LiquidServlet {

    private static final class Upload {
        String accountId;
        long   time;
        String uuid;
        List /* <FileItem */ files;

        /**
         * 
         * @param auth
         * @param attachments a <code>List</code> of
         * <code>org.apache.commonsfileupload.FileItem</code> objects
         */
        Upload(AuthToken auth, List attachments) {
            accountId = auth.getAccountId();
            time      = System.currentTimeMillis();
            uuid      = LdapUtil.generateUUID();
            files     = attachments;
        }

        boolean accessedAfter(long checkpoint)  { return time > checkpoint; }

        void purge() {
            if (files != null)
                for (Iterator it = files.iterator(); it.hasNext(); )
                    ((FileItem) it.next()).delete();
        }
    }

    private static HashMap mPending = new HashMap(100);
    private static Log mLog = LogFactory.getLog(FileUploadServlet.class);
    
    //when you refactor constants across services, make sure this is the same as
    //ContentServlet.PARAM_FORMAT
    private static final String PARAM_FORMAT = "fmt";
    private static final String FORMAT_RAW   = "raw";
    private static final int DEFAULT_MAX_SIZE = 5 * 1024 * 1024;

    public static List fetchUploads(String accountId, String uploadId) {
        if (accountId == null || uploadId == null)
            return null;
        synchronized (mPending) {
            Upload up = (Upload) mPending.get(uploadId);
            if (up == null || !accountId.equals(up.accountId))
                return null;
            up.time = System.currentTimeMillis();
            return up.files;
        }
    }

    public static void deleteUploads(String accountId, String uploadId) {
        if (accountId == null || uploadId == null)
            return;
        Upload up = null;
        synchronized (mPending) {
            up = (Upload) mPending.remove(uploadId);
            if (!accountId.equals(up.accountId)) {
                mPending.put(uploadId, up);
                up = null;
            }
        }
        if (up != null)
            up.purge();
    }

    private static String getTempDirectory() {
    	return System.getProperty("java.io.tmpdir", "/tmp");
    }

    private static class TempFileFilter implements FileFilter {
        private long mNow = System.currentTimeMillis();
        
        /**
         * Returns <code>true</code> if the specified <code>File</code> follows the
         * <code>org.apache.commons.fileupload.DefaultFileItem</code> naming convention
         * (<code>upload_*.tmp</code>) and is older than {@link FileUploadServlet#UPLOAD_TIMEOUT_MSEC}.
         */
    	public boolean accept(File pathname) {
            // upload_ XYZ .tmp
            if (pathname == null) return false;
            String name = pathname.getName();
            // file naming convention used by DefaultFileItem class
            // in Jakarta Commons FileUpload.
            return name.startsWith("upload_") && name.endsWith(".tmp") &&
                (mNow - pathname.lastModified() > UPLOAD_TIMEOUT_MSEC);
        }
    }

    private static void cleanupLeftoverTempFiles() {
        File files[] = new File(getTempDirectory()).listFiles(new TempFileFilter());
        if (files == null || files.length < 1) return;

        mLog.info("Deleting " + files.length + " temporary upload files left over from last time");
        for (int i = 0; i < files.length; i++) {
            String path = files[i].getAbsolutePath();
            if (files[i].delete())
                mLog.info("Deleted left-over upload file " + path);
            else
                mLog.error("Unable to delete left-over upload file " + path);
        }
    }

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		int status = HttpServletResponse.SC_OK;
        String attachmentId = null;
        String reqId = null;
        do {
    		AuthToken authToken = getAuthTokenFromCookie(req, resp, true);
            if (authToken == null) {
                status = HttpServletResponse.SC_UNAUTHORIZED;
                break;
            }

            // file upload requires multipart enctype
            if (!FileUpload.isMultipartContent(req)) {
            	status = HttpServletResponse.SC_BAD_REQUEST;
                break;
            }
            
            // Look up the maximum file size for uploads
            String attrName = Provisioning.A_liquidFileUploadMaxSize;
            int maxSize = DEFAULT_MAX_SIZE;
            try {
                Server config = Provisioning.getInstance().getLocalServer();
                maxSize = config.getIntAttr(attrName, DEFAULT_MAX_SIZE);
            } catch (ServiceException e) {
                mLog.error("Unable to read " + attrName + " attribute", e);
            }
            
            DiskFileUpload upload = new DiskFileUpload();
            upload.setSizeThreshold(4096);     // in-memory limit
            upload.setSizeMax(maxSize);
            upload.setRepositoryPath(getTempDirectory());
            List items = null;
            try {
            	items = upload.parseRequest(req);
            } catch (FileUploadBase.SizeLimitExceededException e) {
                // at least one file was over max allowed size
                status = HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
                mLog.info("Exceeded maximum upload size of " + maxSize + " bytes: " + e);
                break;
            } catch (FileUploadBase.InvalidContentTypeException e) {
                // at least one file was of a type not allowed
                status = HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;
                mLog.info("File upload failed", e);
                break;
            } catch (FileUploadException e) {
            	// parse of request failed for some other reason
                status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                mLog.info("File upload failed", e);
                break;
            }

            // strip form fields out of the list of uploads...
            if (items != null)
                for (Iterator it = items.iterator(); it.hasNext(); ) {
                    FileItem fi = (FileItem) it.next();
                    if (fi != null && fi.getFieldName().equals("requestId")){
                        reqId = fi.getString();
                    }
                    if (fi == null || fi.isFormField() || fi.getName() == null || fi.getName().trim().equals(""))
                        it.remove();
                }

            if (items == null || items.isEmpty()) {
                status = HttpServletResponse.SC_NO_CONTENT;
                break;
            }

            Upload up = new Upload(authToken, items);
            synchronized (mPending) {
            	mPending.put(up.uuid, up);
            }
            attachmentId = up.uuid;
        } while (false);

        StringBuffer results = new StringBuffer();
    	results.append(status);
        results.append(",'");
        if(reqId != null){
            results.append(reqId);
        } else {
            results.append("null");
        }
        results.append("'");
        if (status == HttpServletResponse.SC_OK)
        	results.append(",'" + attachmentId + '\'');
        	
        
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        
        String fmt = req.getParameter(PARAM_FORMAT);
        if( FORMAT_RAW.equals( fmt ) ) {
			out.println( results );
        } else {			
			out.println("<html><head></head><body onload=\"window.parent._uploadManager.loaded(" + results + ");\">");
			out.println("</body></html>");
		}
		
		out.close();
	}


    /**
     * Uploads time out after 15 minutes.
     */
    private static final long UPLOAD_TIMEOUT_MSEC = 15 * Constants.MILLIS_PER_MINUTE;
    /**
     * Purge uploads once every minute.
     */
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

        Liquid.sTimer.schedule(new MapReaperTask(), REAPER_INTERVAL_MSEC, REAPER_INTERVAL_MSEC);
    }

    public void destroy() {
        String name = getServletName();
        mLog.info("Servlet " + name + " shutting down");
        super.destroy();
    }

    private final class MapReaperTask extends TimerTask {
        public void run() {
            ArrayList reaped = new ArrayList();
            int sizeBefore;
            int sizeAfter;
            synchronized(mPending) {
                sizeBefore = mPending.size();
                long cutoffTime = System.currentTimeMillis() - UPLOAD_TIMEOUT_MSEC;
                for (Iterator it = mPending.values().iterator(); it.hasNext(); ) {
                    Upload up = (Upload) it.next();
                    if (!up.accessedAfter(cutoffTime)) {
                        mLog.debug("Purging cached upload: " + up.uuid);
                        it.remove();
                        reaped.add(up);
                        assert(mPending.get(up.uuid) == null);
                    }
                }
                sizeAfter = mPending.size();
            }
            if (mLog.isInfoEnabled()) {
                int removed = sizeBefore - sizeAfter;
                String msg;
                if (removed > 0)
                    mLog.info("Removed " + removed + " expired file uploads; " +
                              sizeAfter + " pending file uploads");
                else if (sizeAfter > 0)
                    mLog.info(sizeAfter + " pending file uploads");
            }
            for (Iterator it = reaped.iterator(); it.hasNext(); )
                ((Upload) it.next()).purge();
        }
    }
}
