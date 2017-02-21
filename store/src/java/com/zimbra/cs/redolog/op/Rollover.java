/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2005. 1. 12.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.redolog.op;

import java.io.File;
import java.io.IOException;

import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

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
        super(MailboxOperation.Rollover);
        mSeq = 0;
    }

    public Rollover(File logfile, long seq) {
        this();
        mSeq = seq;
    	mFilename = logfile.getName();
    }

    public long getSequence() {
    	return mSeq;
    }

    public String getFilename() {
    	return mFilename;
    }

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#redo()
	 */
	public void redo() throws Exception {
        // nothing to do
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#getPrintableData()
	 */
	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("seq=");
        sb.append(mSeq);
        sb.append(", filename=").append(mFilename);
        return sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#serializeData(java.io.RedoLogOutput)
	 */
	protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeLong(mSeq);
        out.writeUTF(mFilename);
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#deserializeData(java.io.RedoLogInput)
	 */
	protected void deserializeData(RedoLogInput in) throws IOException {
        mSeq = in.readLong();
        mFilename = in.readUTF();
	}
}
