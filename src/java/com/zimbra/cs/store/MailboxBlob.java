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

/*
 * Created on 2004. 10. 12.
 */
package com.zimbra.cs.store;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.Blob;

public class MailboxBlob {

    private Mailbox mMailbox;
    private Blob mBlob;

    private int mItemId;
    private int mRevision;

    public MailboxBlob(Mailbox mbox, int itemId, int revision, Blob blob) {
        mItemId = itemId;
        mRevision = revision;
        mMailbox = mbox;
        mBlob = blob;
    }

    public MailboxBlob(MailItem item, Blob blob) {
        mItemId = item.getId();
        mRevision = item.getSavedSequence();
        mMailbox = item.getMailbox();
        mBlob = blob;
    }

    public int getItemId() {
        return mItemId;
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

    public String getLocator() {
        return mBlob.getLocator();
    }

    @Override public String toString() {
        return mMailbox.getId() + ":" + mItemId + ":" + mRevision + "[" + getLocator() + "]";
    }
}
