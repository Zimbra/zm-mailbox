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
package com.zimbra.cs.store.file;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.MailboxBlob;

public class VolumeMailboxBlob extends MailboxBlob {
    private VolumeBlob mBlob;

    protected VolumeMailboxBlob(Mailbox mbox, int itemId, int revision, String locator, VolumeBlob blob) {
        super(mbox, itemId, revision, locator);
        mBlob = blob;
    }

    @Override public MailboxBlob setSize(long size) {
        super.setSize(size);
        if (mBlob != null)
            mBlob.setRawSize(size);
        return this;
    }

    @Override public VolumeBlob getLocalBlob() {
        return mBlob;
    }
}
