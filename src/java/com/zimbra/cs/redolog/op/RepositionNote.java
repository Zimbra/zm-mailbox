/*
 * Created on 2004. 12. 14.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Note;

/**
 * @author jhahm
 */
public class RepositionNote extends RedoableOp {

    private int mId;
    private Note.Rectangle mBounds;

    public RepositionNote() {
        mId = UNKNOWN_ID;
    }

    public RepositionNote(int mailboxId, int id, Note.Rectangle bounds) {
        setMailboxId(mailboxId);
        mId = id;
        mBounds = bounds;
    }

    public int getOpCode() {
        return OP_REPOSITION_NOTE;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId);
        if (mBounds != null)
            sb.append(", bounds=(").append(mBounds).append(")");
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mId);
        out.writeInt(mBounds.x);
        out.writeInt(mBounds.y);
        out.writeInt(mBounds.width);
        out.writeInt(mBounds.height);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mId = in.readInt();
        int x = in.readInt();
        int y = in.readInt();
        int w = in.readInt();
        int h = in.readInt();
        mBounds = new Note.Rectangle(x, y, w, h);
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = Mailbox.getMailboxById(mboxId);
        mailbox.repositionNote(getOperationContext(), mId, mBounds);
    }
}
