/*
 * Created on Nov 9, 2004
 */
package com.liquidsys.coco.session;

import java.util.Iterator;
import java.util.List;

import com.liquidsys.coco.index.LiquidQueryResults;
import com.liquidsys.coco.mailbox.*;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.mail.GetFolder;
import com.liquidsys.coco.service.mail.ToXML;
import com.liquidsys.coco.session.PendingModifications.Change;
import com.liquidsys.soap.LiquidContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



/**
 * @author tim
 *
 * DO NOT INSTANTIATE THIS DIRECTLY -- instead call SessionCache.getNewSession() 
 * to create objects of this type.
 * 
 * Add your own get/set methods here for session data.
 */
public class SoapSession extends Session {

    static Log mLog = LogFactory.getLog(SoapSession.class);

    // Read/write access to all these members requires synchronizing on "this".
    private String  mQueryStr = "";
    private String  mGroupBy  = "";
    private String  mSortBy   = "";
    private boolean mNotify   = true;
    private LiquidQueryResults   mQueryResults = null;

    private PendingModifications mChanges = new PendingModifications();


    SoapSession(String accountId, Object contextId) {
        super(accountId, contextId);
    }

    public void haltNotifications() {
        synchronized (this) {
            mChanges.clear();
            mNotify = false;
        }
    }

    public void resumeNotifications() {
        synchronized (this) {
            mNotify = true;
        }
    }

    /*
     * Notification callback
     */
    protected void notifyPendingChanges(PendingModifications pms) {
        if (!pms.hasNotifications())
            return;

        synchronized (this) {
            // XXX: should constrain to folders, tags, and stuff relevant to the current query?
            if (mNotify && pms.deleted != null)
                for (Iterator it = pms.deleted.values().iterator(); it.hasNext(); ) {
                    Object obj = it.next();
                    if (obj instanceof MailItem)
                        mChanges.recordDeleted((MailItem) obj);
                    else if (obj instanceof Integer)
                        mChanges.recordDeleted(((Integer) obj).intValue());
                }
    
            if (mNotify && pms.created != null)
                for (Iterator it = pms.created.values().iterator(); it.hasNext(); )
                    mChanges.recordCreated((MailItem) it.next());
    
            if (mNotify && pms.modified != null)
                for (Iterator it = pms.modified.values().iterator(); it.hasNext(); ) {
                    Change chg = (Change) it.next();
                    if (chg.what instanceof MailItem)
                    	mChanges.recordModified((MailItem) chg.what, chg.why);
                    else if (chg.what instanceof Mailbox)
                        mChanges.recordModified((Mailbox) chg.what, chg.why);
                }
        }

        try {
        	clearCachedQueryResults();
        } catch (ServiceException e) {
            mLog.warn("ServiceException while clearing query result cache", e);
        }
    }

    private static final String E_NOTIFY   = "notify";
    private static final String E_REFRESH  = "refresh";
    private static final String E_TAGS     = "tags";
    private static final String E_CREATED  = "created";
    private static final String E_DELETED  = "deleted";
    private static final String E_MODIFIED = "modified";

    private static final String A_ID = "id";

    /**
     * Refreshed in the browser
     * 
     * @param ctxt
     */
    public void putRefresh(Element ctxt) throws ServiceException {
        synchronized (this) {
            if (!mNotify)
                return;
            mChanges.clear();
        }

        Element eRefresh = ctxt.addUniqueElement(E_REFRESH);

        Mailbox mbox = Mailbox.getMailboxByAccountId(mAccountId);
        // Lock the mailbox but not the "this" object, to avoid deadlock
        // with another thread that calls a Session method from within a
        // synchronized Mailbox method.
        synchronized (mbox) {
            // dump current mailbox status (currently just size)
            ToXML.encodeMailbox(eRefresh, mbox);

            // dump all tags under a single <tags> parent
            List tags = mbox.getTagList();
            if (tags != null && tags.size() > 0) {
                Element eTags = eRefresh.addUniqueElement(E_TAGS);
                for (Iterator it = tags.iterator(); it.hasNext(); ) {
                    Tag tag = (Tag) it.next();
                    if (tag != null && !(tag instanceof Flag))
                        ToXML.encodeTag(eTags, tag);
                }
            }

            // dump recursive folder hierarchy starting at USER_ROOT (i.e. folders visible to the user)
            Folder root = mbox.getFolderById(Mailbox.ID_FOLDER_USER_ROOT);
            GetFolder.handleFolder(mbox, root, eRefresh);
        }
    }

