/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

public class TaskRetry {

    private int numRetries = 0;
    private RetryParams params;
    private Delay delay;

    public TaskRetry(RetryParams params) {
        this.params = params;
        switch (params.policy) {
        case constant:
            delay = new ConstantDelay(params.initialDelay);
            break;
        case linear:
            delay = new LinearBackoff(params.initialDelay);
            break;
        case exponential:
            delay = new ExponentialBackoff(params.initialDelay);
            break;
        }
    }

    public boolean canRetry() {
        return params.maxRetries > 0 ? numRetries < params.maxRetries : true;
    }

    public void increment() {
        numRetries++;
    }

    public int getNumRetries() {
        return numRetries;
    }

    public long getDelayMillis() {
        if (params.maxDelay > 0) {
            return Math.min(delay.getDelayMillis(), params.maxDelay);
        } else {
            return delay.getDelayMillis();
        }
    }

    public static class RetryParams {
        int maxRetries;
        long initialDelay;
        long maxDelay;
        DelayPolicy policy;

        public static enum DelayPolicy {
            constant, linear, exponential;
        }

        public void setMaxRetries(int retries) { this.maxRetries = retries; }
        public void setInitialDelay(long delay) { this.initialDelay = delay; }
        public void setMaxDelay(long delay) { this.maxDelay = delay; }
        public void setDelayPolicy(DelayPolicy policy) { this.policy = policy; }
    }


    private static abstract class Delay {
        abstract long getDelayMillis();
    }

    private class ConstantDelay extends Delay {
        private long delay;

        ConstantDelay(long delay) {
            this.delay = delay;
        }
        @Override
        long getDelayMillis() {
            return delay;
        }
    }

    private class LinearBackoff extends Delay {
        private static final double MULTIPLIER = 1;
        private long initial;

        LinearBackoff(long initial) {
            this.initial = initial;
        }

        @Override
        long getDelayMillis() {
            return initial + (long) (MULTIPLIER * initial * (numRetries));
        }
    }

    private class ExponentialBackoff extends Delay {
        private static final double BACKOFF_BASE = 2;
        private long initial;

        ExponentialBackoff(long initial) {
            this.initial = initial;
        }

        @Override
        long getDelayMillis() {
            return (long) (initial * Math.pow(BACKOFF_BASE, (numRetries)));
        }
    }
}
