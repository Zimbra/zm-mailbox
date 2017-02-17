/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SaveChat extends CreateChat {
    
    private int mImapId;           // new IMAP id for this message
    
    public SaveChat() {
        mOperation = MailboxOperation.SaveChat;
    }
    
    public SaveChat(int mailboxId, int chatId, String digest, int msgSize, int folderId, int flags, String[] tags) {
        super(mailboxId, digest, msgSize, folderId, flags, tags);
        mOperation = MailboxOperation.SaveChat;
        setMessageId(chatId);
    }

    public int getImapId() {
        return mImapId;
    }

    public void setImapId(int imapId) {
        mImapId = imapId;
    }

    @Override
    protected String getPrintableData() {
        return super.getPrintableData() + ",imap=" + mImapId;
    }
    
    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mImapId);
        super.serializeData(out);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mImapId = in.readInt();
        super.deserializeData(in);
    }
    
    @Override
    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());

        ParsedMessage pm = new ParsedMessage(getMessageBody(), getTimestamp(), mbox.attachmentsIndexingEnabled());
        mbox.updateChat(getOperationContext(), pm, getMessageId());
    }
}
