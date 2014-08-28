package com.zimbra.cs.mailbox;
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

import com.zimbra.common.service.ServiceException;

public interface MailboxDataCache {

    public Mailbox.MailboxData get(Mailbox mbox) throws ServiceException;

    public void put(Mailbox mbox, Mailbox.MailboxData mailboxData) throws ServiceException;

    public void remove(Mailbox mbox) throws ServiceException;
}
