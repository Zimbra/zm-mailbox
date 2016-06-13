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

    @Override
    public boolean supports(StoreFeature feature) {
        switch (feature) {
            case SINGLE_INSTANCE_SERVER_CREATE: return true;
            default:                            return super.supports(feature);
        }
    }
}