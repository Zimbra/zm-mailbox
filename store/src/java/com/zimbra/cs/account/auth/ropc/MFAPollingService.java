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

package com.zimbra.cs.account.auth.ropc;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_USERNAME;

/**
 * Executes out-of-bound MFA Challenges (eg. okta PUSH).
 * <p>
 *     <b>Concurrency and ScalingArchitecture:</b><br>
 *     This Service is designed to protect the server from resource exhaustion during high-concurrency
 *     login events(eg. 1000 simultaneous mobile devices attempting to authenticate).
 * </p>
 *<p>
 *     Instead of forcing the 500 individual HTTP servlet threads to actively run polling
 *     loos-which would cause a massive spike in CPU context-switching and socket allocation-this service offloads
 *     the actual network I/O to a small bounded background thread pool(eg. 16-32 thread
 *     managed by a ScheduledExecutorService.
 *</p>
 * <b>
 *     How it works under heavy load:
 * </b>
 * <ul>
 *     <li><b>This Main HTTP Thread (Waiting):</b>When a client request arrives, the servlet thread
 *     submits the challenge to this service and enters a lightweight wait state via
 *     CompletableFuture. While waiting , the thread consumes almost zeroCPU cycles.</li>
 *     <li><b>The Background poller thread (Working)O:</b>The bounded pool of daemon threads multiplexes
 *     all active MFA challenges. A poller thread wakes up , executes a single HTTP status check against
 *     the IDP for a specific user immediately returns to teh pool to pick up the next user's
 *     challenge.</li>
 * </ul>
 * <p>
 *     This asynchronous execution model allows tiny poll of background threads to efficiently multiplex
 *     and manage hundreds of sleep client connections without overwhelming zimbra server.
 * </p>
 */
public final class MFAPollingService {

    private static final MFAPollingService INSTANCE = new MFAPollingService();

    public static MFAPollingService getInstance() {
        return INSTANCE;
    }

    private final ScheduledExecutorService scheduler;

    private MFAPollingService() {
        final AtomicInteger seq = new AtomicInteger();
        this.scheduler = Executors.newScheduledThreadPool(
                Math.max(4, LC.mfa_idp_pool_max_size.intValue()),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(runnable, "MFA-Poller-" + seq.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    }
                });
    }

    /**
     * Holds the challenge, its deadline, the polling task, and the future
     * that will be completed with the final {@link MFAPollResult}.
     */
    private static final class Pending {
        private final MFAChallenge challenge;

        private final CompletableFuture<MFAPollResult> future = new CompletableFuture<MFAPollResult>();

        private final long deadline;

        private volatile ScheduledFuture<?> task;

        Pending(MFAChallenge challenge, long deadline) {
            this.challenge = challenge;
            this.deadline = deadline;
        }
    }

    /**
     * Blocks until the MFA push challenge is resolved or the polling timeout is exceeded.
     * Schedules repeated polls at the given interval and waits on the result future.
     *
     * @param provider       the {@link IRopcHandler} used to poll the IdP
     * @param challenge      the active MFA challenge to poll
     * @param interval       polling interval in milliseconds
     * @param pollingTimeout maximum time to wait for user approval in milliseconds
     * @return {@link MFAPollResult} indicating the final outcome
     */
    public MFAPollResult await(IRopcHandler provider, MFAChallenge challenge, long interval, long pollingTimeout) {
        if (provider == null || challenge == null) {
            return MFAPollResult.ERROR;
        }
        Pending pendingReq = null;
        try {
            pendingReq = schedule(provider, challenge, pollingTimeout, interval);

            return pendingReq.future.get(pollingTimeout + 1500L, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            if (pendingReq != null) {
                complete(pendingReq, MFAPollResult.EXPIRED);
            }
            return MFAPollResult.EXPIRED;
        } catch (Exception e) {
            ZimbraLog.account.error("Authentication Failed : Error occurred while Polling " +
                    "for %s", challenge.get(REQUEST_PARAM_USERNAME), e);
            if (pendingReq != null) {
                complete(pendingReq, MFAPollResult.ERROR);
            }
            return MFAPollResult.ERROR;
        }
    }

    private Pending schedule(final IRopcHandler provider, final MFAChallenge challenge,
                             long pollingTimeout, long interval) {
        final Pending pendingReq = new Pending(challenge, System.currentTimeMillis() + pollingTimeout);
        pendingReq.task = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if (System.currentTimeMillis() > pendingReq.deadline) {
                        complete(pendingReq, MFAPollResult.EXPIRED);
                        return;
                    }
                    MFAPollResult pollResult = provider.pollChallenge(challenge);
                    if (pollResult != MFAPollResult.WAITING) {
                        complete(pendingReq, pollResult);
                    }
                    ZimbraLog.account.debug("MFA polling : WAITING for user %s to approve challenge.",
                            challenge.get(REQUEST_PARAM_USERNAME));
                } catch (Exception e) {
                    ZimbraLog.account.error("Authentication Failed : Error occurred while Polling " +
                            "for %s", challenge.get(REQUEST_PARAM_USERNAME), e);
                    complete(pendingReq, MFAPollResult.ERROR);
                }
            }
        }, interval, interval, TimeUnit.MILLISECONDS);
        return pendingReq;
    }

    private void complete(Pending pendingReq, MFAPollResult result) {
        ScheduledFuture<?> future = pendingReq.task;
        if (future != null) {
            future.cancel(true);
        }
        pendingReq.future.complete(result);
    }
}
