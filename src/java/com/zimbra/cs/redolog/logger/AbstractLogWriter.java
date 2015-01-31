/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.redolog.logger;

import java.io.IOException;

import com.zimbra.cs.redolog.CommitId;
import com.zimbra.cs.redolog.RedoCommitCallback;
import com.zimbra.cs.redolog.RedoLogManager;
import com.zimbra.cs.redolog.op.CommitTxn;
import com.zimbra.cs.redolog.op.RedoableOp;


public abstract class AbstractLogWriter implements LogWriter {

    private CommitNotifyQueue commitNotifyQueue;
    protected RedoLogManager redoLogMgr;

    protected CommitNotifyQueue getCommitNotifyQueue() {
        return commitNotifyQueue;
    }

    protected void setCommitNotifyQueue(CommitNotifyQueue commitNotifyQueue) {
        this.commitNotifyQueue = commitNotifyQueue;
    }

    public AbstractLogWriter(RedoLogManager redoLogMgr) {
        super();
        this.redoLogMgr = redoLogMgr;
    }

    public AbstractLogWriter(RedoLogManager redoLogMgr, CommitNotifyQueue commitNotifyQueue) {
        super();
        this.redoLogMgr = redoLogMgr;
        this.commitNotifyQueue = commitNotifyQueue;
    }

    protected void notifyCallback(RedoableOp op) throws IOException {
        if (op instanceof CommitTxn) {
            CommitTxn cmt = (CommitTxn) op;
            RedoCommitCallback cb = cmt.getCallback();
            if (cb != null) {
                long redoSeq = redoLogMgr.getRolloverManager().getCurrentSequence();
                CommitId cid = new CommitId(redoSeq, (CommitTxn) op);
                Notif notif = new Notif(cb, cid);
                // We queue it instead making the callback right away.
                // Call it only after the commit record has been fsynced.
                getCommitNotifyQueue().push(notif);
            }
        }
    }
}
