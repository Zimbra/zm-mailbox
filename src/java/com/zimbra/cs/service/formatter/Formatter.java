package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.index.AppointmentHit;
import com.zimbra.cs.index.ContactHit;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.util.ZimbraLog;

public abstract class Formatter {
    
    public abstract String getType();
    private static final byte[] SEARCH_TYPES = new byte[] { MailItem.TYPE_MESSAGE };

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

    public abstract boolean format(UserServlet.Context context, MailItem item) throws IOException, ServiceException;    
    
    public boolean notImplemented(Context context, String message) throws IOException {
        context.resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, message);
        return false;
    }
    
    public boolean notImplemented(Context context) throws IOException {
        context.resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "support for requested item/format not implemented yet");
        return false;
    }

    public Iterator getMailItems(Context context, MailItem item, long startTime, long endTime) throws ServiceException {
        String query = context.getQueryString();
        if (query != null) {
            try {
                if (item instanceof Folder) {
                    Folder f = (Folder) item;
                    ZimbraLog.misc.info("folderId: "+f.getId());
                    if (f.getId() != Mailbox.ID_FOLDER_USER_ROOT)
                        query = "in:"+f.getPath()+" "+query; 
                }
                ZimbraLog.misc.info("query: "+query);
                String searchTypes = context.getTypesString();
                if (searchTypes == null) searchTypes = getDefaultSearchTypes();
                byte[] types = MailboxIndex.parseGroupByString(searchTypes);
                ZimbraQueryResults results = context.targetMailbox.search(context.opContext, query, types, MailboxIndex.SEARCH_ORDER_DATE_DESC, 500);
                return new QueryResultIterator(results);                
            } catch (IOException e) {
                // TODO Auto-generated catch block
                throw ServiceException.FAILURE("search error", e);
            } catch (ParseException e) {
                throw ServiceException.FAILURE("search error", e);
            }
        } else if (item instanceof Folder) {
            Collection items = getMailItemsFromFolder(context, (Folder) item, startTime, endTime);
            return items != null ? items.iterator() : null;
        } else if (item instanceof MailItem) {
            ArrayList result = new ArrayList();
            result.add(item);
            return result.iterator();
        }
        return null;
    }

    private Collection getMailItemsFromFolder(Context context, Folder folder, long startTime, long endTime) throws ServiceException {
        switch (folder.getDefaultView()) {
        case MailItem.TYPE_APPOINTMENT:            
            return context.targetMailbox.getAppointmentsForRange(context.opContext, startTime, endTime, folder.getId(), null);
        case MailItem.TYPE_CONTACT:
            return context.targetMailbox.getContactList(context.opContext, folder.getId());
        default:
            return context.targetMailbox.getItemList(context.opContext, MailItem.TYPE_MESSAGE, folder.getId());
        }
    }
 
    private static class QueryResultIterator implements Iterator {

        private ZimbraQueryResults mResults;
        
        QueryResultIterator(ZimbraQueryResults results) {
            mResults = results;
        }

        public boolean hasNext() {
            try {
                return mResults.hasNext();
            } catch (ServiceException e) {
                ZimbraLog.misc.warn("caught exception", e);
                return false;
            }
        }

        public Object next() {
            try {
                ZimbraHit hit = mResults.getNext();
                if (hit == null) return null;
                if (hit instanceof MessageHit) {
                    return ((MessageHit)hit).getMessage();
                } else if (hit instanceof ContactHit) {
                    return ((ContactHit)hit).getContact();
                } else if (hit instanceof AppointmentHit) {
                    return ((AppointmentHit)hit).getAppointment();
                }
            } catch (ServiceException e) {
                ZimbraLog.misc.warn("caught exception", e);                
            }
            return null;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }
}
