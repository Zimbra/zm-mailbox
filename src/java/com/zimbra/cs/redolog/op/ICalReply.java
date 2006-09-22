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
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.service.ServiceException;

public class ICalReply extends RedoableOp {

    private Invite mInvite;
    
    public ICalReply() {}

    public ICalReply(int mailboxId, Invite inv) {
        setMailboxId(mailboxId);
        mInvite = inv;
    }

    public int getOpCode() {
        return OP_ICAL_REPLY;
    }

    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder();
        ICalTimeZone localTz = mInvite.getTimeZoneMap().getLocalTimeZone();
        sb.append("localTZ=").append(localTz.encodeAsMetadata().toString());
        sb.append(", inv=").append(Invite.encodeMetadata(mInvite).toString());
        return sb.toString();
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        ICalTimeZone localTz = mInvite.getTimeZoneMap().getLocalTimeZone();
        out.writeUTF(localTz.encodeAsMetadata().toString());
        out.writeUTF(Invite.encodeMetadata(mInvite).toString());
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        try {
            ICalTimeZone localTz = ICalTimeZone.decodeFromMetadata(new Metadata(in.readUTF()));
            mInvite = Invite.decodeMetadata(getMailboxId(), new Metadata(in.readUTF()), null, localTz);
        } catch (ServiceException ex) {
            ex.printStackTrace();
            throw new IOException("Cannot read serialized entry for ICalReply " + ex.toString());
        }
    }
    
    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.processICalReply(getOperationContext(), mInvite);
    }
}
