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
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.service.ServiceException;
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
                queue = new RemoteMailQueue(server, queueName);
                mMailQueueCache.put(cacheKey, queue);
            }
            return queue;
        }
    }
    
    public static final String F_ID = "id";
    public static final String F_TIME = "time";
    public static final String F_SIZE = "size";
    public static final String F_FROM = "from";
    public static final String F_TO = "to";
    public static final String F_HOST = "host";
    public static final String F_ADDR = "addr";
    public static final String F_REASON = "reason";
    public static final String F_FILTER = "filter";

    /* calculated fields... */
    public static final String F_TO_DOMAIN = "todomain";
    public static final String F_FROM_DOMAIN = "fromdomain";

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
            
            String id = map.get(F_ID);
            if (id == null) {
            	throw new IOException("no ID defined near line=" + lineNo);
            }
            doc.add(new Field(F_ID, id, true, true, false, false));

            String time = map.get(F_TIME);
            if (time != null && time.length() > 0) {
                long timeMillis = Long.parseLong(time) * 1000;
                doc.add(Field.Keyword(F_TIME, DateField.timeToString(timeMillis)));
            }
            
            addSimpleField(doc, map, F_SIZE);
            addSimpleField(doc, map, F_ADDR);
            addSimpleField(doc, map, F_HOST);
            addSimpleField(doc, map, F_FILTER);
            
            String reason = map.get(F_REASON);
            if (reason != null && reason.length() > 0) {
            	doc.add(new Field(F_REASON, reason, true, true, false, true));
            }
            
            String from = map.get(F_FROM);
            if (from != null && from.length() > 0) {
            	addEmailAddress(doc, id, from, F_FROM, F_FROM_DOMAIN);
            }
            
            String toWithCommas = map.get(F_TO);
            if (toWithCommas != null && toWithCommas.length() > 0) {
            	String[] toArray = toWithCommas.split(",");
            	for (String to : toArray) {
                	addEmailAddress(doc, id, to, F_TO, F_TO_DOMAIN);
            	}
            }

            mIndexWriter.addDocument(doc);
            
            for (String s : map.keySet()) {
                System.out.println(s + "=" + map.get(s));
            }
            System.out.println();
		}
	}
	
	private void addSimpleField(Document doc, Map<String,String> map, String key) {
        String value = map.get(key);
        if (value != null && value.length() > 0) {
        	doc.add(new Field(key, value, true, true, false, false));
        }
	}
	
	private void addEmailAddress(Document doc, String id, String address, String addressField, String domainField) {
		doc.add(new Field(addressField, address, true, true, false, true));
    	String[] parts = address.split("@");
    	if (parts == null || parts.length != 2) {
    		ZimbraLog.rmgmt.warn("queue file " + id + " on " + mServerName + " " + mQueueName + " queue invalid " + addressField+ ": " + address); 
    	} else {
    		doc.add(new Field(domainField, parts[1], true, true, false, true));
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
                synchronized (mScanLock) {
                    mScanInProgress = false;
                    mScanLock.notifyAll();
                }
            } catch (IOException ioe) {
                error(ioe);
            }
        }

        public void error(Throwable t) {
            // TODO
        }
    }

    private Object mScanLock = new Object();
    
    private boolean mScanInProgress;
    
    private long mScanStartTime;
    
    private long mScanEndTime;
    
    public void startScan(Server server, String queueName) throws ServiceException {
    	try {
    		synchronized (mScanLock) {
    			if (mScanInProgress) {
    				// One day, we should interrupt the scan.
    				throw ServiceException.FAILURE("scan already in progress and can not be interrupted", null);
    			}
    			mScanInProgress = true;
    			
    			if (mIndexPath.exists()) {
    				if (!mIndexPath.isDirectory()) {
    					throw new IOException("directory for mail queue cache index is a file: " + mIndexPath);
    				}
    				if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("clearing index directory");
    				File[] files = mIndexPath.listFiles();
    				for (File f : files) {
    					if (!f.isDirectory()) {
    						if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("deleting file: " + f);
    						f.delete();
    					}
    				}
    			}
    			
    			RemoteManager rm = RemoteManager.getRemoteManager(server);
    			rm.executeBackground(RemoteCommands.ZMQSTAT + " " + queueName, new QueueHandler());
    		}
    	} catch (IOException ioe) {
    		
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
	
    public RemoteMailQueue(Server server, String queueName) throws ServiceException {
    	mServerName = server.getName();
    	mQueueName = queueName;
        mIndexPath = new File(LC.zimbra_tmp_directory.value() + File.separator + server.getId() + "-" + queueName);
        startScan(server, queueName);
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
    	mIndexWriter.close();
    }
    
    public static void main(String[] args) throws ServiceException {
        Zimbra.toolSetup("DEBUG");
        Provisioning prov = Provisioning.getInstance();
        Server remote = prov.getServerByName(args[0]);
        RemoteMailQueue queue = RemoteMailQueue.getRemoteMailQueue(remote, args[1], false);
        queue.waitForScan(0);
    }
}
