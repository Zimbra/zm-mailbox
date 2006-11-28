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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Feb 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.index;

import org.apache.lucene.document.Document;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author tim
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CalendarItemHit extends ZimbraHit {
    
    protected int mId;
    protected CalendarItem mCalItem;
    private byte mType = MailItem.TYPE_UNKNOWN;

    /**
     * @param results
     * @param mbx
     * @param d
     * @param score
     */
    public CalendarItemHit(ZimbraQueryResultsImpl results, Mailbox mbx, 
            int mailItemId, Document d, float score, MailItem.UnderlyingData ud) throws ServiceException {
        super(results, mbx, score);
        mId = mailItemId;
        if (ud != null) {
            mCalItem = (CalendarItem)mbx.getItemFromUnderlyingData(ud);
            mType = ud.type;
        }
    }
    
    /**
     * @param results
     * @param mbx
     * @param id
     * @param score
     */
    public CalendarItemHit(ZimbraQueryResultsImpl results, Mailbox mbx, int id,
            float score, MailItem.UnderlyingData ud) throws ServiceException {
        super(results, mbx, score);

        mId = id;
        if (ud != null) {
            mCalItem = (CalendarItem)mbx.getItemFromUnderlyingData(ud);
            mType = ud.type;
        }
    }
    
    public MailItem getMailItem() throws ServiceException { return getCalendarItem(); }
    
    public CalendarItem getCalendarItem() throws ServiceException {
        if (mCalItem == null) {
            mCalItem = this.getMailbox().getCalendarItemById(null, mId);
        }
        return mCalItem;
    }

    public long getDate() throws ServiceException {
        return getCalendarItem().getDate();
    }
    
    public int getSize() throws ServiceException {
        return getCalendarItem().getSize();
    }
    
    public int getConversationId() {
        assert(false);
        return 0;
    }
    
    public int getItemId() {
        return mId;
    }
    
    public byte getItemType() {
        return mType;
    }

    void setItem(MailItem item) {
        mCalItem = (CalendarItem)item;
        if (mCalItem != null)
            mType = mCalItem.getType();
        else
            mType = MailItem.TYPE_UNKNOWN;
    }
    
    boolean itemIsLoaded() {
        return (mId == 0) || (mCalItem != null);
    }
    
    public String getSubject() throws ServiceException {
        return getCalendarItem().getSubject();
    }
    
    public String getName() throws ServiceException {
        return getCalendarItem().getSubject();
    }
}
