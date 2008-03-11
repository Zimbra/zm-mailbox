/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog.op;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.tzfixup.TimeZoneFixupRules;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class FixCalendarItemTZ extends RedoableOp {

    private int mId;
    private Map<String, ICalTimeZone> mReplacementMap;

    public FixCalendarItemTZ() {}

    public FixCalendarItemTZ(int mailboxId, int itemId) {
        setMailboxId(mailboxId);
        mId = itemId;
    }

    public void setReplacementMap(Map<String, ICalTimeZone> replacementMap) {
        mReplacementMap = replacementMap;
    }

    @Override
    public int getOpCode() {
        return OP_FIX_CALENDAR_ITEM_TZ;
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        if (mReplacementMap != null) {
            out.writeInt(mReplacementMap.size());
            for (Entry<String, ICalTimeZone> entry : mReplacementMap.entrySet()) {
                String tzid = entry.getKey();
                ICalTimeZone newTZ = entry.getValue();
                String newTZMeta = newTZ.encodeAsMetadata().toString();
                out.writeUTF(tzid);
                out.writeUTF(newTZMeta);
            }
        } else {
            out.writeInt(0);  // map size == 0
        }
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        int numReplacements = in.readInt();
        if (numReplacements > 0) {
            mReplacementMap = new HashMap<String, ICalTimeZone>(numReplacements);
            for (int i = 0; i < numReplacements; i++) {
                String tzid = in.readUTF();
                String newTZMeta = in.readUTF();
                try {
                    ICalTimeZone newTZ = ICalTimeZone.decodeFromMetadata(new Metadata(newTZMeta));
                    mReplacementMap.put(tzid, newTZ);
                } catch (ServiceException e) {
                    IOException ioe = new IOException("Error deserializing timezone");
                    ioe.initCause(e);
                    throw ioe;
                }
            }
        }
    }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("id=");
        sb.append(mId);
        if (mReplacementMap != null) {
            sb.append(", replacementMap=[");
            for (Entry<String, ICalTimeZone> entry : mReplacementMap.entrySet()) {
                String tzid = entry.getKey();
                ICalTimeZone newTZ = entry.getValue();
                sb.append("\n");
                sb.append("oldTZID=\"").append(tzid).append("\"\n==> newTZ: ").append(newTZ.toString()).append(",");
            }
            sb.append("]");
        }
        return sb.toString();
    }

    @Override
    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);
        TimeZoneFixupRules rules = new TimeZoneFixupRules(mReplacementMap);
        mbox.fixCalendarItemTZ(getOperationContext(), mId, rules);
    }
}
