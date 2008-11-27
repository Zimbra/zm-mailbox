package com.zimbra.common.service;

public class RemoteServiceException extends ServiceException {

    public static final String CLIENT_ERROR       = "remote.CLIENT_ERROR";
    public static final String IO_ERROR           = "remote.IO_ERROR";
    public static final String SSLCERT_MISMATCH   = "remote.SSLCERT_MISMATCH";
    public static final String SSLCERT_ERROR      = "remote.SSLCERT_ERROR";
    public static final String SSLCERT_NOT_ACCEPTED      = "remote.SSLCERT_NOT_ACCEPTED";
    public static final String SSL_HANDSHAKE      = "remote.SSL_HANDSHAKE";
    public static final String AUTH_FAILURE       = "remote.AUTH_FAILURE";
    public static final String SMTP_AUTH_FAILURE  = "remote.SMTP_AUTH_FAILURE";
    public static final String SMTP_AUTH_REQUIRED = "remote.SMTP_AUTH_REQUIRED";

    private RemoteServiceException(String message, String code, boolean isReceiversFault) {
        super(message, code, isReceiversFault);
    }
    
    private RemoteServiceException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault, cause);
    }

    public static RemoteServiceException CLIENT_ERROR(String msg, Throwable cause) {
        return new RemoteServiceException(msg, CLIENT_ERROR, SENDERS_FAULT, cause);
    }
    
    public static RemoteServiceException IO_ERROR(String msg, Throwable cause) {
        return new RemoteServiceException(msg, IO_ERROR, SENDERS_FAULT, cause);
    }
    
    public static RemoteServiceException SSLCERT_MISMATCH(String msg, Throwable cause) {
        return new RemoteServiceException(msg, SSLCERT_MISMATCH, SENDERS_FAULT, cause);
    }
    
    public static RemoteServiceException SSLCERT_ERROR(String msg, Throwable cause) {
        return new RemoteServiceException(msg, SSLCERT_ERROR, SENDERS_FAULT, cause);
    }
    
    public static RemoteServiceException SSLCERT_NOT_ACCEPTED(String msg, Throwable cause) {
        return new RemoteServiceException(msg, SSLCERT_NOT_ACCEPTED, SENDERS_FAULT, cause);
    }

    public static RemoteServiceException SSL_HANDSHAKE(String msg, Throwable cause) {
        return new RemoteServiceException(msg, SSL_HANDSHAKE, SENDERS_FAULT, cause);
    }
    
    public static RemoteServiceException AUTH_FAILURE(String msg, Throwable cause) {
        return new RemoteServiceException(msg, AUTH_FAILURE, SENDERS_FAULT, cause);
    }
    
    public static RemoteServiceException SMTP_AUTH_FAILURE(String msg, Throwable cause) {
        return new RemoteServiceException(msg, SMTP_AUTH_FAILURE, SENDERS_FAULT, cause);
    }
    
    public static RemoteServiceException SMTP_AUTH_REQUIRED(String msg, Throwable cause) {
        return new RemoteServiceException(msg, SMTP_AUTH_REQUIRED, SENDERS_FAULT, cause);
    }
}
