/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.zimbra.common.stats.Accumulator;
import com.zimbra.common.stats.DeltaCalculator;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.mailbox.MessageCache;

public class JmxServerStats implements JmxServerStatsMBean {

    private DeltaCalculator mDbConn = new DeltaCalculator(ZimbraPerf.STOPWATCH_DB_CONN);
    private DeltaCalculator mLdapConn = new DeltaCalculator(ZimbraPerf.STOPWATCH_LDAP_DC);
    private DeltaCalculator mItemCache = new DeltaCalculator(ZimbraPerf.COUNTER_MBOX_ITEM_CACHE);
    private DeltaCalculator mMailboxCache = new DeltaCalculator(ZimbraPerf.COUNTER_MBOX_CACHE);
    private DeltaCalculator mMessageCache = new DeltaCalculator(ZimbraPerf.COUNTER_MBOX_MSG_CACHE);
    
    private DeltaCalculator mAddMessage = new DeltaCalculator(ZimbraPerf.STOPWATCH_MBOX_ADD_MSG);
    private DeltaCalculator mImap = new DeltaCalculator(ZimbraPerf.STOPWATCH_IMAP);
    private DeltaCalculator mPop = new DeltaCalculator(ZimbraPerf.STOPWATCH_POP);
    private DeltaCalculator mSoap = new DeltaCalculator(ZimbraPerf.STOPWATCH_SOAP);
    private DeltaCalculator mBisSeek = new DeltaCalculator(ZimbraPerf.COUNTER_BLOB_INPUT_STREAM_SEEK_RATE); 
    
    private final List<Accumulator> mAccumulators;
    
    JmxServerStats() {
        List<Accumulator> accumulators = new ArrayList<Accumulator>();
        
        accumulators.add(mDbConn);
        accumulators.add(mLdapConn);
        accumulators.add(mItemCache);
        accumulators.add(mMailboxCache);
        accumulators.add(mMessageCache);
        
        accumulators.add(mAddMessage);
        accumulators.add(mImap);
        accumulators.add(mPop);
        accumulators.add(mSoap);
        accumulators.add(mBisSeek);
        
        mAccumulators = Collections.unmodifiableList(accumulators);
    }
    
    public long getDatabaseConnectionGets() {
        return ZimbraPerf.STOPWATCH_DB_CONN.getCount();
    }

    public long getDatabaseConnectionGetMs() {
        return (long) mDbConn.getRealtimeAverage();
    }

    public long getDatabaseConnectionsInUse() {
        return DbPool.getSize();
    }

    public long getLdapDirectoryContextGetMs() {
        return (long) mLdapConn.getRealtimeAverage();
    }
    
    public long getLdapDirectoryContextGets() {
        return ZimbraPerf.STOPWATCH_LDAP_DC.getCount();
    }

    public long getLmtpDeliveredBytes() {
        return ZimbraPerf.COUNTER_LMTP_DLVD_BYTES.getTotal();
    }

    public long getLmtpReceivedBytes() {
        return ZimbraPerf.COUNTER_LMTP_RCVD_BYTES.getTotal();
    }

    public long getLmtpDeliveredMessages() {
        return ZimbraPerf.COUNTER_LMTP_DLVD_MSGS.getTotal();
    }

    public long getLmtpReceivedMessages() {
        return ZimbraPerf.COUNTER_LMTP_RCVD_MSGS.getTotal();
    }

    public long getLmtpRecipients() {
        return ZimbraPerf.COUNTER_LMTP_RCVD_RCPT.getTotal();
    }

    public long getImapRequests() {
        return ZimbraPerf.STOPWATCH_IMAP.getCount();
    }

    public long getItemCacheHitRate() {
        return (long) mItemCache.getRealtimeAverage();
    }

    public long getMailboxCacheHitRate() {
        return (long) mMailboxCache.getRealtimeAverage();
    }

    public long getMailboxCacheSize() {
        return ZimbraPerf.getMailboxCacheSize();
    }

    public long getMailboxGets() {
        return ZimbraPerf.STOPWATCH_MBOX_GET.getCount();
    }

    public long getMailboxGetMs() {
        return (long) mMailboxCache.getRealtimeAverage();
    }

    public long getMessageAddMs() {
        return (long) mAddMessage.getRealtimeAverage();
    }

    public long getMessageCacheSize() {
        return MessageCache.getSize();
    }

    public long getMessageCacheHitRate() {
        return (long) mMessageCache.getRealtimeAverage();
    }

    public long getMessagesAdded() {
        return ZimbraPerf.STOPWATCH_MBOX_ADD_MSG.getCount();
    }

    public long getSoapRequests() {
        return ZimbraPerf.STOPWATCH_SOAP.getCount();
    }

    public long getSoapResponseMs() {
        return (long) mSoap.getRealtimeAverage();
    }

    public long getBlobInputStreamReads() {
        return ZimbraPerf.COUNTER_BLOB_INPUT_STREAM_READ.getCount();
    }

    public long getBlobInputStreamSeekRate() {
        return (long) mBisSeek.getRealtimeAverage();
    }

    public long getImapResponseMs() {
        return (long) mImap.getRealtimeAverage();
    }

    public long getPopRequests() {
        return ZimbraPerf.STOPWATCH_POP.getCount();
    }

    public long getPopResponseMs() {
        return (long) mPop.getRealtimeAverage();
    }
    
    public void reset() {
        for (Accumulator accum : mAccumulators) {
            accum.reset();
        }
    }
}
