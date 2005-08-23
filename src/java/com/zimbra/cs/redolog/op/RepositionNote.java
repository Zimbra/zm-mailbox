/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 12. 14.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Note;

/**
 * @author jhahm
 */
public class RepositionNote extends RedoableOp {

    private int mId;
    private Note.Rectangle mBounds;

    public RepositionNote() {
        mId = UNKNOWN_ID;
    }

    public RepositionNote(int mailboxId, int id, Note.Rectangle bounds) {
        setMailboxId(mailboxId);
        mId = id;
        mBounds = bounds;
    }

    public int getOpCode() {
        return OP_REPOSITION_NOTE;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId);
        if (mBounds != null)
            sb.append(", bounds=(").append(mBounds).append(")");
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mId);
        out.writeInt(mBounds.x);
        out.writeInt(mBounds.y);
        out.writeInt(mBounds.width);
        out.writeInt(mBounds.height);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mId = in.readInt();
        int x = in.readInt();
        int y = in.readInt();
        int w = in.readInt();
        int h = in.readInt();
        mBounds = new Note.Rectangle(x, y, w, h);
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = Mailbox.getMailboxById(mboxId);
        mailbox.repositionNote(getOperationContext(), mId, mBounds);
    }
}
