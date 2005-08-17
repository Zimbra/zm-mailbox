/*
 * Created on 2005. 6. 28.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.redolog;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.Zimbra;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RedoConfig {

    private static Log mLog = LogFactory.getLog(RedoConfig.class);

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

        mServiceHostname = config.getAttr(Provisioning.A_liquidServiceHostname);
        mRedoLogEnabled = config.getBooleanAttr(Provisioning.A_liquidRedoLogEnabled, D_REDOLOG_ENABLED);
        mRedoLogPath =
            Config.getPathRelativeToZimbraHome(
                    config.getAttr(Provisioning.A_liquidRedoLogLogPath,
                                   D_REDOLOG_PATH)).getAbsolutePath();
        mRedoLogArchiveDir =
            Config.getPathRelativeToZimbraHome(
                    config.getAttr(Provisioning.A_liquidRedoLogArchiveDir,
                                   D_REDOLOG_ARCHIVEDIR)).getAbsolutePath();
        mRedoLogRolloverFileSizeKB =
            config.getLongAttr(Provisioning.A_liquidRedoLogRolloverFileSizeKB,
                               D_REDOLOG_ROLLOVER_FILESIZE_KB);
        mRedoLogFsyncIntervalMS =
            config.getLongAttr(Provisioning.A_liquidRedoLogFsyncIntervalMS,
                               D_REDOLOG_FSYNC_INTERVAL_MS);
    }


    public static synchronized void reload() throws ServiceException {
        theInstance.reloadInstance();
    }


    private String mServiceHostname;
    public static synchronized String serviceHostname() {
        return theInstance.mServiceHostname;
    }

    private boolean mRedoLogEnabled;
    private static final boolean D_REDOLOG_ENABLED = true;
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
    private static final long D_REDOLOG_ROLLOVER_FILESIZE_KB = 10240;
    /**
     * Returns the redolog rollover threshold filesize.
     * The current redolog file is rolled over if it reaches or exceeds
     * this size.
     * @return threshold filesize in kilobytes
     */
    public static synchronized long redoLogRolloverFileSizeKB() {
        return theInstance.mRedoLogRolloverFileSizeKB;
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
}
