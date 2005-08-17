/*
 * Created on 2005. 5. 31.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;


/**
 * @author jhahm
 */
public class CopyItem extends RedoableOp {

    private int mSrcId;
    private int mDestId;
    private byte mType;
    private int mDestFolderId;

    public CopyItem() {
        mSrcId = UNKNOWN_ID;
        mDestId = UNKNOWN_ID;
        mType = MailItem.TYPE_UNKNOWN;
        mDestFolderId = 0;
    }

    public CopyItem(int mailboxId, int msgId, byte type, int destId) {
        setMailboxId(mailboxId);
        mSrcId = msgId;
        mType = type;
        mDestFolderId = destId;
    }

    /**
     * Sets the ID of the copied item.
     * @param destId
     */
    public void setDestId(int destId) {
    	mDestId = destId;
    }

    public int getDestId() {
    	return mDestId;
    }

    public int getOpCode() {
        return OP_COPY_ITEM;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("srcId=");
        sb.append(mSrcId);
        sb.append(", destId=").append(mDestId);
        sb.append(", type=").append(mType);
        sb.append(", destFolder=").append(mDestFolderId);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mSrcId);
        out.writeInt(mDestId);
        out.writeByte(mType);
        out.writeInt(mDestFolderId);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mSrcId = in.readInt();
        mDestId = in.readInt();
        mType = in.readByte();
        mDestFolderId = in.readInt();
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = Mailbox.getMailboxById(mboxId);
        try {
            mbox.copy(getOperationContext(), mSrcId, mType, mDestFolderId);
        } catch (MailServiceException e) {
            if (e.getCode() == MailServiceException.ALREADY_EXISTS) {
                mLog.info("Item " + mDestId + " is already in mailbox " + mboxId);
                return;
            } else
                throw e;
        }
    }
}
