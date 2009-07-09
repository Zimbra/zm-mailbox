/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.store;

import java.io.IOException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

public class HttpMailboxBlob extends MailboxBlob {

    protected HttpMailboxBlob(Mailbox mbox, int itemId, int revision, String locator) {
        super(mbox, itemId, revision, locator);
    }

    @Override public Blob getLocalBlob() throws IOException {
        StoreManager sm = StoreManager.getInstance();
        try {
            Blob blob = sm.storeIncoming(sm.getContent(this), mSize == null ? -1 : mSize.intValue(), null);
            setDigest(blob.getDigest());
            setSize(blob.getRawSize());
            return blob;
        } catch (ServiceException e) {
            throw new IOException("fetching local blob: " + e);
        }
    }
}
