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
 * Created on Jul 24, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.text.ParseException;

import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.service.ServiceException;

/**
 * @author jhahm
 */
public class ModifyInvitePartStat extends RedoableOp 
{
    private int mApptId = UNKNOWN_ID; // appointment which contains invite
    private RecurId mRecurId = null;
    private String mCnStr = null;
    private String mAddressStr = null;
    private String mCUTypeStr = null;
    private String mRoleStr = null;
    private String mPartStatStr = null; // "AC" (accepted), "TE" (tentative), etc.
    private Boolean mNeedsReply;
    private int mSeqNo;
    private long mDtStamp;

    public ModifyInvitePartStat() {
    }

    public ModifyInvitePartStat(int mailboxId, int apptId, RecurId recurId, 
            String cnStr, String addressStr, String cutypeStr, String roleStr, String partStatStr, Boolean needsReply, 
            int seqNo, long dtStamp)
    {
        setMailboxId(mailboxId);
        mApptId = apptId;
        mRecurId = recurId;
        mCnStr = cnStr != null ? cnStr : "";
        mAddressStr = addressStr != null ? addressStr : "";
        mCUTypeStr = cutypeStr != null ? cutypeStr : "";
        mRoleStr = roleStr != null ? roleStr : "";
        mPartStatStr = partStatStr != null ? partStatStr : "";
        mNeedsReply = needsReply;
        mSeqNo = seqNo;
        mDtStamp = dtStamp;
    }

    public int getOpCode() {
        return OP_MODIFY_INVITE_PARTSTAT;
    }

    public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        mbox.modifyPartStat(getOperationContext(), mApptId, mRecurId, mCnStr, mAddressStr, mCUTypeStr, mRoleStr, mPartStatStr, mNeedsReply, mSeqNo, mDtStamp); 
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("apptId=").append(mApptId);
        if (mRecurId != null) {
            sb.append(", recurId=").append(mRecurId.toString());
        }
        sb.append(", cn=").append(mCnStr);
        sb.append(", address=").append(mAddressStr);
        sb.append(", cutype=").append(mCUTypeStr);
        sb.append(", role=").append(mRoleStr);
        sb.append(", part=").append(mPartStatStr);
        sb.append(", need-reply=").append(mNeedsReply);
        sb.append(", seqNo=").append(mSeqNo);
        sb.append(", dtStamp=").append(mDtStamp);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mApptId);
        
        out.writeBoolean(mRecurId!=null);
        if (mRecurId != null) {
            out.writeInt(mRecurId.getRange());
            out.writeUTF(mRecurId.getDt().toString());
        }
        
        out.writeUTF(mCnStr);
        out.writeUTF(mAddressStr);
        out.writeUTF(mCUTypeStr);
        out.writeUTF(mRoleStr);
        out.writeUTF(mPartStatStr);
        out.writeBoolean(mNeedsReply.booleanValue());
        out.writeInt(mSeqNo);
        out.writeLong(mDtStamp);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mApptId = in.readInt();
        
        try {
            Appointment appt = Mailbox.getMailboxById(getMailboxId()).getAppointmentById(null, mApptId);
            
            if (in.readBoolean()) {
                int range = in.readInt();
                String dtStr = in.readUTF();
                
                try {
                    mRecurId = new RecurId(ParsedDateTime.parse(dtStr, appt.getTimeZoneMap()), range);
                } catch (ParseException pe) {
                    throw new IOException("Parsing date-time: "+dtStr+" "+pe);
                }
            }
            
            mCnStr = in.readUTF();
            mAddressStr = in.readUTF();
            mCUTypeStr = in.readUTF();
            mRoleStr = in.readUTF();
            mPartStatStr = in.readUTF();
            mNeedsReply = new Boolean(in.readBoolean());
            mSeqNo = in.readInt();
            mDtStamp = in.readLong();
        } catch (ServiceException se) {
            throw new IOException("ServiceException: "+se);
        }
    }
}
