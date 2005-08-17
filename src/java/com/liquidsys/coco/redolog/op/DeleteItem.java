/*
 * Created on 2004. 7. 21.
 */
package com.liquidsys.coco.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.liquidsys.coco.mailbox.MailItem;
import com.liquidsys.coco.mailbox.MailItem.TargetConstraint;
import com.liquidsys.coco.mailbox.MailServiceException;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.service.ServiceException;

/**
 * @author jhahm
 */
public class DeleteItem extends RedoableOp {

	private int mId;
    private byte mType;
    private String mConstraint;

	public DeleteItem() {
		mId = UNKNOWN_ID;
        mType = MailItem.TYPE_UNKNOWN;
        mConstraint = "";
	}

	public DeleteItem(int mailboxId, int id, byte type, TargetConstraint tcon) {
		setMailboxId(mailboxId);
		mId = id;
        mType = type;
        mConstraint = (tcon == null ? "" : tcon.toString());
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.Redoable#getOperationCode()
	 */
	public int getOpCode() {
		return OP_DELETE_ITEM;
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.Redoable#getRedoContent()
	 */
	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId).append(", type=").append(mType);
        if (mConstraint.length() > 0)
            sb.append(", constraint=").append(mConstraint);
        return sb.toString();
	}

	protected void serializeData(DataOutput out) throws IOException {
		out.writeInt(mId);
        out.writeByte(mType);
        out.writeUTF(mConstraint);
	}

	protected void deserializeData(DataInput in) throws IOException {
		mId = in.readInt();
        mType = in.readByte();
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

        try {
    		mbox.delete(getOperationContext(), mId, mType, tcon);
        } catch (MailServiceException.NoSuchItemException e) {
            if (mLog.isInfoEnabled())
                mLog.info("Item " + mId + " was already deleted from mailbox " + mboxId);
        }
	}
}
