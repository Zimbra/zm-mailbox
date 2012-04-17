/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.store;

import org.junit.BeforeClass;

import com.zimbra.cs.store.file.FileBlobStore;

public class FileBlobStoreTest extends AbstractStoreManagerTest {

    @BeforeClass
    public static void disableNative() {
        //don't fail test even if native libraries not installed
        //this makes it easier to run unit tests from command line
        System.setProperty("zimbra.native.required", "false");
    }

    @Override
    protected StoreManager getStoreManager() {
        return new FileBlobStore();
    }
}
