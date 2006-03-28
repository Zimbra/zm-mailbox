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

package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.ServiceException;

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

    public int getDocId() {
    	return mDocId;
    }
    
    public void setDocument(Document doc) {
    	mDocId = doc.getId();
    	setFilename(doc.getFilename());
    	setMimeType(doc.getContentType());
    }
    
    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mDocId);
        super.serializeData(out);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mDocId = in.readInt();
        super.deserializeData(in);
    }
    
    public void redo() throws Exception {
    	OperationContext octxt = getOperationContext();
        int mboxId = getMailboxId();
        Mailbox mbox = Mailbox.getMailboxById(mboxId);
        MailItem doc = mbox.getItemById(octxt, mDocId, MailItem.TYPE_UNKNOWN);
        if (doc.getType() != MailItem.TYPE_WIKI &&
        	doc.getType() != MailItem.TYPE_DOCUMENT) {
        	throw ServiceException.FAILURE("invalid MailItem type "+doc.getType(), null);
        }
        mbox.addDocumentRevision(octxt, 
							(Document)doc, 
							getMessageBody(),
							getAuthor());
    }
}
