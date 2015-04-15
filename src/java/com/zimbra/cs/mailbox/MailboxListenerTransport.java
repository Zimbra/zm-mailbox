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

package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxListener.ChangeNotification;
import com.zimbra.cs.session.Session;

public interface MailboxListenerTransport {

    /** After a local mailbox change completes, publish it to any remote subscribers */
    public void publish(ChangeNotification notification) throws ServiceException;

    /** Subscribe to remote mailbox change notifications */
    public void subscribe(Session session) throws ServiceException;

    /** Unsubscribe from remote mailbox change notifications */
    public void unsubscribe(Session session);
}
