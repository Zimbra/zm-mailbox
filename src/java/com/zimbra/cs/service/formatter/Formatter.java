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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

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
        } catch (ServiceException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UserServletException)
                throw (UserServletException) cause;
            if (cause instanceof ServletException)
                throw (ServletException) cause;
            if (cause instanceof IOException)
                throw (IOException) cause;
            throw e;
        } finally {
            op.finish();
        }
    }

    public final void save(UserServlet.Context context, String contentType, Folder folder, String filename)
    throws UserServletException, IOException, ServletException, ServiceException {
        BlockingOperation op = BlockingOperation.schedule(this.getClass().getSimpleName()+"(SAVE)", null, context.opContext, context.targetMailbox, Requester.REST, Priority.BATCH, 1);
        try {
            saveCallback(context, contentType, folder, filename);
        } catch (ServiceException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UserServletException)
                throw (UserServletException) cause;
            if (cause instanceof ServletException)
                throw (ServletException) cause;
            if (cause instanceof IOException)
                throw (IOException) cause;
            throw e;
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
                    ZimbraLog.misc.info("folderId: " + f.getId());
                    if (f.getId() != Mailbox.ID_FOLDER_USER_ROOT)
                        query = "in:" + f.getPath() + " " + query; 
                }
                ZimbraLog.misc.info("query: " + query);
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
    
}
