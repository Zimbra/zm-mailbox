/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
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

import com.zimbra.common.util.FileCache;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.Blob;

/**
 * Wrapper around Blob for constructor visibility and to allow construction from FileCache.Item
 *
 */
public class ExternalBlob extends Blob {

    protected String locator;
    protected Mailbox mbox;

    protected ExternalBlob(File file) {
        super(file);
    }

    public ExternalBlob(File file, long rawSize, String digest) {
        super(file, rawSize, digest);
    }

    ExternalBlob(FileCache.Item cachedFile) {
        super(cachedFile.file, cachedFile.file.length(), cachedFile.digest);
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
    public File getFile() {
        return super.getFile();
    }
}
