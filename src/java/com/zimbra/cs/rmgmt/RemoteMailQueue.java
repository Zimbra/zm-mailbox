/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;
import org.dom4j.DocumentException;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.index.LuceneIndex;
import com.zimbra.cs.index.Z23FSDirectory;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.service.admin.GetMailQueue;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.soap.Element;

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
        id, time, size, from, to, host, addr, reason, filter, todomain, fromdomain
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
            doc.add(new Field(QueueAttr.id.toString(), id.toLowerCase(),
                    Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));

            String time = map.get(QueueAttr.time.toString());
            if (time != null && time.length() > 0) {
                long timeMillis = Long.parseLong(time) * 1000;
                doc.add(new Field(QueueAttr.time.toString(), Long.toString(timeMillis),
                        Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
            }

            addSimpleField(doc, map, QueueAttr.size);
            addSimpleField(doc, map, QueueAttr.addr);
            addSimpleField(doc, map, QueueAttr.host);
            addSimpleField(doc, map, QueueAttr.filter);
            addSimpleField(doc, map, QueueAttr.reason);

            String from = map.get(QueueAttr.from.toString());
            if (from != null && from.length() > 0) {
                addEmailAddress(doc, id, from, QueueAttr.from, QueueAttr.fromdomain);
            }

            String toWithCommas = map.get(QueueAttr.to.toString());
            if (toWithCommas != null && toWithCommas.length() > 0) {
                String[] toArray = toWithCommas.split(",");
                for (String to : toArray) {
                    addEmailAddress(doc, id, to, QueueAttr.to, QueueAttr.todomain);
                }
            }

            if (ZimbraLog.rmgmt.isDebugEnabled()) { ZimbraLog.rmgmt.debug("[scan id=" + mId + "] " +  doc); }
            mIndexWriter.addDocument(doc);
        }
    }

    void addSimpleField(Document doc, Map<String,String> map, QueueAttr attr) {
        String value = map.get(attr.toString());
        if (value != null && value.length() > 0) {
            doc.add(new Field(attr.toString(), value.toLowerCase(),
                    Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        }
    }

    void addEmailAddress(Document doc, String id, String address, QueueAttr addressAttr, QueueAttr domainAttr) {
        address = address.toLowerCase();
        doc.add(new Field(addressAttr.toString(), address,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        String[] parts = address.split("@");
        if (parts != null && parts.length > 1) {
            doc.add(new Field(domainAttr.toString(), parts[1],
                    Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        }
    }

    private class QueueHandler implements RemoteBackgroundHandler {

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
        try {
            if (ZimbraLog.rmgmt.isDebugEnabled()) {
                ZimbraLog.rmgmt.debug("clearing index (" + mIndexPath + ") for " + this);
            }
            writer = new IndexWriter(new Z23FSDirectory(mIndexPath),
                    new StandardAnalyzer(LuceneIndex.VERSION), true,
                    IndexWriter.MaxFieldLength.LIMITED);
            mNumMessages.set(0);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
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
        mIndexWriter = new IndexWriter(new Z23FSDirectory(mIndexPath),
                new StandardAnalyzer(LuceneIndex.VERSION), true,
                IndexWriter.MaxFieldLength.LIMITED);
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
        mIndexWriter = new IndexWriter(new Z23FSDirectory(mIndexPath),
                new StandardAnalyzer(LuceneIndex.VERSION), false,
                IndexWriter.MaxFieldLength.LIMITED);
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
        TermEnum terms = indexReader.terms();
        boolean hasDeletions = indexReader.hasDeletions();
        do {
            Term term = terms.term();
            if (term != null) {
                String field = term.field();
                if (field != null && field.length() > 0) {
                    QueueAttr attr = QueueAttr.valueOf(field);
                    if (attr == QueueAttr.addr ||
                        attr == QueueAttr.host ||
                        attr == QueueAttr.from ||
                        attr == QueueAttr.to ||
                        attr == QueueAttr.fromdomain ||
                        attr == QueueAttr.todomain ||
                        attr == QueueAttr.reason)
                    {
                        List<SummaryItem> list = result.sitems.get(attr);
                        if (list == null) {
                            list = new LinkedList<SummaryItem>();
                            result.sitems.put(attr, list);
                        }
                        int count = 0;
                        if (hasDeletions) {
                            TermDocs termDocs = indexReader.termDocs(term);
                            while (termDocs.next()) {
                                if (!indexReader.isDeleted(termDocs.doc())) {
                                    count++;
                                }
                            }
                        } else {
                            count = terms.docFreq();
                        }
                        if (count > 0) {
                            list.add(new SummaryItem(term.text(), count));
                        }
                    }
                }
            }
        } while (terms.next());
    }

    private Map<QueueAttr,String> docToQueueItem(Document doc) {
        Map<QueueAttr, String> qitem = new HashMap<QueueAttr,String>();
        for (QueueAttr attr : QueueAttr.values()) {
            Field[] fields = doc.getFields(attr.toString());
            if (fields != null) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (Field field : fields) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(",");
                    }
                    sb.append(field.stringValue());
                }
                if (attr ==  QueueAttr.id) {
                    qitem.put(attr, sb.toString().toUpperCase());
                } else {
                    qitem.put(attr, sb.toString());
                }
            }
        }
        return qitem;
    }

    private void list0(SearchResult result, IndexReader indexReader,
            int offset, int limit) throws IOException {
        if (ZimbraLog.rmgmt.isDebugEnabled()) {
            ZimbraLog.rmgmt.debug("listing offset=" + offset + " limit=" + limit + " " + this);
        }
        int max = indexReader.maxDoc();

        int skip = 0;
        int listed = 0;

        for (int i = 0; i < max; i++) {
            if (indexReader.isDeleted(i)) {
                continue;
            }

            if (skip < offset) {
                skip++;
                continue;
            }

            Document doc = indexReader.document(i);
            Map<QueueAttr,String> qitem = docToQueueItem(doc);
            result.qitems.add(qitem);

            listed++;
            if (listed == limit) {
                break;
            }
        }
        result.hits = getNumMessages();
    }

    private void search0(SearchResult result, IndexReader indexReader,
            Query query, int offset, int limit) throws IOException {
        if (ZimbraLog.rmgmt.isDebugEnabled()) {
            ZimbraLog.rmgmt.debug("searching query=" + query + " offset=" + offset + " limit=" + limit + " " + this);
        }
        Searcher searcher = null;
        try {
            searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, (Filter) null, limit);
            ScoreDoc[] hits = topDocs.scoreDocs;

            if (offset < hits.length) {
                int n;
                if (limit <= 0) {
                    n = hits.length;
                } else {
                    n = Math.min(offset + limit, hits.length);
                }

                for (int i = offset; i < n; i++) {
                    Document doc = searcher.doc(hits[i].doc);
                    Map<QueueAttr,String> qitem = docToQueueItem(doc);
                    result.qitems.add(qitem);
                }
            }
            result.hits = hits.length;
        } finally {
            if (searcher != null) {
                searcher.close();
            }
        }
    }

    public SearchResult search(Query query, int offset, int limit) throws ServiceException {
        SearchResult result = new SearchResult();
        IndexReader indexReader = null;
        try {
            if (!mIndexPath.exists()) {
                return result;
            }
            indexReader = IndexReader.open(new Z23FSDirectory(mIndexPath));
            summarize(result, indexReader);
            if (query == null) {
                list0(result, indexReader, offset, limit);
            } else {
                search0(result, indexReader, query, offset, limit);
            }
        } catch (Exception e) {
            throw ServiceException.FAILURE("exception occurred searching mail queue", e);
        } finally {
            if (indexReader != null) {
                try {
                    indexReader.close();
                } catch  (IOException ioe) {
                    ZimbraLog.rmgmt.warn("exception occured closing index reader from search", ioe);
                }
            }
        }
        return result;
    }

    private static final int MAX_REMOTE_EXECUTION_QUEUEIDS = 50;
    private static final int MAX_LENGTH_OF_QUEUEIDS = 12;

    public void action(Server server, QueueAction action, String[] ids) throws ServiceException {
        if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("action=" + action + " ids=" + Arrays.deepToString(ids) + " " + this);
//    	boolean firstTime = true;
        RemoteManager rm = RemoteManager.getRemoteManager(server);
        IndexReader indexReader = null;

        try {
            boolean all = false;
            if (ids.length == 1 && ids[0].equals("ALL")) {
                // Special case ALL that postsuper supports
                clearIndex();
                all = true;
            } else {
                indexReader = IndexReader.open(new Z23FSDirectory(mIndexPath));
            }

            int done = 0;
            int total = ids.length;
            while (done < total) {
                int last = Math.min(total, done + MAX_REMOTE_EXECUTION_QUEUEIDS);
                StringBuilder sb = new StringBuilder(128 + (last * MAX_LENGTH_OF_QUEUEIDS));
                sb.append("zmqaction " + action.toString() + " " + mQueueName + " ");
                int i;
                boolean first = true;
                for (i = done; i < last; i++) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(",");
                    }
                    if (!all) {
                        Term toDelete = new Term(QueueAttr.id.toString(), ids[i].toLowerCase());
                        int numDeleted = indexReader.deleteDocuments(toDelete);
                        mNumMessages.getAndAdd(-numDeleted);
                        if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("deleting term:" + toDelete + ", docs deleted=" + numDeleted);
                    }
                    sb.append(ids[i].toUpperCase());
                }
                done = last;
                //System.out.println("will execute action command: " + sb.toString());
                rm.execute(sb.toString());
            }
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("exception occurred performing queue action", ioe);
        } finally {
            if (indexReader != null) {
                try {
                    indexReader.close();
                } catch  (IOException ioe) {
                    ZimbraLog.rmgmt.warn("exception occured closing index reader during action", ioe);
                }
            }
        }
    }

    private enum TestTask { scan, search, action };

    private static void usage(String err) {
        if (err != null) {
            System.err.println("ERROR: " + err + "\n");
        }
        System.err.println("Usage: " + RemoteMailQueue.class.getName() + " scan|search|action host queue [query] [action-name queueids]");
        System.exit(1);
    }

    public static void main(String[] args) throws ServiceException, DocumentException {
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

        Server server = prov.get(ServerBy.name, host);
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
