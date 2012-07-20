/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.cs.store.external;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StoreManager;

/**
 * MailboxBlob implementation which accesses ExternalStoreManager to retrieve blobs for local use
 */
public class ExternalMailboxBlob extends MailboxBlob {

    protected ExternalMailboxBlob(Mailbox mbox, int itemId, int revision, String locator) {
        super(mbox, itemId, revision, locator);
    }

    @Override
    public Blob getLocalBlob() throws IOException {
        ExternalStoreManager sm = (ExternalStoreManager) StoreManager.getInstance();
        Blob blob = sm.getLocalBlob(getMailbox(), getLocator());

        setSize(blob.getRawSize());
        if (digest != null) {
            setDigest(blob.getDigest());
        }
        return blob;
    }

    @Override
    public int hashCode() {
        return getLocator().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof ExternalMailboxBlob)) {
            return false;
        }
        return getLocator().equals(((ExternalMailboxBlob) other).getLocator());
    }

}
