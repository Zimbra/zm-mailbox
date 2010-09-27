/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.store.http;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.StagedBlob;

public class HttpStagedBlob extends StagedBlob {
    private final String mLocator;
    private boolean mIsInserted;

    protected HttpStagedBlob(Mailbox mbox, String digest, long size, String locator) {
        super(mbox, digest, size);
        mLocator = locator;
    }

    @Override public String getLocator() {
        return mLocator;
    }

    HttpStagedBlob markInserted() {
        mIsInserted = true;
        return this;
    }

    boolean isInserted() {
        return mIsInserted;
    }

    @Override public int hashCode() {
        return mLocator.hashCode();
    }

    @Override public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof HttpStagedBlob))
            return false;
        return mLocator.equals(((HttpStagedBlob) other).mLocator);
    }
}
