package com.zimbra.cs.redolog.logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.redolog.FileRedoLogManager;
import com.zimbra.cs.redolog.FileRolloverManager;
import com.zimbra.cs.redolog.HttpRedoLogManager;
import com.zimbra.cs.redolog.LeaderChangeListener;
import com.zimbra.cs.redolog.LeaderUnavailableException;
import com.zimbra.cs.redolog.RedologLeaderListener;
import com.zimbra.cs.redolog.RolloverManager;
import com.zimbra.cs.redolog.TransactionId;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.redolog.seq.LocalSequenceNumberGenerator;

/**
 * Log writer implementation which is aware of service leadership
 * When the current session is the leader; this class logs directly to a (shared) file e.g. /opt/zimbra/redolog/redo.log
 * When the another session is the leader; this class forwards the request on
 * When there is no leader; this class stages operations locally and forwards them once a new leader has been elected
 */
public class LeaderAwareFileLogWriter extends FileLogWriter {

    private FileLogWriter alternateWriter;
    private HttpLogWriter remoteWriter;
    private boolean hasAlternateWrites = false;
    private RedologLeaderListener leaderListener;

    private File stagedredofile = new File(LC.zimbra_tmp_directory.value() + "/redolog/stage/stageredo.log");

    public LeaderAwareFileLogWriter(FileRedoLogManager redoLogMgr, File logfile,
            long fsyncIntervalMS, RedologLeaderListener leaderListener) throws ServiceException {
        super(redoLogMgr, logfile, fsyncIntervalMS);
        this.leaderListener = leaderListener;
        leaderListener.addListener(new LeaderChangeListener() {
            @Override
            public void onLeaderChange(String newLeaderSessionId, LeaderStateChange stateChange) {
                try {
                    synchronized (opened) {
                        if (stateChange == LeaderStateChange.LOST_LEADERSHIP) {
                            if (opened.get()) {
                                rollover(null);
                                close();
                            }
                        } else if (stateChange == LeaderStateChange.GAINED_LEADERSHIP) {
                            open();
                        }
                        if (newLeaderSessionId != null) {
                            drainLogOps();
                        }
                    }
                } catch (IOException ioe) {
                    ZimbraLog.redolog.error("IOException opening or closing log writer", ioe);
                }
            }
        });
        stagedredofile.getParentFile().mkdirs();
        this.alternateWriter = new LocalStagingFileLogWriter(redoLogMgr, stagedredofile, fsyncIntervalMS);
        try {
            alternateWriter.open();
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("unable to start redolog", ioe);
        }

        this.remoteWriter = new HttpLogWriter(redoLogMgr) {
            @Override
            protected void notifyCallback(RedoableOp op) throws IOException {
            }

            @Override
            protected String getUrl(boolean fallbackIfNoLeader) throws IOException {
                return HttpRedoLogManager.getUrl(false);
            }
        };
        try {
            remoteWriter.open();
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("unable to start redolog", ioe);
        }
    }

    private void drainLogOps() throws IOException {
        assert(Thread.holdsLock(opened));
        synchronized (alternateWriter) {
            if (hasAlternateWrites) {
                alternateWriter.close();
                FileLogReader logReader = new FileLogReader(stagedredofile);
                logReader.open();

                RedoableOp stagedOp = null;
                LinkedHashMap<TransactionId, RedoableOp> activeOps = new LinkedHashMap<TransactionId, RedoableOp>();
                boolean error = false;
                while((stagedOp = logReader.getNextOp()) != null) {
                    ZimbraLog.redolog.debug("read staged op %s", stagedOp);
                    try {
                        if (!error) {
                            log(stagedOp, stagedOp.getInputStream(), true, false);
                        } else {
                            activeOps.put(stagedOp.getTransactionId(), stagedOp);
                        }
                    } catch (LeaderUnavailableException lue) {
                        ZimbraLog.redolog.debug("leader unavailable while draining", lue);
                        error = true;
                        activeOps.put(stagedOp.getTransactionId(), stagedOp);
                    }
                }
                logReader.close();
                hasAlternateWrites = activeOps.size() > 0;
                alternateWriter.rollover(activeOps);
                alternateWriter.open();
            }
        }
    }

    private void log(RedoableOp op, InputStream data, boolean synchronous, boolean writeToAlt)
            throws IOException {
        synchronized (opened) {
            if (waitForOpen()) {
                ZimbraLog.redolog.debug("the leader - logging directly");
                super.log(op, data, synchronous);
                return;
            }
        }

        //not opened at this moment
        //do remote/alternate writing outside synchronized
        //this avoids deadlock if remote writer is actually a loopback (e.g. race when leader acquired by this node)
        if (leaderListener.getLeaderSessionId() != null) {
            ZimbraLog.redolog.debug("not the leader - sending to existing leader");
            try {
                remoteWriter.log(op, data, synchronous);
            } catch (IOException ioe) {
                throw new LeaderUnavailableException(ioe);
            }
        } else {
            if (writeToAlt) {
                ZimbraLog.redolog.debug("no leader - queuing locally");
                synchronized (alternateWriter) {
                    alternateWriter.log(op, data, true);
                    hasAlternateWrites = true;
                }
            } else {
                throw new LeaderUnavailableException("no leader and local queuing disabled for this call");
            }
        }
    }

    @Override
    public void log(RedoableOp op, InputStream data, boolean synchronous)
            throws IOException {
        log(op, data, synchronous, true);
    }

    private AtomicBoolean opened = new AtomicBoolean(false);

    private boolean waitForOpen() {
        //states
        //1. leader in consul and opened - return true immediately
        //2. leader in consul but not opened - wait for listener to trigger opened.notify
        //3. not leader in consul but still open - return false immediately
        //4. not leader in consul and closed - return false immediately

        synchronized (opened) {
            while (!opened.get() && leaderListener.isLeader()) {
                try {
                    opened.wait(1000);
                } catch (InterruptedException e) {
                }
            }
            return opened.get();
        }
    }

    @Override
    public void open() throws IOException {
        synchronized (opened) {
            if (!leaderListener.isLeader()) {
                populateHeader();
                return;
            }
            super.open();
            if (this.redoLogMgr.getCurrentLogSequence() != this.getSequence()) {
                //sequence changed since we last closed; rollover to catch up
                rollover(null);
            }
            opened.set(true);
            opened.notifyAll();
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (opened) {
            opened.set(false);
            opened.notifyAll();
            super.close();
        }
    }

    private class LocalStagingFileLogWriter extends FileLogWriter {

        private RolloverManager localRolloverManager;

        public LocalStagingFileLogWriter(FileRedoLogManager redoLogMgr,
                File logfile, long fsyncIntervalMS) {
            super(redoLogMgr, logfile, fsyncIntervalMS);
            this.localRolloverManager = new FileRolloverManager(redoLogMgr, logfile, new LocalSequenceNumberGenerator());
        }

        @Override
        public RolloverManager getRolloverManager() {
            return localRolloverManager;
        }

        @Override
        protected boolean deleteOnRollover() {
            return true;
        }

        @Override
        protected FileLogWriter newLogWriter(FileRedoLogManager redoLogMgr,
                File logfile, long fsyncIntervalMS) {
            return new LocalStagingFileLogWriter(redoLogMgr, logfile, fsyncIntervalMS);
        }
    }
}
