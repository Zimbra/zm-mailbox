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
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SaveWiki extends SaveDocument {

	private String mWikiword;
	
	public SaveWiki() {
	}
	
    public SaveWiki(int mailboxId, String digest, int msgSize, int folderId) {
        super(mailboxId, digest, msgSize, folderId);
    }

    public int getOpCode() {
        return OP_SAVE_WIKI;
    }

    public String getWikiword() {
    	return mWikiword;
    }
    
    public void setWikiword(String w) {
    	mWikiword = w;
    }
    
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(mWikiword);
        super.serializeData(out);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mWikiword = in.readUTF();
        super.deserializeData(in);
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);

        mbox.createWiki(getOperationContext(), getFolderId(), mWikiword, getAuthor(), getMessageBody(), null);
    }
}
