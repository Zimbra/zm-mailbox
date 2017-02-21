/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class FixCalendarItemTimeZone extends RedoableOp {

    private int mId;
    private long mAfter;
    private String mCountry;  // ISO-3166 two-letter country code, or null for world

    public FixCalendarItemTimeZone() {
        super(MailboxOperation.FixCalendarItemTimeZone);
    }

    public FixCalendarItemTimeZone(int mailboxId, int itemId, long after, String country) {
        this();
        setMailboxId(mailboxId);
        mId = itemId;
        mAfter = after;
        mCountry = country;
    }

    @Override protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("id=");
        sb.append(mId);
        sb.append(", after=").append(mAfter);
        sb.append(", country=").append(mCountry);
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeLong(mAfter);
        out.writeUTF(mCountry);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mAfter = in.readLong();
        mCountry = in.readUTF();
    }

    @Override public void redo() throws Exception {
        // do nothing; this op has been deprecated
    }
}
