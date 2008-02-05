/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.ParsedMessage;

import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.store.Volume;

public class CreateInvite extends RedoableOp implements CreateCalendarItemRecorder, CreateCalendarItemPlayer 
{
    private int mCalendarItemId;
    private String mCalendarItemPartStat = IcalXmlStrMap.PARTSTAT_NEEDS_ACTION;
    private Invite mInvite;
    private int mFolderId;
    private byte[] mData;
    private short mVolumeId = -1;
    
    public CreateInvite() { }

    public CreateInvite(int mailboxId, Invite inv, int folderId,
    					byte[] data) {
        setMailboxId(mailboxId);
        mInvite = inv;
        mFolderId = folderId;
        mData = data;
    }

    public int getOpCode() {
        return OP_CREATE_INVITE;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("calItemId=").append(mCalendarItemId);
        sb.append(", calItemPartStat=").append(mCalendarItemPartStat);
        sb.append(", folder=").append(mFolderId);
    	if (getVersion().atLeast(1, 0))
	        sb.append(", vol=").append(mVolumeId);
        sb.append(", dataLen=").append(mData != null ? mData.length : 0);
        ICalTimeZone localTz = mInvite.getTimeZoneMap().getLocalTimeZone();
        sb.append(", localTZ=").append(localTz.encodeAsMetadata().toString());
        sb.append(", inv=").append(Invite.encodeMetadata(mInvite).toString());
        return sb.toString();
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mCalendarItemId);
        if (getVersion().atLeast(1, 1))
            out.writeUTF(mCalendarItemPartStat);
        out.writeInt(mFolderId);
        if (getVersion().atLeast(1, 0))
            out.writeShort(mVolumeId);
        out.writeBoolean(true);  // keep this for backward compatibility when there was mForce field
                                 // in this class
        
        int dataLen = mData != null ? mData.length : 0;
        out.writeInt(dataLen);
        if (dataLen > 0) {
        	out.write(mData);
        }
        
        ICalTimeZone localTz = mInvite.getTimeZoneMap().getLocalTimeZone();
        out.writeUTF(localTz.encodeAsMetadata().toString());
        out.writeUTF(Invite.encodeMetadata(mInvite).toString());
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mCalendarItemId = in.readInt();
        if (getVersion().atLeast(1, 1))
            mCalendarItemPartStat = in.readUTF();
        mFolderId = in.readInt();
        if (getVersion().atLeast(1, 0))
            mVolumeId = in.readShort();
        in.readBoolean();  // keep this for backward compatibility when there was mForce field
                           // in this class
        
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
    
    public void setCalendarItemAttrs(int appointmentId,
								     int folderId,
								     short volumeId) {
		mCalendarItemId = appointmentId;
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

    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
		ParsedMessage pm = new ParsedMessage(mData, getTimestamp(), mbox.attachmentsIndexingEnabled());
		mbox.addInvite(getOperationContext(), mInvite, mFolderId, pm);
    }
}
