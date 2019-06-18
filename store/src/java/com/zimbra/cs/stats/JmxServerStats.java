/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

    private DeltaCalculator mStoreCopy = new DeltaCalculator(ZimbraPerf.STOPWATCH_STORE_COPY);
    private DeltaCalculator mStoreDel = new DeltaCalculator(ZimbraPerf.STOPWATCH_STORE_DEL);
    private DeltaCalculator mStoreGet = new DeltaCalculator(ZimbraPerf.STOPWATCH_STORE_GET);
    private DeltaCalculator mStoreLink = new DeltaCalculator(ZimbraPerf.STOPWATCH_STORE_LINK);
    private DeltaCalculator mStorePut = new DeltaCalculator(ZimbraPerf.STOPWATCH_STORE_PUT);
    private DeltaCalculator mStoreStage = new DeltaCalculator(ZimbraPerf.STOPWATCH_STORE_STAGE);
    
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
        accumulators.add(mStoreCopy);
        accumulators.add(mStoreDel);
        accumulators.add(mStoreGet);
        accumulators.add(mStoreLink);
        accumulators.add(mStorePut);
        accumulators.add(mStoreStage);

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

    public long getStoreCopys() { return (long) ZimbraPerf.COUNTER_STORE_COPY.getTotal(); }

    public long getStoreDeletes() { return (long) ZimbraPerf.COUNTER_STORE_DEL.getTotal(); }

    public long getStoreGets() { return (long) ZimbraPerf.COUNTER_STORE_GET.getTotal(); }

    public long getStoreLinks() { return (long) ZimbraPerf.COUNTER_STORE_LINK.getTotal(); }

    public long getStorePuts() { return (long) ZimbraPerf.COUNTER_STORE_PUT.getTotal(); }

    public long getStoreStages() { return (long) ZimbraPerf.COUNTER_STORE_STAGE.getTotal(); }

    public long getStoreCopyMs() { return (long) mStoreCopy.getRealtimeAverage(); }

    public long getStoreDeleteMs() { return (long) mStoreDel.getRealtimeAverage(); }

    public long getStoreGetMs() { return (long) mStoreGet.getRealtimeAverage(); }

    public long getStoreLinkMs() { return (long) mStoreLink.getRealtimeAverage(); }

    public long getStorePutMs() { return (long) mStorePut.getRealtimeAverage(); }

    public long getStoreStageMs() { return (long) mStoreStage.getRealtimeAverage(); }

    public void reset() {
        for (Accumulator accum : mAccumulators) {
            accum.reset();
        }
    }
}
