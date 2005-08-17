/*
 * Created on 2004. 7. 23.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.redolog.TransactionId;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class ControlOp extends RedoableOp {

	public ControlOp() {
	}

	public ControlOp(TransactionId txnId) {
		setTransactionId(txnId);
        setTimestamp(System.currentTimeMillis());
	}

	public void redo() throws Exception {
		// do nothing
	}

	public void commit() {
		// do nothing
	}

	public void abort() {
		// do nothing
	}

	protected String getPrintableData() {
		return null;
	}

	protected void serializeData(DataOutput out) throws IOException {
		// nothing to do
	}

	protected void deserializeData(DataInput in) throws IOException {
		// nothing to do
	}
}
