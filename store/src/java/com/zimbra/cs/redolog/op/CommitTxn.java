/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
 * Created on 2004. 7. 22.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoCommitCallback;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class CommitTxn extends ControlOp {

    private MailboxOperation mTxnOpCode;

    public CommitTxn() {
        super(MailboxOperation.CommitTxn);
	}

    public CommitTxn(RedoableOp changeEntry) {
    	super(MailboxOperation.CommitTxn, changeEntry.getTransactionId());
        setMailboxId(changeEntry.getMailboxId());
        mTxnOpCode = changeEntry.getOperation();
        mCommitCallback = changeEntry.getCommitCallback();
    }

    public MailboxOperation getTxnOpCode() {
        return mTxnOpCode;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("txnType=");
        sb.append(mTxnOpCode.name());
        return sb.toString();
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mTxnOpCode.getCode());
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mTxnOpCode = MailboxOperation.fromInt(in.readInt());
    }

    /**
     * Returns the callback object that was passed in at transaction start time.
     * @return
     */
    public RedoCommitCallback getCallback() {
        return mCommitCallback;
    }
}
