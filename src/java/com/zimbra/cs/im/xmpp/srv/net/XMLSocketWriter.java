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
 * Part of the Zimbra Collaboration Suite Server.
 *
 * The Original Code is Copyright (C) Jive Software. Used with permission
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */
 
package com.zimbra.cs.im.xmpp.srv.net;

import com.zimbra.cs.im.xmpp.util.XMLWriter;

import java.io.IOException;
import java.io.Writer;

/**
 * XMLWriter whose writer is actually sending data on a socket connection. Since sending data over
 * a socket may have particular type of errors this class tries to deal with those errors.
 */
public class XMLSocketWriter extends XMLWriter {

    private SocketConnection connection;

    public XMLSocketWriter(Writer writer, SocketConnection connection) {
        super( writer, DEFAULT_FORMAT );
        this.connection = connection;
    }

    /**
     * Flushes the underlying writer making sure that if the connection is dead then the thread
     * that is flushing does not end up in an endless wait.
     *
     * @throws IOException if an I/O error occurs while flushing the writer.
     */
    public void flush() throws IOException {
        // Register that we have started sending data
        connection.writeStarted();
        try {
            super.flush();
        }
        finally {
            // Register that we have finished sending data
            connection.writeFinished();
        }
    }
}
