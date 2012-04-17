/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.cs.store.triton;

import org.junit.Ignore;

import com.zimbra.cs.store.AbstractStoreManagerTest;
import com.zimbra.cs.store.StoreManager;

@Ignore("requires Triton server")
public class TritonRawStoreManagerTest extends AbstractStoreManagerTest {
    @Override
    protected StoreManager getStoreManager() {
        return new TritonRawStoreManager("http://10.33.30.77");
    }
}
