/*
 * Created on 2004. 12. 13.
 */
package com.liquidsys.coco.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.liquidsys.coco.mailbox.MailServiceException;
import com.liquidsys.coco.mailbox.Mailbox;

/**
 * @author jhahm
 */
public class CreateFolder extends RedoableOp {

    private String mName;
    private byte mAttrs;
    private int mFolderIds[];

    public CreateFolder() { }

    public CreateFolder(int mailboxId, String name, byte attrs) {
        setMailboxId(mailboxId);
        mName = name != null ? name : "";
        mAttrs = attrs;
    }

    public int[] getFolderIds() {
        return mFolderIds;
    }

    public void setFolderIds(int parentIds[]) {
        mFolderIds = parentIds;
    }

    public int getOpCode() {
        return OP_CREATE_FOLDER;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("name=").append(mName);
        sb.append(", attrs=").append(mAttrs);
        if (mFolderIds != null) {
            sb.append(", folderIds=[");
            for (int i = 0; i < mFolderIds.length; i++) {
                sb.append(mFolderIds[i]);
                if (i < mFolderIds.length - 1)
                    sb.append(", ");
            }
            sb.append("]");
        }
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        writeUTF8(out, mName);
        out.writeByte(mAttrs);
        if (mFolderIds != null) {
            out.writeInt(mFolderIds.length);
            for (int i = 0; i < mFolderIds.length; i++)
                out.writeInt(mFolderIds[i]);
        } else
            out.writeInt(0);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mName = readUTF8(in);
        mAttrs = in.readByte();
        int numParentIds = in.readInt();
        if (numParentIds > 0) {
            mFolderIds = new int[numParentIds];
            for (int i = 0; i < numParentIds; i++)
                mFolderIds[i] = in.readInt();
        }
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = Mailbox.getMailboxById(mboxId);
        try {
            mailbox.createFolder(getOperationContext(), mName, mAttrs);
        } catch (MailServiceException e) {
            String code = e.getCode();
            if (code.equals(MailServiceException.ALREADY_EXISTS)) {
                if (mLog.isInfoEnabled())
                    mLog.info("Folder " + mName + " already exists in mailbox " + mboxId);
            } else
                throw e;
        }
    }
}
