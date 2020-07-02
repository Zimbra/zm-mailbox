/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.rmgmt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import com.zimbra.common.account.Key;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.admin.GetMailQueue;

public class RemoteMailQueue {
    private static Map<String,RemoteMailQueue> mMailQueueCache = new HashMap<String,RemoteMailQueue>();

    public static RemoteMailQueue getRemoteMailQueue(Server server, String queueName, boolean forceScan) throws ServiceException {
        synchronized (mMailQueueCache) {
            String cacheKey = server.getId() + "-" + queueName;
            RemoteMailQueue queue;

            queue = mMailQueueCache.get(cacheKey);
            if (queue != null) {
                if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("queue cache: exists " + queue);
                if (forceScan) {
                    if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("queue cache: forcing scan " + queue);
                    queue.startScan(server, queueName);
                }
            } else {
                queue = new RemoteMailQueue(server, queueName, true);
                if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("queue cache: new object " + queue);
                mMailQueueCache.put(cacheKey, queue);
            }
            return queue;
        }
    }

    public enum QueueAttr {
        id, time, size, from, to, host, addr, reason, filter, todomain, fromdomain, received
    }

    public enum QueueAction {
        hold, release, delete, requeue
    }

    public static final int MAIL_QUEUE_INDEX_FLUSH_THRESHOLD = 1000;

    static AtomicInteger mVisitorIdCounter = new AtomicInteger(0);

    AtomicInteger mNumMessages = new AtomicInteger(0);

    public int getNumMessages() {
        return mNumMessages.get();
    }

    private class QueueItemVisitor implements RemoteResultParser.Visitor {

        final int mId;

        QueueItemVisitor() {
            mId = mVisitorIdCounter.incrementAndGet();
        }

        @Override
        public void handle(int lineNo, Map<String, String> map) throws IOException {
            if (map == null) {
                return;
            }

            if (mNumMessages.get() > 0 &&
                    ((mNumMessages.get() % MAIL_QUEUE_INDEX_FLUSH_THRESHOLD) == 0)) {
                reopenIndexWriter();
            }
            mNumMessages.incrementAndGet();

            Document doc = new Document();
            // public Field(String name, String string, boolean store, boolean index, boolean token, boolean storeTermVector) {
            String id = map.get(QueueAttr.id.toString());
            if (id == null) {
                throw new IOException("no ID defined near line=" + lineNo);
            }
            /* Note: Removed Lucene references in RemoteMailQueue */

            addSimpleField(doc, map, QueueAttr.size);
            addSimpleField(doc, map, QueueAttr.addr);
            addSimpleField(doc, map, QueueAttr.host);
            addSimpleField(doc, map, QueueAttr.filter);
            addSimpleField(doc, map, QueueAttr.reason);
            addSimpleField(doc, map, QueueAttr.received);

            String from = map.get(QueueAttr.from.toString());
            if (from != null && from.length() > 0) {
                addEmailAddress(doc, from, QueueAttr.from, QueueAttr.fromdomain);
            }

            String toWithCommas = map.get(QueueAttr.to.toString());
            if (toWithCommas != null && toWithCommas.length() > 0) {
                String[] toArray = toWithCommas.split(",");
                for (String to : toArray) {
                    addEmailAddress(doc, to, QueueAttr.to, QueueAttr.todomain);
                }
            }

            if (ZimbraLog.rmgmt.isDebugEnabled()) { ZimbraLog.rmgmt.debug("[scan id=" + mId + "] " +  doc); }
            mIndexWriter.addDocument(doc);
        }
    }

    void addSimpleField(Document doc, Map<String,String> map, QueueAttr attr) {
        /* Note: Removed Lucene references in RemoteMailQueue */
    }

    void addEmailAddress(Document doc, String address, QueueAttr addressAttr, QueueAttr domainAttr) {
        address = address.toLowerCase();
        /* Note: Removed Lucene references in RemoteMailQueue */
    }

    private class QueueHandler implements RemoteBackgroundHandler {
        @Override
        public void read(InputStream stdout, InputStream stderr) {
            try {
                mScanStartTime = System.currentTimeMillis();
                QueueItemVisitor v = new QueueItemVisitor();
                if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("starting scan with visitor id=" + v.mId + " " + mDescription);
                // This is a long running if the mail queues are backed up
                clearIndexInternal();
                openIndexWriter();
                mNumMessages.set(0);
                RemoteResultParser.parse(stdout, v);
                closeIndexWriter();
                mScanEndTime = System.currentTimeMillis();
                if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("finished scan with visitor id=" + v.mId + " total=" + mNumMessages + " " + mDescription);
                byte[] err = ByteUtil.getContent(stderr, 0);
                if (err != null && err.length > 0) {
                    ZimbraLog.rmgmt.error("error scanning " + this + ": " + new String(err));
                }
            } catch (IOException ioe) {
                error(ioe);
            } finally {
                synchronized (mScanLock) {
                    mScanInProgress = false;
                    mScanLock.notifyAll();
                }
            }
        }

