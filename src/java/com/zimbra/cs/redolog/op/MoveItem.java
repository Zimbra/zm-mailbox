/*
 * Created on 2004. 7. 21.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;


/**
 * @author jhahm
 */
public class MoveItem extends RedoableOp {

	private int mId;
    private byte mType;
	private int mDestId;
    private String mConstraint;

	public MoveItem() {
		mId = UNKNOWN_ID;
        mType = MailItem.TYPE_UNKNOWN;
		mDestId = 0;
        mConstraint = "";
	}

	public MoveItem(int mailboxId, int msgId, byte type, int destId, TargetConstraint tcon) {
		setMailboxId(mailboxId);
		mId = msgId;
        mType = type;
		mDestId = destId;
        mConstraint = (tcon == null ? "" : tcon.toString());
	}

	public int getOpCode() {
		return OP_MOVE_ITEM;
	}

	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId).append(", type=").append(mType);
        sb.append(", dest=").append(mDestId);
        if (mConstraint.length() > 0)
            sb.append(", constraint=").append(mConstraint);
        return sb.toString();
	}

	protected void serializeData(DataOutput out) throws IOException {
		out.writeInt(mId);
        out.writeByte(mType);
		out.writeInt(mDestId);
        out.writeUTF(mConstraint);
	}

	protected void deserializeData(DataInput in) throws IOException {
		mId = in.readInt();
        mType = in.readByte();
		mDestId = in.readInt();
        mConstraint = in.readUTF();
	}

	public void redo() throws Exception {
		int mboxId = getMailboxId();
		Mailbox mbox = Mailbox.getMailboxById(mboxId);

        TargetConstraint tcon = null;
        if (mConstraint != null && mConstraint.length() > 0)
            try {
                tcon = TargetConstraint.parseConstraint(mbox, mConstraint);
            } catch (ServiceException e) {
                mLog.warn(e);
            }

        // No extra checking needed because Mailbox.move() is already idempotent.
        mbox.move(getOperationContext(), mId, mType, mDestId, tcon);
	}
}
