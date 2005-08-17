/*
 * Created on 2004. 12. 14.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author jhahm
 */
public class EditNote extends RedoableOp {

    private int mId;
    private String mContent;

    public EditNote() {
        mId = UNKNOWN_ID;
    }

    public EditNote(int mailboxId, int id, String content) {
        setMailboxId(mailboxId);
        mId = id;
        mContent = content != null ? content : "";
    }

    public int getOpCode() {
        return OP_EDIT_NOTE;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId).append(", content=").append(mContent);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mId);
        writeUTF8(out, mContent);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mId = in.readInt();
        mContent = readUTF8(in);
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = Mailbox.getMailboxById(mboxId);
        mailbox.editNote(getOperationContext(), mId, mContent);
    }
}
