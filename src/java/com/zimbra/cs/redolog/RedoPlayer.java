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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 7. 22.
 */
package com.zimbra.cs.redolog;

import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.redolog.logger.FileLogReader;
import com.zimbra.cs.redolog.op.AbortTxn;
import com.zimbra.cs.redolog.op.Checkpoint;
import com.zimbra.cs.redolog.op.CommitTxn;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.redolog.op.StoreIncomingBlob;
import com.zimbra.cs.service.ServiceException;

/**
 * @author jhahm
 */
public class RedoPlayer {

    private static Log mLog = LogFactory.getLog(RedoPlayer.class);

    private static final int INITIAL_MAP_SIZE = 1000;

    // Use a separate guard object to synchronize access to mOpsMap.
    // Don't synchronize on mOpsMap itself because it can get reassigned.
    private LinkedHashMap mOpsMap;  // LinkedHashMap to ensure iteration order == insertion order
    private final Object mOpsMapGuard = new Object();

    private boolean mWriteable;

    public RedoPlayer(boolean writeable) {
		mOpsMap = new LinkedHashMap(INITIAL_MAP_SIZE);
        mWriteable = writeable;
    }

    public void shutdown() {
        mOpsMap.clear();
    }

    /**
	 * Scans a redo log file.  An op that is neither committed nor aborted is
	 * added to mOpsMap.  These are the ops that need to be reattempted during
	 * crash recovery.  If redoCommitted is true, an op is reattempted as soon
	 * as its COMMIT entry is encountered.  This case is for replaying the logs
	 * during mailbox restore.
	 * @param logfile
	 * @param redoCommitted
     * @param mailboxIds[] - if set, then restrict the replay of log entries
     *                       to ones which match Mailbox IDs 
	 * @throws IOException
	 */
	public void scanLog(File logfile, boolean redoCommitted, int mailboxIds[], long startingTime)
    throws IOException, ServiceException {
		FileLogReader logReader = new FileLogReader(logfile, mWriteable);
		logReader.open();
		long lastPosition = 0;

		// Read all ops in redo log, discarding those with commit/abort entries.
		try {
			RedoableOp op = null;
			while ((op = logReader.getNextOp()) != null) {
				lastPosition = logReader.position();

				if (mLog.isDebugEnabled())
					mLog.debug("Read: " + op);

                playOp(op, redoCommitted, mailboxIds, startingTime);
			}
		} catch (IOException e) {
			// The IOException could be a real I/O problem or it could mean
			// there was a server crash previously and there were half-written
			// log entries.  We can't really tell which case it is, so just
			// assume the second case and truncate the file after the last
			// successfully read item.

            mLog.warn("IOException while reading redolog file", e);

			long size = logReader.getSize();
			if (lastPosition < size) {
				long diff = size - lastPosition;
                String msg =
                    "There were " + diff +
                    " bytes of junk data at the end of " +
                    logfile.getAbsolutePath() +
                    ".";
                if (mWriteable) {
                    mLog.warn(msg + "  File will be truncated to " +
                              lastPosition + " bytes");
                    logReader.truncate(lastPosition);
                } else
                    mLog.warn(msg);
			}
		} finally {
			logReader.close();
		}
	}

    // used to detect/track if a commit/abort record is played back
    // before its change record
    private boolean mHasOrphanOps = false;
    private Map mOrphanOps = new HashMap();

