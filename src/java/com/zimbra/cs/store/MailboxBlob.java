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
 * Created on 2004. 10. 12.
 */
package com.zimbra.cs.store;

import java.io.IOException;
import java.io.Serializable;

import com.zimbra.cs.mailbox.Mailbox;

public abstract class MailboxBlob {
    public static class MailboxBlobInfo implements Serializable {
        private static final long serialVersionUID = 6378518636677970767L;

        public String accountId;
        public int itemId;
        public int revision;
        public String locator;

        public MailboxBlobInfo(String accountId, int itemId, int revision, String locator) {
            this.accountId = accountId;
            this.itemId = itemId;
            this.revision = revision;
            this.locator = locator;
        }
    }

    private final Mailbox mailbox;

    private final int itemId;
    private final int revision;
    private final String locator;
    protected Long size;
    protected String digest;

    protected MailboxBlob(Mailbox mbox, int itemId, int revision, String locator) {
        this.mailbox = mbox;
        this.itemId = itemId;
        this.revision = revision;
        this.locator = locator;
    }

    public int getItemId() {
        return itemId;
    }

    public int getRevision() {
        return revision;
    }

    public String getLocator() {
        return locator;
    }

    public String getDigest() throws IOException {
        if (digest == null) {
            digest = getLocalBlob().getDigest();
        }
        return digest;
    }

    public MailboxBlob setDigest(String digest) {
        this.digest = digest;
        return this;
    }

    public long getSize() throws IOException {
        if (size == null) {
            this.size = Long.valueOf(getLocalBlob().getRawSize());
        }
        return size;
    }

    public MailboxBlob setSize(long size) {
        this.size = size;
        return this;
    }

    public Mailbox getMailbox() {
        return mailbox;
    }

    abstract public Blob getLocalBlob() throws IOException;

    @Override
    public String toString() {
        return mailbox.getId() + ":" + itemId + ":" + revision + "[" + getLocator() + "]";
    }
}
