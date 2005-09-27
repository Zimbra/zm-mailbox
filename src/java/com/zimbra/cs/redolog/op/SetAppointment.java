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

/* created 9/26/2005 */

package com.zimbra.cs.redolog.op;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.cs.mailbox.Invite;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mime.ParsedMessage;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.JMSession;


public class SetAppointment extends RedoableOp implements CreateAppointmentRecorder, CreateAppointmentPlayer 
{
    private int mFolderId;
    private int mApptId;
    private Mailbox.SetAppointmentData mDefaultInvite;
    private Mailbox.SetAppointmentData mExceptions[];
    
    
    static void serializeSetAppointmentData(DataOutput out, Mailbox.SetAppointmentData data) throws IOException, MessagingException {
        out.writeBoolean(data.mForce);
        
        ICalTimeZone localTz = data.mInv.getTimeZoneMap().getLocalTimeZone();
        out.writeUTF(localTz.encodeAsMetadata().toString());
        
        out.writeUTF(Invite.encodeMetadata(data.mInv).toString());
        
        out.writeLong(data.mPm.getReceivedDate());
        
        byte[] pmData = data.mPm.getRawData();
        out.writeInt(pmData.length);
        out.write(pmData);
    }
    
    static Mailbox.SetAppointmentData deserializeSetAppointmentData(DataInput in, Mailbox mbox) throws IOException, MessagingException {
        Mailbox.SetAppointmentData toRet = new Mailbox.SetAppointmentData();
        
        try {
            toRet.mForce = in.readBoolean();
        
            ICalTimeZone localTz = ICalTimeZone.decodeFromMetadata(new Metadata(in.readUTF()));
            
            toRet.mInv = Invite.decodeMetadata(mbox.getId(), new Metadata(in.readUTF()), null, localTz);
            
            long receivedDate = in.readLong();
            
            int dataLen = in.readInt();
            byte[] rawPmData = new byte[dataLen];
            in.readFully(rawPmData, 0, dataLen);
            
            InputStream is = new ByteArrayInputStream(rawPmData);
            MimeMessage mm = new MimeMessage(JMSession.getSession(), is);
            
            toRet.mPm = new ParsedMessage(mm, receivedDate, mbox.attachmentsIndexingEnabled());
        } catch (ServiceException ex) {
            ex.printStackTrace();
            throw new IOException("Cannot read serialized entry for CreateInvite "+ex.toString());
        }
        
        return toRet;
    }
    
    protected void serializeData(DataOutput out) throws IOException 
    {
        out.writeInt(mFolderId);
        out.writeInt(mApptId);
        
        try {
            serializeSetAppointmentData(out, mDefaultInvite);
        } catch(MessagingException me) { 
            throw new IOException("Caught MessagingException trying to serialize Default invite: "+me);
        }
        
        if (mExceptions == null) {
            out.writeInt(0);
        } else {
            out.writeInt(mExceptions.length);
            for (int i = 0; i < mExceptions.length; i++) {
                try {
                    serializeSetAppointmentData(out, mExceptions[i]);
                } catch(MessagingException me) { 
                    throw new IOException("Caught MessagingException trying to serialize Exception invite #"+i+": "+me);
                }
            }
        }
    }

    protected void deserializeData(DataInput in) throws IOException {
        mFolderId = in.readInt();
        mApptId = in.readInt();
        
        try {
            Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        
            mDefaultInvite = deserializeSetAppointmentData(in, mbox);
            
            int numExceptions = in.readInt();
            if (numExceptions > 0) {
                mExceptions = new Mailbox.SetAppointmentData[numExceptions];
                for (int i = 0; i < numExceptions; i++){
                    mExceptions[i] = deserializeSetAppointmentData(in, mbox);
                }
            }
        
        } catch (MessagingException ex) {
            ex.printStackTrace();
            throw new IOException("Cannot read serialized entry for SetAppointment"+ex.toString());
        } catch (ServiceException ex) {
            ex.printStackTrace();
            throw new IOException("Cannot read serialized entry for SetAppointment"+ex.toString());
        }
    }

    public SetAppointment() 
    {
        super(); 
    }
    
    public void setData(Mailbox.SetAppointmentData defaultInvite, Mailbox.SetAppointmentData exceptions[]) {
        mDefaultInvite = defaultInvite;
        mExceptions = exceptions;
    }
    
    public Mailbox.SetAppointmentData getDefaultData() {
        return mDefaultInvite;
    }
    
    public int getNumExceptions() {
        if (mExceptions == null) {
            return 0;
        }
        return mExceptions.length;
    }
    
    public Mailbox.SetAppointmentData getExceptionData(int exceptionNum) {
        return mExceptions[exceptionNum];
    }
    
    public int getAppointmentId() {
        return mApptId;
    }
    
    public void setAppointmentId(int id) {
        mApptId = id;
    }

    public int getOpCode() {
        return OP_SET_APPOINTMENT;
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = Mailbox.getMailboxById(mboxId);
        
        mbox.setAppointment(getOperationContext(), mFolderId, mDefaultInvite, mExceptions);
    }
    
    protected String getPrintableData() {
        StringBuffer toRet = new StringBuffer();
        toRet.append("Folder=").append(mFolderId).append(",");
        toRet.append("ApptId=").append(mApptId).append(",");
        toRet.append("Default=").append(mDefaultInvite.toString()).append("\n");
        if (mExceptions != null) {
            for (int i = 0; i < mExceptions.length; i++) {
                toRet.append("Exception").append(i).append("=").append(mExceptions[i].toString()).append("\n");
            }
        }
        return toRet.toString();
    }
}
