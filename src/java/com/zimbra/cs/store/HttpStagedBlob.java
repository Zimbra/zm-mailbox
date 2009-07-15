/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.store;

import com.zimbra.cs.mailbox.Mailbox;

public class HttpStagedBlob extends StagedBlob {
    private String mLocator;
    private boolean mIsInserted;

    HttpStagedBlob(Mailbox mbox, String locator) {
        super(mbox);
        mLocator = locator;
    }

    String getLocator() {
        return mLocator;
    }

    HttpStagedBlob markInserted() {
        mIsInserted = true;
        return this;
    }

    boolean isInserted() {
        return mIsInserted;
    }
}
