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
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author jhahm
 */
public class FixCalendarItemTimeZone extends RedoableOp {

    private int mId;
    private long mAfter;
    private String mCountry;  // ISO-3166 two-letter country code, or null for world

    public FixCalendarItemTimeZone() {}

    public FixCalendarItemTimeZone(int mailboxId, int itemId, long after, String country) {
        setMailboxId(mailboxId);
        mId = itemId;
        mAfter = after;
        mCountry = country;
    }

    public int getOpCode() {
        return OP_FIX_CALENDAR_ITEM_TZ;
    }

    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("id=");
        sb.append(mId);
        sb.append(", after=").append(mAfter);
        sb.append(", country=").append(mCountry);
        return sb.toString();
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeLong(mAfter);
        out.writeUTF(mCountry);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mAfter = in.readLong();
        mCountry = in.readUTF();
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);
        mbox.fixCalendarItemTimeZone(getOperationContext(), mId, mAfter, mCountry);
    }
}
