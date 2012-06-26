/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account.auth;

public class AuthContext {
    /*
     * Originating client IP address.
     * Present in context for SOAP, IMAP, POP3, and http basic authentication.
     * 
     * type: String
     */
    public static final String AC_ORIGINATING_CLIENT_IP = "ocip";
    
    /*
     * Remote address as seen by ServletRequest.getRemoteAddr()
     * Present in context for SOAP, IMAP, POP3, and http basic authentication.
     * 
     * type: String
     */
    public static final String AC_REMOTE_IP = "remoteip";
    
    /*
     * Account name passed in to the interface.
     * Present in context for SOAP and http basic authentication.
     * 
     * type: String
     */
    public static final String AC_ACCOUNT_NAME_PASSEDIN = "anp";
    
    /*
     * User agent sending in the auth request.
     * 
     * type: String
     */
    public static final String AC_USER_AGENT = "ua";
    
    /*
     * Whether the auth request came to the admin port and attempting 
     * to acquire an admin auth token
     * 
     * type: Boolean
     */
    public static final String AC_AS_ADMIN = "asAdmin";
    
    /*
     * 
     */
    public static final String AC_AUTHED_BY_MECH = "authedByMech";
    
    /*
     * Protocol from which the auth request went in.
     * 
     * type: AuthContext.Protocol
     */
    public static final String AC_PROTOCOL = "proto";

    public enum Protocol {
        client_certificate,
        http_basic,
        im,
        imap,
        pop3,
        soap,
        spnego,
        zsync,
        
        //for internal use only
        test;
    };
    
}
