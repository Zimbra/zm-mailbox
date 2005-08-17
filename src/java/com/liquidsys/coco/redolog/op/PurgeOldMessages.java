/*
 * Created on 2005. 4. 4.
 */
package com.liquidsys.coco.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;

import com.liquidsys.coco.mailbox.Mailbox;

/**
 * @author jhahm
 *
 * Purge old messages.  The arguments to this operation are mailbox ID and
 * operation timestamp, both of which are managed by the superclass.  See
 * Mailbox.purgeMessages() for more info.
 */
public class PurgeOldMessages extends RedoableOp {

    public PurgeOldMessages() {
    }

    public PurgeOldMessages(int mailboxId) {
        setMailboxId(mailboxId);
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.redolog.op.RedoableOp#getOpCode()
     */
    public int getOpCode() {
        return OP_PURGE_OLD_MESSAGES;
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.redolog.op.RedoableOp#redo()
     */
    public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        mbox.purgeMessages(getOperationContext());
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.redolog.op.RedoableOp#getPrintableData()
     */
    protected String getPrintableData() {
        // no members to print
        return null;
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.redolog.op.RedoableOp#serializeData(java.io.DataOutput)
     */
    protected void serializeData(DataOutput out) {
        // no members to serialize
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.redolog.op.RedoableOp#deserializeData(java.io.DataInput)
     */
    protected void deserializeData(DataInput in) {
        // no members to deserialize
    }
}
