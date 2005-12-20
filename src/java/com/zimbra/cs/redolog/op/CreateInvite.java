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

package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.ParsedMessage;

import com.zimbra.cs.service.ServiceException;

public class CreateInvite extends RedoableOp implements CreateAppointmentRecorder, CreateAppointmentPlayer 
{
    private int mApptId;
    private Invite mInvite;
    private int mFolderId;
    private boolean mForce;
    private byte[] mData;
    
    public CreateInvite() { }

    public CreateInvite(int mailboxId, Invite inv, int folderId, byte[] data, boolean force) {
        setMailboxId(mailboxId);
        mInvite = inv;
        mFolderId = folderId;
        mForce = force;
        mData = data;
    }

    public int getOpCode() {
        return OP_CREATE_INVITE;
    }

    protected String getPrintableData() {
        return new String("ApptId = "+mApptId);
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mApptId);
        out.writeInt(mFolderId);
        out.writeBoolean(mForce);
        
        int dataLen = mData != null ? mData.length : 0;
        out.writeInt(dataLen);
        if (dataLen > 0) {
        	out.write(mData);
        }
        
        ICalTimeZone localTz = mInvite.getTimeZoneMap().getLocalTimeZone();
        out.writeUTF(localTz.encodeAsMetadata().toString());
        out.writeUTF(Invite.encodeMetadata(mInvite).toString());
    }

    protected void deserializeData(DataInput in) throws IOException {
        mApptId = in.readInt();
        mFolderId = in.readInt();
        mForce = in.readBoolean();
        
        int dataLen = in.readInt();
        if (dataLen > 0) {
        	mData = new byte[dataLen];
        	in.readFully(mData);
        }
        
        try {
            ICalTimeZone localTz = ICalTimeZone.decodeFromMetadata(new Metadata(in.readUTF()));
            
            mInvite = Invite.decodeMetadata(getMailboxId(), new Metadata(in.readUTF()), null, localTz); 
        
        } catch (ServiceException ex) {
            ex.printStackTrace();
            throw new IOException("Cannot read serialized entry for CreateInvite "+ex.toString());
        }
    }
    
    public int getAppointmentId() {
        return mApptId;
    }
    
    public void setAppointmentId(int id) {
        mApptId = id;
    }

    public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
		ParsedMessage pm = new ParsedMessage(mData, getTimestamp(), mbox.attachmentsIndexingEnabled());
		mbox.addInvite(getOperationContext(), mInvite, mFolderId, mForce, pm);
    }
}
