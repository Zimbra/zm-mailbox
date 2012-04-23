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

package com.zimbra.cs.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.zimbra.common.service.ServiceException;

/**
 * Represents incoming blob, i.e. blob that may not be yet completely received.
 */
public abstract class IncomingBlob {
    /**
     * Returns the incoming blob id.
     *
     * @return the id
     */
    public abstract String getId();

    /**
     * Returns the current size of the incoming blob.
     *
     * @return the size
     * @throws ServiceException
     * @throws IOException
     */
    public abstract long getCurrentSize() throws IOException, ServiceException;

    /**
     * Allows to check if expected size has been set.
     *
     * @return True if expected size was set, false otherwise.
     */
    public abstract boolean hasExpectedSize();

    /**
     * Gets the expected size, if set.
     *
     * @pre hasExpectedSize() returned true
     * @return the expected size
     */
    public abstract long getExpectedSize();

    /**
     * Sets the expected size.
     *
     * @pre Must have not been set yet
     * @param value
     *            The expected size.
     */

    public abstract void setExpectedSize(long value);

    /**
     * Gets the output stream for the incoming blob. The stream is used to write
     * to the end of the incoming blob.
     *
     * @return the output stream
     */
    public abstract OutputStream getAppendingOutputStream() throws IOException;

    /**
     * Gets the input stream. The return stream can be used to read the already
     * written data.
     *
     * The stream must not return data that was written to the incoming blob
     * after the InputStream instance was obtained via this call. EOF should be
     * reported upon attempt to read byte past the current size of the blob at
     * the time of this call.
     *
     * @return the input stream
     */
    public abstract InputStream getInputStream() throws IOException;

    /**
     * Returns the user settable context data. Must not be used or otherwise
     * interpreted by the implementations.
     *
     * Future note: should instances of IncomingBlob be serialized, the Object
     * stored here must be serializable.
     *
     * @return the context
     */
    public abstract Object getContext();

    /**
     * Returns the previously set user context data.
     *
     * @param value
     *            the new context
     */
    public abstract void setContext(Object value);

    /**
     * Checks if the incoming blob is complete, i.e. the current size matches
     * expected size
     *
     * @return True if complete, false otherwise
     * @throws ServiceException
     * @throws IOException
     */
    public boolean isComplete() throws IOException, ServiceException {
        return hasExpectedSize() && getExpectedSize() == getCurrentSize();
    }

    public abstract Blob getBlob() throws IOException, ServiceException;

    public abstract void cancel();

    public abstract long getLastAccessTime();

}
