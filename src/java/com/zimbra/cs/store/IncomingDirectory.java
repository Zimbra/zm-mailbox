/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.store;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

public class IncomingDirectory {
    private String mPath;
    private UniqueFileNameGenerator mNameGenerator;

    public IncomingDirectory(String path) {
        mPath = path;
        mNameGenerator = new UniqueFileNameGenerator();
    }

    public String getPath() {
        return mPath;
    }

    void setPath(String path) {
        mPath = path;
    }

    public File getNewIncomingFile() {
        return new File(mPath + File.separator + mNameGenerator.getFilename());
    }

    private static class UniqueFileNameGenerator {
        private long mTime, mSequence;

        public UniqueFileNameGenerator() {
            reset();
        }

        private void reset() {
            mTime = System.currentTimeMillis();
            mSequence = 0;
        }

        public String getFilename() {
            long time, sequence;
            synchronized (this) {
                if (mSequence >= 1000)
                    reset();
                time = mTime;
                sequence = mSequence++;
            }
            return time + "-" + sequence + ".msg";
        }
    }


    private static final long SWEEP_INTERVAL_MS = 60 * 1000;  // 1 minute

    private static IncomingDirectorySweeper mSweeper;

    public synchronized static void startSweeper() {
        if (mSweeper != null)
            return;

        long sweepMaxAgeMS = LC.zimbra_store_sweeper_max_age.intValue() * 60 * 1000;
        IncomingDirectorySweeper sweeper = new IncomingDirectorySweeper(SWEEP_INTERVAL_MS, sweepMaxAgeMS);
        sweeper.start();
        mSweeper = sweeper;
    }

    public synchronized static void stopSweeper() {
        if (mSweeper == null)
            return;

        mSweeper.signalShutdown();
        try {
            mSweeper.join();
        } catch (InterruptedException e) {}
        mSweeper = null;
    }

    public synchronized static void setSweptDirectories(IncomingDirectory inc) {
        IncomingDirectorySweeper.sSweptDirectories = Arrays.asList(inc);
    }

    public synchronized static void setSweptDirectories(Collection<IncomingDirectory> swept) {
        if (swept == null)
            IncomingDirectorySweeper.sSweptDirectories = Collections.emptyList();
        else
            IncomingDirectorySweeper.sSweptDirectories = new ArrayList<IncomingDirectory>(swept);
    }

    private static class IncomingDirectorySweeper extends Thread {
        static List<IncomingDirectory> sSweptDirectories;

        private boolean mShutdown = false;
        private long mSweepIntervalMS;
        private long mMaxAgeMS;

        public IncomingDirectorySweeper(long sweepIntervalMS, long maxAgeMS) {
            super("IncomingDirectorySweeper");
            setDaemon(true);
            mSweepIntervalMS = sweepIntervalMS;
            mMaxAgeMS = maxAgeMS;
        }

        public synchronized void signalShutdown() {
            mShutdown = true;
            wakeup();
        }

        public synchronized void wakeup() {
            notify();
        }

        @Override public void run() {
            ZimbraLog.store.info(getName() + " thread starting");

            boolean shutdown = false;
            long startTime = System.currentTimeMillis();

            while (!shutdown) {
                // sleep until next scheduled wake-up time, or until notified
                synchronized (this) {
                    if (!mShutdown) {
                        long now = System.currentTimeMillis();
                        long until = startTime + mSweepIntervalMS;
                        if (until > now) {
                            try {
                                wait(until - now);
                            } catch (InterruptedException e) {}
                        }
                    }
                    shutdown = mShutdown;
                    if (shutdown)
                        break;
                }

                int numDeleted = 0;
                startTime = System.currentTimeMillis();

                // delete old files in all incoming directories
                for (IncomingDirectory inc : sSweptDirectories) {
                    File directory = new File(inc.getPath());
                    if (!directory.exists())
                        continue;
                    File[] files = directory.listFiles();
                    if (files == null)
                        continue;

                    for (int i = 0; i < files.length; i++) {
                        // Check for shutdown after every 100 files.
                        if (i % 100 == 0) {
                            synchronized (this) {
                                shutdown = mShutdown;
                            }
                            if (shutdown)
                                break;
                        }

                        File file = files[i];
                        if (file.isDirectory())
                            continue;

                        long lastMod = file.lastModified();
                        // lastModified() returns 0L if file doesn't exist (i.e. deleted by another thread
                        // after this thread did directory.listFiles())
                        if (lastMod <= 0L)
                            continue;

                        long age = startTime - lastMod;
                        if (age >= mMaxAgeMS) {
                            boolean deleted = file.delete();
                            if (!deleted) {
                                // Let's warn only if delete failure wasn't caused by file having been
                                // deleted by someone else already.
                                if (file.exists())
                                    ZimbraLog.store.warn("Sweeper unable to delete " + file.getAbsolutePath());
                            } else if (ZimbraLog.store.isDebugEnabled()) {
                                ZimbraLog.store.debug("Sweeper deleted " + file.getAbsolutePath());
                                numDeleted++;
                            }
                        }
                    }

                    synchronized (this) {
                        shutdown = mShutdown;
                    }
                    if (shutdown)
                        break;
                }

                long elapsed = System.currentTimeMillis() - startTime;

                ZimbraLog.store.debug("Incoming directory sweep deleted " + numDeleted + " files in " + elapsed + "ms");
            }

            ZimbraLog.store.info(getName() + " thread exiting");
        }
    }
}
