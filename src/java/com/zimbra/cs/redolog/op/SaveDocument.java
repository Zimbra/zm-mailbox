/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SaveDocument extends CreateMessage {

	private String mFilename;
	private String mMimeType;
	private String mAuthor;
	private byte mItemType;
	
	public SaveDocument() {
	}
	
    public SaveDocument(int mailboxId, String digest, int msgSize, int folderId) {
        super(mailboxId, ":API:", false, digest, msgSize, folderId, true, 0, null);
    }

    public int getOpCode() {
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
    
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(mFilename);
        out.writeUTF(mMimeType);
        out.writeUTF(mAuthor);
        out.writeByte(mItemType);
        super.serializeData(out);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mFilename = in.readUTF();
        mMimeType = in.readUTF();
        mAuthor = in.readUTF();
        mItemType = in.readByte();
        super.deserializeData(in);
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);
        try {
            mbox.createDocument(getOperationContext(), getFolderId(), mFilename, mMimeType, mAuthor, getMessageBody(), mItemType);
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
