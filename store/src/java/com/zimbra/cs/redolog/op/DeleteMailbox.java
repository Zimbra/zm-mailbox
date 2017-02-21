/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
 * Created on 2005. 4. 4.
 */
package com.zimbra.cs.redolog.op;

import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class DeleteMailbox extends RedoableOp {

    public DeleteMailbox() {
        super(MailboxOperation.DeleteMailbox);
    }

    public DeleteMailbox(int mailboxId) {
        this();
        setMailboxId(mailboxId);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#redo()
     */
    @Override public void redo() throws Exception {
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#getPrintableData()
     */
    @Override protected String getPrintableData() {
        // no members to print
        return null;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#serializeData(java.io.RedoLogOutput)
     */
    @Override protected void serializeData(RedoLogOutput out) {
        // no members to serialize
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#deserializeData(java.io.RedoLogInput)
     */
    @Override protected void deserializeData(RedoLogInput in) {
        // no members to deserialize
    }

    @Override public boolean isDeleteOp() {
        return true;
    }
}
