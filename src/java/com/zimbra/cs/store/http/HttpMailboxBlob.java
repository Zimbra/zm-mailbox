/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.store.http;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StoreManager;

public class HttpMailboxBlob extends MailboxBlob {
    protected HttpMailboxBlob(Mailbox mbox, int itemId, int revision, String locator) {
        super(mbox, itemId, revision, locator);
    }

    @Override public Blob getLocalBlob() throws IOException {
        HttpStoreManager hsm = (HttpStoreManager) StoreManager.getInstance();
        Blob blob = hsm.getLocalBlob(getMailbox(), getLocator(), mSize == null ? -1 : mSize.intValue());

        setSize(blob.getRawSize());
        if (mDigest != null)
            setDigest(blob.getDigest());
        return blob;
    }

    @Override public int hashCode() {
        return getLocator().hashCode();
    }

    @Override public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof HttpMailboxBlob))
            return false;
        return getLocator().equals(((HttpMailboxBlob) other).getLocator());
    }
}
