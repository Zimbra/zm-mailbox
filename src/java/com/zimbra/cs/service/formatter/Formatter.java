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
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.mime.MimeConstants;

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

    public final void format(UserServlet.Context context)
        throws UserServletException, IOException, ServletException, ServiceException {

        try {
            formatCallback(context);
            updateClient(context, null);
        } catch (Exception e) {
            updateClient(context, e);
        }
    }

    public final void save(UserServlet.Context context, String contentType, Folder folder, String filename)
        throws UserServletException, IOException, ServletException, ServiceException {

        Mailbox mbox = context.targetMailbox;
        try {
            mbox.setIndexImmediatelyMode();
            saveCallback(context, contentType, folder, filename);
            updateClient(context, null);
        } catch (Exception e) {
            updateClient(context, e);
        } finally {
            mbox.clearIndexImmediatelyMode();
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
                ZimbraQueryResults results = context.targetMailbox.search(context.opContext, query, types, SortBy.DATE_DESCENDING, context.getOffset() + context.getLimit());
                return new QueryResultIterator(results);
            } catch (IOException e) {
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

    protected Collection<? extends MailItem> getMailItemsFromFolder(Context context, Folder folder, long startTime, long endTime, long chunkSize) throws ServiceException {
        switch (folder.getDefaultView()) {
            case MailItem.TYPE_APPOINTMENT:
            case MailItem.TYPE_TASK:
                return context.targetMailbox.getCalendarItemsForRange(context.opContext, startTime, endTime, folder.getId(), null);
            case MailItem.TYPE_CONTACT:
                return context.targetMailbox.getContactList(context.opContext, folder.getId(), SortBy.NAME_ASCENDING);
            case MailItem.TYPE_DOCUMENT:
            case MailItem.TYPE_WIKI:
                return context.targetMailbox.getDocumentList(context.opContext, folder.getId(), SortBy.NAME_ASCENDING);
            default:
                return context.targetMailbox.getItemList(context.opContext, MailItem.TYPE_MESSAGE, folder.getId());
        }
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

        @Override
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

        @Override
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

        @Override
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

    protected void updateClient(UserServlet.Context context, Exception e)
        throws UserServletException, IOException, ServletException,
        ServiceException {
        updateClient(context, e, null);
    }

    protected void updateClient(UserServlet.Context context, Exception e,
        List<ServiceException> w) throws UserServletException, IOException,
        ServletException, ServiceException {
        String callback = context.params.get("callback");
        Throwable exception = null;
        PrintWriter out;
        ServiceException se = null;

        if (e != null) {
            Throwable cause = e.getCause();

            exception = cause instanceof UserServletException ||
                cause instanceof ServletException ||
                cause instanceof IOException ? cause : e;
        }
        if (exception != null)
            se = exception instanceof ServiceException ?
                (ServiceException) exception :
                FormatterServiceException.UNKNOWN_ERROR(exception);
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
                throw ServiceException.FAILURE(
                    getType() + " formatter failure", exception);
            }
            out = updateClient(context, false);
            if (exception == null && w == null) {
                out.println("<body></body>\n</html>");
            } else {
                ZimbraLog.misc.warn(getType() + " formatter exception",
                    exception);
                out.println("<body>\n<pre>");
                if (exception != null)
                    out.print(exception.getLocalizedMessage());
                for (ServiceException warning : w) {
                    out.println("<br>");
                    out.println(warning.toString().replace("\n", "<br>"));
                }
                out.println("</pre>\n</body>\n</html>");
            }
        } else if (!"2".equals(context.params.get(PROGRESS))) {
            out = updateClient(context, false);
            // mark done no matter what happens next
            context.params.put(PROGRESS, "2");
            out.println("<body onload='onLoad()'>");
            out.println("<script>");
            out.println("function onLoad() {");
            if (exception == null && (w == null || w.size() == 0)) {
                out.print("    window.parent." + callback + "('success');");
            } else {
                String result = exception != null ? "fail" : "warn";

                out.print("    window.parent." + callback + "('" + result + "'");
                if (exception != null) {
                    out.print(",\n\t");
                    out.print(SoapProtocol.SoapJS.soapFault(se));
                }
                if (w != null) {
                    for (ServiceException warning : w) {
                        out.print(",\n\t");
                        out.print(SoapProtocol.SoapJS.soapFault(warning));
                    }
                }
                out.println(");");
            }
            out.println("}");
            out.println("</script>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    protected String getContentType(Context context, String fallback) {
        String mime = context.req.getParameter("mime");
        if (mime != null && !mime.equals("")) {
            return mime;
        } else if (fallback != null && !fallback.equals("")) {
            return fallback;
        } else {
            return MimeConstants.CT_TEXT_PLAIN;
        }
    }

    protected boolean mayAttach(Context context) {
        String noAttach = context.req.getParameter("noAttach");
        return !(noAttach != null && (noAttach.equals("1") || noAttach.equalsIgnoreCase("true")));
    }
}
