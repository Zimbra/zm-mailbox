package com.zimbra.cs.redolog;

import java.io.File;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.redolog.logger.LeaderAwareFileLogWriter;
import com.zimbra.cs.redolog.logger.LogWriter;
import com.zimbra.cs.redolog.op.RedoableOp;

/**
 * Redolog manager which implements specific behavior depending on service leader status
 */
public class LeaderAwareRedoLogManager extends FileRedoLogManager {

    private RedologLeaderListener leaderListener;

    LeaderAwareRedoLogManager(File redolog, File archdir,
            boolean supportsCrashRecovery, RedologLeaderListener leaderListener) throws ServiceException {
        super(redolog, archdir, supportsCrashRecovery);
        this.leaderListener = leaderListener;
    }

    @Override
    public LogWriter createLogWriter(long fsyncIntervalMS) throws ServiceException {
        return new LeaderAwareFileLogWriter(this, mLogFile, fsyncIntervalMS, leaderListener);
    }

    @Override
    public void log(RedoableOp op, boolean synchronous) throws ServiceException {
        super.log(op, synchronous);
    }

    @Override
    public void logOnly(RedoableOp op, boolean synchronous)
            throws ServiceException {
        super.logOnly(op, synchronous);
    }

    @Override
    protected void signalLogError(Throwable e) throws ServiceException {
        if (isCausedBy(e, LeaderUnavailableException.class)) {
            throw ServiceException.TEMPORARILY_UNAVAILABLE();
        } else {
            throw ServiceException.FAILURE("redolog exception", e);
        }
    }

    private boolean isCausedBy(Throwable throwable, Class<? extends Throwable> targetCause) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause.getClass().isAssignableFrom(targetCause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

}
