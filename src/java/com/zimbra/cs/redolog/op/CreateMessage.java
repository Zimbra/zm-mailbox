/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on 2004. 7. 21.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.mail.internet.MimeMessage;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.redolog.RedoException;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.JMSession;


/**
 * @author jhahm
 */
public class CreateMessage extends RedoableOp {

    private static final byte MSGBODY_INLINE = 1;   // message body buffer is included in this op
    private static final byte MSGBODY_LINK   = 2;   // message link information is included in this op

    private String mRcptEmail;      // email address the message was delivered to
                                    // tracked for logging purpose only; useful because the same
                                    // mailbox may be addressed using any of the defined aliases
    private boolean mShared;        // whether message is shared with other mailboxes
    private String mDigest;			// Message blob is referenced by digest rather than blob ID.
	private int mMsgSize;			// original, uncompressed message size in bytes
	private int mMsgId;				// ID assigned to newly created message
	private int mFolderId;			// folder to which the message belongs
	private int mConvId;			// conversation to which the message belongs; may be newly created
	private int mFlags;				// flags applied to the new message
	private String mTags;			// tags applied to the new message
    private int mAppointmentId;     // new appointment created if this is meeting invite message
    private boolean mNoICal;        // true if we should NOT process the iCalendar part

    private byte mMsgBodyType;
    private byte[] mData;           // used if mMsgBodyType == MSGBODY_INLINE
    private String mPath;           // if mMsgBodyType == MSGBODY_LINK, source file to link to
                                    // if mMsgBodyType == MSGBODY_INLINE, path of saved blob file 
    private short mVolumeId = -1;   // volume on which this message file is saved

	public CreateMessage() {
        mShared = false;
		mMsgId = UNKNOWN_ID;
		mFolderId = UNKNOWN_ID;
		mConvId = UNKNOWN_ID;
		mFlags = 0;
        mMsgBodyType = MSGBODY_INLINE;
        mNoICal = false;
	}

	public CreateMessage(int mailboxId,
                         String rcptEmail,
                         boolean shared,
						 String digest,
						 int msgSize,
						 int folderId,
                         boolean noICal,
						 int flags,
						 String tags) {
		setMailboxId(mailboxId);
        mRcptEmail = rcptEmail;
        mShared = shared;
		mDigest = digest != null ? digest : "";
		mMsgSize = msgSize;
		mMsgId = UNKNOWN_ID;
		mFolderId = folderId;
		mConvId = UNKNOWN_ID;
		mFlags = flags;
		mTags = tags != null ? tags : "";
        mMsgBodyType = MSGBODY_INLINE;
        mNoICal = noICal;
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

	public int getConversationId() {
		return mConvId;
	}

	public void setConversationId(int convId) {
		mConvId = convId;
	}

    public int getAppointmentId() {
    	return mAppointmentId;
    }

    public void setAppointmentId(int appointmentId) {
    	mAppointmentId = appointmentId;
    }

	public int getOpCode() {
		return OP_CREATE_MESSAGE;
	}

    public byte[] getMessageBody() {
    	return mData;
    }

    public String getBlobPath() {
    	return mPath;
    }

    public short getBlobVolumeId() {
    	return mVolumeId;
    }

    public void setMessageBodyInfo(byte[] data, String path, short volumeId) {
        mMsgBodyType = MSGBODY_INLINE;
        mData = data;
        mPath = path;
        mVolumeId = volumeId;
    }

    public void setMessageLinkInfo(String linkSrcPath, short volumeId) {
        mMsgBodyType = MSGBODY_LINK;
        assert(linkSrcPath != null);
        mPath = linkSrcPath;
        mVolumeId = volumeId;
    }

    public String getRcptEmail() {
    	return mRcptEmail;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mMsgId);
        sb.append(", rcpt=").append(mRcptEmail);
        sb.append(", shared=").append(mShared ? "true" : "false");
        sb.append(", blobDigest=\"").append(mDigest).append("\", size=").append(mMsgSize);
        sb.append(", conv=").append(mConvId).append(", folder=").append(mFolderId);
        if (mAppointmentId != UNKNOWN_ID)
            sb.append(", appointment=").append(mAppointmentId);
        sb.append(", noICal=").append(mNoICal);
        sb.append(", flags=").append(mFlags).append(", tags=\"").append(mTags).append("\"");
        sb.append(", bodyType=").append(mMsgBodyType);
        sb.append(", vol=").append(mVolumeId);
        if (mMsgBodyType == MSGBODY_LINK)
            sb.append(", linkSourcePath=").append(mPath);
        else
            sb.append(", path=").append(mPath);
        return sb.toString();
	}

