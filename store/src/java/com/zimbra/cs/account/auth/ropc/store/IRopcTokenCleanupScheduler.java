/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.account.auth.ropc.store;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler for periodic cleanup of expired ROPC session tokens from the database.
 *
 * <p>Runs as a single daemon thread named {@value #THREAD_NAME}, started lazily
 * the first time a user authenticates via the IdP ROPC flow with DB-backed storage.
 * The first execution is aligned to the next midnight; subsequent runs follow the
 * configured interval ({@code mfa_idp_db_cleanup_interval_days}).
 * Disabled automatically if the configured interval is {@code <= 0}.
 */
public class IRopcTokenCleanupScheduler {

    private static final String THREAD_NAME = "D-RopcTokenCleanup";

    private static boolean started = false;

    private static final long CLEANUP_INTERVAL_IN_SECONDS = TimeUnit.DAYS.
            toSeconds(LC.mfa_idp_db_cleanup_interval_days.intValue());

    private static final ScheduledExecutorService CLEANUP_EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor(
            r -> {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setName(THREAD_NAME);
                t.setDaemon(true);
                return t;
            }
    );

    public static synchronized void start() {
        if (!started) {
            ZimbraLog.account.info("Starting ROPC token cleanup Scheduler");

            int intervalDays = LC.mfa_idp_db_cleanup_interval_days.intValue();
            if (intervalDays <= 0) {
                ZimbraLog.account.info("ROPC token cleanup scheduler is disabled (interval <= 0)");
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
            long initialDelay = Duration.between(now, nextMidnight).getSeconds();

            CLEANUP_EXECUTOR_SERVICE.scheduleAtFixedRate(
                    IRopcTokenCleanupScheduler::cleanExpiredTokens,
                    initialDelay,
                    CLEANUP_INTERVAL_IN_SECONDS,
                    TimeUnit.SECONDS
            );
            started = true;
        } else {
            ZimbraLog.account.debug("ROPC token cleanup Scheduler is already started");
        }
    }

    public static boolean isStarted() {
        return started;
    }

    private static void cleanExpiredTokens() {
        try {
            ZimbraLog.account.info("[ROPC Cleanup] Initiating database sweep...");
            DbRopcTokenStore.deleteExpiredTokens();
        } catch (Exception e) {
            ZimbraLog.account.error("[ROPC Cleanup] Encountered a server error during token cleanup", e);
        }
    }
}
