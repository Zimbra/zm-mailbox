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
 * Created on Jun 14, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mime.ParsedMessage;

/**
 * @author dkarp
 */
public class SaveDraft extends CreateMessage {

    private int mImapId;           // new IMAP id for this message

    public SaveDraft() {
    }

    public SaveDraft(int mailboxId, int draftId, String digest, int msgSize) {
        super(mailboxId, ":API:", false, digest, msgSize, -1, true, 0, null);
        setMessageId(draftId);
    }

    public int getImapId() {
        return mImapId;
    }

    public void setImapId(int imapId) {
        mImapId = imapId;
    }

    protected String getPrintableData() {
        return super.getPrintableData() + ",imap=" + mImapId;
    }

    public int getOpCode() {
        return OP_SAVE_DRAFT;
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mImapId);
        super.serializeData(out);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mImapId = in.readInt();
        super.deserializeData(in);
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = Mailbox.getMailboxById(mboxId);

        ParsedMessage pm = new ParsedMessage(getMessageBody(), getTimestamp(), mbox.attachmentsIndexingEnabled());
        mbox.saveDraft(getOperationContext(), pm, getMessageId(), 0, null);
    }
}
