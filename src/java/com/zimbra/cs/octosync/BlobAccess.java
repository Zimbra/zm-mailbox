/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
