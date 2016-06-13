/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 12. 14.
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.common.mailbox.Color;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class CreateNote extends RedoableOp {

    private int mId;
    private int mFolderId;
    private String mContent;
    private long mColor;
    private Note.Rectangle mBounds;

    public CreateNote() {
        super(MailboxOperation.CreateNote);
        mId = UNKNOWN_ID;
        mFolderId = UNKNOWN_ID;
    }

    public CreateNote(int mailboxId, int folderId,
                      String content, Color color, Note.Rectangle bounds) {
        this();
        setMailboxId(mailboxId);
        mId = UNKNOWN_ID;
        mFolderId = folderId;
        mContent = content != null ? content : "";
        mColor = color.getValue();
        mBounds = bounds;
    }

    public int getNoteId() {
        return mId;
    }

    public void setNoteId(int id) {
        mId = id;
    }

    @Override protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mId);
        sb.append(", folder=").append(mFolderId);
        sb.append(", content=").append(mContent);
        sb.append(", color=").append(mColor);
        if (mBounds != null)
            sb.append(", bounds=(").append(mBounds).append(")");
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeInt(mFolderId);
        out.writeShort((short) -1);
        out.writeUTF(mContent);
        // mColor from byte to long in Version 1.27
        out.writeLong(mColor);
        out.writeInt(mBounds.x);
        out.writeInt(mBounds.y);
        out.writeInt(mBounds.width);
        out.writeInt(mBounds.height);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mFolderId = in.readInt();
        in.readShort();
        mContent = in.readUTF();
        if (getVersion().atLeast(1, 27))
            mColor = in.readLong();
        else
            mColor = in.readByte();
        int x = in.readInt();
        int y = in.readInt();
        int w = in.readInt();
        int h = in.readInt();
        mBounds = new Note.Rectangle(x, y, w, h);
    }

    @Override public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);

        try {
            mailbox.createNote(getOperationContext(), mContent, mBounds, Color.fromMetadata(mColor), mFolderId);
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
