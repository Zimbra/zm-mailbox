package com.zimbra.cs.lmtpserver;

/**
 * Handling of this type of exception should be to simply drop the connection. 
 */
public class UnrecoverableLmtpException extends Exception {

    public UnrecoverableLmtpException(String message) {
        super(message);
    }

    public UnrecoverableLmtpException(String message, Throwable cause) {
        super(message, cause);
    }
}
