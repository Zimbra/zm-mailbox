/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.servlet.continuation.ResumeContinuationListener;

public class WaitSetCallback {

    public boolean completed = false;
    public boolean canceled;
    public HashMap<String /* accountId */, WaitSetSession> signalledSessions;
    public Set<String> signalledAccounts;
    public Map<String /*accountId*/, PendingModifications> pendingModifications;
    public IWaitSet waitSet;
    public String seqNo;
    public IWaitSet ws;
    public List<WaitSetError> errors = Lists.newArrayList();
    public ResumeContinuationListener continuationResume;
    public CountDownLatch completedLatch;

    public void dataReady(IWaitSet wset, String seqNum, boolean setCanceled, List<WaitSetError> inErrors,
            Set<WaitSetSession> signalledSessions, Set<String> signalledAccounts, Map<String /*accountId*/, PendingModifications> pms) {
        boolean trace = ZimbraLog.session.isTraceEnabled();
        synchronized(this) {
            if (inErrors != null && inErrors.size() > 0) {
                errors.addAll(inErrors);
            }
            this.waitSet = wset;
            this.canceled = setCanceled;
            if(signalledSessions == null) {
                this.signalledSessions =  Maps.newHashMapWithExpectedSize(0);
            } else {
                this.signalledSessions = Maps.newHashMapWithExpectedSize(signalledSessions.size());
                for(WaitSetSession s : signalledSessions) {
                    this.signalledSessions.put(s.getTargetAccountId(), s);
                }
            }

            this.signalledAccounts = (signalledAccounts == null) ? Sets.newHashSetWithExpectedSize(0)
                    : Sets.newCopyOnWriteArraySet(signalledAccounts);
            if(pms != null) {
                this.pendingModifications = Maps.newHashMapWithExpectedSize(pms.size());
                this.pendingModifications.putAll(pms);
            } else {
                this.pendingModifications = Maps.newHashMapWithExpectedSize(0);
            }
            this.seqNo = seqNum;
            this.completed = true;
            if (completedLatch != null) {
                completedLatch.countDown();
            }
            ZimbraLog.session.debug("dataReady called. %s", this);
            if (continuationResume != null) {
                if (trace) {
                    ZimbraLog.session.trace("dataReady calling resumeIfSuspended(). %s", this);
                }
                continuationResume.resumeIfSuspended();
            }
        }
        if (trace) {
            ZimbraLog.session.trace("dataReady DONE. %s", this);
        }
    }
    public void dataReadySetCanceled(IWaitSet wset, String seqNum) {
        dataReady(wset, seqNum, true, null, null, null, null);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("seqNo", seqNo)
                .add("completed", completed)
                .add("canceled", canceled)
                .add("signalledAccounts", signalledAccounts)
                .add("errors", errors)
                .add("hashCode()", hashCode())
                .toString();
    }
}
