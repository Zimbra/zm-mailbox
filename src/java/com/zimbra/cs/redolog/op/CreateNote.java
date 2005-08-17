/*
 * Created on 2004. 12. 14.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Note;

/**
 * @author jhahm
 */
public class CreateNote extends RedoableOp {

    private int mId;
    private int mFolderId;
    private String mContent;
    private byte mColor;
    private Note.Rectangle mBounds;

    public CreateNote() {
        mId = UNKNOWN_ID;
        mFolderId = UNKNOWN_ID;
    }

    public CreateNote(int mailboxId, int folderId,
                      String content, byte color, Note.Rectangle bounds) {
        setMailboxId(mailboxId);
        mId = UNKNOWN_ID;
        mFolderId = folderId;
        mContent = content != null ? content : "";
        mColor = color;
        mBounds = bounds;
    }

    public int getNoteId() {
        return mId;
    }

    public void setNoteId(int id) {
        mId = id;
    }

    public int getOpCode() {
        return OP_CREATE_NOTE;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mId);
        sb.append(", folder=").append(mFolderId);
        sb.append(", content=").append(mContent);
        sb.append(", color=").append(mColor);
        if (mBounds != null)
            sb.append(", bounds=(").append(mBounds).append(")");
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mId);
        out.writeInt(mFolderId);
        writeUTF8(out, mContent);
        out.writeByte(mColor);
        out.writeInt(mBounds.x);
        out.writeInt(mBounds.y);
        out.writeInt(mBounds.width);
        out.writeInt(mBounds.height);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mId = in.readInt();
        mFolderId = in.readInt();
        mContent = readUTF8(in);
        mColor = in.readByte();
        int x = in.readInt();
        int y = in.readInt();
        int w = in.readInt();
        int h = in.readInt();
        mBounds = new Note.Rectangle(x, y, w, h);
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = Mailbox.getMailboxById(mboxId);
        try {
            mailbox.createNote(getOperationContext(), mContent, mBounds, mColor, mFolderId);
        } catch (MailServiceException e) {
            String code = e.getCode();
            if (code.equals(MailServiceException.ALREADY_EXISTS)) {
                if (mLog.isInfoEnabled())
                    mLog.info("Note " + mId + " already exists in mailbox " + mboxId);
            } else
                throw e;
        }
    }
}
