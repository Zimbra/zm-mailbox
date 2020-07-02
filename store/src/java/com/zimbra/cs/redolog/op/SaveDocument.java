/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.store.Blob;

public class SaveDocument extends CreateMessage {

    private String mUuid;
    private String mFilename;
    private String mMimeType;
    private String mAuthor;
    private MailItem.Type type;
    private String mDescription;
    private boolean mDescEnabled;

    public SaveDocument() {
        mOperation = MailboxOperation.SaveDocument;
    }

    public SaveDocument(int mailboxId, String digest, long msgSize, int folderId, int flags) {
        super(mailboxId, ":API:", false, digest, msgSize, folderId, true, flags, null);
        mOperation = MailboxOperation.SaveDocument;
    }

    public String getUuid() {
        return mUuid;
    }

    public void setUuid(String uuid) {
        mUuid = uuid;
    }

    public String getFilename() {
        return mFilename;
    }

    public void setFilename(String filename) {
        mFilename = filename;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setMimeType(String mimeType) {
        mMimeType = mimeType;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public void setAuthor(String a) {
        mAuthor = a;
    }

    public MailItem.Type getItemType() {
        return type;
    }

    public void setItemType(MailItem.Type type) {
        this.type = type;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String d) {
        mDescription = d;
    }

    public boolean isDescriptionEnabled() {
        return mDescEnabled;
    }

    public void setDescriptionEnabled(boolean descEnabled) {
        mDescEnabled = descEnabled;
    }

    public void setDocument(ParsedDocument doc) {
        setFilename(doc.getFilename());
        setMimeType(doc.getContentType());
        setAuthor(doc.getCreator());
        setDescription(doc.getDescription());
        setDescriptionEnabled(doc.isDescriptionEnabled());
    }

    @Override protected String getPrintableData() {
        return "uuid=" + mUuid + ", " + super.getPrintableData();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(mFilename);
        out.writeUTF(mMimeType);
        out.writeUTF(mAuthor);
        out.writeByte(type.toByte());
        if (getVersion().atLeast(1, 29)) {
            out.writeUTF(mDescription);
        }
        if (getVersion().atLeast(1, 31)) {
            out.writeBoolean(mDescEnabled);
        }
        if (getVersion().atLeast(1, 37)) {
            out.writeUTF(mUuid);
        }
        super.serializeData(out);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mFilename = in.readUTF();
        mMimeType = in.readUTF();
        mAuthor = in.readUTF();
        type = MailItem.Type.of(in.readByte());
        if (getVersion().atLeast(1, 29)) {
            mDescription = in.readUTF();
        }
        if (getVersion().atLeast(1, 31)) {
            mDescEnabled = in.readBoolean();
        } else {
            mDescEnabled = true;
        }
        if (getVersion().atLeast(1, 37)) {
            mUuid = in.readUTF();
        }
        super.deserializeData(in);
    }

    @Override
    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        try {
            ParsedDocument pd;
            if (mMsgBodyType == MSGBODY_EXTERNAL) {
                Blob blob = getExternalBlob();
                pd = new ParsedDocument(blob, mFilename, mMimeType,
                        System.currentTimeMillis(), mAuthor, mDescription, mDescEnabled);
            } else {
                pd = new ParsedDocument(getAdditionalDataStream(), mFilename, mMimeType,
                        System.currentTimeMillis(), mAuthor, mDescription, mDescEnabled);
            }
            mbox.createDocument(getOperationContext(), getFolderId(), pd, type, getFlags());
        } catch (MailServiceException e) {
            if (e.getCode() == MailServiceException.ALREADY_EXISTS) {
                mLog.info("Document " + getMessageId() + " is already in mailbox " + mbox.getId());
                return;
            } else {
                throw e;
            }
        }

    }
}
