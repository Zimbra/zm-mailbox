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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Feb 11, 2006
 */
package com.zimbra.cs.redolog.op;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author dkarp
 */
public class TrackImap extends RedoableOp {

    public TrackImap() {
    }

    public TrackImap(int mailboxId) {
        setMailboxId(mailboxId);
    }

    public int getOpCode() {
        return OP_TRACK_IMAP;
    }

    protected String getPrintableData() {
        // no members to print
        return null;
    }

    protected void serializeData(RedoLogOutput out) {
        // no members to serialize
    }

    protected void deserializeData(RedoLogInput in) {
        // no members to deserialize
    }

    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.beginTrackingImap(getOperationContext());
    }
}
