/*
 * Created on 2005. 4. 4.
 */
package com.liquidsys.coco.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Mailbox.OperationContext;

/**
 * @author jhahm
 */
public class ReindexMailbox extends RedoableOp {

    public ReindexMailbox() {
    }

    public ReindexMailbox(int mailboxId) {
    	setMailboxId(mailboxId);
    }

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.op.RedoableOp#getOpCode()
	 */
	public int getOpCode() {
		return OP_REINDEX_MAILBOX;
	}

    public boolean deferCrashRecovery() {
        return true;
    }

    /* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.op.RedoableOp#redo()
	 */
	public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        mbox.reIndex(new OperationContext(this));
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
	protected void serializeData(DataOutput out) throws IOException {
        // no members to serialize
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.op.RedoableOp#deserializeData(java.io.DataInput)
	 */
	protected void deserializeData(DataInput in) throws IOException {
        // no members to deserialize
	}
}
