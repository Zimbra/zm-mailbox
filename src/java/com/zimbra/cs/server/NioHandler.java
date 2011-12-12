/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.server;

import java.io.IOException;

import org.apache.mina.filter.codec.ProtocolDecoderException;

/**
 * Protocol handler for MINA-based server connections. The protocol handler defines the action to take whenever a new
 * connection is opened, is closed, becomes idle, or a new request is received on the connection.
 */
public interface NioHandler {
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
     * Called when the connection becomes idle after a specified period of inactivity.
     *
     * @throws IOException if an I/O error occurs
     */
    void connectionIdle() throws IOException;

    /**
     * Called when a new message has been received on the connection.
     *
     * @param msg the message that has been received
     * @throws IOException if an I/O error occurs
     * @throws ProtocolDecoderException 
     */
    void messageReceived(Object msg) throws IOException, ProtocolDecoderException;

    /**
     * Called when any exception is thrown by user IoHandler implementation or by MINA. If cause is instanceof
     * IOException, MINA will close the connection automatically.
     *
     * @param e
     * @throws IOException
     */
    void exceptionCaught(Throwable e) throws IOException;

    /**
     * Drop the connection and wait up to writeTimeout in the session config for last write to complete before the
     * connection is closed.
     *
     * @throws IOException if an I/O error occurs
     */
    void dropConnection() throws IOException;

    /**
     * Set the current context (mid, ip, and etc) to the account logger.
     */
    void setLoggingContext();
}
