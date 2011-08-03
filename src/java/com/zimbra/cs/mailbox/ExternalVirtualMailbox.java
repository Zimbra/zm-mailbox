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
 * Mailbox for accounts with {@link com.zimbra.cs.account.Provisioning.A_zimbraIsExternalVirtualAccount}
 * set to TRUE.
 */
public class ExternalVirtualMailbox extends Mailbox {

    protected ExternalVirtualMailbox(MailboxData data) {
        super(data);
    }

    @Override
    protected void createDefaultFolders() throws ServiceException {
        lock.lock();
        try {
            byte hidden = Folder.FOLDER_IS_IMMUTABLE | Folder.FOLDER_DONT_TRACK_COUNTS;
            Folder root = Folder.create(ID_FOLDER_ROOT, this, null, "ROOT", hidden, MailItem.Type.UNKNOWN, 0,
                    MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_TAGS, this, root, "Tags", hidden, MailItem.Type.TAG, 0,
                    MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_CONVERSATIONS, this, root, "Conversations", hidden, MailItem.Type.CONVERSATION, 0,
                    MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_COMMENTS, this, root, "Comments", hidden, MailItem.Type.COMMENT, 0,
                    MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_PROFILE, this, root, "Profile", hidden, MailItem.Type.DOCUMENT, 0,
                    MailItem.DEFAULT_COLOR_RGB, null, null);

            byte system = Folder.FOLDER_IS_IMMUTABLE;
            Folder.create(ID_FOLDER_USER_ROOT, this, root, "USER_ROOT", system, MailItem.Type.UNKNOWN, 0,
                    MailItem.DEFAULT_COLOR_RGB, null, null);
        } finally {
            lock.release();
        }
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
