/*
 * Created on 2004. 11. 15.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author jhahm
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

    public BackupMailbox(int mailboxId, long backupSetTstamp, long startTime, long endTime, String label) {
        setMailboxId(mailboxId);
        mBackupSetTstamp = backupSetTstamp;
        mStartTime = startTime;
        mEndTime = endTime;
        mLabel = label;
    }

    /* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#getOperationCode()
	 */
	public int getOpCode() {
		return OP_BACKUP_MAILBOX;
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#redo()
	 */
	public void redo() throws Exception {
        // Nothing to do.  This operation only serves as a marker within a
        // redo log file to find the correct starting log to replay after
        // restoring a particular backup.
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#getPrintableData()
	 */
	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("backupSetTstamp=");
        sb.append(mBackupSetTstamp).append(", startTime=").append(mStartTime);
        sb.append(", endTime=").append(mEndTime);
        if (mLabel != null)
        	sb.append("label=\"").append(mLabel).append("\"");
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#serializeData(java.io.DataOutput)
	 */
	protected void serializeData(DataOutput out) throws IOException {
        out.writeLong(mBackupSetTstamp);
        out.writeLong(mStartTime);
        out.writeLong(mEndTime);
        writeUTF8(out, mLabel != null ? mLabel : "");
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#deserializeData(java.io.DataInput)
	 */
	protected void deserializeData(DataInput in) throws IOException {
        mBackupSetTstamp = in.readLong();
        mStartTime = in.readLong();
        mEndTime = in.readLong();
        mLabel = readUTF8(in);
	}

}
