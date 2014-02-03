/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.store.external;

import java.io.File;
import java.io.IOException;

import com.ibm.icu.impl.Assert;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.BlobInputStream;
import com.zimbra.cs.store.StoreManager;

public class ExternalBlobInputStream extends BlobInputStream {

    protected String locator;
    protected Mailbox mbox;

    public ExternalBlobInputStream(Blob blob) throws IOException {
        super(blob);
        if (blob instanceof ExternalBlob) {
            locator = ((ExternalBlob) blob).getLocator();
            mbox = ((ExternalBlob) blob).getMbox();
        } else {
            Assert.fail("ExternalBlobInputStream constructor Blob instance not ExternalBlob");
        }
    }

    private ExternalBlobInputStream(File file, long rawSize, Long start, Long end, BlobInputStream parent) throws IOException {
        super(file, rawSize, start, end, parent);
    }

    public String getLocator() {
        return locator;
    }

    public void setLocator(String locator) {
        this.locator = locator;
    }

    public Mailbox getMbox() {
        return mbox;
    }

    public void setMbox(Mailbox mbox) {
        this.mbox = mbox;
    }

    @Override
    protected void setMailboxLocator(BlobInputStream parent) {
        if (parent instanceof ExternalBlobInputStream) {
            this.locator = ((ExternalBlobInputStream) parent).getLocator();
            this.mbox = ((ExternalBlobInputStream) parent).getMbox();
        } else {
            Assert.fail("Parent of ExternalBlobInputStream must always be ExternalBlobInputStream");
        }
    }

    @Override
    protected BlobInputStream initializeSubStream(File file, long rawSize,
            Long start, Long end, BlobInputStream parent) throws IOException {
        return new ExternalBlobInputStream(file, rawSize, start, end, parent);
    }

    @Override
    protected File getRootFile() throws IOException {
        File file = super.getRootFile();
        if (file != null && file.exists()) {
            return file;
        } else {
            ZimbraLog.store.debug("blob file no longer on disk, fetching from remote store");
            ExternalStoreManager sm = (ExternalStoreManager) StoreManager.getInstance();
            Blob blob = sm.getLocalBlob(mbox, locator, false);
            return blob.getFile();
        }
    }
}
