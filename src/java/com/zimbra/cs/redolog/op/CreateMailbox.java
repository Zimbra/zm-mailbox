/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 11. 2.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.redolog.RedoException;

/**
 * @author jhahm
 */
public class CreateMailbox extends RedoableOp {

	private String mAccountId;

	public CreateMailbox() {
	}

	public CreateMailbox(String accountId) {
		mAccountId = accountId;
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#getOperationCode()
	 */
	public int getOpCode() {
		return OP_CREATE_MAILBOX;
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#getPrintableData()
	 */
	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("account=").append(mAccountId != null ? mAccountId : "");
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#serializeData(java.io.DataOutput)
	 */
	protected void serializeData(DataOutput out) throws IOException {
		writeUTF8(out, mAccountId);
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#deserializeData(java.io.DataInput)
	 */
	protected void deserializeData(DataInput in) throws IOException {
		mAccountId = readUTF8(in);
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#redo()
	 */
	public void redo() throws Exception {
		int opMboxId = getMailboxId();
		Mailbox mbox = null;
		try {
			mbox = Mailbox.getMailboxByAccountId(mAccountId);
			int mboxId = mbox.getId();
			if (opMboxId == mboxId) {
				mLog.info("Mailbox " + opMboxId + " for account " + mAccountId + " already exists");
				return;
			} else
				throw new RedoException(
						"Mailbox for account " + mAccountId +
						" already exists, but with wrong mailbox id value of " + mboxId +
						"; was expecting mailbox id of " + opMboxId, this);
		} catch (MailServiceException e) {
			if (e.getCode() != MailServiceException.NO_SUCH_MBOX)
				throw e;
		}

		Account account = Provisioning.getInstance().getAccountById(mAccountId);
		if (account == null)
			throw new RedoException("Account " + mAccountId + " does not exist", this);

		mbox = Mailbox.createMailbox(getOperationContext(), account);
	}
}
