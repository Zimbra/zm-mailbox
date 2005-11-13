/*
 * Created on Nov 12, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;

import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author dkarp
 */
public class TrackSync extends RedoableOp {

    public TrackSync() {
    }

    public TrackSync(int mailboxId) {
        setMailboxId(mailboxId);
    }

    public int getOpCode() {
        return OP_TRACK_SYNC;
    }

    protected String getPrintableData() {
        // no members to print
        return null;
    }

    protected void serializeData(DataOutput out) {
        // no members to serialize
    }

    protected void deserializeData(DataInput in) {
        // no members to deserialize
    }

    public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        mbox.beginTrackingSync(getOperationContext());
    }
}
