/*
 * Created on 2005. 4. 4.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author jhahm
 */
public class DeleteMailbox extends RedoableOp {

    public DeleteMailbox() {
    }

    public DeleteMailbox(int mailboxId) {
        setMailboxId(mailboxId);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#getOpCode()
     */
    public int getOpCode() {
        return OP_DELETE_MAILBOX;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#redo()
     */
    public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        mbox.deleteMailbox(getOperationContext());
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#getPrintableData()
     */
    protected String getPrintableData() {
        // no members to print
        return null;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#serializeData(java.io.DataOutput)
     */
    protected void serializeData(DataOutput out) {
        // no members to serialize
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#deserializeData(java.io.DataInput)
     */
    protected void deserializeData(DataInput in) {
        // no members to deserialize
    }
}
