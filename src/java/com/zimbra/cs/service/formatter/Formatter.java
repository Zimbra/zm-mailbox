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
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.operation.BlockingOperation;
import com.zimbra.cs.operation.Requester;
import com.zimbra.cs.operation.Scheduler.Priority;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public abstract class Formatter {
    public abstract String getType();
    private static String PROGRESS = "-progress";

    public String[] getDefaultMimeTypes() {
        return new String[0];
    }

    /**
     * @return true if this formatter requires auth
     */
    public boolean requiresAuth() {
        return true;
    }
    
    /**
     * 
     * @return true if this formatter can be blocked by zimbraAttachmentsBlocked attr.
     */
    public abstract boolean canBeBlocked();
    
    // eventually get this from query param ?start=long|YYYYMMMDDHHMMSS
    public long getDefaultStartTime() {
        return -1;
    }
    
    public long getDefaultEndTime() {
        return -1;
    }
    
    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_MESSAGES;
    }

    public final void format(UserServlet.Context context) throws UserServletException, IOException, ServletException, ServiceException {
        BlockingOperation op = BlockingOperation.schedule(this.getClass().getSimpleName()+"(FORMAT)", null, context.opContext, context.targetMailbox, Requester.REST, Priority.BATCH, 1);

        try {
            formatCallback(context);
            updateClient(context, null);
        } catch (Exception e) {
            updateClient(context, e);
        } finally {
            op.finish();
        }
    }

    public final void save(UserServlet.Context context, String contentType, Folder folder, String filename)
    throws UserServletException, IOException, ServletException, ServiceException {
        BlockingOperation op = BlockingOperation.schedule(this.getClass().getSimpleName()+"(SAVE)", null, context.opContext, context.targetMailbox, Requester.REST, Priority.BATCH, 1);

        try {
            saveCallback(context, contentType, folder, filename);
            updateClient(context, null);
        } catch (Exception e) {
            updateClient(context, e);
        } finally {
            op.finish();
        }
    }

    public abstract void formatCallback(UserServlet.Context context)
    throws UserServletException, ServiceException, IOException, ServletException;

    public void saveCallback(UserServlet.Context context, String contentType, Folder folder, String filename)
    throws UserServletException, ServiceException, IOException, ServletException {
        throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "format not supported for save");
    }

    public boolean supportsSave() {
        return false;
    }

    // Caller is responsible for filtering out Appointments/Tasks marked private if the requester
    // is not the mailbox owner.
    public Iterator<? extends MailItem> getMailItems(Context context, long startTime, long endTime, long chunkSize) throws ServiceException {
    	if (context.respListItems != null) {
    		return context.respListItems.iterator();
    	}
    	
    	assert(context.target != null);
        String query = context.getQueryString();
        if (query != null) {
            try {
                if (context.target instanceof Folder) {
                    Folder f = (Folder) context.target;
                    if (f.getId() != Mailbox.ID_FOLDER_USER_ROOT)
                        query = "in:" + f.getPath() + " " + query; 
                }
                String searchTypes = context.getTypesString();
                if (searchTypes == null)
                    searchTypes = getDefaultSearchTypes();
                byte[] types = MailboxIndex.parseTypesString(searchTypes);
                ZimbraQueryResults results = context.targetMailbox.search(context.opContext, query, types, MailboxIndex.SortBy.DATE_DESCENDING, context.getOffset() + context.getLimit());
                return new QueryResultIterator(results);                
            } catch (IOException e) {
                // TODO Auto-generated catch block
                throw ServiceException.FAILURE("search error", e);
            } catch (ParseException e) {
                throw ServiceException.FAILURE("search error", e);
            }
        } else if (context.target instanceof Folder) {
            Collection<? extends MailItem> items = getMailItemsFromFolder(context, (Folder) context.target, startTime, endTime, chunkSize);
            return items != null ? items.iterator() : null;
        } else {
            ArrayList<MailItem> result = new ArrayList<MailItem>();
            result.add(context.target);
            return result.iterator();
        }
    }

    private Collection<? extends MailItem> getMailItemsFromFolder(Context context, Folder folder, long startTime, long endTime, long chunkSize) throws ServiceException {
        switch (folder.getDefaultView()) {
            case MailItem.TYPE_APPOINTMENT:
            case MailItem.TYPE_TASK:
                return context.targetMailbox.getCalendarItemsForRange(context.opContext, startTime, endTime, folder.getId(), null);
            case MailItem.TYPE_CONTACT:
                return context.targetMailbox.getContactList(context.opContext, folder.getId());
            case MailItem.TYPE_DOCUMENT:
            case MailItem.TYPE_WIKI:
                return context.targetMailbox.getDocumentList(context.opContext, folder.getId());
            default:
                return context.targetMailbox.getItemList(context.opContext, MailItem.TYPE_MESSAGE, folder.getId());
        }
    }
 
    private static final class UploadInputStream extends InputStream {
        private InputStream is;
        private long curSize = 0;
        private long maxSize;
        private long markSize = 0;
        
        UploadInputStream(InputStream is, long maxSize) throws IOException {
            this.is = is;
            this.maxSize = maxSize;
        }

        public void close() throws IOException { is.close(); }
        
        public int available() throws IOException { return is.available(); }

        public void mark(int where) { is.mark(where); markSize = curSize; }

        public boolean markSupported() { return is.markSupported(); }
        
        public int read() throws IOException { return (int)check(is.read()); }
        
        public int read(byte b[]) throws IOException { return (int)check(is.read(b)); }

        public int read(byte b[], int off, int len) throws IOException {
            return (int)check(is.read(b, off, len));
        }

        public void reset() throws IOException { is.reset(); curSize = markSize; }

        public long skip(long n) throws IOException { return check(is.skip(n)); }

        private long check(long in) throws IOException {
            if (in > 0) {
                curSize += in;
                if (maxSize > 0 && curSize > maxSize)
                    throw new IOException("upload too large");
            }
            return in;
        }
    }
    
    
    public InputStream getRequestInputStream(UserServlet.Context context) 
        throws IOException, ServiceException, UserServletException {
        return getRequestInputStream(context, true);
    }
    
    public InputStream getRequestInputStream(UserServlet.Context context, boolean limit) 
        throws IOException, ServiceException, UserServletException {
        InputStream is = null;
        long maxSize = -1;
        final long DEFAULT_MAX_SIZE = 10 * 1024 * 1024;
        
        if (limit) {
            if (context.req.getParameter("lbfums") != null)
                maxSize = Provisioning.getInstance().getLocalServer().
                    getLongAttr(Provisioning.A_zimbraFileUploadMaxSize,
                    DEFAULT_MAX_SIZE);
            else
                maxSize = Provisioning.getInstance().getConfig().
                    getLongAttr(Provisioning.A_zimbraMtaMaxMessageSize,
                    DEFAULT_MAX_SIZE);
        }
        if (ServletFileUpload.isMultipartContent(context.req)) {
            ServletFileUpload sfu = new ServletFileUpload();
            
            try {
                FileItemIterator iter = sfu.getItemIterator(context.req);
                
                while (iter.hasNext()) {
                    FileItemStream fis = iter.next();
                    
                    if (!fis.isFormField()) {
                        is = new UploadInputStream(fis.openStream(), maxSize);
                        break;
                    }
                }
            } catch (Exception e) {
                throw new UserServletException(HttpServletResponse.
                    SC_UNSUPPORTED_MEDIA_TYPE, e.toString());
            }
            if (is == null)
                throw new UserServletException(HttpServletResponse.
                    SC_NO_CONTENT, "No file content");
        } else {
            String ce = context.req.getHeader("content-encoding");
            
            is = new UploadInputStream(ce != null && ce.indexOf("gzip") != -1 ?
                new GZIPInputStream(context.req.getInputStream()) :
                    context.req.getInputStream(), maxSize);
        }
        return is;
    }
    
    /**
     * 
     * @param attr
     * @param accountId
     * @return
     * @throws ServletException
     */
    public static boolean checkGlobalOverride(String attr, Account account) throws ServletException {
        Provisioning prov = Provisioning.getInstance();
        try {
            return prov.getConfig().getBooleanAttr(attr, false) || account.getBooleanAttr(attr, false);
        } catch (ServiceException e) {
            throw new ServletException(e);
        }
    }

    protected static class QueryResultIterator implements Iterator<MailItem> {
        private ZimbraQueryResults mResults;
        
        QueryResultIterator(ZimbraQueryResults results) {
            mResults = results;
        }

        public boolean hasNext() {
            if (mResults == null)
                return false;
            try {
                return mResults.hasNext();
            } catch (ServiceException e) {
                ZimbraLog.misc.warn("caught exception", e);
                return false;
            }
        }

        public MailItem next() {
            if (mResults == null)
                return null;
            try {
                ZimbraHit hit = mResults.getNext();
                if (hit != null)
                    return hit.getMailItem();
            } catch (ServiceException e) {
                ZimbraLog.misc.warn("caught exception", e);                
            }
            return null;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void finished() {
            try {
                if (mResults != null)
                    mResults.doneWithSearchResults();
            } catch (ServiceException e) { }
            mResults = null;
        }
    }
    
    protected PrintWriter updateClient(Context context, boolean flush) throws IOException {
        PrintWriter pw;
        
        if (context.params.get(PROGRESS) == null) {
            context.resp.reset();
            context.resp.setContentType("text/html; charset=\"utf-8\"");
            context.resp.setCharacterEncoding("utf-8");
            context.params.put(PROGRESS, "1");
            pw = context.resp.getWriter();
            pw.print("<html>\n<head>\n</head>\n");
        } else {
            pw = context.resp.getWriter();
            pw.println();
        }
        if (flush)
            pw.flush();
        return pw;
    }
    
    private void updateClient(UserServlet.Context context, Exception e)
        throws UserServletException, IOException, ServletException, ServiceException {
        String callback = context.params.get("callback");
        Throwable exception = null;
        
        if (e != null) {
            Throwable cause = e.getCause();
            
            exception = cause instanceof UserServletException ||
                cause instanceof ServletException ||
                cause instanceof IOException ? cause : e;
        }
        if (callback == null || callback.equals("")) {
            if (context.params.get(PROGRESS) == null) {
                if (exception == null)
                    return;
                else if (exception instanceof UserServletException)
                    throw (UserServletException)exception;
                else if (exception instanceof ServletException)
                    throw (ServletException)exception;
                else if (exception instanceof IOException)
                    throw (IOException)exception;
                throw ServiceException.FAILURE(getType() + " formatter failure",
                    exception);
            }
            if (exception == null) {
                context.resp.getWriter().print("<body></body>\n</html>\n");
            } else {
                String s = exception.getLocalizedMessage();

                ZimbraLog.misc.warn(getType() + " formatter exception", exception);
                s = s.replace("\n", "<br>");
                context.resp.getWriter().print("<body>\n<pre>\n" + s +
                    "\n</pre>\n</body>\n</html>\n");
            }
        } else {
            String s;
            
            if (exception == null) {
                s = "''";
            } else {
                ZimbraLog.misc.warn(getType() + " formatter exception", exception);
                s = exception.getLocalizedMessage();
                s.substring(0, s.length() > 4096 ? 4096 : s.length());
                s = s.replace("\\", "\\\\");
                s = s.replace("'", "\\\'");
                s = s.replace("\"", "\\\'");
                s = s.replace("\n", "\\n");
                s = s.replace("\r", "\\r");
                s = "'" + s + "', '" + exception.getClass().getName() + "'";
                if (exception instanceof ServiceException)
                    s += ", '" + ((ServiceException)exception).getCode() + "'";
                else if (exception instanceof UserServletException)
                    s += ", '" + ((UserServletException)exception).getHttpStatusCode() + "'";
            }
            updateClient(context, false);
            context.resp.getWriter().print("<body onload=\"window.parent." +
                callback + "(" + s + ");\">\n</body>\n</html>\n");
        }
    }
}
