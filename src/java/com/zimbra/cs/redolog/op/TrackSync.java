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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
/*
 * Created on Nov 12, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;

import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author dkarp
 */
public class TrackSync extends RedoableOp {

    public TrackSync() {
    }

    public TrackSync(int mailboxId) {
        setMailboxId(mailboxId);
    }

    public int getOpCode() {
        return OP_TRACK_SYNC;
    }

    protected String getPrintableData() {
        // no members to print
        return null;
    }

    protected void serializeData(DataOutput out) {
        // no members to serialize
    }

    protected void deserializeData(DataInput in) {
        // no members to deserialize
    }

    public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        mbox.beginTrackingSync(getOperationContext());
    }
}
