/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class CreateChat extends CreateMessage {
    
    public CreateChat() {
        mOperation = MailboxOperation.CreateChat;
    }

    public CreateChat(int mailboxId, String digest, int msgSize, int folderId, int flags, String[] tags) {
        super(mailboxId, ":API:", false, digest, msgSize, folderId, true, flags, tags);
        mOperation = MailboxOperation.CreateChat;
    }
    
    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        super.serializeData(out);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        super.deserializeData(in);
    }
    
    @Override
    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);

        ParsedMessage  pm = new ParsedMessage(getMessageBody(), getTimestamp(), mbox.attachmentsIndexingEnabled());
        try {
            mbox.createChat(getOperationContext(), pm, getFolderId(), getFlags(), getTags());
        } catch (MailServiceException e) {
            if (e.getCode() == MailServiceException.ALREADY_EXISTS) {
                mLog.info("Chat " + getMessageId() + " is already in mailbox " + mboxId);
                return;
            } else {
                throw e;
            }
        }
    }
    
}
