/*
 * Created on 2004. 12. 13.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author jhahm
 */
public class RenameFolder extends RedoableOp {

    private int mId;
    private String mName;
    private int mParentIds[];

    public RenameFolder() {
        mId = UNKNOWN_ID;
    }

    public RenameFolder(int mailboxId, int id, String name) {
        setMailboxId(mailboxId);
        mId = id;
        mName = name != null ? name : "";
    }

    public int[] getParentIds() {
        return mParentIds;
    }

    public void setParentIds(int parentIds[]) {
        mParentIds = parentIds;
    }

    public int getOpCode() {
        return OP_RENAME_FOLDER;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId).append(", name=").append(mName);
        if (mParentIds != null) {
        	sb.append(", destParentIds=[");
            for (int i = 0; i < mParentIds.length; i++) {
            	sb.append(mParentIds[i]);
                if (i < mParentIds.length - 1)
                    sb.append(", ");
            }
            sb.append("]");
        }
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mId);
        writeUTF8(out, mName);
        if (mParentIds != null) {
            out.writeInt(mParentIds.length);
            for (int i = 0; i < mParentIds.length; i++)
                out.writeInt(mParentIds[i]);
        } else
            out.writeInt(0);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mId = in.readInt();
        mName = readUTF8(in);
        int numParentIds = in.readInt();
        if (numParentIds > 0) {
        	mParentIds = new int[numParentIds];
            for (int i = 0; i < numParentIds; i++)
            	mParentIds[i] = in.readInt();
        }
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = Mailbox.getMailboxById(mboxId);
        mailbox.renameFolder(getOperationContext(), mId, mName);
    }
}
