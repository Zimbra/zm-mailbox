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
