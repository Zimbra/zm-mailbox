/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
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

/*
 * Created on 2004. 10. 12.
 */
package com.zimbra.cs.store;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.Blob;

public abstract class MailboxBlob {

    private Mailbox mMailbox;

    private int mItemId;
    private int mRevision;
    private String mLocator;
    protected Long mSize;
    protected String mDigest;

    protected MailboxBlob(Mailbox mbox, int itemId, int revision, String locator) {
        mMailbox = mbox;
        mItemId = itemId;
        mRevision = revision;
        mLocator = locator;
    }

    public int getItemId() {
        return mItemId;
    }

    public int getRevision() {
        return mRevision;
    }

    public String getLocator() {
        return mLocator;
    }

    public String getDigest() throws IOException {
        if (mDigest == null)
            mDigest = getLocalBlob().getDigest();
        return mDigest;
    }

    public MailboxBlob setDigest(String digest) {
        mDigest = digest;
        return this;
    }

    public long getSize() throws IOException {
        if (mSize == null)
            mSize = new Long(getLocalBlob().getRawSize());
        return mSize;
    }

    public MailboxBlob setSize(long size) {
        mSize = size;
        return this;
    }

    public Mailbox getMailbox() {
        return mMailbox;
    }

    abstract public Blob getLocalBlob() throws IOException;

    @Override public String toString() {
        return mMailbox.getId() + ":" + mItemId + ":" + mRevision + "[" + getLocator() + "]";
    }
}
