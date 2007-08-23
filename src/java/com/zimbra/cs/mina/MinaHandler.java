/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
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
}