    public void playOp(RedoableOp op, boolean redoCommitted, int mailboxIds[], long startingTime)
    throws ServiceException {

        if (op.isStartMarker()) {
            synchronized (mOpsMapGuard) {
                mOpsMap.put(op.getTransactionId(), op);
                if (mHasOrphanOps) {
                    RedoableOp x = (RedoableOp) mOrphanOps.remove(op.getTransactionId());
                    if (x != null)
                        mLog.error("Detected out-of-order insertion of change record for orphans commit/abort: change=" + op + ", orphan=" + x);
                }
            }
        } else {

            // When a checkpoint is encountered, discard all ops except
            // those listed in the checkpoint.
            if (op instanceof Checkpoint) {
                Checkpoint ckpt = (Checkpoint) op;
                Set txns = ckpt.getActiveTxns();
                if (txns.size() > 0) {
                    synchronized (mOpsMapGuard) {
                        if (mOpsMap.size() != txns.size()) {
                            // Unexpected discrepancy
                            if (mLog.isDebugEnabled()) {
                                StringBuffer sb1 = new StringBuffer("Current Uncommitted Ops: ");
                                StringBuffer sb2 = new StringBuffer("Checkpoint Uncommitted Ops: ");
                                int i = 0;
                                for (Iterator it = mOpsMap.keySet().iterator(); it.hasNext(); i++) {
                                	TransactionId id = (TransactionId) it.next();
                                    if (i > 0)
                                        sb1.append(", ");
                                    sb1.append(id);
                                }
                                i = 0;
                                for (Iterator it = txns.iterator(); it.hasNext(); i++) {
                                    TransactionId id = (TransactionId) it.next();
                                    if (i > 0)
                                        sb2.append(", ");
                                    sb2.append(id);
                                }
                                mLog.info("Checkpoint discrepancy: # current uncommitted ops = " + mOpsMap.size() +
                                          ", # checkpoint uncommitted ops = " + txns.size() +
                                          "\nMAP DUMP:\n" + sb1.toString() + "\n" + sb2.toString());
                            }
                        }
                    }
                } else {
                    synchronized (mOpsMapGuard) {
                        if (mOpsMap.size() != 0) {
                            // Unexpected discrepancy
                            if (mLog.isDebugEnabled()) {
                                StringBuffer sb1 = new StringBuffer("Current Uncommitted Ops: ");
                                int i = 0;
                                for (Iterator it = mOpsMap.keySet().iterator(); it.hasNext(); i++) {
                                    TransactionId id = (TransactionId) it.next();
                                    if (i > 0)
                                        sb1.append(", ");
                                    sb1.append(id);
                                }
                                mLog.info("Checkpoint discrepancy: # current uncommitted ops = " +
                                          mOpsMap.size() + " instead of 0\nMAP DUMP:\n" +
                                          sb1.toString());
                            }
                        }
                    }
                }
            } else if (op.isEndMarker()) {
                // Encountered COMMIT or ABORT.  Discard the
                // corresponding op from map, and optionally execute the committed op.
                RedoableOp prepareOp;
                synchronized (mOpsMapGuard) {
                    prepareOp = (RedoableOp) mOpsMap.remove(op.getTransactionId());
                    if (prepareOp == null) {
                        mHasOrphanOps = true;
                        mLog.error("Commit/abort record encountered before corresponding change record (" + op + ")");
                        TransactionId tid = op.getTransactionId();
                        RedoableOp x = (RedoableOp) mOrphanOps.get(tid);
                        if (x != null)
                        	mLog.error("Op [" + op + "] is already in orphans map: value=" + x);
                        mOrphanOps.put(tid, op);
                    }
                }

                if (redoCommitted && prepareOp != null && (op instanceof CommitTxn) &&
                	(startingTime == -1 || prepareOp.getTimestamp() >= startingTime)) {
                	boolean allowRedo = false;
                	if (mailboxIds == null) {
                		// Caller says to ignore which mailbox(es) the op is for.
                		allowRedo = true;
                	} else {
                        int opMailboxId = prepareOp.getMailboxId();
                        if (prepareOp instanceof StoreIncomingBlob) {
                        	// special case for StoreIncomingBlob op that has
                        	// a list of mailbox IDs (It has getMailboxId()
                        	// == MAILBOX_ID_ALL.)
                        	List<Integer> list =
                        		((StoreIncomingBlob) prepareOp).getMailboxIdList();
                        	if (list != null) {
	                        	Set<Integer> opMboxIds =
	                        		new HashSet<Integer>(
	                        				((StoreIncomingBlob) prepareOp).
	                        				getMailboxIdList());
		                        for (int i = 0; i < mailboxIds.length; i++) {
		                        	Integer mboxId = new Integer(mailboxIds[i]);
		                        	if (opMboxIds.contains(mboxId)) {
		                        		allowRedo = true;
		                        		break;
		                        	}
		                        }
                        	} else {
                        		// Prior to redolog version 1.0 StoreIncomingBlob
                        		// didn't keep track of mailbox list.  Always recreate
                        		// the blob since we don't know which mailboxes will
                        		// need it.
                        		allowRedo = true;
                        	}
                        } else if (opMailboxId == RedoableOp.MAILBOX_ID_ALL) {
                        	// This case should be checked after StoreIncomingBlob
                        	// case because StoreIncomingBlob has mailbox ID of
                        	// MAILBOX_ID_ALL.
                        	allowRedo = true;
                        } else {
	                        for (int i = 0; i < mailboxIds.length; i++) {
	                        	if (opMailboxId == mailboxIds[i]) {
	                        		allowRedo = true;
	                        		break;
	                        	}
	                        }
                        }
                	}
                	if (allowRedo) {
                        try {
                            if (mLog.isDebugEnabled())
                                mLog.debug("Redoing: " + prepareOp.toString());
                            prepareOp.redo();
                        } catch(Exception e) {
                            throw ServiceException.FAILURE("Error executing redoOp", e);
                        }
                	}
                }
            }
        }
    }

