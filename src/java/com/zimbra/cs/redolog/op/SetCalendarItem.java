/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/* created 9/26/2005 */

package com.zimbra.cs.redolog.op;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.CalendarItem.ReplyInfo;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.util.JMSession;


public class SetCalendarItem extends RedoableOp implements CreateCalendarItemRecorder, CreateCalendarItemPlayer 
{
    private int mFolderId;
    private int mCalendarItemId;
    private String mCalendarItemPartStat = IcalXmlStrMap.PARTSTAT_NEEDS_ACTION;
    private boolean mAttachmentIndexingEnabled;
    private int mFlags;
    private long mTags;
    private Mailbox.SetCalendarItemData mDefaultInvite;
    private Mailbox.SetCalendarItemData mExceptions[];
    private List<ReplyInfo> mReplies;
    private long mNextAlarm;

    public SetCalendarItem() {}
    
    private void serializeSetCalendarItemData(RedoLogOutput out, Mailbox.SetCalendarItemData data)
    throws IOException {
        out.writeBoolean(true);  // keep this for backward compatibility with when SetCalendarItemData
                                 // used to have mForce field
        
        ICalTimeZone localTz = data.mInv.getTimeZoneMap().getLocalTimeZone();
        out.writeUTF(localTz.encodeAsMetadata().toString());
        
        out.writeUTF(Invite.encodeMetadata(data.mInv).toString());

        if (getVersion().atLeast(1, 24))
            out.writeBoolean(data.mPm != null);
        // If version is earlier than 1.24, we always have non-null data.mPm.
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

        long mboxId = getMailboxId();
        try {
            in.readBoolean();  // keep this for backward compatibility with when SetCalendarItemData
                               // used to have mForce field
        
            ICalTimeZone localTz = ICalTimeZone.decodeFromMetadata(new Metadata(in.readUTF()));
            
            toRet.mInv = Invite.decodeMetadata(mboxId, new Metadata(in.readUTF()), null, localTz);
            
            boolean hasPm;
            if (getVersion().atLeast(1, 24))
                hasPm = in.readBoolean();
            else
                hasPm = true;
            // If version is earlier than 1.24, we always have ParsedMessage array.
            if (hasPm) {
                long receivedDate = in.readLong();
                int dataLen = in.readInt();
                byte[] rawPmData = new byte[dataLen];
                in.readFully(rawPmData, 0, dataLen);
    
                InputStream is = new ByteArrayInputStream(rawPmData);
                MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), is);
                
                toRet.mPm = new ParsedMessage(mm, receivedDate, attachmentIndexingEnabled);
            }
        } catch (ServiceException ex) {
            ex.printStackTrace();
            throw new IOException("Cannot read serialized entry for CreateInvite "+ex.toString());
        }
        
        return toRet;
    }
    
    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        assert(getMailboxId() != 0);
        out.writeInt(mFolderId);
        if (getVersion().atLeast(1, 0))
            out.writeShort((short) -1);
        out.writeInt(mCalendarItemId);
        if (getVersion().atLeast(1, 1))
            out.writeUTF(mCalendarItemPartStat);
        if (getVersion().atLeast(1, 2))
            out.writeBoolean(mAttachmentIndexingEnabled);
        if (getVersion().atLeast(1, 11)) {
            out.writeInt(mFlags);
            out.writeLong(mTags);
        }

        boolean hasDefaultInvite = mDefaultInvite != null;
        if (getVersion().atLeast(1, 17))
            out.writeBoolean(hasDefaultInvite);
        if (hasDefaultInvite) {
            serializeSetCalendarItemData(out, mDefaultInvite);
        }
        
        if (mExceptions == null) {
            out.writeInt(0);
        } else {
            out.writeInt(mExceptions.length);
            for (int i = 0; i < mExceptions.length; i++) {
                serializeSetCalendarItemData(out, mExceptions[i]);
            }
        }

        if (getVersion().atLeast(1, 15)) {
            if (mReplies == null) {
                // special case: -1 means there was no replies list.  This is distinct from there
                // being a non-null list with 0 replies.  No list means to leave current replies
                // alone; 0-length list means to clear current list.
                out.writeInt(-1);
            } else {
                int num = mReplies.size();
                out.writeInt(num);
                for (ReplyInfo ri : mReplies) {
                    out.writeUTF(ri.encodeAsMetadata().toString());
                }
            }
        }

        if (getVersion().atLeast(1, 21)) {
            out.writeLong(mNextAlarm);
        }
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mFolderId = in.readInt();
        if (getVersion().atLeast(1, 0))
            in.readShort();
        mCalendarItemId = in.readInt();
        if (getVersion().atLeast(1, 1))
            mCalendarItemPartStat = in.readUTF();
        if (getVersion().atLeast(1, 2))
            mAttachmentIndexingEnabled = in.readBoolean();
        else
            mAttachmentIndexingEnabled = false;
        if (getVersion().atLeast(1, 11)) {
            mFlags = in.readInt();
            mTags = in.readLong();
        }

        Invite tzmapInv = null;
        boolean hasDefaultInvite = true;
        if (getVersion().atLeast(1, 17))
            hasDefaultInvite = in.readBoolean();
        try {
            if (hasDefaultInvite) {
                mDefaultInvite = deserializeSetCalendarItemData(in, mAttachmentIndexingEnabled);
                if (tzmapInv == null)
                    tzmapInv = mDefaultInvite.mInv;
            }
            int numExceptions = in.readInt();
            if (numExceptions > 0) {
                mExceptions = new Mailbox.SetCalendarItemData[numExceptions];
                for (int i = 0; i < numExceptions; i++){
                    mExceptions[i] = deserializeSetCalendarItemData(in, mAttachmentIndexingEnabled);
                    if (tzmapInv == null)
                        tzmapInv = mExceptions[i].mInv;
                }
            }
        } catch (MessagingException ex) {
            ex.printStackTrace();
            throw new IOException("Cannot read serialized entry for SetCalendarItem"+ex.toString());
        }

        if (getVersion().atLeast(1, 15)) {
            int num = in.readInt();
            if (num > 10000 && !getVersion().atLeast(1, 24)) {
                // Prior to redolog version 1.24 we had a problem in serialization code when ParsedMessage of
                // an Invite was null.  This couldn't happen before blobless appointment feature was added in
                // 5.0.16, but the bad serialization code has been around and so there was potential for
                // creating bad redolog.  The problem tends to manifest during deserialization as OutOfMemoryError
                // when allocating the mReplies ArrayList with a very large length.  Let's check for an arbitrarily
                // big size (10000) to detect it, and cause the deserialization to skip it by throwing an
                // exception.
                throw new IOException("Replies count > 10000.  Looks like a corrupted pre-v1.24 redo entry.  Skipping");
            }
            if (num < 0) {
                // no replies list
                mReplies = null;
            } else {
                mReplies = new ArrayList<ReplyInfo>(num);
                TimeZoneMap tzMap = tzmapInv.getTimeZoneMap();
                for (int i = 0; i < num; i++) {
                    String data = in.readUTF();
                    try {
                        Metadata md = new Metadata(data);
                        ReplyInfo reply = ReplyInfo.decodeFromMetadata(md, tzMap);
                        mReplies.add(reply);
                    } catch (ServiceException e) {
                        IOException ioe = new IOException("Cannot read serialized entry for ReplyInfo");
                        ioe.initCause(e);
                        throw ioe;
                    }
                }
            }
        }

        if (getVersion().atLeast(1, 21)) {
            mNextAlarm = in.readLong();
        }
    }

    public SetCalendarItem(long mailboxId, boolean attachmentIndexingEnabled,
                           int flags, long tags) {
        super(); 
        setMailboxId(mailboxId);
        mAttachmentIndexingEnabled = attachmentIndexingEnabled;
        mFlags = flags;
        mTags = tags;
    }
    
    public void setData(Mailbox.SetCalendarItemData defaultInvite,
                        Mailbox.SetCalendarItemData exceptions[],
                        List<ReplyInfo> replies, long nextAlarm) {
        mDefaultInvite = defaultInvite;
        mExceptions = exceptions;
        mReplies = replies;
        mNextAlarm = nextAlarm;
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

    public void setCalendarItemAttrs(int calItemId, int folderId) {
        mCalendarItemId = calItemId;
        mFolderId = folderId;
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

    @Override public int getOpCode() {
        return OP_SET_CALENDAR_ITEM;
    }

    @Override protected String getPrintableData() {
        StringBuffer toRet = new StringBuffer();
        toRet.append("calItemId=").append(mCalendarItemId);
        toRet.append(", calItemPartStat=").append(mCalendarItemPartStat);
        toRet.append(", folder=").append(mFolderId);
        if (getVersion().atLeast(1, 11)) {
            toRet.append(", flags=").append(mFlags);
            toRet.append(", tags=").append(mTags);
        }
        toRet.append("\n");
        if (mDefaultInvite != null)
            toRet.append("Default=").append(mDefaultInvite.toString()).append("\n");
        if (mExceptions != null) {
            for (int i = 0; i < mExceptions.length; i++) {
                toRet.append("Exception").append(i).append("=").append(mExceptions[i].toString()).append("\n");
            }
        }
        if (mReplies != null) {
            int i = 0;
            for (ReplyInfo ri : mReplies) {
                toRet.append("Reply").append(i).append("=").append(ri.toString()).append("\n");
                i++;
            }
        }
        toRet.append("nextAlarm=").append(mNextAlarm).append("\n");
        return toRet.toString();
    }

    public void redo() throws Exception {
        long mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);
        
        mbox.setCalendarItem(getOperationContext(), mFolderId, mFlags, mTags,
                             mDefaultInvite, mExceptions, mReplies, mNextAlarm);
    }
}