        @Override
        public void error(Throwable t) {
            ZimbraLog.rmgmt.error("error when scanning mail queue " + mQueueName + " on host " + mServerName, t);
        }
    }

    Object mScanLock = new Object();
    boolean mScanInProgress;
    long mScanStartTime;
    long mScanEndTime;

    public long getScanTime() {
        synchronized (mScanLock) {
            if (mScanInProgress) {
                return System.currentTimeMillis();
            } else {
                return mScanEndTime;
            }
        }
    }

    void clearIndexInternal() throws IOException {
        IndexWriter writer = null;
        /* Note: Removed Lucene references in RemoteMailQueue */
    }

    public void clearIndex() throws ServiceException {
        synchronized (mScanLock) {
            try {
                if (mScanInProgress) {
                    throw ServiceException.TEMPORARILY_UNAVAILABLE();
                }
                clearIndexInternal();
            } catch (IOException ioe) {
                throw ServiceException.FAILURE("could not clear queue cache", ioe);
            }
        }
    }

    public void startScan(Server server, String queueName) throws ServiceException {
        synchronized (mScanLock) {
            if (mScanInProgress) {
                // One day, we should interrupt the scan.
                throw ServiceException.ALREADY_IN_PROGRESS("scan server=" + mServerName + " queue=" + mQueueName);
            }
            mScanInProgress = true;
            RemoteManager rm = RemoteManager.getRemoteManager(server);
            if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("initiating scan in background " + this);
            rm.executeBackground(RemoteCommands.ZMQSTAT + " " + queueName, new QueueHandler());
        }
    }

    public boolean waitForScan(long timeout) {
        synchronized (mScanLock) {
            if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("scan wait " + this);
            long waitTime = timeout;
            long startTime = System.currentTimeMillis();
            while (mScanInProgress) {
                try {
                    if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("scan wait time " + waitTime + "ms " + this);
                    mScanLock.wait(waitTime);
                    // Re-check condition so we can see if this was (a) a spurious wakeup, (b) timeout
                    // or (c) condition was reached.
                    if (!mScanInProgress) {
                        if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("scan wait done " + this);
                        break; // (c) condition was reached
                    }

                    long now = System.currentTimeMillis(); // Doug Lea - 1ed
                    long timeSoFar = now - startTime;
                    if (timeSoFar >= timeout) {
                        if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("scan wait timed out " + this);
                        break; // (b) timeout
                    }
                    waitTime = timeout - timeSoFar;
                    // (a) spurious wakeup
                } catch (InterruptedException ie) {
                    ZimbraLog.rmgmt.warn("interrupted while waiting for queue scan", ie);
                }
            }
            if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("scan wait returning progress=" + mScanInProgress + " " + this);
            return mScanInProgress;
        }
    }

    final String mQueueName;
    final String mServerName;
    final String mDescription;

    @Override
    public String toString() {
        return mDescription;
    }

    private RemoteMailQueue(Server server, String queueName, boolean scan) throws ServiceException {
        mServerName = server.getName();
        mQueueName = queueName;
        mDescription = "[mail-queue: server=" + mServerName + " name=" + mQueueName + " hash=" + hashCode() + "]";
        mIndexPath = new File(LC.zimbra_tmp_directory.value() + File.separator + server.getId() + "-" + queueName);
        if (scan) {
            startScan(server, queueName);
        }
    }

    IndexWriter mIndexWriter;

    private final File mIndexPath;

    void openIndexWriter() throws IOException {
        if (ZimbraLog.rmgmt.isDebugEnabled()) {
            ZimbraLog.rmgmt.debug("opening indexwriter " + this);
        }
        /* Note: Removed Lucene references in RemoteMailQueue */
    }

    void closeIndexWriter() throws IOException {
        if (ZimbraLog.rmgmt.isDebugEnabled()) {
            ZimbraLog.rmgmt.debug("closing indexwriter " + this);
        }
        mIndexWriter.close();
    }

    void reopenIndexWriter() throws IOException {
        if (ZimbraLog.rmgmt.isDebugEnabled()) {
            ZimbraLog.rmgmt.debug("reopening indexwriter " + this);
        }
        mIndexWriter.close();
        /* Note: Removed Lucene references in RemoteMailQueue */
    }

    public static final class SummaryItem implements Comparable<SummaryItem> {
        private String mTerm;
        private int mCount;

        public SummaryItem(String term, int count) {
            mTerm = term;
            mCount = count;
        }

