/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbMailItem;

import java.util.Arrays;
import java.util.List;

/**
 */
public class ExternalVirtualMailbox extends Mailbox {

    protected ExternalVirtualMailbox(MailboxData data) {
        super(data);
    }

    @Override
    public MailSender getMailSender() throws ServiceException {
        throw ServiceException.PERM_DENIED("operation denied");
    }

    @Override
    public Folder createFolder(OperationContext octxt, String name, int parentId, byte attrs,
                               MailItem.Type defaultView, int flags, Color color, String url)
            throws ServiceException {
        throw ServiceException.PERM_DENIED("operation denied");
    }

    @Override
    public Folder createFolder(OperationContext octxt, String path, byte attrs, MailItem.Type defaultView,
                               int flags, Color color, String url)
            throws ServiceException {
        throw ServiceException.PERM_DENIED("operation denied");
    }
}
