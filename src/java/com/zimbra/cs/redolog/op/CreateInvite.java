/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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

import com.zimbra.cs.mailbox.Invite;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;

import com.zimbra.cs.service.ServiceException;

public class CreateInvite extends RedoableOp implements CreateAppointmentRecorder, CreateAppointmentPlayer 
{
    private int mApptId;
    private Invite mInvite;
    private boolean mForce;
    private int mFolderId;
    
    protected void serializeData(DataOutput out) throws IOException 
    {
        out.writeInt(mApptId);
        out.writeInt(mFolderId);
        out.writeBoolean(mForce);
        
        ICalTimeZone localTz = mInvite.getTimeZoneMap().getLocalTimeZone();
        out.writeUTF(localTz.encodeAsMetadata().toString());
        
        out.writeUTF(Invite.encodeMetadata(mInvite).toString());
    }

    protected void deserializeData(DataInput in) throws IOException {
        mApptId = in.readInt();
        mFolderId = in.readInt();
        mForce = in.readBoolean();
        
        try {
            ICalTimeZone localTz = ICalTimeZone.decodeFromMetadata(new Metadata(in.readUTF()));
            
            mInvite = Invite.decodeMetadata(getMailboxId(), new Metadata(in.readUTF()), null, localTz); 
        
        } catch (ServiceException ex) {
            ex.printStackTrace();
            throw new IOException("Cannot read serialized entry for CreateInvite "+ex.toString());
        }
    }

    public CreateInvite() 
    {
        super(); 
        mApptId = 0;
    }
    
    public void setInvite(Invite inv) {
        mInvite = inv;
    }
    
    public Invite getInvite() { 
        return mInvite;
    }
    
    public void setForce(boolean force) { mForce = force; }
    public boolean getForce() { return mForce; }
    
    public void setFolderId(int folderId) {
        mFolderId = folderId;
    }
    
    public int getAppointmentId() {
        return mApptId;
    }
    
    public void setAppointmentId(int id) {
        mApptId = id;
    }

    public int getOpCode() {
        return OP_CREATE_INVITE;
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = Mailbox.getMailboxById(mboxId);
        
        mailbox.addInvite(getOperationContext(), mFolderId, mInvite, mForce, null);
    }

    protected String getPrintableData() {
        return new String("ApptId = "+mApptId);
    }

}
