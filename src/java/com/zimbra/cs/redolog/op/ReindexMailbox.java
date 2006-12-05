/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author jhahm
 */
public class ReindexMailbox extends RedoableOp {
    
    private Set<Byte> mTypes = null;
    private int mCompletionId = 0;

    public ReindexMailbox() { }

    public ReindexMailbox(int mailboxId, Set<Byte> types, int completionId) {
        setMailboxId(mailboxId);
        mTypes = types;
        mCompletionId = completionId;
    }
    

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#getOpCode()
     */
    public int getOpCode() {
        return OP_REINDEX_MAILBOX;
    }

    public boolean deferCrashRecovery() {
        return true;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#redo()
     */
    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.reIndex(new OperationContext(this), mTypes, mCompletionId);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#getPrintableData()
     */
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("Completion="+mCompletionId);
        if (mTypes != null) {
            sb.append(" TYPES[");
            boolean atStart = true;
            for (Byte b : mTypes) {
                if (!atStart) 
                    sb.append(',');
                else
                    atStart = false;
                sb.append(MailItem.getNameForType(b));
            }
            sb.append(']');
                        
            return sb.toString();
        } else {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#serializeData(java.io.RedoLogOutput)
     */
    protected void serializeData(RedoLogOutput out) throws IOException {
        if (getVersion().atLeast(1,9)) {
            out.writeInt(mCompletionId);
            if (mTypes != null) {
                out.writeBoolean(true);
                int count = mTypes.size();
                out.writeInt(count);
                for (Byte b : mTypes) {
                    out.writeByte(b);
                    count--;
                }
                assert(count == 0);
            } else {
                out.writeBoolean(false);
            }
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.op.RedoableOp#deserializeData(java.io.RedoLogInput)
     */
    protected void deserializeData(RedoLogInput in) throws IOException {
        if (getVersion().atLeast(1,9)) {
            mCompletionId = in.readInt();
            if (in.readBoolean()) {
                mTypes = new HashSet<Byte>();
                for (int count = in.readInt(); count > 0; count--) {
                    mTypes.add(in.readByte());
                }
            }
        }
    }
}
