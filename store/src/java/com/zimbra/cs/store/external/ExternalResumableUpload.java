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

import com.zimbra.common.service.ServiceException;

public interface ExternalResumableUpload {

    /**
     * Create a new ExternalResumableIncomingBlob instance to handle the upload
     * of a single object. The implementation should compute all remote metadata
     * such as remote id, size, and content hash inline with the upload process
     * so that finishUpload() does not need to traverse the data again
     *
     * @param id: local upload ID. Used internally; must be passed to super constructor
     * @param ctxt: local upload context. Used internally; must be passed to super constructor
     * @return initialized ExternalResumableIncomingBlob instance ready to accept a new data upload
     * @throws IOException
     * @throws ServiceException
     */
    public ExternalResumableIncomingBlob newIncomingBlob(String id, Object ctxt) throws IOException, ServiceException;

    /**
     * Finalize an upload. Depending on store semantics this may involve a
     * commit, checksum, or other similar operation.
     *
     * @param blob: The ExternalUploadedBlob which data has been written into
     * @return String identifier (locator) for the permanent storage location for the uploaded content
     * @throws IOException
     * @throws ServiceException
     */
    public String finishUpload(ExternalUploadedBlob blob) throws IOException, ServiceException;
}
