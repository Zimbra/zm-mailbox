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
package com.zimbra.cs.store.external;

import java.io.IOException;

import com.zimbra.cs.store.Blob;

/**
 * ContentAddressableStoreManager which supports SIS (single instance server) operations.
 * The store contains a single copy of each unique blob and tracks reference count when more than one object is associated with that blob.
 * Blobs are only deleted when the reference count reaches zero.
 */
public abstract class SisStore extends ContentAddressableStoreManager {

    /**
     * Retrieve a blob from the remote system based on content hash. The remote system is expected to increment reference count if a blob is found
     * @param hash: The content hash of the blob
     * @return a new Blob instance which holds the content from the remote server, or null if none exists
     * @throws IOException
     */
    public abstract Blob getSisBlob(byte[] hash) throws IOException;
}