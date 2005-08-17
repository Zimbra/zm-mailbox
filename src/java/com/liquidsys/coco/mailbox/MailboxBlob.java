/*
 * Created on 2004. 10. 12.
 */
package com.liquidsys.coco.mailbox;

import com.liquidsys.coco.mime.Mime;
import com.liquidsys.coco.store.Blob;

/**
 * @author jhahm
 */
public class MailboxBlob {

	private int mMsgId;
    private int mRevision;

	private Mailbox mMailbox;
    private Blob mBlob;
	private String mMimeType;

//	public MailboxBlob(Mailbox mbox, int msgId, Blob blob) {
//        this(mbox, msgId, 0, blob);
//    }
    public MailboxBlob(Mailbox mbox, int msgId, int revision, Blob blob) {
		mMsgId = msgId;
        mRevision = revision;
		mMailbox = mbox;
        mBlob = blob;
		mMimeType = Mime.CT_MESSAGE_RFC822;
	}

    public int getMessageId() {
        return mMsgId;
    }

    public int getRevision() {
        return mRevision;
    }

	public Mailbox getMailbox() {
		return mMailbox;
	}

    public Blob getBlob() {
        return mBlob;   
    }

    public String getPath() {
    	return mBlob.getPath();
    }

    public String getMimeType() {
        return mMimeType;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("mailbox=").append(mMailbox.getId());
        sb.append(", message=").append(mMsgId);
        sb.append(", path=").append(getPath());
        return sb.toString();
	}
}
