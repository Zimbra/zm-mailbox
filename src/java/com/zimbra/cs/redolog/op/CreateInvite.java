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
    
    protected void serializeData(DataOutput out) throws IOException 
    {
        out.writeInt(mApptId);
        
        ICalTimeZone localTz = mInvite.getTimeZoneMap().getLocalTimeZone();
        out.writeUTF(localTz.encodeAsMetadata().toString());
        
        out.writeUTF(Invite.encodeMetadata(mInvite).toString());
    }

    protected void deserializeData(DataInput in) throws IOException {
        mApptId = in.readInt();
        
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
        
        mailbox.addInvite(getOperationContext(), mInvite);
    }

    protected String getPrintableData() {
        return new String("ApptId = "+mApptId);
    }

}
