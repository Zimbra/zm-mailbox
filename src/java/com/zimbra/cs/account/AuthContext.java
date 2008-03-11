package com.zimbra.cs.account;

public class AuthContext {
    /*
     * Originating client IP address.
     * Present in context for SOAP, IMAP, POP3, and http basic authentication.
     */
    public static final String AC_ORIGINATING_CLIENT_IP = "ocip";
    
    /*
     * Account name passed in to the interface.
     * Present in context for SOAP and http basic authentication.
     */
    public static final String AC_ACCOUNT_NAME_PASSEDIN = "anp";
}