    public Element putNotifications(LiquidContext lc, Element ctxt) {
        synchronized (this) {
            if (ctxt == null)
                ctxt = lc.createElement(LiquidContext.CONTEXT);

            try {
                Mailbox mbox = Mailbox.getMailboxByAccountId(lc.getRequestedAccountId());
                ctxt.addUniqueElement(LiquidContext.E_CHANGE).addAttribute(LiquidContext.A_CHANGE_ID, mbox.getLastChangeID());
            } catch (ServiceException e) {
                mLog.warn("error putting change checkpoint to SOAP header response", e);
            }

            if (!mNotify || !mChanges.hasNotifications())
                return ctxt;

            Element eNotify = ctxt.addUniqueElement(E_NOTIFY);

            if (mChanges.deleted != null && mChanges.deleted.size() > 0) {
                StringBuffer ids = new StringBuffer();
                for (Iterator it = mChanges.deleted.values().iterator(); it.hasNext(); ) {
                    if (ids.length() != 0)
                        ids.append(',');
                    Object obj = it.next();
                    if (obj instanceof MailItem)
                        ids.append(((MailItem) obj).getId());
                    else if (obj instanceof Integer)
                        ids.append(obj);
                }
                Element eDeleted = eNotify.addUniqueElement(E_DELETED);
                eDeleted.addAttribute(A_ID, ids.toString());
            }
    
            if (mChanges.created != null && mChanges.created.size() > 0) {
                Element eCreated = eNotify.addUniqueElement(E_CREATED);
                for (Iterator it = mChanges.created.values().iterator(); it.hasNext(); )
                    ToXML.encodeItem(eCreated, (MailItem) it.next(), Change.ALL_FIELDS);
            }

            if (mChanges.modified != null && mChanges.modified.size() > 0) {
                Element eModified = eNotify.addUniqueElement(E_MODIFIED);
                for (Iterator it = mChanges.modified.values().iterator(); it.hasNext(); ) {
                    Change chg = (Change) it.next();
                    if (chg.why != 0 && chg.what instanceof MailItem)
                        ToXML.encodeItem(eModified, (MailItem) chg.what, chg.why);
                    else if (chg.why != 0 && chg.what instanceof Mailbox)
                        ToXML.encodeMailbox(eModified, (Mailbox) chg.what, chg.why);
                }
            }

            mChanges.clear();
        }
        return ctxt;
    }


    public void clearCachedQueryResults() throws ServiceException {
        synchronized (this) {
            try {
                if (mQueryResults != null)
                    mQueryResults.doneWithSearchResults();
            } finally {
                mQueryStr = "";
                mGroupBy  = "";
                mSortBy   = "";
                mQueryResults = null;
            }
        }
    }
    
    public void putQueryResults(String queryStr, String groupBy, String sortBy, LiquidQueryResults res) throws ServiceException
    {
        synchronized (this) {
            clearCachedQueryResults();
            mQueryStr = queryStr;
            mGroupBy = groupBy;
            mSortBy = sortBy;
            mQueryResults = res;
        }
    }
    
    public LiquidQueryResults getQueryResults(String queryStr, String groupBy, String sortBy) {
        synchronized (this) {
            if (mQueryStr.equals(queryStr) && mGroupBy.equals(groupBy) && mSortBy.equals(sortBy))
                return mQueryResults;
            else
                return null;
        }
    }
    
    public void cleanup() {
        try {
            clearCachedQueryResults();
        } catch (ServiceException e) {
        	mLog.warn("ServiceException while cleaning up Session", e);
        }
    }
}
