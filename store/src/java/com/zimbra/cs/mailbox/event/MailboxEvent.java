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

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.zimbra.common.util.BEncoding;
import com.zimbra.common.util.BEncoding.BEncodingException;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.service.util.ItemId;

public class MailboxEvent implements Comparable<MailboxEvent> {

    public static class EventFilter {
        public ArrayList<String> ids;
        public ArrayList<MailboxOperation> ops;
        public long since;
        public EventFilter() {
            ids = new ArrayList<String>();
            ops = new ArrayList<MailboxOperation>();
        }
        public EventFilter(String idsStr, String opsStr) {
            this();
            if (idsStr != null) {
                for (String id : idsStr.split(",")) {
                    ids.add(id.trim());
                }
            }
            if (opsStr != null) {
                for (String op : opsStr.split(",")) {
                    try {
                        ops.add(MailboxOperation.valueOf(op.trim()));
                    } catch (IllegalArgumentException e) {
                        // client is sending invalid op
                    }
                }
            }
        }
    }

    private final String accountId;
    private final MailboxOperation op;
    private final String userAgent;
    private String ownerId;
    private final int itemId;
    private final int version;
    private final int folderId;
    private final long timestamp;
    private Map<String,String> args;

    public MailboxEvent(String accountId, MailboxOperation op, int itemId, int version, int folderId, long timestamp, String userAgent, String encodedArgs) {
        this(accountId, op, null, itemId, version, folderId, timestamp, userAgent, encodedArgs);
    }

    public MailboxEvent(String accountId, MailboxOperation op, String ownerId, int itemId, int version, int folderId, long timestamp, String userAgent, String encodedArgs) {
        this.accountId = accountId;
        this.op = op;
        this.ownerId = ownerId;
        this.itemId = itemId;
        this.version = version;
        this.folderId = folderId;
        this.timestamp = timestamp;
        this.userAgent = userAgent;
        try {
            this.args = BEncoding.decode(encodedArgs);
        } catch (BEncodingException e) {
        }
    }
    public MailboxEvent(String accountId, MailboxOperation op, int itemId, int version, int folderId, long timestamp, String userAgent, Map<String,String> args) {
        this.accountId = accountId;
        this.op = op;
        this.itemId = itemId;
        this.version = version;
        this.folderId = folderId;
        this.timestamp = timestamp;
        this.userAgent = userAgent;
        this.args = args;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public MailboxOperation getOperation() {
        return op;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public ItemId getItemId() {
        return new ItemId(ownerId, itemId);
    }

    public int getVersion() {
        return version;
    }

    public int getFolderId() {
        return folderId;
    }

    public Map<String,String> getArgs() {
        return args;
    }

    public String getArgString() {
        return BEncoding.encode(args);
    }

    @Override
    public String toString() {
        ToStringBuilder sb =  new ToStringBuilder(this)
                .append(accountId)
                .append(op)
                .append(itemId)
                .append(userAgent);
        if (args != null)
            sb.append(args);
        sb.append(timestamp);
        return sb.toString();
    }

    @Override
    public int compareTo(MailboxEvent that) {
        // sort descending order from latest to oldest
        int diff = (int)(that.timestamp - this.timestamp);
        if (diff != 0) {
            return diff;
        } else if (this.itemId != that.itemId) {
            return that.itemId - this.itemId;
        }
        return toString().compareTo(that.toString());
    }
}
