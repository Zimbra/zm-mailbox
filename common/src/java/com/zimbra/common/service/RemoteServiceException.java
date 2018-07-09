/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.service;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.http.NoHttpResponseException;
import org.apache.http.ProtocolException;
import org.apache.http.conn.ConnectTimeoutException;



@SuppressWarnings("serial")
public class RemoteServiceException extends ServiceException {

    public static final String TIMEOUT              = "remote.TIMEOUT";
    public static final String UNKNOWN_HOST         = "remote.UNKNOWN_HOST";
    public static final String NOROUTE              = "remote.NOROUTE";
    public static final String PROTOCOL_EXCEPTION   = "remote.PROTOCOL_EXCEPTION";
    public static final String CONNECT_FAILURE      = "remote.CONNECT_FAILURE";
    
    public static final String SSLCERT_MISMATCH     = "remote.SSLCERT_MISMATCH";
    public static final String SSLCERT_ERROR        = "remote.SSLCERT_ERROR";
    public static final String SSLCERT_NOT_ACCEPTED = "remote.SSLCERT_NOT_ACCEPTED";
    public static final String SSL_HANDSHAKE        = "remote.SSL_HANDSHAKE";
    public static final String SSL_FAILURE          = "remote.SSL_FAILURE";
    
    public static final String AUTH_DENIED          = "remote.AUTH_DENIED";
    public static final String AUTH_FAILURE         = "remote.AUTH_FAILURE";
    public static final String SMTP_AUTH_FAILURE    = "remote.SMTP_AUTH_FAILURE";
    public static final String SMTP_AUTH_REQUIRED   = "remote.SMTP_AUTH_REQUIRED";

    public static final String POP3_UIDL_REQUIRED   = "remote.POP3_UIDL_REQUIRED";
    public static final String YMAIL_INCONSISTENT_STATE = "remote.YMAIL_INCONSISTENT_STATE";

    private RemoteServiceException(String message, String code, boolean isReceiversFault) {
        super(message, code, isReceiversFault);
    }
    
    private RemoteServiceException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault, cause);
    }

    public static RemoteServiceException TIMEOUT(String msg, Throwable cause) {
        return new RemoteServiceException(msg, TIMEOUT, SENDERS_FAULT, cause);
    }
    
    public static RemoteServiceException UNKNOWN_HOST(String msg, Throwable cause) {
        return new RemoteServiceException(msg, UNKNOWN_HOST, SENDERS_FAULT, cause);
    }
    
    public static RemoteServiceException NOROUTE(String msg, Throwable cause) {
        return new RemoteServiceException(msg, NOROUTE, SENDERS_FAULT, cause);
    }
    
    public static RemoteServiceException PROTOCOL_EXCEPTION(String msg, Throwable cause) {
        return new RemoteServiceException(msg, PROTOCOL_EXCEPTION, SENDERS_FAULT, cause);
    }
    
    public static RemoteServiceException CONNECT_FAILURE(String msg, Throwable cause) {
        return new RemoteServiceException(msg, CONNECT_FAILURE, SENDERS_FAULT, cause);
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
    
    public static RemoteServiceException SSL_FAILURE(String msg, Throwable cause) {
        return new RemoteServiceException(msg, SSL_FAILURE, SENDERS_FAULT, cause);
    }
    
    public static RemoteServiceException AUTH_DENIED(String msg, Throwable cause) {
        return new RemoteServiceException(msg, AUTH_DENIED, SENDERS_FAULT, cause);
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

    public static RemoteServiceException POP3_UIDL_REQUIRED() {
        return new RemoteServiceException("leaving messages on server requires support for UIDL", POP3_UIDL_REQUIRED, RECEIVERS_FAULT);
    }

    public static RemoteServiceException YMAIL_INCONSISTENT_STATE() {
        return new RemoteServiceException("Yahoo! mailbox inconsistent and is being rebuilt", YMAIL_INCONSISTENT_STATE, RECEIVERS_FAULT);
    }
    
    public static String getErrorCode(Throwable t) {
        if (t instanceof SocketTimeoutException || t instanceof ConnectTimeoutException || t instanceof NoHttpResponseException)
        	return TIMEOUT;
        if (t instanceof UnknownHostException)
            return UNKNOWN_HOST;
        if (t instanceof NoRouteToHostException)
        	return NOROUTE;
        if (t instanceof UnknownHostException)
            return UNKNOWN_HOST;
        if (t instanceof ProtocolException)
        	return PROTOCOL_EXCEPTION;
        if (t instanceof ConnectException || t instanceof SocketException)
            return CONNECT_FAILURE;
        if (t instanceof CertificateException)
            return SSLCERT_ERROR;
        if (t instanceof SSLPeerUnverifiedException)
            return SSLCERT_MISMATCH;
        if (t instanceof SSLHandshakeException)
        	return SSL_HANDSHAKE;
        if (t instanceof SSLException)
            return SSL_FAILURE;
        return null;
    }
    
    public static void doConnectionFailures(String msg, Throwable t) throws RemoteServiceException {
    	if (t instanceof SocketTimeoutException || t instanceof ConnectTimeoutException || t instanceof NoHttpResponseException)
    		throw RemoteServiceException.TIMEOUT(msg, t);
    	if (t instanceof UnknownHostException)
    		throw RemoteServiceException.UNKNOWN_HOST(msg, t);
    	if (t instanceof NoRouteToHostException)
    		throw RemoteServiceException.NOROUTE(msg, t);
    	if (t instanceof UnknownHostException)
    		throw RemoteServiceException.UNKNOWN_HOST(msg, t);
    	if (t instanceof ProtocolException)
    		throw RemoteServiceException.PROTOCOL_EXCEPTION(msg, t);
    	if (t instanceof ConnectException || t instanceof SocketException)
    		throw RemoteServiceException.CONNECT_FAILURE(msg, t);
    }
    
    public static void doSSLFailures(String msg, Throwable t) throws RemoteServiceException {
        if (t instanceof CertificateException)
    		throw RemoteServiceException.SSLCERT_ERROR(msg, t);
        if (t instanceof SSLPeerUnverifiedException)
        	throw RemoteServiceException.SSLCERT_MISMATCH(msg, t);
    	if (t instanceof SSLHandshakeException)
    		throw RemoteServiceException.SSL_HANDSHAKE(msg, t);
        if (t instanceof SSLException)
        	throw RemoteServiceException.SSL_FAILURE(msg, t);
    }
}
