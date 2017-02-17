/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SetPermissions extends RedoableOp {

    private int mFolderId;
    private String mACL;

    public SetPermissions() {
        super(MailboxOperation.SetPermissions);
        mFolderId = UNKNOWN_ID;
        mACL = "";
    }

    public SetPermissions(int mailboxId, int folderId, ACL acl) {
        this();
        setMailboxId(mailboxId);
        mFolderId = folderId;
        mACL = acl == null ? "" : acl.toString();
    }

    @Override protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mFolderId);
        sb.append(", acl=").append(mACL);
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mFolderId);
        out.writeUTF(mACL);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mFolderId = in.readInt();
        mACL = in.readUTF();
    }

    
    @Override public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        ACL acl;
        if (mACL.equals("")) {
            acl = null;
        } else if (getVersion().atLeast(1, 36)) {
            acl = new ACL(new Metadata(mACL));
        } else {
            acl = new ACL(new MetadataList(mACL));
        }
        mbox.setPermissions(getOperationContext(), mFolderId, acl);
    }
}
