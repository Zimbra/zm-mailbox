/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
