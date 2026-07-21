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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_USERNAME;

/**
 * Drives out-of-band MFA challenges (e.g. push) to completion in a way that scales to many
 * simultaneous EAS clients. Key properties:
 *
 * <ol>
 *   <li><b>Shared, bounded poller pool</b> — one {@link ScheduledExecutorService} sized by
 *       {@code mfa_idp_pool_max_size} performs all IdP polls. Poll concurrency is therefore
 *       capped regardless of how many clients are waiting (no thread-per-client polling).</li>
 *   <li><b>De-duplication</b> — concurrent/retried requests sharing a {@code dedupeKey}
 *       reuse a single challenge + single poll loop, so only one push is
 *       issued per login attempt (EAS clients retry aggressively).</li>
 *   <li><b>Admission control</b> — a {@link Semaphore} ({@code mfa_idp_max_connection_allowed})
 *       bounds in-flight challenges; over the limit, {@link #await} returns {@link MFAPollResult#ERROR}.
 *       </li>
 * </ol>
 *
 * <p>NOTE: the calling (servlet) thread still blocks in {@link #await} until resolution/timeout.
 * Converting the auth wait async is the follow-up
 * for freeing request threads at very high concurrency.
 */
public final class MFAPollingService {

    private static final MFAPollingService INSTANCE = new MFAPollingService();

    public static MFAPollingService getInstance() {
        return INSTANCE;
    }

    private final ScheduledExecutorService scheduler;

    private final Semaphore admission;

    private final ConcurrentHashMap<String, Pending> active = new ConcurrentHashMap<String, Pending>();

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
        this.admission = new Semaphore(Math.max(10, LC.mfa_idp_max_connection_allowed.intValue()));
    }

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

    public MFAPollResult await(IRopcHandler provider, MFAChallenge challenge, long interval, long pollingTimeout) {
        if (provider == null || challenge == null) {
            return MFAPollResult.ERROR;
        }
        if (!admission.tryAcquire()) {
            ZimbraLog.account.warn("Authentication failed : MFA admission limit reached (%d); " +
                            "rejecting challenge for %s",
                    LC.mfa_idp_max_connection_allowed.intValue(), challenge.getDedupeKey());
            return MFAPollResult.ERROR;
        }
        Pending pendingReq = null;
        try {
            pendingReq = active.computeIfAbsent(challenge.getDedupeKey(),
                    new java.util.function.Function<String, Pending>() {
                        @Override
                        public Pending apply(String key) {
                            return schedule(provider, challenge, pollingTimeout, interval);
                        }
                    });
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
        } finally {
            admission.release();
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
        active.remove(pendingReq.challenge.getDedupeKey(), pendingReq);
        pendingReq.future.complete(result);
    }
}
