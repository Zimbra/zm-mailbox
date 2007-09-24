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

package com.zimbra.cs.security.sasl;

import com.zimbra.common.util.Log;

import java.io.IOException;

/**
 * Implemented by user of the Authenticator class.
 */
public interface AuthenticatorUser {
    /**
     * Returns the protocol for the authenticator (i.e. pop3, imap)
     *
     * @return the authenticator protocol
     */
    String getProtocol();

    /**
     * Sends an error response to the client indicating that a bad request
     * has been received.
     *
     * @param s the error message to be sent
     * @throws IOException if an I/O error occurred
     */
    void sendBadRequest(String s) throws IOException;

    /**
     * Sends an error response to the client indicating that authentication
     * has failed.
     *
     * @param s the error message to be sent
     * @throws IOException if an I/O error occurred
     */
    void sendFailed(String s) throws IOException;

    /**
     * Sends a response to the client indicating that authentication was
     * successful.
     *
     * @param s the success message to be sent
     * @throws IOException if an I/O error has occurred
     */
    void sendSuccessful(String s) throws IOException;

    /**
     * Sends a continuation response to the client.
     *
     * @param s the continuation message
     * @throws IOException if an I/O error has occurred
     */
    void sendContinuation(String s) throws IOException;

    /**
     * Authenticates the user with the server.
     * 
     * @param authorizationId   the authorization id for the user
     * @param authenticationId  the authentication id for the user
     * @param password          the user password, or null if none
     * @param auth              the Authenticator performing the authentication
     * @return true if the user was authenticated, false otherwise
     * @throws IOException if an I/O error occurred
     */
    boolean authenticate(String authorizationId, String authenticationId,
                         String password, Authenticator auth)
        throws IOException;

    /**
     * Returns the logger to be used by the authenticator.
     *
     * @return the Log to be used
     */
    Log getLog();

    /**
     * Returns true if SSL encryption is enabled for the connection.
     *
     * @return if SSL encryption has been enabled, false otherwise
     */
    boolean isSSLEnabled();
}
