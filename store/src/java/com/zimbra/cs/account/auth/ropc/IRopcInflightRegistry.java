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
import com.zimbra.cs.account.auth.AuthContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Single-flight coordinator. Ensures at most ONE authentication (ROPC + push) runs per key at a
 * time; concurrent/retried callers for the same key attach to the in-flight result instead of
 * issuing a second username/password call to the IdP (and a second push).
 *
 * <p>The key is so different devices of the SAME account
 * authenticate concurrently (each device gets its own push), while chatty retries from one device
 * collapse to one challenge.
 *
 * <p>A {@link Semaphore} bounds distinct in-flight challenges globally (admission control); over the
 * limit, {@link #execute} returns the supplied {@code fallback}.
 */
public final class IRopcInflightRegistry {

    private final ConcurrentHashMap<String, CompletableFuture<Object>> inflight =
            new ConcurrentHashMap<String, CompletableFuture<Object>>();

    private final Semaphore admission;

    private final long maxWaitMillis;

    public IRopcInflightRegistry() {
        this.admission = new Semaphore(Math.max(10, LC.mfa_idp_max_connection_allowed.intValue()));
        this.maxWaitMillis = Math.max(1000, LC.mfa_idp_max_retry_wait_timeout.intValue());
    }

    public static String key(String username, String userAgent, String provider,
                             AuthContext.Protocol protocol, String deviceId) {
        return userAgent + "|" + username + "|" +  provider + "|" + (protocol == null ? "" : protocol)
                + "|" + (deviceId == null ? "" : deviceId);
    }

    @SuppressWarnings("unchecked")
    public <T> T execute(String key, Supplier<T> task, T fallback) {
        CompletableFuture<Object> mine = new CompletableFuture<Object>();
        CompletableFuture<Object> existing = inflight.putIfAbsent(key, mine);

        if (existing != null) {
            return await(existing, fallback);
        }

        boolean acquired = false;
        try {
            acquired = admission.tryAcquire();
            if (!acquired) {
                ZimbraLog.account.warn("Authentication failed :ROPC admission limit reached; " +
                        "rejecting auth request.");
                mine.complete(fallback);
                return fallback;
            }
            T result;
            try {
                result = task.get();
            } catch (Exception e) {
                ZimbraLog.account.error("Authentication Failed : Error occurred in IDP based auth for %s", key, e);
                result = fallback;
            }
            mine.complete(result);
            return result;
        } finally {
            inflight.remove(key, mine);
            if (acquired) {
                admission.release();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T await(CompletableFuture<Object> future, T fallback) {
        try {
            return (T) future.get(maxWaitMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return fallback;
        } catch (ExecutionException e) {
            return fallback;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}

