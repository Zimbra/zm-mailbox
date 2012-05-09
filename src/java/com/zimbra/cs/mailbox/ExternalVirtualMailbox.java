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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.UUIDUtil;

/**
 * Mailbox for accounts with zimbraIsExternalVirtualAccount set to TRUE.
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
            Folder root = Folder.create(ID_FOLDER_ROOT, UUIDUtil.generateUUID(), this, null, "ROOT", hidden, MailItem.Type.UNKNOWN,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_TAGS, UUIDUtil.generateUUID(), this, root, "Tags", hidden, MailItem.Type.TAG,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_CONVERSATIONS, UUIDUtil.generateUUID(), this, root, "Conversations", hidden, MailItem.Type.CONVERSATION,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_COMMENTS, UUIDUtil.generateUUID(), this, root, "Comments", hidden, MailItem.Type.COMMENT,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_PROFILE, UUIDUtil.generateUUID(), this, root, "Profile", hidden, MailItem.Type.DOCUMENT,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null, null);

            byte system = Folder.FOLDER_IS_IMMUTABLE;
            Folder userRoot = Folder.create(ID_FOLDER_USER_ROOT, UUIDUtil.generateUUID(), this, root, "USER_ROOT", system, MailItem.Type.UNKNOWN,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_INBOX, UUIDUtil.generateUUID(), this, userRoot, "Inbox", system, MailItem.Type.MESSAGE,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_CONTACTS, UUIDUtil.generateUUID(), this, userRoot, "Contacts", system, MailItem.Type.CONTACT,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_CALENDAR, UUIDUtil.generateUUID(), this, userRoot, "Calendar", system, MailItem.Type.APPOINTMENT,
                    Flag.BITMASK_CHECKED, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_TASKS, UUIDUtil.generateUUID(), this, userRoot, "Tasks", system, MailItem.Type.TASK,
                    Flag.BITMASK_CHECKED, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_BRIEFCASE, UUIDUtil.generateUUID(), this, userRoot, "Briefcase", system, MailItem.Type.DOCUMENT,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
        } finally {
            lock.release();
        }
    }

    @Override
    public MailSender getMailSender() throws ServiceException {
        throw ServiceException.PERM_DENIED("permission denied for external account");
    }
}
