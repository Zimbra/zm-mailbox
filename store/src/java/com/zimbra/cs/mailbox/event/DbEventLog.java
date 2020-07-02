/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.event;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbEvent;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.event.MailboxEvent.EventFilter;

public class DbEventLog extends ItemEventLog {

    private final Mailbox mbox;
    private final Collection<Integer> itemIds;
    private EventFilter filter;

    protected SimpleDateFormat mDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");

    @Override
    public int getEventCount() throws ServiceException {
        return DbEvent.getEventCount(mbox, itemIds, filter);
    }

    @Override
    public Collection<MailboxEvent> getEvents(int offset, int count) throws ServiceException {
        return DbEvent.getEvents(mbox, itemIds, offset, count, filter);
    }

    @Override
    public Collection<MailboxEvent> getEventsBefore(long timestamp, int count) throws ServiceException {
        return DbEvent.getEventsBefore(mbox, itemIds, timestamp, count, filter);
    }

    @Override
    public Collection<MailboxOperation> getLoggedOps() throws ServiceException {
        return DbEvent.getEventOps(mbox, itemIds, filter);
    }

    @Override
    public Collection<String> getLoggedUsers() throws ServiceException {
        return DbEvent.getEventUsers(mbox, itemIds, filter);
    }

    @Override
    public void addEvent(MailboxEvent event) throws ServiceException {
        LogEvent(event);
        DbEvent.logEvent(mbox, event);
    }

    public DbEventLog(MailItem item) throws ServiceException {
        super();
        mbox = item.getMailbox();
        itemIds = new HashSet<Integer>();
        itemIds.add(item.getId());
    }

    public DbEventLog(Mailbox mbox, Collection<Integer> itemIds) {
        super();
        this.mbox = mbox;
        this.itemIds = itemIds;
        this.filter = new EventFilter();
    }

    public DbEventLog(Mailbox mbox, Collection<Integer> itemIds, EventFilter filter) {
        this(mbox, itemIds);
        if (filter != null) {
            this.filter.ids.addAll(filter.ids);
            this.filter.ops.addAll(filter.ops);
            this.filter.since = filter.since;
        }
    }

    private void LogEvent(MailboxEvent event) {
        String Arguments = "";
        if (event.getArgs() != null) {
            Arguments += "[";
            for (Map.Entry<String, String> entry : event.getArgs().entrySet())
                Arguments += "{\"name\":\"" + entry.getKey() + "\"," + "\"_content\":\"" + entry.getValue() + "\"},";

            if (Arguments.endsWith(","))
                Arguments = Arguments.substring(0, Arguments.length() - 1);

            Arguments += "]";
        }

        String Message = "\"cee:{\"Event\":{\"id\":" + "\"" + event.getItemId() + "\"," + "\"time\":\""
                + mDateFormat.format(new Date(event.getTimestamp())) + "\"," + "\"action\":\"" + event.getOperation()
                + "\"," + "\"status\":\"" + "success\"," + "\"acct_id\":\"" + event.getAccountId() + "\","
                + "\"p_prod_id\":\"" + event.getUserAgent() + "\"";

        if (!Arguments.isEmpty())
            Message += ",\"args\":" + Arguments;

        Message += "}}";

        ZimbraLog.activity.info(Message);
    }
}
