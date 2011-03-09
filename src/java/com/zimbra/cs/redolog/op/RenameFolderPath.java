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
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @since 2004. 12. 13.
 */
public class RenameFolderPath extends RenameItemPath {

    public RenameFolderPath() {
        mOperation = MailboxOperation.RenameFolderPath;
        mId = UNKNOWN_ID;
        type = MailItem.Type.FOLDER;
    }

    public RenameFolderPath(int mailboxId, int id, String path) {
        super(mailboxId, id, MailItem.Type.FOLDER, path);
        mOperation = MailboxOperation.RenameFolderPath;
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeUTF(mPath);
        if (mParentIds != null) {
            out.writeInt(mParentIds.length);
            for (int i = 0; i < mParentIds.length; i++) {
                out.writeInt(mParentIds[i]);
            }
        } else {
            out.writeInt(0);
        }
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mPath = in.readUTF();
        int numParentIds = in.readInt();
        if (numParentIds > 0) {
            mParentIds = new int[numParentIds];
            for (int i = 0; i < numParentIds; i++) {
                mParentIds[i] = in.readInt();
            }
        }
    }
}
