/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * @since Feb 15, 2005
 */
public class CalendarItemHit extends ZimbraHit {

    protected int id;
    protected CalendarItem item;

    CalendarItemHit(ZimbraQueryResultsImpl results, Mailbox mbx, int id, CalendarItem cal) {
        super(results, mbx);
        this.id = id;
        item = cal;
    }

    @Override
    public MailItem getMailItem() throws ServiceException {
        return getCalendarItem();
    }

    public CalendarItem getCalendarItem() throws ServiceException {
        if (item == null) {
            item = getMailbox().getCalendarItemById(null, id);
        }
        return item;
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
        return id;
    }

    @Override
    void setItem(MailItem value) {
        item = (CalendarItem) value;
    }

    @Override
    boolean itemIsLoaded() {
        return (id == 0) || (item != null);
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
    public String getRecipients() throws ServiceException {
        if (cachedRecipients == null) {
            cachedRecipients = Strings.nullToEmpty(getCalendarItem().getSortRecipients());
        }
        return cachedRecipients;
    }

    @Override
    public String toString() {
        try {
            return Objects.toStringHelper(this).add("name", getName()).add("subject", getSubject()).toString();
        } catch (Exception e) {
            return e.toString();
        }
    }
}
