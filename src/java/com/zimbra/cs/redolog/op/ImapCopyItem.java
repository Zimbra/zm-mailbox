package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;

public class ImapCopyItem extends RedoableOp {

    private int mSrcId;
    private int mDestId;
    private byte mType;
    private int mSrcImapId = UNKNOWN_ID;
    private int mDestFolderId;
    private short mDestVolumeId = -1;

    public ImapCopyItem() {
        mSrcId = UNKNOWN_ID;
        mDestId = UNKNOWN_ID;
        mType = MailItem.TYPE_UNKNOWN;
        mDestFolderId = 0;
    }

    public ImapCopyItem(int mailboxId, int msgId, byte type, int destId) {
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

    /**
     * Sets the volume ID for the copied blob.
     * @param volId
     */
    public void setDestVolumeId(short volId) {
        mDestVolumeId = volId;
    }

    public short getDestVolumeId() {
        return mDestVolumeId;
    }

    /**
     * Sets the IMAP UID for the moved item.
     * @param volId
     */
    public void setSrcImapId(int srcImapId) {
        mSrcImapId = srcImapId;
    }

    public int getSrcImapId() {
        return mSrcImapId;
    }

    public int getOpCode() {
        return OP_IMAP_COPY_ITEM;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("srcId=");
        sb.append(mSrcId);
        sb.append(", destId=").append(mDestId);
        sb.append(", type=").append(mType);
        sb.append(", destFolder=").append(mDestFolderId);
        sb.append(", srcImapId=").append(mSrcImapId);
        sb.append(", destVolumeId=").append(mDestVolumeId);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mSrcId);
        out.writeInt(mDestId);
        out.writeByte(mType);
        out.writeInt(mDestFolderId);
        out.writeInt(mSrcImapId);
        out.writeShort(mDestVolumeId);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mSrcId = in.readInt();
        mDestId = in.readInt();
        mType = in.readByte();
        mDestFolderId = in.readInt();
        mSrcImapId = in.readInt();
        mDestVolumeId = in.readShort();
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
