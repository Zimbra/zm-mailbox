/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
     * Sends a generic error response to the client indicating that authentication
     * has failed.
     *
     * @throws IOException if an I/O error occurred
     */
    void sendFailed() throws IOException;

    /**
     * Sends an error response to the client indicating that authentication
     * has failed.
     *
     * @param msg the error message to be sent
     * @throws IOException if an I/O error occurred
     */
    void sendFailed(String msg) throws IOException;

    /**
     * Sends a generic response to the client indicating that authentication was
     * successful.
     *
     * @throws IOException if an I/O error has occurred
     */
    void sendSuccessful() throws IOException;

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
     * @param authorizationId   the authorization id for the user, or null if none
     * @param authenticationId  the authentication id for the user (required)
     * @param password          the user password, or null if none
     * @param auth              the Authenticator performing the authentication
     * @return true if the user was authenticated, false otherwise
     * @throws IOException if an I/O error occurred
     */
    boolean authenticate(String authorizationId, String authenticationId, String password, Authenticator auth)
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
     * @return true if SSL encryption has been enabled, false otherwise
     */
    boolean isSSLEnabled();

    /**
     * Returns true if plain username/password login is permitted without
     * SSL encryption active.
     *
     * @return true if login is permitted without SSL encryption, false otherwise
     */
    boolean allowCleartextLogin();

    /**
     * Returns true if plain username/password login is permitted without
     * SSL encryption active.
     *
     * @return true if login is permitted without SSL encryption, false otherwise
     */
    boolean isGssapiAvailable();
}
