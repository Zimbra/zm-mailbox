/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 11. 2.
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.MailboxIdConflictException;
import com.zimbra.cs.redolog.RedoException;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

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
	 * @see com.zimbra.cs.redolog.op.RedoableOp#serializeData(java.io.RedoLogOutput)
	 */
	protected void serializeData(RedoLogOutput out) throws IOException {
		out.writeUTF(mAccountId);
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#deserializeData(java.io.RedoLogInput)
	 */
	protected void deserializeData(RedoLogInput in) throws IOException {
		mAccountId = in.readUTF();
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#redo()
	 */
	public void redo() throws Exception {
		int opMboxId = getMailboxId();
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId, false);
		if (mbox != null) {
			int mboxId = mbox.getId();
			if (opMboxId == mboxId) {
				mLog.info("Mailbox " + opMboxId + " for account " + mAccountId + " already exists");
				return;
			} else {
			    throw new MailboxIdConflictException(mAccountId, opMboxId, mboxId, this);
			}
		} else {
    		Account account = Provisioning.getInstance().get(AccountBy.id, mAccountId);
    		if (account == null)
    			throw new RedoException("Account " + mAccountId + " does not exist", this);
    
    		mbox = MailboxManager.getInstance().createMailbox(getOperationContext(), account);
		}
	}
}
