/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on 2004. 7. 22.
 */
package com.zimbra.cs.redolog;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.redolog.logger.FileLogReader;
import com.zimbra.cs.redolog.logger.LogWriter;
import com.zimbra.cs.redolog.op.AbortTxn;
import com.zimbra.cs.redolog.op.Checkpoint;
import com.zimbra.cs.redolog.op.CommitTxn;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.redolog.op.StoreIncomingBlob;

/**
 * @author jhahm
 */
public class RedoPlayer {

    private static Log mLog = LogFactory.getLog(RedoPlayer.class);

    private static final int INITIAL_MAP_SIZE = 1000;

    // Use a separate guard object to synchronize access to mOpsMap.
    // Don't synchronize on mOpsMap itself because it can get reassigned.
    private final Object mOpsMapGuard = new Object();

    // LinkedHashMap to ensure iteration order == insertion order
    private LinkedHashMap<TransactionId, RedoableOp> mOpsMap;

    private boolean mWritable;
    private boolean mUnloggedReplay;
    private boolean mIgnoreReplayErrors;

    public RedoPlayer(boolean writable) {
        this(writable, false, false);
    }

    public RedoPlayer(boolean writable, boolean unloggedReplay, boolean ignoreReplayErrors) {
		mOpsMap = new LinkedHashMap<TransactionId, RedoableOp>(INITIAL_MAP_SIZE);
        mWritable = writable;
        mUnloggedReplay = unloggedReplay;
        mIgnoreReplayErrors = ignoreReplayErrors;
    }

    public void shutdown() {
        mOpsMap.clear();
    }

	public void scanLog(File logfile,
            boolean redoCommitted,
            Map<Integer, Integer> mboxIDsMap,
            long startTime)
	throws IOException, ServiceException {
        scanLog(logfile, redoCommitted, mboxIDsMap, startTime, Long.MAX_VALUE);
	}

	/**
	 * Scans a redo log file.  An op that is neither committed nor aborted is
	 * added to mOpsMap.  These are the ops that need to be reattempted during
	 * crash recovery.  If redoCommitted is true, an op is reattempted as soon
	 * as its COMMIT entry is encountered.  This case is for replaying the logs
	 * during mailbox restore.
	 * @param logfile
	 * @param redoCommitted
     * @param mboxIDsMap If not null, restrict replay of log entries to
     *                   mailboxes whose IDs are given by the key set of the
     *                   map.  Replay is done against mailboxes whose IDs are
     *                   given by the value set of the map.  Thus, it is
     *                   possible to replay operations from one mailbox in
     *                   a different mailbox.
     * @param startTime  Only process ops whose prepare time is at or later than
     *                   this time.
     * @param endTime    Only process ops whose commit time is before (but not
     *                   at) this time.
	 * @throws IOException
	 */
	public void scanLog(File logfile,
                        boolean redoCommitted,
                        Map<Integer, Integer> mboxIDsMap,
                        long startTime,
                        long endTime)
    throws IOException, ServiceException {
		FileLogReader logReader = new FileLogReader(logfile, mWritable);
		logReader.open();
		long lastPosition = 0;

		// Read all ops in redo log, discarding those with commit/abort entries.
		try {
			RedoableOp op = null;
			while ((op = logReader.getNextOp()) != null) {
				lastPosition = logReader.position();

				if (mLog.isDebugEnabled())
					mLog.debug("Read: " + op);

                processOp(op, redoCommitted, mboxIDsMap, startTime, endTime);
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
                if (mWritable) {
                    mLog.warn(msg + "  File will be truncated to " +
                              lastPosition + " bytes.");
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
    private Map<TransactionId, RedoableOp> mOrphanOps =
    	new HashMap<TransactionId, RedoableOp>();

    private final void processOp(RedoableOp op,
                                 boolean redoCommitted,
                                 Map<Integer, Integer> mboxIDsMap,
                                 long startTime,
                                 long endTime)
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
                	(startTime == -1 || prepareOp.getTimestamp() >= startTime) &&
                	op.getTimestamp() < endTime) {
                	boolean allowRedo = false;
                	if (mboxIDsMap == null) {
                		// Caller doesn't care which mailbox(es) the op is for.
                		allowRedo = true;
                	} else {
                        int opMailboxId = prepareOp.getMailboxId();
                        if (prepareOp instanceof StoreIncomingBlob) {
                            assert(opMailboxId == RedoableOp.MAILBOX_ID_ALL);
                        	// special case for StoreIncomingBlob op that has
                        	// a list of mailbox IDs.
                            StoreIncomingBlob storeOp = (StoreIncomingBlob) prepareOp;
                        	List<Integer> list = storeOp.getMailboxIdList();
                        	if (list != null) {
	                        	Set<Integer> opMboxIds = new HashSet<Integer>(list);
                                for (Map.Entry<Integer, Integer> entry : mboxIDsMap.entrySet()) {
                                    if (opMboxIds.contains(entry.getKey())) {
                                        allowRedo = true;
                                        // Replace the mailbox ID list in the op.  We're
                                        // replaying it only for the target mailbox ID we're
                                        // interested in.
                                        List<Integer> newList =
                                            new ArrayList<Integer>(mboxIDsMap.values());
                                        storeOp.setMailboxIdList(newList);
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
                            for (Map.Entry<Integer, Integer> entry : mboxIDsMap.entrySet()) {
                                if (opMailboxId == entry.getKey().intValue()) {
                                    if (entry.getValue() != null) {
                                        // restore to a different mailbox
                                        prepareOp.setMailboxId(entry.getValue().intValue());
                                    }
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
                            prepareOp.setUnloggedReplay(mUnloggedReplay);
                            playOp(prepareOp);
                        } catch(Exception e) {
                            if (!ignoreReplayErrors())
                                throw ServiceException.FAILURE("Error executing redoOp", e);
                            else
                                ZimbraLog.redolog.warn(
                                        "Ignoring error during redo log replay: " + e.getMessage(), e);
                        }
                	}
                }
            }
        }
    }

    protected boolean ignoreReplayErrors() { return mIgnoreReplayErrors; }

    /**
     * Actually execute the operation.
     * @param op
     * @throws Exception
     */
    protected void playOp(RedoableOp op) throws Exception {
        op.redo();
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
	public int runCrashRecovery(RedoLogManager redoLogMgr,
								List<RedoableOp> postStartupRecoveryOps)
	throws Exception {
        File redoLog = redoLogMgr.getLogFile();
        if (!redoLog.exists())
        	return 0;

        // scanLog can truncate the current redo.log if it finds junk data at the end
        // from the previous crash.  Close log writer before scanning and reopen after
        // so we don't accidentally undo the truncation on the next write to the log.
        LogWriter logWriter = redoLogMgr.getLogWriter();
        logWriter.close();
        scanLog(redoLog, false, null, 0);
        logWriter.open();

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
        MailboxIndex.flushAllWriters();

        return numOps;
	}

    /**
     * Returns a copy of the pending ops map.
     * @return
     */
    protected LinkedHashMap<TransactionId, RedoableOp> getCopyOfUncommittedOpsMap() {
        LinkedHashMap<TransactionId, RedoableOp> map;
        synchronized (mOpsMapGuard) {
            if (mOpsMap != null)
                map = new LinkedHashMap<TransactionId, RedoableOp>(mOpsMap);
            else
                map = new LinkedHashMap<TransactionId, RedoableOp>();
        }
        return map;
    }
}
