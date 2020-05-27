/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.util.TagUtil;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.ParsedMessageOptions;
import com.zimbra.cs.redolog.RedoException;
import com.zimbra.cs.redolog.RedoLogBlobStore.PendingRedoBlobOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;

public class CreateMessage extends RedoableOp
implements CreateCalendarItemPlayer, CreateCalendarItemRecorder, BlobRecorder {

    private static final long RECEIVED_DATE_UNSET = -1;

    protected static final byte MSGBODY_INLINE = 1;   // message body buffer is included in this op
    protected static final byte MSGBODY_LINK   = 2;   // message link information is included in this op
    protected static final byte MSGBODY_EXTERNAL = 3;   // digest of blob, to be looked up in RedoLogBlobStore

    protected long mReceivedDate;     // email received date; not necessarily equal to operation time
    private String mRcptEmail;      // email address the message was delivered to
                                    // tracked for logging purpose only; useful because the same
                                    // mailbox may be addressed using any of the defined aliases
    private boolean mShared;        // whether message is shared with other mailboxes
    private String mDigest;         // Message blob is referenced by digest rather than blob ID.
    protected long mMsgSize;        // original, uncompressed message size in bytes
    private int mMsgId;             // ID assigned to newly created message
    private int mFolderId;          // folder to which the message belongs
    private int mConvId;            // conversation to which the message belongs; may be newly created
    private int mConvFirstMsgId;    // first message of conversation, if creating new conversation
    private List<Integer> mMergedConvIds;  // existing conversations to merge into the message's conversation
    private int mFlags;             // flags applied to the new message
    private String[] mTags;         // tags applied to the new message
    private String mTagIds;         // (deprecated) tag ids applied to the new message
    private int mCalendarItemId;    // new calendar item created if this is meeting or task invite message
    private String mCalendarItemPartStat = IcalXmlStrMap.PARTSTAT_NEEDS_ACTION;
    private boolean mNoICal;        // true if we should NOT process the iCalendar part
    private CustomMetadata mExtendedData; // extra data associated with the message at delivery time
    protected RedoableOpData mData;

    protected byte mMsgBodyType;
    protected String mPath;           // if mMsgBodyType == MSGBODY_LINK, source file to link to
                                      // if mMsgBodyType == MSGBODY_INLINE, path of saved blob file
                                      // if mMsgBodyType == MSGBODY_EXTERNAL, unset
    protected PendingRedoBlobOperation pendingRedoBlobOp = null;


    public CreateMessage() {
        super(MailboxOperation.CreateMessage);
        mShared = false;
        mMsgId = UNKNOWN_ID;
        mFolderId = UNKNOWN_ID;
        mConvId = UNKNOWN_ID;
        mConvFirstMsgId = UNKNOWN_ID;
        mMergedConvIds = Collections.emptyList();
        mFlags = 0;
        mMsgBodyType = MSGBODY_EXTERNAL;
        mNoICal = false;
    }

    protected CreateMessage(int mailboxId, String rcptEmail, boolean shared,
                            String digest, long msgSize, int folderId, boolean noICal,
                            int flags, String[] tags) {
        this(mailboxId, rcptEmail, RECEIVED_DATE_UNSET, shared, digest, msgSize, folderId, noICal, flags, tags, null);
    }

    public CreateMessage(int mailboxId, String rcptEmail, long receivedDate,
                         boolean shared, String digest, long msgSize, int folderId,
                         boolean noICal, int flags, String[] tags, CustomMetadata extended) {
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
        mTags = tags != null ? tags : new String[0];
        mMsgBodyType = MSGBODY_EXTERNAL;
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
            if (pendingRedoBlobOp != null) {
                pendingRedoBlobOp.commit();
                pendingRedoBlobOp = null;
            }
        } finally {
            mData = null;
            if (pendingRedoBlobOp != null) {
                pendingRedoBlobOp.abort();
                pendingRedoBlobOp = null;
            }
        }
    }

    @Override public synchronized void abort() {
        // see comments in commit()
        try {
            super.abort();
        } finally {
            mData = null;
            pendingRedoBlobOp = null;
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

    public void setFlags(int flags) {
        mFlags = flags;
    }

    public String[] getTags() {
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
        sb.append(", flags=").append(mFlags);
        sb.append(", tags=[").append(mTags == null ? "" : StringUtil.join(",", mTags)).append("]");
        sb.append(", bodyType=").append(mMsgBodyType);
        if (mMsgBodyType == MSGBODY_LINK) {
            sb.append(", linkSourcePath=").append(mPath);
        } else if (mMsgBodyType == MSGBODY_INLINE){
            sb.append(", path=").append(mPath);
        } else {
            sb.append(", [external blob]");
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
        out.writeLong(mMsgSize);
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
        if (getVersion().atLeast(1, 33)) {
            out.writeUTFArray(mTags);
        } else {
            out.writeUTF(mTagIds);
        }
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
        if (getVersion().atLeast(1, 42)) {
            mMsgSize = in.readLong();
        } else {
            mMsgSize = in.readInt();
        }
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
        if (getVersion().atLeast(1, 33)) {
            mTags = in.readUTFArray();
        } else {
            mTagIds = in.readUTF();
        }
        mPath = in.readUTF();
        in.readShort();
        if (getVersion().atLeast(1, 25)) {
            String extendedKey = in.readUTF();
            if (extendedKey != null) {
                try {
                    mExtendedData = new CustomMetadata(extendedKey, in.readUTF());
                } catch (ServiceException e) {
                    mLog.warn("could not deserialize custom metadata for message", e);
                }
            }
        }

        mMsgBodyType = in.readByte();
        if (mMsgBodyType == MSGBODY_INLINE) {
            int dataLength = in.readInt();
            boolean inMemory = false;
            try {
                inMemory = dataLength <= StoreManager.getDiskStreamingThreshold();
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

    DeliveryOptions getDeliveryOptions() {
        return new DeliveryOptions()
            .setFolderId(mFolderId)
            .setNoICal(mNoICal)
            .setFlags(mFlags)
            .setTags(mTags)
            .setConversationId(mConvId)
            .setRecipientEmail(mRcptEmail)
            .setCustomMetadata(mExtendedData);
    }

    @Override
    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);
        OperationContext octxt = getOperationContext();

        if (mTags == null && mTagIds != null) {
            mTags = TagUtil.tagIdStringToNames(mbox, octxt, mTagIds);
        }

        DeliveryContext dctxt = new DeliveryContext(mShared, Arrays.asList(mboxId));

        Blob blob = null;
        if (mMsgBodyType == MSGBODY_LINK) {
            // backwards compatibility - if using StoreIncomingBlob ops,
            // the blob is referenced by its path
            blob = mRedoLogMgr.getBlobStore().fetchBlob(mPath);
        } else if (mMsgBodyType == MSGBODY_EXTERNAL) {
            blob = mRedoLogMgr.getBlobStore().fetchBlob(getBlobDigest());
        }
        if (mMsgBodyType == MSGBODY_LINK || mMsgBodyType == MSGBODY_EXTERNAL) {
            if (blob == null)
                throw new RedoException("Missing link source blob " + mPath + " (digest=" + mDigest + ")", this);
            dctxt.setIncomingBlob(blob);

            ParsedMessage pm = null;
            try {
                ParsedMessageOptions opt = new ParsedMessageOptions()
                    .setContent(blob.getFile())
                    .setReceivedDate(mReceivedDate)
                    .setAttachmentIndexing(mbox.attachmentsIndexingEnabled())
                    .setSize(mMsgSize)
                    .setDigest(mDigest);
                pm = new ParsedMessage(opt);
                mbox.addMessage(octxt, pm, getDeliveryOptions(), dctxt);
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
                mbox.addMessage(octxt, in, mMsgSize, mReceivedDate, getDeliveryOptions(), dctxt);
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

    @Override
    public String getBlobDigest() {
        return mDigest;
    }

    @Override
    public void setRedoBlobOperation(PendingRedoBlobOperation op) {
        this.pendingRedoBlobOp = op;
    }
}