    public byte[][] getSerializedByteArrayVector() throws IOException {
        synchronized (mSBAVGuard) {
            if (mSerializedByteArrayVector != null)
                return mSerializedByteArrayVector;
    
            if (mMsgBodyType == MSGBODY_INLINE) {
                mSerializedByteArrayVector = new byte[2][];
                mSerializedByteArrayVector[1] = mData;
            } else {
                mSerializedByteArrayVector = new byte[1][];
            }
            mSerializedByteArrayVector[0] = serializeToByteArray();
            return mSerializedByteArrayVector;
        }
    }

    protected void serializeData(DataOutput out) throws IOException {
        writeUTF8(out, mRcptEmail != null ? mRcptEmail : "");
        out.writeBoolean(mShared);
		writeUTF8(out, mDigest);
		out.writeInt(mMsgSize);
		out.writeInt(mMsgId);
		out.writeInt(mFolderId);
		out.writeInt(mConvId);
        out.writeInt(mAppointmentId);
        out.writeInt(mFlags);
        out.writeBoolean(mNoICal);
		writeUTF8(out, mTags);
        writeUTF8(out, mPath);
        out.writeShort(mVolumeId);

        out.writeByte(mMsgBodyType);
        if (mMsgBodyType == MSGBODY_INLINE) {
            out.writeInt(mData.length);
            // During serialize, do not serialize the message data buffer.
            // Message buffer is handled by getSerializedByteArrayVector()
            // implementation in this class as the last vector element.
            // Consequently, in the serialized stream message data comes last.
            // deserializeData() should take this into account.
            //out.write(mData);  // Don't do this here!
        }
    }

	protected void deserializeData(DataInput in) throws IOException {
        mRcptEmail = readUTF8(in);
        mShared = in.readBoolean();
		mDigest = readUTF8(in);
		mMsgSize = in.readInt();
		mMsgId = in.readInt();
		mFolderId = in.readInt();
		mConvId = in.readInt();
        mAppointmentId = in.readInt();
		mFlags = in.readInt();
        mNoICal = in.readBoolean();
		mTags = readUTF8(in);
        mPath = readUTF8(in);
        mVolumeId = in.readShort();

        mMsgBodyType = in.readByte();
        if (mMsgBodyType == MSGBODY_INLINE) {
            int dataLen = in.readInt();
            if (dataLen > StoreIncomingBlob.MAX_BLOB_SIZE) {
            	throw new IOException("Deserialized message size too large (" + dataLen + " bytes)");
            }
            mData = new byte[dataLen];
            in.readFully(mData, 0, dataLen);
            // mData must be the last thing deserialized.  See comments in
            // serializeData().
            return;
        }
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = Mailbox.getMailboxById(mboxId);

        SharedDeliveryContext sharedDeliveryCtxt = new SharedDeliveryContext(mShared);
        ParsedMessage pm = null;
        if (mMsgBodyType == MSGBODY_LINK) {
            File file = new File(mPath);
            if (!file.exists())
                throw new RedoException("Missing link source blob " + mPath +
                                        " (digest=" + mDigest + ")",
                                        this);
            Blob src = new Blob(file, mVolumeId);
            sharedDeliveryCtxt.setBlob(src);

            InputStream is = StoreManager.getInstance().getContent(src);
            MimeMessage mm = new MimeMessage(JMSession.getSession(), is);
            is.close();
            pm = new ParsedMessage(mm, getTimestamp(), mbox.attachmentsIndexingEnabled());
        } else { // mMsgBodyType == MSGBODY_INLINE
            pm = new ParsedMessage(mData, getTimestamp(), mbox.attachmentsIndexingEnabled());
        }

        try {
            mbox.addMessage(getOperationContext(), pm, mFolderId, mNoICal, mFlags, mTags, mConvId, mRcptEmail, sharedDeliveryCtxt);
        } catch (MailServiceException e) {
            if (e.getCode() == MailServiceException.ALREADY_EXISTS) {
                mLog.info("Message " + mMsgId + " is already in mailbox " + mboxId);
                return;
            } else
                throw e;
        }
    }
}
