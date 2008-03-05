/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class CreateChat extends CreateMessage {
    
    public CreateChat() {
    }

    public CreateChat(int mailboxId, 
                String digest,
                int msgSize,
                int folderId,
                int flags,
                String tags) 
    {
        super(mailboxId, ":API:", false, digest, msgSize, folderId, true, flags, tags);
    }
    
    public int getOpCode() {
        return OP_CREATE_CHAT;
    }
    
    protected void serializeData(RedoLogOutput out) throws IOException {
        super.serializeData(out);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        super.deserializeData(in);
    }
    
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
