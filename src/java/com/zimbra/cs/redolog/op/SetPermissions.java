/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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
