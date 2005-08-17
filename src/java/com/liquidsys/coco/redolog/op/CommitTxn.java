/*
 * Created on 2004. 7. 22.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.liquidsys.coco.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class CommitTxn extends ControlOp {

    int mTxnOpCode;

    public CommitTxn() {
        mTxnOpCode = OP_UNKNOWN;
	}

    public CommitTxn(RedoableOp changeEntry) {
    	super(changeEntry.getTransactionId());
        setMailboxId(changeEntry.getMailboxId());
        mTxnOpCode = changeEntry.getOpCode();
    }

	public int getOpCode() {
		return OP_COMMIT_TXN;
	}

    public int getTxnOpCode() {
        return mTxnOpCode;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("txnType=");
        sb.append(getOpClassName(mTxnOpCode));
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mTxnOpCode);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mTxnOpCode = in.readInt();
    }
}
