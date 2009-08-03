/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.io.IOException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.MailboxBlob;

public class Chat extends Message {

    /**
     * this one will call back into decodeMetadata() to do our initialization
     * 
     * @param mbox
     * @param ud
     * @throws ServiceException
     */
    Chat(Mailbox mbox, UnderlyingData ud) throws ServiceException {
        super(mbox, ud);
        if (mData.type != TYPE_CHAT)
            throw new IllegalArgumentException();
        if (mData.parentId < 0)
            mData.parentId = -mId;
    }

    static class ChatCreateFactory extends MessageCreateFactory {
        @Override Message create(Mailbox mbox, UnderlyingData data) throws ServiceException {
            return new Chat(mbox, data);
        }
        @Override byte getType() { return TYPE_CHAT; }
    }

    static Chat create(int id, Folder folder, ParsedMessage pm, MailboxBlob mblob, boolean unread, int flags, long tags)  
    throws ServiceException, IOException {
        return (Chat) Message.createInternal(id, folder, null, pm, mblob, unread, flags, tags, null, true, null, null, new ChatCreateFactory());
    }

    @Override boolean isMutable() { return true; }
}
