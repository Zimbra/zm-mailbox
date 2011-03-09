/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on 2004. 7. 21.
 */
package com.zimbra.cs.redolog.op;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.activation.DataSource;

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.ParsedMessageOptions;
import com.zimbra.cs.redolog.RedoException;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StorageCallback;

public class CreateMessage extends RedoableOp
implements CreateCalendarItemPlayer, CreateCalendarItemRecorder {

    private static final long RECEIVED_DATE_UNSET = -1;

    private static final byte MSGBODY_INLINE = 1;   // message body buffer is included in this op
    private static final byte MSGBODY_LINK   = 2;   // message link information is included in this op

    private long mReceivedDate;     // email received date; not necessarily equal to operation time
    private String mRcptEmail;      // email address the message was delivered to
                                    // tracked for logging purpose only; useful because the same
                                    // mailbox may be addressed using any of the defined aliases
    private boolean mShared;        // whether message is shared with other mailboxes
    private String mDigest;         // Message blob is referenced by digest rather than blob ID.
    protected int mMsgSize;         // original, uncompressed message size in bytes
    private int mMsgId;             // ID assigned to newly created message
    private int mFolderId;          // folder to which the message belongs
    private int mConvId;            // conversation to which the message belongs; may be newly created
    private int mConvFirstMsgId;    // first message of conversation, if creating new conversation
    private List<Integer> mMergedConvIds;  // existing conversations to merge into the message's conversation
    private int mFlags;             // flags applied to the new message
    private String mTags;           // tags applied to the new message
    private int mCalendarItemId;    // new calendar item created if this is meeting or task invite message
    private String mCalendarItemPartStat = IcalXmlStrMap.PARTSTAT_NEEDS_ACTION;
    private boolean mNoICal;        // true if we should NOT process the iCalendar part
    private CustomMetadata mExtendedData; // extra data associated with the message at delivery time
    protected RedoableOpData mData;

    private byte mMsgBodyType;
    private String mPath;           // if mMsgBodyType == MSGBODY_LINK, source file to link to
    // if mMsgBodyType == MSGBODY_INLINE, path of saved blob file 

    public CreateMessage() {
        super(MailboxOperation.CreateMessage);
        mShared = false;
        mMsgId = UNKNOWN_ID;
        mFolderId = UNKNOWN_ID;
        mConvId = UNKNOWN_ID;
        mConvFirstMsgId = UNKNOWN_ID;
        mMergedConvIds = Collections.emptyList();
        mFlags = 0;
        mMsgBodyType = MSGBODY_INLINE;
        mNoICal = false;
    }

    protected CreateMessage(int mailboxId, String rcptEmail, boolean shared,
                            String digest, int msgSize, int folderId, boolean noICal,
                            int flags, String tags) {
        this(mailboxId, rcptEmail, RECEIVED_DATE_UNSET, shared, digest, msgSize, folderId, noICal, flags, tags, null);
    }

    public CreateMessage(int mailboxId, String rcptEmail, long receivedDate,
                         boolean shared, String digest, int msgSize, int folderId,
                         boolean noICal, int flags, String tags, CustomMetadata extended) {
        super(MailboxOperation.CreateMessage);
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
        mMergedConvIds = Collections.emptyList();
        mFlags = flags;
        mTags = tags != null ? tags : "";
        mMsgBodyType = MSGBODY_INLINE;
        mNoICal = noICal;
        mExtendedData = extended;
    }

    @Override public void start(long timestamp) {
        super.start(timestamp);
        if (mReceivedDate == RECEIVED_DATE_UNSET) {
            mReceivedDate = timestamp;
        }
    }

    @Override public synchronized void commit() {
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

    @Override public synchronized void abort() {
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

    public List<Integer> getMergedConvIds() {
        return mMergedConvIds;
    }

    public void setMergedConvIds(List<Integer> mergedConvIds) {
        mMergedConvIds = mergedConvIds == null ? Collections.<Integer>emptyList() : mergedConvIds;
    }

    public void setMergedConversations(List<Conversation> mergedConvs) {
        if (mergedConvs == null) {
            mMergedConvIds = Collections.emptyList();
        } else {
            mMergedConvIds = Lists.newArrayList();
            for (Conversation conv : mergedConvs) {
                mMergedConvIds.add(conv.getId());
            }
        }
    }

    @Override
    public void setCalendarItemAttrs(int calItemId, int folderId) {
        mCalendarItemId = calItemId;
        mFolderId = folderId;
    }

    @Override
    public int getCalendarItemId() {
        return mCalendarItemId;
    }

    @Override
    public String getCalendarItemPartStat() {
        return mCalendarItemPartStat;
    }

    @Override
    public void setCalendarItemPartStat(String partStat) {
        mCalendarItemPartStat = partStat;
    }

    @Override
    public int getFolderId() {
        return mFolderId;
    }
    
    public int getFlags() { 
        return mFlags;
    }
    
    public String getTags() {
        return mTags;
    }        

    public byte[] getMessageBody() throws IOException {
        if (mMsgBodyType == MSGBODY_LINK)
            return null;
        return mData.getData();
    }

    public String getPath() {
        return mPath;
    }

    public void setMessageBodyInfo(DataSource ds, long size) {
        mMsgBodyType = MSGBODY_INLINE;
        mData = new RedoableOpData(ds, (int) size);
        mPath = ":streamed:";
    }
    
    public void setMessageBodyInfo(File dataFile) {
        mMsgBodyType = MSGBODY_INLINE;
        mData = new RedoableOpData(dataFile);
        mPath = dataFile.getPath();
    }

    public void setMessageLinkInfo(String linkSrcPath) {
        mMsgBodyType = MSGBODY_LINK;
        assert(linkSrcPath != null);
        mPath = linkSrcPath;
    }

    public String getRcptEmail() {
        return mRcptEmail;
    }
    
    protected RedoableOpData getData() {
        return mData;
    }

    @Override protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("id=").append(mMsgId);
        sb.append(", rcpt=").append(mRcptEmail);
        sb.append(", rcvDate=").append(mReceivedDate);
        sb.append(", shared=").append(mShared ? "true" : "false");
        sb.append(", blobDigest=\"").append(mDigest).append("\", size=").append(mMsgSize);
        if (mData != null) {
            sb.append(", dataLen=").append(mData.getLength());
        }
        sb.append(", folder=").append(mFolderId);
        sb.append(", conv=").append(mConvId);
        sb.append(", convFirstMsgId=").append(mConvFirstMsgId);
        if (mMergedConvIds != null && !mMergedConvIds.isEmpty()) {
            sb.append(", mergedConvIds=").append(mMergedConvIds);
        }
        if (mCalendarItemId != UNKNOWN_ID) {
            sb.append(", calItemId=").append(mCalendarItemId);
        }
        sb.append(", calItemPartStat=").append(mCalendarItemPartStat);
        sb.append(", noICal=").append(mNoICal);
        if (mExtendedData != null) {
            sb.append(", extended=").append(mExtendedData);
        }
        sb.append(", flags=").append(mFlags).append(", tags=\"").append(mTags).append("\"");
        sb.append(", bodyType=").append(mMsgBodyType);
        if (mMsgBodyType == MSGBODY_LINK) {
            sb.append(", linkSourcePath=").append(mPath);
        } else {
            sb.append(", path=").append(mPath);
        }
        return sb.toString();
    }

    @Override
    public InputStream getAdditionalDataStream() throws IOException {
        if (mMsgBodyType == MSGBODY_INLINE) {
            return mData.getInputStream();
        } else {
            return null;
        }
    }
    
    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(mRcptEmail != null ? mRcptEmail : "");
        if (getVersion().atLeast(1, 4)) {
            out.writeLong(mReceivedDate);
        }
        out.writeBoolean(mShared);
        out.writeUTF(mDigest);
        out.writeInt(mMsgSize);
        out.writeInt(mMsgId);
        out.writeInt(mFolderId);
        out.writeInt(mConvId);
        if (getVersion().atLeast(1, 5)) {
            out.writeInt(mConvFirstMsgId);
        }
        if (getVersion().atLeast(1, 32)) {
            out.writeInt(mMergedConvIds.size());
            for (int mergeId : mMergedConvIds) {
                out.writeInt(mergeId);
            }
        }
        out.writeInt(mCalendarItemId);
        if (getVersion().atLeast(1, 1)) {
            out.writeUTF(mCalendarItemPartStat);
        }
        out.writeInt(mFlags);
        out.writeBoolean(mNoICal);
        out.writeUTF(mTags);
        out.writeUTF(mPath);
        out.writeShort((short) -1);
        if (getVersion().atLeast(1, 25)) {
            if (mExtendedData == null) {
                out.writeUTF(null);
            } else {
                out.writeUTF(mExtendedData.getSectionKey());
                out.writeUTF(mExtendedData.getSerializedValue());
            }
        }

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
            out.writeShort((short) -1);
        }
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mRcptEmail = in.readUTF();
        if (getVersion().atLeast(1, 4)) {
            mReceivedDate = in.readLong();
        } else {
            mReceivedDate = getTimestamp();
        }
        mShared = in.readBoolean();
        mDigest = in.readUTF();
        mMsgSize = in.readInt();
        mMsgId = in.readInt();
        mFolderId = in.readInt();
        mConvId = in.readInt();
        if (getVersion().atLeast(1, 5)) {
            mConvFirstMsgId = in.readInt();
        }
        if (getVersion().atLeast(1, 32)) {
            int mergeCount = in.readInt();
            mMergedConvIds = new ArrayList<Integer>(mergeCount);
            for (int i = 0; i < mergeCount; i++) {
                mMergedConvIds.add(in.readInt());
            }
        }
        mCalendarItemId = in.readInt();
        if (getVersion().atLeast(1, 1)) {
            mCalendarItemPartStat = in.readUTF();
        }
        mFlags = in.readInt();
        mNoICal = in.readBoolean();
        mTags = in.readUTF();
        mPath = in.readUTF();
        in.readShort();
        if (getVersion().atLeast(1, 25)) {
            mExtendedData = null;
            String extendedKey = in.readUTF();
            if (extendedKey != null) {
                mExtendedData = new CustomMetadata(extendedKey, in.readUTF());
            }
        }

        mMsgBodyType = in.readByte();
        if (mMsgBodyType == MSGBODY_INLINE) {
            int dataLength = in.readInt();
            boolean inMemory = false;
            try {
                inMemory = dataLength <= StorageCallback.getDiskStreamingThreshold();
            } catch (ServiceException e) {}

            // mData must be the last thing deserialized.  See comments in serializeData()
            if (inMemory) {
                byte[] data = new byte[dataLength];
                in.readFully(data, 0, dataLength);
                mData = new RedoableOpData(data);
            } else {
                long pos = in.getFilePointer();
                mData = new RedoableOpData(new File(in.getPath()), pos, dataLength);
                
                // Now that we have a stream to the data, skip to the next op.
                int numSkipped = in.skipBytes(dataLength);
                if (numSkipped != dataLength) {
                    String msg = String.format("Attempted to skip %d bytes at position %d in %s, but actually skipped %d.",
                            dataLength, pos, in.getPath(), numSkipped);
                    throw new IOException(msg);
                }
            }

            // Blob data must be the last thing deserialized.  See comments in
            // serializeData().
        } else {
            in.readShort();
        }
    }

    @Override
    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);

        DeliveryContext deliveryCtxt = new DeliveryContext(mShared, Arrays.asList(mboxId));
        
        if (mMsgBodyType == MSGBODY_LINK) {
            Blob blob = StoreIncomingBlob.fetchBlob(mPath); 
            if (blob == null)
                throw new RedoException("Missing link source blob " + mPath + " (digest=" + mDigest + ")", this);
            deliveryCtxt.setIncomingBlob(blob);

            ParsedMessage pm = null;
            try {
                ParsedMessageOptions opt = new ParsedMessageOptions()
                    .setContent(blob.getFile())
                    .setReceivedDate(mReceivedDate)
                    .setAttachmentIndexing(mbox.attachmentsIndexingEnabled())
                    .setSize(mMsgSize)
                    .setDigest(mDigest);
                pm = new ParsedMessage(opt);
                mbox.addMessage(getOperationContext(), pm, mFolderId, mNoICal, mFlags,
                                mTags, mConvId, mRcptEmail, mExtendedData, deliveryCtxt);
            } catch (MailServiceException e) {
                if (e.getCode() == MailServiceException.ALREADY_EXISTS) {
                    mLog.info("Message " + mMsgId + " is already in mailbox " + mboxId);
                    return;
                } else {
                    throw e;
                }
            } finally {
                if (pm != null) {
                    ByteUtil.closeStream(pm.getBlobInputStream());
                }
            }
        } else { // mMsgBodyType == MSGBODY_INLINE
            // Just one recipient.  Blob data is stored inline.
            InputStream in = null;
            try {
                in = mData.getInputStream();
                if (mData.getLength() != mMsgSize) {
                    in = new GZIPInputStream(in);
                }
                mbox.addMessage(getOperationContext(), in, mMsgSize, mReceivedDate, mFolderId, mNoICal, mFlags,
                    mTags, mConvId, mRcptEmail, mExtendedData, deliveryCtxt); 
            } catch (MailServiceException e) {
                if (e.getCode() == MailServiceException.ALREADY_EXISTS) {
                    mLog.info("Message " + mMsgId + " is already in mailbox " + mboxId);
                    return;
                } else {
                    throw e;
                }
            } finally {
                ByteUtil.closeStream(in);
            }
        }
    }
}
