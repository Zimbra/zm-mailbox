/*
 * Created on 2004. 11. 3.
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
public class HeaderOnlyOp extends RedoableOp {

	private int mOpCode;
	private static final String sPrintable = "(detail skipped)";

	public HeaderOnlyOp(int code) {
		super();
		mOpCode = code;
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.op.RedoableOp#getOperationCode()
	 */
	public int getOpCode() {
		// TODO Auto-generated method stub
		return mOpCode;
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.op.RedoableOp#redo()
	 */
	public void redo() throws Exception {
		// nothing to do
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.op.RedoableOp#getPrintableData()
	 */
	protected String getPrintableData() {
		return sPrintable;
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.op.RedoableOp#serializeData(java.io.DataOutput)
	 */
	protected void serializeData(DataOutput out) throws IOException {
		// nothing to do
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.op.RedoableOp#deserializeData(java.io.DataInput)
	 */
	protected void deserializeData(DataInput in) throws IOException {
		// nothing to do
	}

}
