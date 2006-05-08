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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.ozserver;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface OzConnectionHandler {
    
    /**
     * A new connection has been accepted by the server. Initialize any per
     * connection protocol specific state.
     * 
     * @throws IOException
     */
    void handleConnect() throws IOException;

    /**
     * Input has arrived on the connection, and the current matcher may or may
     * not have matched the input.
     */
    void handleInput(ByteBuffer buffer, boolean matched) throws IOException;

    /**
     * There was an error on the socket, and the underlying socket has been
     * closed.  Cleanup any connection related data.
     */
    void handleDisconnect();
    
    /**
     * When an alarm set on the connection fires, this method is called.
     * 
     * @throws IOException
     */
    void handleAlarm() throws IOException;
}
