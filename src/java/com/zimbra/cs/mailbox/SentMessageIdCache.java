package com.zimbra.cs.mailbox;
/*
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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


public interface SentMessageIdCache {

    /** Gets a Sent Message ID from the cache, by Message-ID header */
    public Integer get(Mailbox mbox, String msgidHeader) throws ServiceException;

    /** Puts a Sent Message ID into the cache, by Message-ID header */
    public void put(Mailbox mbox, String msgidHeader, int id) throws ServiceException;
}

