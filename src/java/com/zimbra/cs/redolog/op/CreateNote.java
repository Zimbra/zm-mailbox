/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

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
    private short mVolumeId = -1;

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

    public void setVolumeId(short volId) {
        mVolumeId = volId;
    }

    public short getVolumeId() {
        return mVolumeId;
    }

    public int getOpCode() {
        return OP_CREATE_NOTE;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mId);
        sb.append(", folder=").append(mFolderId);
        sb.append(", vol=").append(mVolumeId);
        sb.append(", content=").append(mContent);
        sb.append(", color=").append(mColor);
        if (mBounds != null)
            sb.append(", bounds=(").append(mBounds).append(")");
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mId);
        out.writeInt(mFolderId);
        out.writeShort(mVolumeId);
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
        mVolumeId = in.readShort();
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
