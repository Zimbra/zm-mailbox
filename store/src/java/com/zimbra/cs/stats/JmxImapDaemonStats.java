/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

package com.zimbra.cs.stats;

import com.zimbra.common.stats.DeltaCalculator;

public class JmxImapDaemonStats implements JmxImapDaemonStatsMBean {

    private final DeltaCalculator imapDeltaCalc = new DeltaCalculator(ZimbraPerf.STOPWATCH_IMAP);

    JmxImapDaemonStats() {
    }

    @Override
    public long getImapRequests() {
        return ZimbraPerf.STOPWATCH_IMAP.getCount();
    }

    @Override
    public long getImapResponseMs() {
        return (long) imapDeltaCalc.getRealtimeAverage();
    }

    @Override
    public void reset() {
        imapDeltaCalc.reset();
    }
}