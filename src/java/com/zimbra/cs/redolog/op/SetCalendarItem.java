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

/* created 9/26/2005 */

package com.zimbra.cs.redolog.op;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;

import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.store.Volume;
import com.zimbra.cs.util.JMSession;


public class SetCalendarItem extends RedoableOp implements CreateCalendarItemRecorder, CreateCalendarItemPlayer 
{
    private int mFolderId;
    private int mCalendarItemId;
    private String mCalendarItemPartStat = IcalXmlStrMap.PARTSTAT_NEEDS_ACTION;
    private boolean mAttachmentIndexingEnabled;
    private Mailbox.SetCalendarItemData mDefaultInvite;
    private Mailbox.SetCalendarItemData mExceptions[];
    private short mVolumeId = -1;

    public SetCalendarItem() {}
    
    static void serializeSetCalendarItemData(RedoLogOutput out, Mailbox.SetCalendarItemData data) throws IOException, MessagingException {
        out.writeBoolean(data.mForce);
        
        ICalTimeZone localTz = data.mInv.getTimeZoneMap().getLocalTimeZone();
        out.writeUTF(localTz.encodeAsMetadata().toString());
        
        out.writeUTF(Invite.encodeMetadata(data.mInv).toString());
        
        if (data.mPm != null) {
            out.writeLong(data.mPm.getReceivedDate());
            
            byte[] pmData = data.mPm.getRawData();
            out.writeInt(pmData.length);
            out.write(pmData);
        }
    }
    
    private Mailbox.SetCalendarItemData deserializeSetCalendarItemData(
            RedoLogInput in, boolean attachmentIndexingEnabled)
    throws IOException, MessagingException {
        Mailbox.SetCalendarItemData toRet = new Mailbox.SetCalendarItemData();

        int mboxId = getMailboxId();
        try {
            toRet.mForce = in.readBoolean();
        
            ICalTimeZone localTz = ICalTimeZone.decodeFromMetadata(new Metadata(in.readUTF()));
            
            toRet.mInv = Invite.decodeMetadata(mboxId, new Metadata(in.readUTF()), null, localTz);
            
            long receivedDate = in.readLong();
            
            int dataLen = in.readInt();
            byte[] rawPmData = new byte[dataLen];
            in.readFully(rawPmData, 0, dataLen);

            InputStream is = new ByteArrayInputStream(rawPmData);
            MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), is);
            
            toRet.mPm = new ParsedMessage(mm, receivedDate, attachmentIndexingEnabled);
        } catch (ServiceException ex) {
            ex.printStackTrace();
            throw new IOException("Cannot read serialized entry for CreateInvite "+ex.toString());
        }
        
        return toRet;
    }
    
    protected void serializeData(RedoLogOutput out) throws IOException 
    {
        assert(getMailboxId() != 0);
        out.writeInt(mFolderId);
        if (getVersion().atLeast(1, 0))
            out.writeShort(mVolumeId);
        out.writeInt(mCalendarItemId);
        if (getVersion().atLeast(1, 1))
            out.writeUTF(mCalendarItemPartStat);
        if (getVersion().atLeast(1, 2))
            out.writeBoolean(mAttachmentIndexingEnabled);
        
        try {
            serializeSetCalendarItemData(out, mDefaultInvite);
        } catch(MessagingException me) { 
            throw new IOException("Caught MessagingException trying to serialize Default invite: "+me);
        }
        
        if (mExceptions == null) {
            out.writeInt(0);
        } else {
            out.writeInt(mExceptions.length);
            for (int i = 0; i < mExceptions.length; i++) {
                try {
                    serializeSetCalendarItemData(out, mExceptions[i]);
                } catch(MessagingException me) { 
                    throw new IOException("Caught MessagingException trying to serialize Exception invite #"+i+": "+me);
                }
            }
        }
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mFolderId = in.readInt();
        if (getVersion().atLeast(1, 0))
            mVolumeId = in.readShort();
        mCalendarItemId = in.readInt();
        if (getVersion().atLeast(1, 1))
            mCalendarItemPartStat = in.readUTF();
        if (getVersion().atLeast(1, 2))
            mAttachmentIndexingEnabled = in.readBoolean();
        else
            mAttachmentIndexingEnabled = false;
        
        try {
            mDefaultInvite = deserializeSetCalendarItemData(in, mAttachmentIndexingEnabled);
            
            int numExceptions = in.readInt();
            if (numExceptions > 0) {
                mExceptions = new Mailbox.SetCalendarItemData[numExceptions];
                for (int i = 0; i < numExceptions; i++){
                    mExceptions[i] = deserializeSetCalendarItemData(in, mAttachmentIndexingEnabled);
                }
            }
        
        } catch (MessagingException ex) {
            ex.printStackTrace();
            throw new IOException("Cannot read serialized entry for SetCalendarItem"+ex.toString());
        }
    }

    public SetCalendarItem(int mailboxId, boolean attachmentIndexingEnabled) 
    {
        super(); 
        setMailboxId(mailboxId);
        mAttachmentIndexingEnabled = attachmentIndexingEnabled;
    }
    
    public void setData(Mailbox.SetCalendarItemData defaultInvite, Mailbox.SetCalendarItemData exceptions[]) {
        mDefaultInvite = defaultInvite;
        mExceptions = exceptions;
    }
    
    public Mailbox.SetCalendarItemData getDefaultData() {
        return mDefaultInvite;
    }
    
    public int getNumExceptions() {
        if (mExceptions == null) {
            return 0;
        }
        return mExceptions.length;
    }
    
    public Mailbox.SetCalendarItemData getExceptionData(int exceptionNum) {
        return mExceptions[exceptionNum];
    }
    
    public void setCalendarItemAttrs(int calItemId,
                                     int folderId,
                                     short volumeId) {
        mCalendarItemId = calItemId;
        mFolderId = folderId;
        mVolumeId = volumeId;
    }
    
    public int getCalendarItemId() {
        return mCalendarItemId;
    }

    public String getCalendarItemPartStat() {
        return mCalendarItemPartStat;
    }

    public void setCalendarItemPartStat(String partStat) {
        mCalendarItemPartStat = partStat;
    }

    public int getFolderId() {
        return mFolderId;
    }
    
    public short getVolumeId() {
        if (mVolumeId == -1)
            return Volume.getCurrentMessageVolume().getId();
        else
            return mVolumeId;
    }

    public int getOpCode() {
        return OP_SET_CALENDAR_ITEM;
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);
        
        mbox.setCalendarItem(getOperationContext(), mFolderId, mDefaultInvite, mExceptions);
    }

    protected String getPrintableData() {
        StringBuffer toRet = new StringBuffer();
        toRet.append("calItemId=").append(mCalendarItemId).append(",");
        toRet.append("calItemPartStat=").append(mCalendarItemPartStat).append(".");
        toRet.append("folder=").append(mFolderId).append(",");
        if (getVersion().atLeast(1, 0))
            toRet.append(", vol=").append(mVolumeId);
        toRet.append("default=").append(mDefaultInvite.toString()).append("\n");
        if (mExceptions != null) {
            for (int i = 0; i < mExceptions.length; i++) {
                toRet.append("Exception").append(i).append("=").append(mExceptions[i].toString()).append("\n");
            }
        }
        return toRet.toString();
    }
}
