/*
 * Created on Nov 12, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author dkarp
 */
public class SetFolderUrl extends RedoableOp {

    private int mFolderId;
    private String mURL;

    public SetFolderUrl() {
        mFolderId = Mailbox.ID_AUTO_INCREMENT;
        mURL = "";
    }

    public SetFolderUrl(int mailboxId, int folderId, String url) {
        setMailboxId(mailboxId);
        mFolderId = folderId;
        mURL = url == null ? "" : url;
    }

    public int getOpCode() {
        return OP_SET_URL;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mFolderId);
        sb.append(", url=").append(mURL);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mFolderId);
        writeUTF8(out, mURL);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mFolderId = in.readInt();
        mURL = readUTF8(in);
    }

    public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        mbox.setFolderUrl(getOperationContext(), mFolderId, mURL);
    }
}
