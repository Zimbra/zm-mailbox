package com.zimbra.cs.mailbox;
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

import com.zimbra.common.service.ServiceException;


public interface ConversationIdCache {

    /** Gets a Conversation ID from the cache, by subject hash */
    public Integer get(Mailbox mbox, String subjectHash) throws ServiceException;

    /** Puts a Conversation ID into the cache, by subject hash */
    public void put(Mailbox mbox, String subjectHash, int conversationId) throws ServiceException;

    /** Removes a Conversation ID from the cache, by subject hash */
    public void remove(Mailbox mbox, String subjectHash) throws ServiceException;
}

