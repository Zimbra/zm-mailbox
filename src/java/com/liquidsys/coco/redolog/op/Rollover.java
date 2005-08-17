/*
 * Created on 2005. 1. 12.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Rollover extends RedoableOp {

    long mSeq;
    String mFilename;

    public Rollover() {
        mSeq = 0;
    }

    public Rollover(File logfile, long seq) {
        mSeq = seq;
    	mFilename = logfile.getName();
    }

    public long getSequence() {
    	return mSeq;
    }

    public String getFilename() {
    	return mFilename;
    }

	public int getOpCode() {
		return OP_ROLLOVER;
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
        StringBuffer sb = new StringBuffer("seq=");
        sb.append(mSeq);
        sb.append(", filename=").append(mFilename);
        return sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.op.RedoableOp#serializeData(java.io.DataOutput)
	 */
	protected void serializeData(DataOutput out) throws IOException {
        out.writeLong(mSeq);
        writeUTF8(out, mFilename);
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.op.RedoableOp#deserializeData(java.io.DataInput)
	 */
	protected void deserializeData(DataInput in) throws IOException {
        mSeq = in.readLong();
        mFilename = readUTF8(in);
	}
}
