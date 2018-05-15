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
package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.StagedBlob;

public class Chat extends Message {

    Chat(Mailbox mbox, UnderlyingData ud) throws ServiceException {
        this(mbox, ud, false);
    }

    /**
     * this one will call back into decodeMetadata() to do our initialization
     *
     * @param mbox
     * @param ud
     * @throws ServiceException
     */
    Chat(Mailbox mbox, UnderlyingData ud, boolean skipCache) throws ServiceException {
        super(mbox, ud, skipCache);
        init();
    }

    Chat(Account acc, UnderlyingData data, int mailboxId) throws ServiceException {
        super(acc, data, mailboxId);
        init();
    }

    private void init() throws ServiceException {
        if (type != Type.CHAT.toByte()) {
            throw new IllegalArgumentException();
        }
        if (getParentId() < 0) {
            state.setParentId(-mId);
        }
    }

    static class ChatCreateFactory extends MessageCreateFactory {
        @Override
        Message create(Mailbox mbox, UnderlyingData data) throws ServiceException {
            return new Chat(mbox, data);
        }

        @Override
        Type getType() {
            return Type.CHAT;
        }
    }

    static Chat create(int id, Folder folder, ParsedMessage pm, StagedBlob staged, boolean unread, int flags, Tag.NormalizedTags ntags)
    throws ServiceException {
        return (Chat) Message.createInternal(id, folder, null, pm, staged, unread, flags, ntags, null, true, null, null, null, new ChatCreateFactory());
    }

    @Override boolean isMutable() { return true; }
}
