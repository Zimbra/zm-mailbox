/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on 2004. 7. 21.
 */
package com.zimbra.cs.redolog.op;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.redolog.RedoException;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogInputStream;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.Volume;

/**
 * @author jhahm
 */
public class CreateMessage extends RedoableOp
implements CreateCalendarItemPlayer,CreateCalendarItemRecorder {

    private static final long RECEIVED_DATE_UNSET = -1;

    private static final byte MSGBODY_INLINE = 1;   // message body buffer is included in this op
    private static final byte MSGBODY_LINK   = 2;   // message link information is included in this op

    private long mReceivedDate;     // email received date; not necessarily equal to operation time
    private String mRcptEmail;      // email address the message was delivered to
    // tracked for logging purpose only; useful because the same
    // mailbox may be addressed using any of the defined aliases
    private boolean mShared;        // whether message is shared with other mailboxes
    private String mDigest;			// Message blob is referenced by digest rather than blob ID.
    private int mMsgSize;			// original, uncompressed message size in bytes
    private int mMsgId;				// ID assigned to newly created message
    private int mFolderId;			// folder to which the message belongs
    private int mConvId;			// conversation to which the message belongs; may be newly created
    private int mConvFirstMsgId;    // first message of conversation, if creating new conversation
    private int mFlags;				// flags applied to the new message
    private String mTags;			// tags applied to the new message
    private int mCalendarItemId;    // new calendar item created if this is meeting or task invite message
    private String mCalendarItemPartStat = IcalXmlStrMap.PARTSTAT_NEEDS_ACTION;
    private boolean mNoICal;        // true if we should NOT process the iCalendar part
    private RedoableOpData mData;

    private byte mMsgBodyType;
    private String mPath;           // if mMsgBodyType == MSGBODY_LINK, source file to link to
    // if mMsgBodyType == MSGBODY_INLINE, path of saved blob file 
    private short mVolumeId = -1;   // volume on which this message file is saved
    private short mLinkSrcVolumeId = -1;  // volume on which the link source file is saved;
    // used only when mMsgBodyType == MSGBODY_LINK

    public CreateMessage() {
        mShared = false;
        mMsgId = UNKNOWN_ID;
        mFolderId = UNKNOWN_ID;
        mConvId = UNKNOWN_ID;
        mConvFirstMsgId = UNKNOWN_ID;
        mFlags = 0;
        mMsgBodyType = MSGBODY_INLINE;
        mNoICal = false;
    }

    protected CreateMessage(int mailboxId,
                String rcptEmail,
                boolean shared,
                String digest,
                int msgSize,
                int folderId,
                boolean noICal,
                int flags,
                String tags) {
        this(mailboxId, rcptEmail, RECEIVED_DATE_UNSET, shared, digest,
                    msgSize, folderId, noICal, flags, tags);
    }

    public CreateMessage(int mailboxId,
                String rcptEmail,
                long receivedDate,
                boolean shared,
                String digest,
                int msgSize,
                int folderId,
                boolean noICal,
                int flags,
                String tags) {
        setMailboxId(mailboxId);
        mRcptEmail = rcptEmail;
        mReceivedDate = receivedDate;
        mShared = shared;
        mDigest = digest != null ? digest : "";
        mMsgSize = msgSize;
        mMsgId = UNKNOWN_ID;
        mFolderId = folderId;
        mConvId = UNKNOWN_ID;
        mConvFirstMsgId = UNKNOWN_ID;
        mFlags = flags;
        mTags = tags != null ? tags : "";
        mMsgBodyType = MSGBODY_INLINE;
        mNoICal = noICal;
    }

    public void start(long timestamp) {
        super.start(timestamp);
        if (mReceivedDate == RECEIVED_DATE_UNSET)
            mReceivedDate = timestamp;
    }

    public synchronized void commit() {
        // Override commit() and abort().  Null out mData (reference to message
        // body byte array) after calling superclass' commit/abort.
        // Indexer keeps many IndexItem redo objects in memory because of batch
        // commit behavior, and each IndexItem object hangs on to CreateMessage
        // object. (this class)  If we don't null out mData, we would be keeping
        // the byte arrays around too.  So set it to null and let it get gc'd
        // early.
        //
        // Previously this was being done in overridden log() method, but that
        // was too early because log() can get called again if there is a log
        // rollover between log() and commit/abort() of a CreateMessage.
        // After commit() or abort(), the redo object is really finished with,
        // so nulling out mData member is safe.
        try {
            super.commit();
        } finally {
            mData = null;
        }
    }

    public synchronized void abort() {
        // see comments in commit()
        try {
            super.abort();
        } finally {
            mData = null;
        }
    }

    public int getMessageId() {
        return mMsgId;
    }

    public void setMessageId(int msgId) {
        mMsgId = msgId;
    }

    public int getConvId() {
        return mConvId;
    }

    public void setConvId(int convId) {
        mConvId = convId;
    }

    public int getConvFirstMsgId() {
        return mConvFirstMsgId;
    }

    public void setConvFirstMsgId(int convFirstMsgId) {
        mConvFirstMsgId = convFirstMsgId;
    }

    public void setCalendarItemAttrs(int calItemId, int folderId, short volumeId) {
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
    
    public int getFlags() { 
        return mFlags;
    }
    
    public String getTags() {
        return mTags;
    }        

    public short getVolumeId() {
        if (mVolumeId == -1)
            return Volume.getCurrentMessageVolume().getId();
        else
            return mVolumeId;
    }

    public int getOpCode() {
        return OP_CREATE_MESSAGE;
    }

    public byte[] getMessageBody() throws IOException {
        if (mMsgBodyType == MSGBODY_LINK) {
            return null;
        }
        return mData.getData();
    }

    public String getPath() {
        return mPath;
    }

    public void setMessageBodyInfo(byte[] data, String path, short volumeId) {
        mMsgBodyType = MSGBODY_INLINE;
        mData = new RedoableOpData(data);
        mPath = path;
        mVolumeId = volumeId;
    }
    
    public void setMessageBodyInfo(File dataFile, short volumeId) {
        mMsgBodyType = MSGBODY_INLINE;
        mData = new RedoableOpData(dataFile);
        mPath = dataFile.getPath();
        mVolumeId = volumeId;
    }

    public void setMessageLinkInfo(String linkSrcPath, short linkSrcVolumeId, short destVolumeId) {
        mMsgBodyType = MSGBODY_LINK;
        assert(linkSrcPath != null);
        mPath = linkSrcPath;
        mLinkSrcVolumeId = linkSrcVolumeId;
        mVolumeId = destVolumeId;
    }

    public String getRcptEmail() {
        return mRcptEmail;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mMsgId);
        sb.append(", rcpt=").append(mRcptEmail);
        sb.append(", rcvDate=").append(mReceivedDate);
        sb.append(", shared=").append(mShared ? "true" : "false");
        sb.append(", blobDigest=\"").append(mDigest).append("\", size=").append(mMsgSize);
        sb.append(", folder=").append(mFolderId);
        sb.append(", conv=").append(mConvId);
        sb.append(", convFirstMsgId=").append(mConvFirstMsgId);
        if (mCalendarItemId != UNKNOWN_ID)
            sb.append(", calItemId=").append(mCalendarItemId);
        sb.append(", calItemPartStat=").append(mCalendarItemPartStat);
        sb.append(", noICal=").append(mNoICal);
        sb.append(", flags=").append(mFlags).append(", tags=\"").append(mTags).append("\"");
        sb.append(", bodyType=").append(mMsgBodyType);
        sb.append(", vol=").append(mVolumeId);
        if (mMsgBodyType == MSGBODY_LINK) {
            sb.append(", linkSourcePath=").append(mPath);
            sb.append(", linkSrcVol=").append(mLinkSrcVolumeId);
        } else
            sb.append(", path=").append(mPath);
        return sb.toString();
    }

    @Override
    protected InputStream getAdditionalDataStream()
    throws IOException {
        if (mMsgBodyType == MSGBODY_INLINE) {
            return mData.getInputStream();
        } else {
            return null;
        }
    }
    
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(mRcptEmail != null ? mRcptEmail : "");
        if (getVersion().atLeast(1, 4))
            out.writeLong(mReceivedDate);
        out.writeBoolean(mShared);
        out.writeUTF(mDigest);
        out.writeInt(mMsgSize);
        out.writeInt(mMsgId);
        out.writeInt(mFolderId);
        out.writeInt(mConvId);
        if (getVersion().atLeast(1, 5))
            out.writeInt(mConvFirstMsgId);
        out.writeInt(mCalendarItemId);
        if (getVersion().atLeast(1, 1))
            out.writeUTF(mCalendarItemPartStat);
        out.writeInt(mFlags);
        out.writeBoolean(mNoICal);
        out.writeUTF(mTags);
        out.writeUTF(mPath);
        out.writeShort(mVolumeId);

        out.writeByte(mMsgBodyType);
        if (mMsgBodyType == MSGBODY_INLINE) {
            out.writeInt(mData.getLength());
            // During serialize, do not serialize the message data buffer.
            // Message buffer is handled by getSerializedByteArrayVector()
            // implementation in this class as the last vector element.
            // Consequently, in the serialized stream message data comes last.
            // deserializeData() should take this into account.
            //out.write(mData);  // Don't do this here!
        } else {
            out.writeShort(mLinkSrcVolumeId);
        }
    }
    
    protected void deserializeData(RedoLogInput in) throws IOException {
        mRcptEmail = in.readUTF();
        if (getVersion().atLeast(1, 4))
            mReceivedDate = in.readLong();
        else
            mReceivedDate = getTimestamp();
        mShared = in.readBoolean();
        mDigest = in.readUTF();
        mMsgSize = in.readInt();
        mMsgId = in.readInt();
        mFolderId = in.readInt();
        mConvId = in.readInt();
        if (getVersion().atLeast(1, 5))
            mConvFirstMsgId = in.readInt();
        mCalendarItemId = in.readInt();
        if (getVersion().atLeast(1, 1))
            mCalendarItemPartStat = in.readUTF();
        mFlags = in.readInt();
        mNoICal = in.readBoolean();
        mTags = in.readUTF();
        mPath = in.readUTF();
        mVolumeId = in.readShort();

        mMsgBodyType = in.readByte();
        if (mMsgBodyType == MSGBODY_INLINE) {
            int dataLength = in.readInt();
            int threshold = Integer.MAX_VALUE;
            try {
                threshold = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraMailDiskStreamingThreshold, Integer.MAX_VALUE);
            } catch (ServiceException e) {
                ZimbraLog.redolog.warn("Unable to determine disk streaming threshold.  Reading message into memory.", e);
            }
            if (dataLength <= threshold) {
                byte[] data = new byte[dataLength];
                in.readFully(data, 0, dataLength);
                mData = new RedoableOpData(data);
            } else {
                mData = new RedoableOpData(new RedoLogInputStream(in, dataLength), dataLength);
            }
            
            // Blob data must be the last thing deserialized.  See comments in
            // serializeData().
        } else {
            mLinkSrcVolumeId = in.readShort();
        }
    }
    
    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);

        List<Integer> mboxList = new ArrayList<Integer>(1);
        mboxList.add(new Integer(mboxId));
        SharedDeliveryContext sharedDeliveryCtxt = new SharedDeliveryContext(mShared, mboxList);
        ParsedMessage pm = null;
        
        if (mMsgBodyType == MSGBODY_LINK) {
            File file = new File(mPath);
            if (!file.exists())
                throw new RedoException("Missing link source blob " + mPath +
                            " (digest=" + mDigest + ")",
                            this);
            Blob src = new Blob(file, mLinkSrcVolumeId);

            // Manipulate the shared delivery context to make it look like we're calling addMessage
            // for 2nd or later of a shared delivery.  The purpose is to prevent addMessage from
            // saving the blob to incoming directory or write StoreIncomingBlob redo op.
            sharedDeliveryCtxt.setFirst(false);
            sharedDeliveryCtxt.setBlob(src);

            pm = new ParsedMessage(file, mReceivedDate, mbox.attachmentsIndexingEnabled());
        } else { // mMsgBodyType == MSGBODY_INLINE
            // Just one recipient.  Store the blob, then add the message.
            if (mData.hasDataInMemory()) {
                pm = new ParsedMessage(mData.getData(), mReceivedDate, mbox.attachmentsIndexingEnabled());
            } else {
                Volume volume = Volume.getCurrentMessageVolume();
                Blob blob = StoreManager.getInstance().storeIncoming(mData.getInputStream(), mData.getLength(), null, volume.getId());
                pm = new ParsedMessage(blob.getFile(), mReceivedDate, mbox.attachmentsIndexingEnabled());
            }
        }

        try {
            mbox.addMessage(getOperationContext(), pm, mFolderId, mNoICal, mFlags, mTags, mConvId, mRcptEmail, sharedDeliveryCtxt);
        } catch (MailServiceException e) {
            if (e.getCode() == MailServiceException.ALREADY_EXISTS) {
                mLog.info("Message " + mMsgId + " is already in mailbox " + mboxId);
                return;
            } else {
                throw e;
            }
        }
    }
}
