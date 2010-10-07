/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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

public class CreateInvite extends RedoableOp implements CreateCalendarItemRecorder, CreateCalendarItemPlayer 
{
    private int mCalendarItemId;
    private String mCalendarItemPartStat = IcalXmlStrMap.PARTSTAT_NEEDS_ACTION;
    private Invite mInvite;
    private int mFolderId;
    private byte[] mData;
    private boolean mPreserveExistingAlarms;
    private boolean mDiscardExistingInvites;
    private boolean mAddRevision;
    
    public CreateInvite() { }

    public CreateInvite(int mailboxId, Invite inv, int folderId, byte[] data,
                        boolean preserveExistingAlarms, boolean discardExistingInvites, boolean addRevision) {
        setMailboxId(mailboxId);
        mInvite = inv;
        mFolderId = folderId;
        mData = data;
        mPreserveExistingAlarms = preserveExistingAlarms;
        mDiscardExistingInvites = discardExistingInvites;
        mAddRevision = addRevision;
    }

    @Override public int getOpCode() {
        return OP_CREATE_INVITE;
    }

    @Override protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("calItemId=").append(mCalendarItemId);
        sb.append(", calItemPartStat=").append(mCalendarItemPartStat);
        sb.append(", folder=").append(mFolderId);
        sb.append(", dataLen=").append(mData != null ? mData.length : 0);
        ICalTimeZone localTz = mInvite.getTimeZoneMap().getLocalTimeZone();
        sb.append(", localTZ=").append(localTz.encodeAsMetadata().toString());
        sb.append(", inv=").append(Invite.encodeMetadata(mInvite).toString());
        sb.append(", preserveExistingAlarms=").append(mPreserveExistingAlarms);
        sb.append(", discardExistingInvites=").append(mDiscardExistingInvites);
        sb.append(", addRevision=").append(mAddRevision);
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mCalendarItemId);
        if (getVersion().atLeast(1, 1))
            out.writeUTF(mCalendarItemPartStat);
        out.writeInt(mFolderId);
        if (getVersion().atLeast(1, 0))
            out.writeShort((short) -1);
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

        if (getVersion().atLeast(1, 22)) {
            out.writeBoolean(mPreserveExistingAlarms);
            out.writeBoolean(mDiscardExistingInvites);
        }
        if (getVersion().atLeast(1, 23))
            out.writeBoolean(mAddRevision);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mCalendarItemId = in.readInt();
        if (getVersion().atLeast(1, 1))
            mCalendarItemPartStat = in.readUTF();
        mFolderId = in.readInt();
        if (getVersion().atLeast(1, 0))
            in.readShort();
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

        if (getVersion().atLeast(1, 22)) {
            mPreserveExistingAlarms = in.readBoolean();
            mDiscardExistingInvites = in.readBoolean();
        } else {
            mPreserveExistingAlarms = false;
            mDiscardExistingInvites = false;
        }
        if (getVersion().atLeast(1, 23))
            mAddRevision = in.readBoolean();
        else
            mAddRevision = false;
    }

    @Override public void setCalendarItemAttrs(int appointmentId, int folderId) {
        mCalendarItemId = appointmentId;
        mFolderId = folderId;
    }

    @Override public int getCalendarItemId() {
        return mCalendarItemId;
    }

    @Override public String getCalendarItemPartStat() {
        return mCalendarItemPartStat;
    }

    @Override public void setCalendarItemPartStat(String partStat) {
        mCalendarItemPartStat = partStat;
    }

    @Override public int getFolderId() {
        return mFolderId;
    }

    @Override public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        ParsedMessage pm = null;
        if (mData != null && mData.length > 0)
            pm = new ParsedMessage(mData, getTimestamp(), mbox.attachmentsIndexingEnabled());
        mbox.addInvite(getOperationContext(), mInvite, mFolderId, pm,
                       mPreserveExistingAlarms, mDiscardExistingInvites, mAddRevision);
    }
}
