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
    
	public static final String MQ_ID = "id";         // id=A4578828AC1
	public static final String MQ_TIME = "time"; 	 // time=1142475151
	public static final String MQ_SIZE = "size"; 	 // size=550
	public static final String MQ_SENDER = "sender"; //	sender=anandp@phillip.liquidsys.com
	public static final String MQ_CADDR = "caddr"; 	 // caddr=10.10.130.27
	public static final String MQ_CNAME = "cname"; 	 // cname=phillip.liquidsys.com
	public static final String MQ_REASON = "reason"; // reason=connect to 127.0.0.1[127.0.0.1]: Connection refused
	public static final String MQ_RECIP = "recip"; 	 // recip=admin@bolton.liquidsys.com,anandp@bolton.liquidsys.com

	private String mQueueName;
	
	private boolean mParseCompleted = false;
	
	private class QueueItemVisitor implements RemoteResultParser.Visitor {
		public void handle(int lineNo, Map<String, String> map) throws IOException {
            if (map == null) {
                return;
            }
			String id = map.get(MQ_ID);
            if (id == null) {
                throw new IOException("Found no " + MQ_ID + " attribute in input after line " + lineNo);
			}
            
            for (String s : map.keySet()) {
                System.out.println(s + "=" + map.get(s));
            }
            System.out.println();
		}
	}
	
    private class QueueHandler implements RemoteBackgroundHandler {

        private int mNumMessages = 0;
        
        private IndexWriter mIndexWriter;
        
        public QueueHandler(Server server, String queueName) throws IOException {
            File indexPath = new File(LC.zimbra_tmp_directory.value() + File.separator + server.getId() + "-" + queueName);
            if (indexPath.exists()) {
                if (!indexPath.isDirectory()) {
                    throw new IOException("directory for mail queue cache index is a file: " + indexPath);
                }
                File[] files = indexPath.listFiles();
            }
            
            
            // TODO - what if index is corrupt...
            mIndexWriter = new IndexWriter(indexPath, new StandardAnalyzer(), true);
            
        }
        
        public void read(InputStream stdout, InputStream stderr) {
            try {
                mScanStartTime = System.currentTimeMillis();
                QueueItemVisitor v = new QueueItemVisitor();
                RemoteResultParser.parse(stdout, v);
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
        synchronized (mScanLock) {
            try {
                if (mScanInProgress) {
                    // One day, we should interrupt the scan.
                    throw ServiceException.FAILURE("scan already in progress and can not be interrupted", null);
                }
                mScanInProgress = true;
                RemoteManager rm = RemoteManager.getRemoteManager(server);
                rm.executeBackground(RemoteCommands.ZMQSTAT + " " + queueName, new QueueHandler(server, queueName));
            } catch (IOException ioe) {
                throw ServiceException.FAILURE("exception initiating queue scan", ioe);
            }
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
    
    public RemoteMailQueue(Server server, String queueName) throws ServiceException {
        startScan(server, queueName);
	}
    
    public static void main(String[] args) throws ServiceException {
        Zimbra.toolSetup("DEBUG");
        Provisioning prov = Provisioning.getInstance();
        Server remote = prov.getServerByName(args[0]);
        RemoteMailQueue queue = RemoteMailQueue.getRemoteMailQueue(remote, args[1], false);
        queue.waitForScan(0);
    }
}
