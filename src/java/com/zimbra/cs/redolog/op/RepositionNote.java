/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on 2004. 12. 14.
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class RepositionNote extends RedoableOp {

    private int mId;
    private Note.Rectangle mBounds;

    public RepositionNote() {
        super(MailboxOperation.RepositionNote);
        mId = UNKNOWN_ID;
    }

    public RepositionNote(int mailboxId, int id, Note.Rectangle bounds) {
        this();
        setMailboxId(mailboxId);
        mId = id;
        mBounds = bounds;
    }

    @Override protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId);
        if (mBounds != null)
            sb.append(", bounds=(").append(mBounds).append(")");
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeInt(mBounds.x);
        out.writeInt(mBounds.y);
        out.writeInt(mBounds.width);
        out.writeInt(mBounds.height);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        int x = in.readInt();
        int y = in.readInt();
        int w = in.readInt();
        int h = in.readInt();
        mBounds = new Note.Rectangle(x, y, w, h);
    }

    @Override public void redo() throws Exception {
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mailbox.repositionNote(getOperationContext(), mId, mBounds);
    }
}
