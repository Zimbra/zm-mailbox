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

package com.zimbra.cs.index;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * @since Feb 15, 2005
 */
public class CalendarItemHit extends ZimbraHit {

    protected int mId;
    protected CalendarItem mCalItem;
    private byte mType = MailItem.TYPE_UNKNOWN;

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
            mCalItem = (CalendarItem) mbx.toItem(ud);
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
        float score, CalendarItem calItem, byte type) {
        super(results, mbx, score);

        mId = id;
        mCalItem = calItem;
        mType = type;
    }

    @Override
    public MailItem getMailItem() throws ServiceException {
        return getCalendarItem();
    }

    public CalendarItem getCalendarItem() throws ServiceException {
        if (mCalItem == null) {
            mCalItem = this.getMailbox().getCalendarItemById(null, mId);
        }
        return mCalItem;
    }

    @Override
    public long getDate() throws ServiceException {
        return getCalendarItem().getDate();
    }

    @Override
    public long getSize() throws ServiceException {
        return getCalendarItem().getSize();
    }

    @Override
    public int getConversationId() {
        assert(false);
        return 0;
    }

    @Override
    public int getItemId() {
        return mId;
    }

    public byte getItemType() {
        return mType;
    }

    @Override
    void setItem(MailItem item) {
        mCalItem = (CalendarItem)item;
        if (mCalItem != null) {
            mType = mCalItem.getType();
        } else {
            mType = MailItem.TYPE_UNKNOWN;
        }
    }

    @Override
    boolean itemIsLoaded() {
        return (mId == 0) || (mCalItem != null);
    }

    @Override
    public String getSubject() throws ServiceException {
        return getCalendarItem().getSubject();
    }

    @Override
    public String getName() throws ServiceException {
        return getCalendarItem().getSubject();
    }

    @Override
    public String toString() {
        String name= "";
        String subject= "";
        try {
            name = getName();
        } catch(Exception e) {
        }
        try {
            subject=getSubject();
        } catch(Exception e) {
        }
        return "CalendarItem: " + super.toString() + " " + name + " " + subject;
    }
}
