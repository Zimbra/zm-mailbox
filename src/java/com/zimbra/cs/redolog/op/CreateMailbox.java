/*
 * Created on 2004. 11. 2.
 */
package com.liquidsys.coco.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.mailbox.MailServiceException;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.redolog.RedoException;

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
	 * @see com.liquidsys.coco.redolog.op.RedoableOp#getOperationCode()
	 */
	public int getOpCode() {
		return OP_CREATE_MAILBOX;
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.op.RedoableOp#getPrintableData()
	 */
	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("account=").append(mAccountId != null ? mAccountId : "");
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.op.RedoableOp#serializeData(java.io.DataOutput)
	 */
	protected void serializeData(DataOutput out) throws IOException {
		writeUTF8(out, mAccountId);
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.op.RedoableOp#deserializeData(java.io.DataInput)
	 */
	protected void deserializeData(DataInput in) throws IOException {
		mAccountId = readUTF8(in);
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.op.RedoableOp#redo()
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
