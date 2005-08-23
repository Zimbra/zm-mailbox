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
 * Created on 2005. 4. 4.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;

import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author jhahm
 */
public class DeleteMailbox extends RedoableOp {

    public DeleteMailbox() {
    }

    public DeleteMailbox(int mailboxId) {
        setMailboxId(mailboxId);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#getOpCode()
     */
    public int getOpCode() {
        return OP_DELETE_MAILBOX;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#redo()
     */
    public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        mbox.deleteMailbox(getOperationContext());
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#getPrintableData()
     */
    protected String getPrintableData() {
        // no members to print
        return null;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#serializeData(java.io.DataOutput)
     */
    protected void serializeData(DataOutput out) {
        // no members to serialize
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#deserializeData(java.io.DataInput)
     */
    protected void deserializeData(DataInput in) {
        // no members to deserialize
    }
}
