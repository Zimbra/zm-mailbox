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
package com.zimbra.cs.octosync;

import java.io.InputStream;

import com.zimbra.common.service.ServiceException;


/**
 * Provides access to files referenced from a patch processed by PatchInputStream.
 *
 * The type does not purport to become universal way to access blobs, rather to abstract out
 * this functionality from OpatchInputStream only.
 */
public interface BlobAccess
{

    /**
     * Gets the blob input stream for specified file/version.
     *
     * @param fileId The file id
     * @param version The version
     *
     * @return The blob input stream
     *
     * @throws ServiceException The service exception
     * @throws InvalidPatchReferenceException Patch contains reference that cannot be resolved
     */
    public abstract InputStream getBlobInputStream(int fileId, int version)
        throws ServiceException, InvalidPatchReferenceException;

    /**
     * For given file id/version returns the actual file id and version
     * that will be referenced by this BlobAccess.
     *
     * @param fileId File id as referenced
     * @param version The verison number
     *
     * @return Array with file id (0 index) and version (1 index) to reference
     */
    public abstract int[] getActualReference(int fileId, int version);

}
