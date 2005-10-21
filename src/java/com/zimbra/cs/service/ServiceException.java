/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 1, 2004
 *
 */
package com.zimbra.cs.service;

/**
 * @author schemers
 *
 */
public class ServiceException extends Exception {

    public static final String FAILURE = "service.FAILURE";
    public static final String INVALID_REQUEST = "service.INVALID_REQUEST";
    public static final String UNKNOWN_DOCUMENT = "service.UNKNOWN_DOCUMENT";
    public static final String PARSE_ERROR = "service.PARSE_ERROR";
    public static final String TEMPORARILY_UNAVAILABLE = "service.TEMPORARILY_UNAVAILABLE";
    public static final String PERM_DENIED = "service.PERM_DENIED";
    public static final String AUTH_REQUIRED = "service.AUTH_REQUIRED";
    public static final String AUTH_EXPIRED = "service.AUTH_EXPIRED";
    public static final String WRONG_HOST = "service.WRONG_HOST";
    public static final String NON_READONLY_OPERATION_DENIED = "service.NON_READONLY_OPERATION_DENIED";
    public static final String PROXY_ERROR = "service.PROXY_ERROR";
    public static final String TOO_MANY_HOPS = "service.TOO_MANY_HOPS";
    public static final String ALREADY_IN_PROGRESS = "service.ALREADY_IN_PROGRESS";
    public static final String NOT_IN_PROGRESS = "service.NOT_IN_PROGRESS";
    public static final String INTERRUPTED = "service.INTERRUPTED";
    
    private String mCode;
    
    
    
    /**
     * Comment for <code>mReceiver</code>
     * 
     * Causes a Sender/Receiver element to show up in a soap fault. It is supposed to let the client know whether it 
     * did something wrong or something is wrong on the server.  

     * For example, ServiceException.FAILURE sets it to true, meaning something bad happened on the server-side and 
     * the client could attempt a retry. The rest are false, as it generally means the client made a bad request.
     * 
     */
    public static final boolean RECEIVERS_FAULT = true; // server's fault
    public static final boolean SENDERS_FAULT = false; // client's fault
    private boolean mReceiver;
    
    protected ServiceException(String message, String code, boolean isReceiversFault) {
        super(message);
        mCode = code;
        mReceiver = isReceiversFault;
    }
    
    protected ServiceException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message,cause);
        mCode = code;
        mReceiver = isReceiversFault;
    }
    
    public String getCode() {
        return mCode;
    }
    
    /**
     * @return See the comment for the mReceiver member
     */
    public boolean isReceiversFault() {
        return mReceiver;
    }
    
    /**
     * generic system failure. most likely a temporary situation.
     */
    public static ServiceException FAILURE(String message, Throwable cause) {
        return new ServiceException("system failure: "+message, FAILURE, RECEIVERS_FAULT, cause);
    }

    /**
     * The request was somehow invalid (wrong parameter, wrong target, etc)
     */
    public static ServiceException INVALID_REQUEST(String message, Throwable cause) {
        return new ServiceException("invalid request: "+message, INVALID_REQUEST, SENDERS_FAULT, cause);
    }
    
    /**
     * User sent an unknown SOAP command (the "document" is the Soap Request)
     */
    public static ServiceException UNKNOWN_DOCUMENT(String message, Throwable cause) {
        return new ServiceException("unknown document: "+message, UNKNOWN_DOCUMENT, SENDERS_FAULT, cause);
    }
    
    public static ServiceException PARSE_ERROR(String message, Throwable cause) {
        return new ServiceException("parse error: "+message, PARSE_ERROR, SENDERS_FAULT, cause);
    }

    public static ServiceException TEMPORARILY_UNAVAILABLE() {
    	return new ServiceException("service temporarily unavailable", TEMPORARILY_UNAVAILABLE, RECEIVERS_FAULT);
    }

    public static ServiceException PERM_DENIED(String message) {
        return new ServiceException("permission denied: "+message, PERM_DENIED, SENDERS_FAULT, null);
    }
    
    public static ServiceException AUTH_EXPIRED() {
        return new ServiceException("auth credentials have expired", AUTH_EXPIRED, SENDERS_FAULT, null);
    }
    
    public static ServiceException AUTH_REQUIRED() {
        return new ServiceException("no valid authtoken present", AUTH_REQUIRED, SENDERS_FAULT, null);
    }

    public static ServiceException WRONG_HOST(String target, Throwable cause) {
        return new ServiceException("operation sent to wrong host (you want '" + target + "')", WRONG_HOST, 
                SENDERS_FAULT, cause);
    }

    public static ServiceException NON_READONLY_OPERATION_DENIED() {
        return new ServiceException("non-readonly operation denied", NON_READONLY_OPERATION_DENIED, 
                SENDERS_FAULT, null);
    }

    public static ServiceException PROXY_ERROR(Throwable cause) {
        return new ServiceException("error while proxying request to target server: " + (cause != null ? cause.getMessage() : "unknown reason"), 
                PROXY_ERROR, RECEIVERS_FAULT, cause);
    }

    public static ServiceException TOO_MANY_HOPS() {
        return new ServiceException("mountpoint loop detected", TOO_MANY_HOPS, SENDERS_FAULT, null);
    }
    
    public static ServiceException ALREADY_IN_PROGRESS(String mboxId, String action) {
        return new ServiceException("mbox "+mboxId+" is already running action "+action, ALREADY_IN_PROGRESS, SENDERS_FAULT, null);
    }
    
    public static ServiceException NOT_IN_PROGRESS(String mboxId, String action) {
        return new ServiceException("mbox "+mboxId+" is not currently running action "+action, NOT_IN_PROGRESS, SENDERS_FAULT, null);
    }

    public static ServiceException INTERRUPTED(String str) {
        return new ServiceException("The operation has been interrupted "+str!=null?str:"", INTERRUPTED, RECEIVERS_FAULT, null);
    }
}
