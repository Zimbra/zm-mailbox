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
 * Created on Jul 24, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.RecurId;

/**
 * @author jhahm
 */
public class ModifyInvitePartStat extends RedoableOp 
{
    private int mApptId = UNKNOWN_ID; // appointment which contains invite
    private String mRecurIdDt = null;
    private int mRecurIdRange = RecurId.RANGE_NONE;
    private String mCnStr = null;
    private String mAddressStr = null;
    private String mCUTypeStr = null;
    private String mRoleStr = null;
    private String mPartStatStr = null; // "AC" (accepted), "TE" (tentative), etc.
    private Boolean mRsvp;
    private int mSeqNo;
    private long mDtStamp;

    public ModifyInvitePartStat() {
    }

    public ModifyInvitePartStat(int mailboxId, int apptId, RecurId recurId, 
            String cnStr, String addressStr, String cutypeStr, String roleStr, String partStatStr, Boolean rsvp, 
            int seqNo, long dtStamp)
    {
        setMailboxId(mailboxId);
        mApptId = apptId;
        if (recurId != null) {
            mRecurIdDt = recurId.getDt().toString();
            mRecurIdRange = recurId.getRange();
        }
        mCnStr = cnStr != null ? cnStr : "";
        mAddressStr = addressStr != null ? addressStr : "";
        mCUTypeStr = cutypeStr != null ? cutypeStr : "";
        mRoleStr = roleStr != null ? roleStr : "";
        mPartStatStr = partStatStr != null ? partStatStr : "";
        mRsvp = rsvp;
        mSeqNo = seqNo;
        mDtStamp = dtStamp;
    }

    public int getOpCode() {
        return OP_MODIFY_INVITE_PARTSTAT;
    }

    public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        Appointment appt =
            Mailbox.getMailboxById(getMailboxId()).
            getAppointmentById(null, mApptId);
        RecurId recurId =
            new RecurId(ParsedDateTime.parse(mRecurIdDt,
                                             appt.getTimeZoneMap()),
                        mRecurIdRange);
        mbox.modifyPartStat(
                getOperationContext(),
                mApptId, recurId, mCnStr, mAddressStr,
                mCUTypeStr, mRoleStr, mPartStatStr,
                mRsvp, mSeqNo, mDtStamp); 
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("apptId=").append(mApptId);
        if (mRecurIdDt != null) {
            sb.append(", recurIdDt=").append(mRecurIdDt);
            sb.append(", recurIdRange=").append(mRecurIdRange);
        }
        sb.append(", cn=").append(mCnStr);
        sb.append(", address=").append(mAddressStr);
        sb.append(", cutype=").append(mCUTypeStr);
        sb.append(", role=").append(mRoleStr);
        sb.append(", part=").append(mPartStatStr);
        sb.append(", rsvp=").append(mRsvp);
        sb.append(", seqNo=").append(mSeqNo);
        sb.append(", dtStamp=").append(mDtStamp);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mApptId);
        boolean hasRecurId = mRecurIdDt != null;
        out.writeBoolean(hasRecurId);
        if (hasRecurId) {
            out.writeInt(mRecurIdRange);
            out.writeUTF(mRecurIdDt);
        }

        out.writeUTF(mCnStr);
        out.writeUTF(mAddressStr);
        out.writeUTF(mCUTypeStr);
        out.writeUTF(mRoleStr);
        out.writeUTF(mPartStatStr);
        out.writeBoolean(mRsvp.booleanValue());
        out.writeInt(mSeqNo);
        out.writeLong(mDtStamp);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mApptId = in.readInt();
        boolean hasRecurId = in.readBoolean();
        if (hasRecurId) {
            mRecurIdRange = in.readInt();
            mRecurIdDt = in.readUTF();
        }

        mCnStr = in.readUTF();
        mAddressStr = in.readUTF();
        mCUTypeStr = in.readUTF();
        mRoleStr = in.readUTF();
        mPartStatStr = in.readUTF();
        mRsvp = new Boolean(in.readBoolean());
        mSeqNo = in.readInt();
        mDtStamp = in.readLong();
    }
}
