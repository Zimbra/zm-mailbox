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
 * Created on 2004. 10. 12.
 */
package com.zimbra.cs.mailbox;

import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.store.Blob;

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
        sb.append("MailboxBlob: {");
        sb.append("mailbox=").append(mMailbox.getId());
        sb.append(", message=").append(mMsgId);
        sb.append(", path=").append(getPath());
        sb.append("}");
        return sb.toString();
	}
}
