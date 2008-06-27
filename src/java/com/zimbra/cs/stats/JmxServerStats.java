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

package com.zimbra.cs.stats;

import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.mailbox.MessageCache;

public class JmxServerStats implements JmxServerStatsMBean {

    public long getDatabaseConnectionGetsPerMinute() {
        return ZimbraPerf.STOPWATCH_DB_CONN.getPreviousCount();
    }

    public long getDatabaseConnectionGetMs() {
        return (long) ZimbraPerf.STOPWATCH_DB_CONN.getAverage();
    }

    public long getDatabaseConnectionsInUse() {
        return DbPool.getSize();
    }

    public long getLdapDirectoryContextGetMs() {
        return (long) ZimbraPerf.STOPWATCH_LDAP_DC.getAverage();
    }
    
    public long getLdapDirectoryContextGetsPerMinute() {
        return ZimbraPerf.STOPWATCH_LDAP_DC.getPreviousCount();
    }

    public long getLmtpDeliveredBytesPerMinute() {
        return ZimbraPerf.COUNTER_LMTP_DLVD_BYTES.getPreviousTotal();
    }

    public long getLmtpReceivedBytesPerMinute() {
        return ZimbraPerf.COUNTER_LMTP_RCVD_BYTES.getPreviousTotal();
    }

    public long getLmtpDeliveredMessagesPerMinute() {
        return ZimbraPerf.COUNTER_LMTP_DLVD_MSGS.getPreviousTotal();
    }

    public long getLmtpReceivedMessagesPerMinute() {
        return ZimbraPerf.COUNTER_LMTP_RCVD_MSGS.getPreviousTotal();
    }

    public long getLmtpRecipientsPerMinute() {
        return ZimbraPerf.COUNTER_LMTP_RCVD_RCPT.getPreviousTotal();
    }

    public long getImapRequestsPerMinute() {
        return ZimbraPerf.STOPWATCH_IMAP.getPreviousCount();
    }

    public long getItemCacheHitRate() {
        return (long) ZimbraPerf.COUNTER_MBOX_ITEM_CACHE.getAverage();
    }

    public long getMailboxCacheHitRate() {
        return (long) ZimbraPerf.COUNTER_MBOX_CACHE.getAverage();
    }

    public long getMailboxCacheSize() {
        return ZimbraPerf.getMailboxCacheSize();
    }

    public long getMailboxGetsPerMinute() {
        return ZimbraPerf.STOPWATCH_MBOX_GET.getPreviousCount();
    }

    public long getMailboxGetMs() {
        return (long) ZimbraPerf.STOPWATCH_MBOX_GET.getAverage();
    }

    public long getMessageAddMs() {
        return (long) ZimbraPerf.STOPWATCH_MBOX_ADD_MSG.getAverage();
    }

    public long getMessageCacheBytes() {
        return MessageCache.getDataSize();
    }
    
    public long getMessageCacheSize() {
        return MessageCache.getSize();
    }

    public long getMessageCacheHitRate() {
        return (long) ZimbraPerf.COUNTER_MBOX_MSG_CACHE.getAverage();
    }

    public long getMessagesAddedPerMinute() {
        return ZimbraPerf.STOPWATCH_MBOX_ADD_MSG.getPreviousCount();
    }

    public long getSoapRequestsPerMinute() {
        return ZimbraPerf.STOPWATCH_SOAP.getPreviousCount();
    }

    public long getSoapResponseMs() {
        return (long) ZimbraPerf.STOPWATCH_SOAP.getAverage();
    }

    public long getBlobInputStreamReadsPerMinute() {
        return ZimbraPerf.COUNTER_BLOB_INPUT_STREAM_READ.getPreviousCount();
    }

    public long getBlobInputStreamSeekRate() {
        return (long) ZimbraPerf.COUNTER_BLOB_INPUT_STREAM_SEEK_RATE.getAverage();
    }

    public long getImapResponseMs() {
        return (long) ZimbraPerf.STOPWATCH_IMAP.getAverage();
    }

    public long getPopRequestsPerMinute() {
        return ZimbraPerf.STOPWATCH_POP.getPreviousCount();
    }

    public long getPopResponseMs() {
        return (long) ZimbraPerf.STOPWATCH_POP.getAverage();
    }
}
