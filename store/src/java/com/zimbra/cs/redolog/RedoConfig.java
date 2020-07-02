/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on 2005. 6. 28.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.redolog;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.Zimbra;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RedoConfig {

    private static RedoConfig theInstance;
    static {
        try {
            theInstance = new RedoConfig();
        } catch (ServiceException e) {
            Zimbra.halt("Unable to read redolog configuration", e);
        }
    }

    private RedoConfig() throws ServiceException {
        reloadInstance();
    }

    private void reloadInstance() throws ServiceException {
        Server config = Provisioning.getInstance().getLocalServer();

        mServiceHostname = config.getAttr(Provisioning.A_zimbraServiceHostname);
        mRedoLogEnabled = config.getBooleanAttr(Provisioning.A_zimbraRedoLogEnabled, D_REDOLOG_ENABLED);
        mRedoLogPath =
            Config.getPathRelativeToZimbraHome(
                    config.getAttr(Provisioning.A_zimbraRedoLogLogPath,
                                   D_REDOLOG_PATH)).getAbsolutePath();
        mRedoLogArchiveDir =
            Config.getPathRelativeToZimbraHome(
                    config.getAttr(Provisioning.A_zimbraRedoLogArchiveDir,
                                   D_REDOLOG_ARCHIVEDIR)).getAbsolutePath();
        mRedoLogRolloverFileSizeKB =
            config.getLongAttr(Provisioning.A_zimbraRedoLogRolloverFileSizeKB,
                               D_REDOLOG_ROLLOVER_FILESIZE_KB);
        mRedoLogRolloverHardMaxFileSizeKB =
            config.getLongAttr(Provisioning.A_zimbraRedoLogRolloverHardMaxFileSizeKB,
                               D_REDOLOG_ROLLOVER_HARDMAX_FILESIZE_KB);
        mRedoLogRolloverMinFileAge =
            config.getLongAttr(Provisioning.A_zimbraRedoLogRolloverMinFileAge,
                               D_REDOLOG_ROLLOVER_MIN_FILE_AGE);
        mRedoLogDeleteOnRollover =
            config.getBooleanAttr(Provisioning.A_zimbraRedoLogDeleteOnRollover,
                                  D_REDOLOG_DELETE_ON_ROLLOVER);
        mRedoLogFsyncIntervalMS =
            config.getLongAttr(Provisioning.A_zimbraRedoLogFsyncIntervalMS,
                               D_REDOLOG_FSYNC_INTERVAL_MS);

        mRedoLogCrashRecoveryLookbackSec =
            config.getLongAttr(Provisioning.A_zimbraRedoLogCrashRecoveryLookbackSec,
                               D_REDOLOG_CRASH_RECOVERY_LOOKBACK_SEC);
    }


    public static synchronized void reload() throws ServiceException {
        theInstance.reloadInstance();
    }


    private String mServiceHostname;
    public static synchronized String serviceHostname() {
        return theInstance.mServiceHostname;
    }

    private boolean mRedoLogEnabled;
    private static final boolean D_REDOLOG_ENABLED = false;
    /**
     * Indicates whether redo logging is enabled.
     * @return
     */
    public static synchronized boolean redoLogEnabled() {
        return theInstance.mRedoLogEnabled;
    }

    private String mRedoLogPath;
    private static final String D_REDOLOG_PATH = "redolog/redo.log";
    /**
     * The path to the redo.log file.  Relative path is resolved against
     * ZIMBRA_HOME.  Default value is "$ZIMBRA_HOME/redolog/redo.log".
     * @return absolute path to redo log file
     */
    public static synchronized String redoLogPath() {
        return theInstance.mRedoLogPath;
    }

    private String mRedoLogArchiveDir;
    private static final String D_REDOLOG_ARCHIVEDIR = "redolog/archive";
    /**
     * Directory in which redo logs are archived.  When the current redo.log
     * file reaches a certain threshold, it is rolled over and archived.
     * That is, the current redo.log file is renamed to a timestamped name,
     * moved into the archive directory, and a new empty redo.log file is
     * created.
     * @return absolute path to the archive directory
     * @see #redoLogRolloverFileSizeKB()
     */
    public static synchronized String redoLogArchiveDir() {
        return theInstance.mRedoLogArchiveDir;
    }

    private long mRedoLogRolloverFileSizeKB;
    private static final long D_REDOLOG_ROLLOVER_FILESIZE_KB = 102400;
    /**
     * Returns the redolog rollover threshold filesize.
     * The current redolog file is rolled over if it reaches or exceeds
     * this size and file is old enough.
     * @return threshold filesize in kilobytes
     */
    public static synchronized long redoLogRolloverFileSizeKB() {
        return theInstance.mRedoLogRolloverFileSizeKB;
    }

    private long mRedoLogRolloverHardMaxFileSizeKB;
    private static final long D_REDOLOG_ROLLOVER_HARDMAX_FILESIZE_KB = 1048576;
    /**
     * Returns the redolog rollover hard threshold filesize.
     * The current redolog file is rolled over if it reaches or exceeds
     * this size, regardless of file age.
     * @return threshold filesize in kilobytes
     */
    public static synchronized long redoLogRolloverHardMaxFileSizeKB() {
        return theInstance.mRedoLogRolloverHardMaxFileSizeKB;
    }

    private long mRedoLogRolloverMinFileAge;
    private static final long D_REDOLOG_ROLLOVER_MIN_FILE_AGE = 30;
    /**
     * Returns the redolog rollover minimum file age.
     * The current redolog file is rolled over if it is older than this
     * and reaches the size threshold.
     * @return age in minutes
     */
    public static synchronized long redoLogRolloverMinFileAge() {
        return theInstance.mRedoLogRolloverMinFileAge;
    }

    private boolean mRedoLogDeleteOnRollover;
    private static final boolean D_REDOLOG_DELETE_ON_ROLLOVER = true;
    /**
     * If true, redo log file is deleted and discarded upon rollover.
     * If false, redo log being rolled over is archived.
     * @return
     */
    public static synchronized boolean redoLogDeleteOnRollover() {
        return theInstance.mRedoLogDeleteOnRollover;
    }

    private long mRedoLogFsyncIntervalMS;
    private static final long D_REDOLOG_FSYNC_INTERVAL_MS = 10;
    /**
     * Returns the fsync interval for flush/fsync thread.  Writes to the log
     * are written securely to disk by forcing an fsync.  But fsyncs are very
     * slow, so instead of each logging thread calling fsync individually,
     * they wait for a dedicated thread to fsync the accumulated changes
     * periodically.  This configuration value controls the interval between
     * the fsyncs.
     * 
     * With a longer interval, there will be fewer fsyncs compared to the
     * number of logging calls.  This can improve throughput under heavy
     * load but increases the latency on individual logging calls.
     * 
     * @return interval in milliseconds; default is 10ms
     */
    public static synchronized long redoLogFsyncIntervalMS() {
        return theInstance.mRedoLogFsyncIntervalMS;
    }

    private long mRedoLogCrashRecoveryLookbackSec;
    private static final long D_REDOLOG_CRASH_RECOVERY_LOOKBACK_SEC = 10;
    /**
     * This parameter is also related to running mysql with innodb_flush_log_at_trx_commit=0.
     * When recovering from a crash, mysql may not have the committed changes from roughly the last second. 
     * ZCS must re-execute enough past operations to bring mysql into consistent state.
     * 
     * This parameter controls how long to look back.  Default is 10 seconds.  Crash recovery normally
     * re-executes only pending changes.  But with this parameter committed changes within the last 10
     * seconds are also re-executed.
     * @return
     */
    public static synchronized long redoLogCrashRecoveryLookbackSec() {
        return theInstance.mRedoLogCrashRecoveryLookbackSec;
    }
}
