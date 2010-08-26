/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SaveDocument extends CreateMessage {

    private String mFilename;
    private String mMimeType;
    private String mAuthor;
    private byte mItemType;
    private String mDescription;

    public SaveDocument() {
    }

    public SaveDocument(long mailboxId, String digest, int msgSize, int folderId) {
        super(mailboxId, ":API:", false, digest, msgSize, folderId, true, 0, null);
    }

    @Override public int getOpCode() {
        return OP_SAVE_DOCUMENT;
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

    public byte getItemType() {
        return mItemType;
    }

    public void setItemType(byte type) {
        mItemType = type;
    }
    
    public String getDescription() {
        return mDescription;
    }
    
    public void setDescription(String d) {
        mDescription = d;
    }

    public void setDocument(ParsedDocument doc) {
        setFilename(doc.getFilename());
        setMimeType(doc.getContentType());
        setAuthor(doc.getCreator());
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(mFilename);
        out.writeUTF(mMimeType);
        out.writeUTF(mAuthor);
        out.writeByte(mItemType);
        if (getVersion().atLeast(1, 29))
            out.writeUTF(mDescription);
        super.serializeData(out);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mFilename = in.readUTF();
        mMimeType = in.readUTF();
        mAuthor = in.readUTF();
        mItemType = in.readByte();
        if (getVersion().atLeast(1, 29))
            mDescription = in.readUTF();
        super.deserializeData(in);
    }

    @Override public void redo() throws Exception {
        long mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);

        try {
            mbox.createDocument(getOperationContext(), getFolderId(), mFilename, mMimeType, mAuthor, mDescription, getAdditionalDataStream(), mItemType);
        } catch (MailServiceException e) {
            if (e.getCode() == MailServiceException.ALREADY_EXISTS) {
                mLog.info("Document " + getMessageId() + " is already in mailbox " + mboxId);
                return;
            } else {
                throw e;
            }
        }

    }
}
