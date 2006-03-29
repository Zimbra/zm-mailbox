/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.rmgmt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;

public class RemoteMailQueue {

    private static Map<String,RemoteMailQueue> mMailQueueCache = new HashMap<String,RemoteMailQueue>();
    
    public static RemoteMailQueue getRemoteMailQueue(Server server, String queueName, boolean forceScan) throws ServiceException {
        synchronized (mMailQueueCache) {
            String cacheKey = server.getId() + "-" + queueName;
            RemoteMailQueue queue;
            
            queue = mMailQueueCache.get(cacheKey);
            if (queue != null) {
                if (forceScan) {
                    queue.startScan(server, queueName);
                }
            } else {
                queue = new RemoteMailQueue(server, queueName, true);
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
        
    private class QueueItemVisitor implements RemoteResultParser.Visitor {
        public void handle(int lineNo, Map<String, String> map) throws IOException {
            if (map == null) {
                return;
            }
                        
            if ((mNumMessages % MAIL_QUEUE_INDEX_FLUSH_THRESHOLD) == 0) {
                openIndexWriter();
            }   
            mNumMessages++;
            
            Document doc = new Document();
            // public Field(String name, String string, boolean store, boolean index, boolean token, boolean storeTermVector) {
            String id = map.get(QueueAttr.id.toString());
            if (id == null) {
                throw new IOException("no ID defined near line=" + lineNo);
            }
            doc.add(new Field(QueueAttr.id.toString(), id.toLowerCase(), true, true, false, false));

            String time = map.get(QueueAttr.time.toString());
            if (time != null && time.length() > 0) {
                long timeMillis = Long.parseLong(time) * 1000;
                doc.add(new Field(QueueAttr.time.toString(), Long.toString(timeMillis), true, true, false, false));
            }

            addSimpleField(doc, map, QueueAttr.size);
            addSimpleField(doc, map, QueueAttr.addr);
            addSimpleField(doc, map, QueueAttr.host);
            addSimpleField(doc, map, QueueAttr.filter);
            
            String reason = map.get(QueueAttr.reason.toString());
            if (reason != null && reason.length() > 0) {
                doc.add(new Field(QueueAttr.reason.toString(), reason.toLowerCase(), true, true, false, true));
            }
            
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

            if (ZimbraLog.rmgmt.isDebugEnabled()) { ZimbraLog.rmgmt.debug("adding: " + doc); }
            mIndexWriter.addDocument(doc);
        }
    }
        
    private void addSimpleField(Document doc, Map<String,String> map, QueueAttr attr) {
        String value = map.get(attr.toString());
        if (value != null && value.length() > 0) {
            doc.add(new Field(attr.toString(), value.toLowerCase(), true, true, false, false));
        }
    }
        
    private void addEmailAddress(Document doc, String id, String address, QueueAttr addressAttr, QueueAttr domainAttr) {
        address = address.toLowerCase();
        doc.add(new Field(addressAttr.toString(), address, true, true, false, false));
        String[] parts = address.split("@");
        if (parts != null && parts.length > 1) {
            doc.add(new Field(domainAttr.toString(), parts[1], true, true, false, false));
        }
    }
    
    private int mNumMessages = 0;
    
    private class QueueHandler implements RemoteBackgroundHandler {

        public void read(InputStream stdout, InputStream stderr) {
            try {
                mScanStartTime = System.currentTimeMillis();
                QueueItemVisitor v = new QueueItemVisitor();
                RemoteResultParser.parse(stdout, v);
                closeIndexWriter();
                mScanEndTime = System.currentTimeMillis();
                
                byte[] err = ByteUtil.getContent(stderr, 0);
                if (err != null && err.length > 0) {
                	ZimbraLog.rmgmt.error("error scanning mail queue " + mQueueName + " on host " + mServerName + ": " + new String(err));
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

    private Object mScanLock = new Object();
    
    private boolean mScanInProgress;
    
    private long mScanStartTime;
    
    private long mScanEndTime;

    public long getScanTime() {
        synchronized (mScanLock) {
            if (mScanInProgress) {
                return System.currentTimeMillis();
            } else {
                return mScanEndTime;
            }
        }
    }
    
    public boolean scanInProgress() {
        synchronized (mScanLock) {
            return mScanInProgress;
        }
    }
    
    public void clearIndex() throws ServiceException {
        try {
            IndexWriter writer = null;
            try {
                if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("deleting index " + mIndexPath);
                writer = new IndexWriter(mIndexPath, new StandardAnalyzer(), true);
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("exception deleting queue index", ioe);
        }
    }
    
    public void startScan(Server server, String queueName) throws ServiceException {
        synchronized (mScanLock) {
            if (mScanInProgress) {
                // One day, we should interrupt the scan.
                throw ServiceException.ALREADY_IN_PROGRESS("scan server=" + mServerName + " queue=" + mQueueName);
            }
            mScanInProgress = true;
            clearIndex();
            RemoteManager rm = RemoteManager.getRemoteManager(server);
            rm.executeBackground(RemoteCommands.ZMQSTAT + " " + queueName, new QueueHandler());
        }
    }

    public void waitForScan(long timeout) {
        synchronized (mScanLock) {
            while (mScanInProgress) {
                try {
                    mScanLock.wait(timeout);
                } catch (InterruptedException ie) {
                    ZimbraLog.rmgmt.warn("interrupted while waiting for queue scan", ie);
                }
            }
        }
    }
    
    private final String mQueueName;
        
    private final String mServerName;
        
    private RemoteMailQueue(Server server, String queueName, boolean scan) throws ServiceException {
        mServerName = server.getName();
        mQueueName = queueName;
        mIndexPath = new File(LC.zimbra_tmp_directory.value() + File.separator + server.getId() + "-" + queueName);
        if (scan) {
            if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("starting mailq queue scan on " + server.getName() + " for queue " + queueName);
            startScan(server, queueName);
        }
    }

    private IndexWriter mIndexWriter;
    
    private final File mIndexPath;

    private void openIndexWriter() throws IOException {
        if (mIndexWriter != null) {
            closeIndexWriter();
        }
        mIndexWriter = new IndexWriter(mIndexPath, new StandardAnalyzer(), true);
    }
    
    private void closeIndexWriter() throws IOException {
    	if (mIndexWriter != null) {
        	mIndexWriter.close();
        }
    }
    
    public static final class SummaryItem implements Comparable {
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
       
        public int compareTo(Object o) {
            SummaryItem other = (SummaryItem)o;
            return other.mCount - mCount;
        }
    }

  
    public static class SearchResult {
        public Map<QueueAttr, List<SummaryItem>> sitems = new HashMap<QueueAttr,List<SummaryItem>>(); 
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
                        list.add(new SummaryItem(term.text(), terms.docFreq()));
                    }
                    //System.out.println(term + "[" + terms.docFreq() + "]");
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

    private void list0(SearchResult result, IndexReader indexReader, int offset, int limit) throws IOException {
    	int num = indexReader.numDocs();
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
            if (listed >= limit) {
            	break;
            }
    	}
    }
    
    private void search0(SearchResult result, IndexReader indexReader, String queryText, int offset, int limit) throws ParseException, IOException {
        Searcher searcher = null;
        try {
            searcher = new IndexSearcher(indexReader);
            Analyzer analyzer = new StandardAnalyzer();
            Query query = QueryParser.parse(queryText, "id", analyzer);
            Hits hits = searcher.search(query);
            
            if (offset < hits.length()) {
                int n;
                if (limit <= 0) {
                    n = hits.length();
                } else {
                    n = Math.min(offset + limit, hits.length());
                }
                
                for (int i = offset; i < n; i++) {
                    Document doc = hits.doc(i);
                    Map<QueueAttr,String> qitem = docToQueueItem(doc);
                    result.qitems.add(qitem);
                }
            }
        } finally {
            if (searcher != null) {
                searcher.close();
            }
        }
    }
    
    public SearchResult search(String queryText, int offset, int limit) throws ServiceException {
        SearchResult result = new SearchResult();
        IndexReader indexReader = null;
        try {
        	if (!mIndexPath.exists()) {
        		return result;
        	}
            indexReader = IndexReader.open(mIndexPath);
            summarize(result, indexReader);
            if (queryText == null || queryText.length() == 0) {
            	list0(result, indexReader, offset, limit);
            } else {
            	search0(result, indexReader, queryText, offset, limit);
            }
        } catch (Exception e) {
            throw ServiceException.FAILURE("exception occurred searching mail queue", e);
        } finally {
            if (indexReader != null) {
                try {
                    indexReader.close();
                } catch  (IOException ioe) {
                    throw ServiceException.FAILURE("exception occured closing index reader", ioe);
                }
            }
        }
        return result;
    }
    
    private static final int MAX_REMOTE_EXECUTION_QUEUEIDS = 50;
    private static final int MAX_LENGTH_OF_QUEUEIDS = 12;

    public void action(Server server, QueueAction action, String[] ids) throws ServiceException {
    	int done = 0;
    	int total = ids.length;
    	boolean firstTime = true;
    	RemoteManager rm = RemoteManager.getRemoteManager(server);
    	IndexReader indexReader = null;
    	try {
    		indexReader = IndexReader.open(mIndexPath);
    		while (done < total) {
    			int numQueueIds = Math.min(total - done, MAX_REMOTE_EXECUTION_QUEUEIDS);
    			StringBuilder sb = new StringBuilder(128 + (numQueueIds * MAX_LENGTH_OF_QUEUEIDS));
    			sb.append("zmqaction " + action.toString() + " " + mQueueName + " "); 
    			int i;
    			for (i = 0; i < numQueueIds; i++) {
    				if (i > 0) {
    					sb.append(",");
    				}
    	            Term toDelete = new Term(QueueAttr.id.toString(), ids[i].toLowerCase());
    	            int numDeleted = indexReader.delete(toDelete);
    	            if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("deleting term:" + toDelete + ", docs deleted=" + numDeleted);
    				sb.append(ids[done + i].toUpperCase());
    			}
    			done = i;
    			//System.out.println("will execute action command: " + sb.toString());
    			RemoteResult rr = rm.execute(sb.toString());
    		}
    	} catch (IOException ioe) {
    		throw ServiceException.FAILURE("exception occurred performing queue action", ioe);
    	} finally {
    		if (indexReader != null) {
    			try {
    				indexReader.close();
    			} catch  (IOException ioe) {
    				throw ServiceException.FAILURE("exception occured closing index reader", ioe);
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

    public static void main(String[] args) throws ServiceException {
        Zimbra.toolSetup("DEBUG");
        Provisioning prov = Provisioning.getInstance();

        if (args.length < 3) {
        	usage(null);
        }
        
        TestTask task = TestTask.valueOf(args[0]);
        
        String host = args[1];
        String queueName = args[2];

        String query = null;
        if (task == TestTask.search) {
        	if (args.length < 4) {
        		usage("no query specified");
        	}
        	query = args[3];
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
        
        Server server = prov.getServerByName(host);
        RemoteMailQueue queue = new RemoteMailQueue(server, queueName, task == TestTask.scan);
        queue.waitForScan(0);

        if (task == TestTask.search) {
            SearchResult sr = queue.search(query, 0, 30);
            
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
