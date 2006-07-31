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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SaveDocument extends CreateMessage {

	private String mFilename;
	private String mMimeType;
	private String mAuthor;
	
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
    
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(mFilename);
        out.writeUTF(mMimeType);
        out.writeUTF(mAuthor);
        super.serializeData(out);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mFilename = in.readUTF();
        mMimeType = in.readUTF();
        mAuthor = in.readUTF();
        super.deserializeData(in);
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = Mailbox.getMailboxById(mboxId);
        mbox.createDocument(getOperationContext(), 
							getFolderId(), 
							mFilename, 
							mMimeType,
							mAuthor,
							getMessageBody(),
							null);
    }
}