    /**
     * 
     * @param redoLogMgr
     * @param postStartupRecoveryOps operations to recover/redo after startup
     *                               completes and clients are allowed to
     *                               connect
     * @return number of operations redone (regardless of their success)
     * @throws Exception
     */
	public int runCrashRecovery(RedoLogManager redoLogMgr, List postStartupRecoveryOps) throws Exception {
        File redoLog = redoLogMgr.getLogFile();
        if (!redoLog.exists())
        	return 0;

        scanLog(redoLog, false, null, 0);

		int numOps;
        synchronized (mOpsMapGuard) {
        	numOps = mOpsMap.size();
        }
		if (numOps == 0) {
			mLog.info("No uncommitted transactions to redo");
			return 0;
		}

        synchronized (mOpsMapGuard) {
            Set entrySet = mOpsMap.entrySet();
            mLog.info("Redoing " + numOps + " uncommitted transactions");
    		for (Iterator it = entrySet.iterator(); it.hasNext(); ) {
                Map.Entry entry = (Entry) it.next();
                RedoableOp op = (RedoableOp) entry.getValue();
                if (op == null)
                    continue;

                if (op.deferCrashRecovery()) {
                    mLog.info("Deferring crash recovery to after startup: " + op);
                    postStartupRecoveryOps.add(op);
                    continue;
                }

                if (mLog.isInfoEnabled())
    				mLog.info("REDOING: " + op);
    
    			boolean success = false;
                try {
    	            op.redo();
    	            success = true;
                } catch (Exception e) {
                    mLog.error("Redo failed for [" + op + "]." +
                               "  Backend state of affected item is indeterminate." +
                               "  Marking operation as aborted and moving on.", e);
                } finally {
                	if (success) {
            			CommitTxn commit = new CommitTxn(op);
        				redoLogMgr.logOnly(commit, true);
                	} else {
    	    			AbortTxn abort = new AbortTxn(op);
    	    			redoLogMgr.logOnly(abort, true);
                	}
                }
    		}
            mOpsMap.clear();
        }

        // Flush out all uncommitted Lucene index writes.
        Indexer.GetInstance().flush();

        return numOps;
	}

    /**
     * Returns a copy of the pending ops map.
     * @return
     */
    protected LinkedHashMap getCopyOfUncommittedOpsMap() {
        LinkedHashMap map;
        synchronized (mOpsMapGuard) {
            if (mOpsMap != null)
                map = new LinkedHashMap(mOpsMap);
            else
                map = new LinkedHashMap();
        }
        return map;
    }
}
