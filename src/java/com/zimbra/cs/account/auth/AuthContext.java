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
     * Protocol from which the auth request went in.
     * 
     * type: AuthContext.Protocol
     */
    public static final String AC_PROTOCOL = "proto";

    public enum Protocol {
        http_basic,
        im,
        imap,
        pop3,
        soap,
        zsync,
        
        //for internal use only
        test;

    }
    
}
