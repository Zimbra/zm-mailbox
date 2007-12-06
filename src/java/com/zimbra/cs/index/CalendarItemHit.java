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
    
    /**
     * @param results
     * @param mbx
     * @param id
     * @param score
     */
    public CalendarItemHit(ZimbraQueryResultsImpl results, Mailbox mbx, int id,
        Document d, float score, CalendarItem calItem, byte type) throws ServiceException {
        super(results, mbx, score);

        mId = id;
        mCalItem = calItem;
        mType = type;
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
    
    public long getSize() throws ServiceException {
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
