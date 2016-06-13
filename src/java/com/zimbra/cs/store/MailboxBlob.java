/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
        public int mailboxId;
        public int itemId;
        public int revision;
        public String locator;
        public String digest;

        public MailboxBlobInfo(String accountId, int mailboxId, int itemId, int revision, String locator, String digest) {
            this.accountId = accountId;
            this.mailboxId = mailboxId;
            this.itemId = itemId;
            this.revision = revision;
            this.locator = locator;
            this.digest = digest;
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