        public String term() {
            return mTerm;
        }

        public int count() {
            return mCount;
        }

        @Override
        public int compareTo(SummaryItem other) {
            return other.mCount - mCount;
        }
    }


    public static class SearchResult {
        public Map<QueueAttr, List<SummaryItem>> sitems = new HashMap<QueueAttr,List<SummaryItem>>();
        public int hits;
        public List<Map<QueueAttr, String>> qitems = new LinkedList<Map<QueueAttr, String>>();
    }

    private void summarize(SearchResult result, IndexReader indexReader) throws IOException {
        /* Note: Removed Lucene references in RemoteMailQueue */
    }

    private Map<QueueAttr,String> docToQueueItem(Document doc) {
        Map<QueueAttr, String> qitem = new HashMap<QueueAttr,String>();
        /* Note: Removed Lucene references in RemoteMailQueue */
        return qitem;
    }

    private void list0(SearchResult result, IndexReader indexReader,
            int offset, int limit) throws IOException {
        if (ZimbraLog.rmgmt.isDebugEnabled()) {
            ZimbraLog.rmgmt.debug("listing offset=" + offset + " limit=" + limit + " " + this);
        }
        /* Note: Removed Lucene references in RemoteMailQueue */
        result.hits = getNumMessages();
    }

    private void search0(SearchResult result, IndexReader indexReader,
            Query query, int offset, int limit) throws IOException {
        if (ZimbraLog.rmgmt.isDebugEnabled()) {
            ZimbraLog.rmgmt.debug("searching query=" + query + " offset=" + offset + " limit=" + limit + " " + this);
        }
        /* Note: Removed Lucene references in RemoteMailQueue */
    }

    public SearchResult search(Query query, int offset, int limit) throws ServiceException {
        SearchResult result = new SearchResult();
        IndexReader indexReader = null;
        /* Note: Removed Lucene references in RemoteMailQueue */
        return result;
    }

    private static final int MAX_REMOTE_EXECUTION_QUEUEIDS = 50;
    private static final int MAX_LENGTH_OF_QUEUEIDS = 12;

    public void action(Server server, QueueAction action, String[] ids) throws ServiceException {
        if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("action=" + action + " ids=" + Arrays.deepToString(ids) + " " + this);
        RemoteManager rm = RemoteManager.getRemoteManager(server);
        IndexReader indexReader = null;

        /* Note: Removed Lucene references in RemoteMailQueue */
    }

    private enum TestTask { scan, search, action };

    private static void usage(String err) {
        if (err != null) {
            System.err.println("ERROR: " + err + "\n");
        }
        System.err.println("Usage: " + RemoteMailQueue.class.getName() + " scan|search|action host queue [query] [action-name queueids]");
        System.exit(1);
    }

    public static void main(String[] args) throws ServiceException {
        CliUtil.toolSetup("DEBUG");
        Provisioning prov = Provisioning.getInstance();

        if (args.length < 3) {
            usage(null);
        }

        TestTask task = TestTask.valueOf(args[0]);

        String host = args[1];
        String queueName = args[2];

        Query query = null;
        if (task == TestTask.search) {
            Element queryElement = Element.parseXML(System.in);
            query = GetMailQueue.buildLuceneQuery(queryElement);
        }

        QueueAction action = null;
        String queueIds = null;
        if (task == TestTask.action) {
            if (args.length < 5) {
                usage("not enough arguments for action");
            }
            action = QueueAction.valueOf(args[3]);
            if (action == null) {
                usage("invalid action " + args[3]);
            }
            queueIds = args[4];
        }

        Server server = prov.get(Key.ServerBy.name, host);
        RemoteMailQueue queue = new RemoteMailQueue(server, queueName, task == TestTask.scan);
        queue.waitForScan(0);

        if (task == TestTask.search) {
            SearchResult sr = queue.search(query, 0, 250);

            for (QueueAttr attr : sr.sitems.keySet()) {
                List<SummaryItem> slist = sr.sitems.get(attr);
                System.out.println("qs attr=" + attr);
                Collections.sort(slist);
                for (SummaryItem sitem : slist) {
                    System.out.println("   " + sitem.term() + "=" + sitem.count());
                }
            }

            //public List<Map<QueueAttr, String>> qitems = new LinkedList<Map<QueueAttr, String>>();
            int i = 0;
            for (Map<QueueAttr,String> qitem : sr.qitems) {
                System.out.println("qi[" + i++ + "]");
                for (QueueAttr attr : qitem.keySet()) {
                    System.out.println("   " + attr + "=" + qitem.get(attr));
                }
            }
        }

        if (task == TestTask.action) {
            queue.action(server, action, queueIds.split(","));
        }
    }
}
