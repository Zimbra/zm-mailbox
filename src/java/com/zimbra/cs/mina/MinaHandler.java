/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

package com.zimbra.cs.mina;

import java.io.IOException;

/**
 * Protocol handler for MINA-based server connections. The protocol handler
 * defines the action to take whenever a new connection is opened, is closed,
 * becomes idle, or a new request is received on the connection.
 */
public interface MinaHandler {
    /**
     * Called when a new connection has been opened.
     * 
     * @throws IOException if an I/O error occurs
     */
    void connectionOpened() throws IOException;

    /**
     * Called when the connection has been closed.
     *
     * @throws IOException if an I/O error occurs
     */
    void connectionClosed() throws IOException;

    /**
     * Called when the connection becomes idle after a specified period of
     * inactivity.
     * 
     * @throws IOException if an I/O error occurs
     */
    void connectionIdle() throws IOException;

    /**
     * Called when a new request has been received on the connection
     * \
     * @param req the MinaRequest that has been received
     * @throws IOException if an I/O error occurs
     */
    void requestReceived(MinaRequest req) throws IOException;

    /**
     * Drop connection and wait up to 'timeout' milliseconds for last write
     * to complete before connection is closed.
     * 
     * @param timeout timeout grace per
     * @throws IOException if an I/O error occurs
     */
    void dropConnection(long timeout) throws IOException;
}
