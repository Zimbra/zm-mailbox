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
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class AddDocumentRevision extends SaveDocument {
	private int mDocId;
	
	public AddDocumentRevision() {
	}
	
    public AddDocumentRevision(int mailboxId, String digest, int msgSize, int folderId) {
        super(mailboxId, digest, msgSize, folderId);
    }

    public int getOpCode() {
        return OP_ADD_DOCUMENT_REVISION;
    }

    public void setDocId(int docId) {
    	mDocId = docId;
    }
    
    public int getDocId() {
    	return mDocId;
    }
    
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mDocId);
        super.serializeData(out);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mDocId = in.readInt();
        super.deserializeData(in);
    }
    
    public void redo() throws Exception {
    	OperationContext octxt = getOperationContext();
        int mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);
        try {
            mbox.addDocumentRevision(octxt, mDocId, MailItem.TYPE_DOCUMENT, getInputStream(), getAuthor());
        } catch (MailServiceException e) {
            if (e.getCode() == MailServiceException.ALREADY_EXISTS) {
                mLog.info("Document revision " + getMessageId() + " is already in mailbox " + mboxId);
                return;
            } else {
                throw e;
            }
        }
    }
}
