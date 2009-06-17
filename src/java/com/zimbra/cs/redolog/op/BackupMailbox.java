/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 11. 15.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * THIS REDOLOG OPERATION IS DEPRECATED.  Backing up a mailbox will no longer
 * log this operation in redolog.
 *
 * This operation is a marker within a redo log file to help locate the
 * redo log file that corresponds to a particular backup.  When restoring
 * a mailbox from backup, the mailbox is reinitialized first, the data
 * from the most recent backup is restored, and finally all redos since
 * that backup should be replayed.  This marker helps us determine where
 * to start doing the redos.
 */
public class BackupMailbox extends RedoableOp {

    private long mBackupSetTstamp;  // timestamp of when backup set started (backup set = one or more mailboxes)
    private long mStartTime;        // timestamp of when backup of this mailbox started
    private long mEndTime;          // when backup of this mailbox finished (probably not that important)
    private String mLabel;          // any random label/description for this backup

    public BackupMailbox() {
    }

    public BackupMailbox(long mailboxId, long backupSetTstamp, long startTime, long endTime, String label) {
        setMailboxId(mailboxId);
        mBackupSetTstamp = backupSetTstamp;
        mStartTime = startTime;
        mEndTime = endTime;
        mLabel = label;
    }

    @Override public int getOpCode() {
        return OP_DEPRECATED_BACKUP_MAILBOX;
    }

    @Override public void redo() throws Exception {
        // Nothing to do.  This operation only serves as a marker within a
        // redo log file to find the correct starting log to replay after
        // restoring a particular backup.
    }

    @Override protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("backupSetTstamp=");
        sb.append(mBackupSetTstamp).append(", startTime=").append(mStartTime);
        sb.append(", endTime=").append(mEndTime);
        if (mLabel != null)
            sb.append("label=\"").append(mLabel).append("\"");
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeLong(mBackupSetTstamp);
        out.writeLong(mStartTime);
        out.writeLong(mEndTime);
        out.writeUTF(mLabel != null ? mLabel : "");
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mBackupSetTstamp = in.readLong();
        mStartTime = in.readLong();
        mEndTime = in.readLong();
        mLabel = in.readUTF();
    }

}
