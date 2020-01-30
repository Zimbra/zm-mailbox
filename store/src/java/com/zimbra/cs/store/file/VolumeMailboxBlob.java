/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.store.file;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.MailboxBlob;

public class VolumeMailboxBlob extends MailboxBlob {
    private final VolumeBlob blob;

    protected VolumeMailboxBlob(Mailbox mbox, int itemId, int revision, String locator, VolumeBlob blob) {
        super(mbox, itemId, revision, locator);
        this.blob = blob;
    }

    protected VolumeMailboxBlob(Mailbox mbox, int itemId, long revision, String locator, VolumeBlob blob) {
        super(mbox, itemId, revision, locator);
        this.blob = blob;
    }

    @Override
    public MailboxBlob setSize(long size) {
        super.setSize(size);
        if (blob != null) {
            blob.setRawSize(size);
        }
        return this;
    }

    @Override
    public VolumeBlob getLocalBlob() {
        return blob;
    }
}
