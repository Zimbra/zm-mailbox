/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Nov 9, 2004
 */
package com.zimbra.cs.session;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.zimbra.cs.im.IMNotification;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.GetFolder;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.Constants;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



/**
 * @author tim
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
    private ZimbraQueryResults   mQueryResults = null;

    private PendingModifications mChanges = new PendingModifications();
    private List<IMNotification> mIMNotifications = new LinkedList();

    private static final long SOAP_SESSION_TIMEOUT_MSEC = 10 * Constants.MILLIS_PER_MINUTE;


    SoapSession(String accountId, String contextId) throws ServiceException {
        super(accountId, contextId, SessionCache.SESSION_SOAP);
    }

    protected long getSessionIdleLifetime() {
        return SOAP_SESSION_TIMEOUT_MSEC;
    }
    
    public void dumpState(Writer w) {
    	try {
    		w.write("SoapSession - ");
    	} catch(IOException e) { e.printStackTrace(); }
    	super.dumpState(w);
    }

    /** Clears all cached notifications and stops recording future notifications
     *  for this session. */
    public void haltNotifications() {
        synchronized (this) {
            mChanges.clear();
            mNotify = false;
        }
    }

    /** Resumes caching notifications for this session. */
    public void resumeNotifications() {
        synchronized (this) {
            mNotify = true;
        }
    }
    
    public void notifyIM(IMNotification imn) { 
        mIMNotifications.add(imn);
    }
    
    protected boolean shouldRegisterWithIM() { return true; }
    

    /** Handles the set of changes from a single Mailbox transaction.
     *  <p>
     *  Takes a set of new mailbox changes and caches it locally.  This is
     *  currently initiated from inside the Mailbox transaction commit, but we
     *  still shouldn't assume that execution of this method is synchronized
     *  on the Mailbox.
     *  <p>
     *  *All* changes are currently cached, regardless of the client's state/views.
     * 
     * @param pms   A set of new change notifications from our Mailbox  */
    public void notifyPendingChanges(PendingModifications pms) {
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

    /** Serializes basic folder/tag structure to a SOAP response header.
     *  <p>
     *  Adds a &lt;refresh> block to the existing &lt;context> element.
     *  This &lt;refresh> block contains the basic folder, tag, and mailbox
     *  size information needed to display and update the web UI's overview
     *  pane.  The &lt;refresh> block is sent when a new session is created.
     * @param ctxt  An existing SOAP header <context> element 
     * @param lc    The SOAP request's encapsulated context */
    public void putRefresh(Element ctxt, ZimbraContext lc) throws ServiceException {
        synchronized (this) {
            if (!mNotify)
                return;
            mChanges.clear();
        }

        Element eRefresh = ctxt.addUniqueElement(E_REFRESH);

        Mailbox mbox = Mailbox.getMailboxByAccountId(mAccountId);
        Mailbox.OperationContext octxt = lc.getOperationContext();
        // Lock the mailbox but not the "this" object, to avoid deadlock
        // with another thread that calls a Session method from within a
        // synchronized Mailbox method.
        synchronized (mbox) {
            // dump current mailbox status (currently just size)
            ToXML.encodeMailbox(eRefresh, mbox);

            // dump all tags under a single <tags> parent
            List tags = mbox.getTagList(octxt);
            if (tags != null && tags.size() > 0) {
                Element eTags = eRefresh.addUniqueElement(E_TAGS);
                for (Iterator it = tags.iterator(); it.hasNext(); ) {
                    Tag tag = (Tag) it.next();
                    if (tag != null && !(tag instanceof Flag))
                        ToXML.encodeTag(eTags, lc, tag);
                }
            }

            // dump recursive folder hierarchy starting at USER_ROOT (i.e. folders visible to the user)
            try {
                Folder root = mbox.getFolderById(octxt, Mailbox.ID_FOLDER_USER_ROOT);
                GetFolder.handleFolder(mbox, root, eRefresh, lc, octxt);
            } catch (ServiceException e) {
                if (e.getCode() != ServiceException.PERM_DENIED)
                    throw e;
            }
        }
    }

    /** Serializes cached notifications to a SOAP response header.
     *  <p>
     *  Adds a <code>&lt;notify></code> block to an existing <code>&lt;context></code>
     *  element, creating an enclosing <code>&lt;context></code> element if none
     *  is passed in.  This <code>&lt;notify></code> block contains information
     *  about all items deleted, created, or modified in the {@link Mailbox} since
     *  the last client interaction, without regard to the client's state/views.
     *  <p>
     *  For deleted items, only the item IDs are returned.  For created items, the
     *  entire item is serialized.  For modified items, only the modified attributes
     *  are included in the response.
     *  <p>
     *  Example:
     *  <pre>
     *     &lt;notify>
     *       &lt;deleted id="665,66,452,883"/>
     *       &lt;created>
     *         &lt;tag id="66" name="phlox" u="8"/>
     *         &lt;folder id="4353" name="a&p" u="2" l="1"/>
     *       &lt;/created>
     *       &lt;modified>
     *         &lt;tag id="65" u="0"/>
     *         &lt;m id="553" f="ua"/>
     *         &lt;note id="774" color="4">
     *           This is the new content.
     *         &lt;/note>
     *       &lt;/modified>
     *     &lt;/notify>
     *  </pre>
     *  Also adds a "last server change" changestamp to the <context> block.
     *  <p>
     * @param lc    The SOAP request context from the client's request
     * @param ctxt  An existing SOAP header &lt;context> element
     * @return The passed-in <code>&lt;context></code> element */
    public Element putNotifications(ZimbraContext lc, Element ctxt) {
        if (ctxt == null)
            return null;

        Mailbox mbox;
        try {
            mbox = Mailbox.getMailboxByAccountId(getAccountId());
        } catch (ServiceException e) {
            mLog.warn("error fetching mailbox for account " + getAccountId(), e);
            return ctxt;
        }
        String explicitAcct = getAccountId().equals(lc.getAuthtokenAccountId()) ? null : getAccountId();

        // must lock the Mailbox before locking the Session to avoid deadlock
        //   because ToXML functions can now call back into the Mailbox
        synchronized (mbox) {
            synchronized (this) {
                // <change token="555" [acct="4f778920-1a84-11da-b804-6b188d2a20c4"]/>
                ctxt.addUniqueElement(ZimbraContext.E_CHANGE)
                    .addAttribute(ZimbraContext.A_CHANGE_ID, mbox.getLastChangeID())
                    .addAttribute(ZimbraContext.A_ACCOUNT_ID, explicitAcct);
                if (!mNotify || (!mChanges.hasNotifications() && mIMNotifications.size()==0))
                    return ctxt;

                // <notify [acct="4f778920-1a84-11da-b804-6b188d2a20c4"]/>
                Element eNotify = ctxt.addUniqueElement(E_NOTIFY)
                                      .addAttribute(ZimbraContext.A_ACCOUNT_ID, explicitAcct);

                if (mChanges.deleted != null && mChanges.deleted.size() > 0) {
                    StringBuilder ids = new StringBuilder ();
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
                    for (Iterator it = mChanges.created.values().iterator(); it.hasNext(); ) {
                        MailItem mi = (MailItem) it.next();
                        try {
                            ToXML.encodeItem(eCreated, lc, mi, ToXML.NOTIFY_FIELDS);
                        } catch (ServiceException e) {
                            mLog.warn("error encoding item " + mi.getId(), e);
                            return ctxt;
                        }
                    }
                }

                if (mChanges.modified != null && mChanges.modified.size() > 0) {
                    Element eModified = eNotify.addUniqueElement(E_MODIFIED);
                    for (Iterator it = mChanges.modified.values().iterator(); it.hasNext(); ) {
                        Change chg = (Change) it.next();
                        if (chg.why != 0 && chg.what instanceof MailItem) {
                            MailItem mi = (MailItem) chg.what;
                            try {
                                ToXML.encodeItem(eModified, lc, mi, chg.why);
                            } catch (ServiceException e) {
                                mLog.warn("error encoding item " + mi.getId(), e);
                                return ctxt;
                            }
                        } else if (chg.why != 0 && chg.what instanceof Mailbox)
                            ToXML.encodeMailbox(eModified, (Mailbox) chg.what, chg.why);
                    }
                }
                
                if (mIMNotifications.size() > 0) {
                    Element eIM = eNotify.addUniqueElement("im");
                    for (IMNotification imn : mIMNotifications) {
                        try {
                            imn.toXml(eIM);
                        } catch (ServiceException e) {
                            e.printStackTrace();
                        }
                    }
                    mIMNotifications.clear();
                }

                mChanges.clear();
            }
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
    
    public void putQueryResults(String queryStr, String groupBy, String sortBy, ZimbraQueryResults res)
    throws ServiceException {
        synchronized (this) {
            clearCachedQueryResults();
            mQueryStr = queryStr;
            mGroupBy = groupBy;
            mSortBy = sortBy;
            mQueryResults = res;
        }
    }
    
    public ZimbraQueryResults getQueryResults(String queryStr, String groupBy, String sortBy) {
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
