/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @since 2004. 7. 21.
 */
public class RenameTag extends RenameItem {

    public RenameTag() {
        super();
        mOperation = MailboxOperation.RenameTag;
        type = MailItem.Type.TAG;
        mFolderId = Mailbox.ID_FOLDER_TAGS;
    }

    public RenameTag(int mailboxId, int tagId, String name) {
        super(mailboxId, tagId, MailItem.Type.TAG, name, Mailbox.ID_FOLDER_TAGS);
        mOperation = MailboxOperation.RenameTag;
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeUTF(mName);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mName = in.readUTF();
    }
}
