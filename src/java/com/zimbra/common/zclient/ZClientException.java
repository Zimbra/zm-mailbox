/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 1, 2004
 *
 */
package com.zimbra.common.zclient;

import com.zimbra.common.service.ServiceException;

/**
 * @author schemers
 * 
 */
public class ZClientException extends ServiceException {

    public static final String CLIENT_ERROR       = "zclient.CLIENT_ERROR";
    public static final String IO_ERROR           = "zclient.IO_ERROR";
    public static final String UPLOAD_SIZE_LIMIT_EXCEEDED = "zclient.UPLOAD_SIZE_LIMIT_EXCEEDED";
    public static final String UPLOAD_FAILED = "zclient.UPLOAD_FAILED";
    public static final String ZIMBRA_SHARE_PARSE_ERROR = "zclient.ZIMBRA_SHARE_PARSE_ERROR";

    private ZClientException(String message, String code, boolean isReceiversFault) {
        super(message, code, isReceiversFault);
    }
    
    private ZClientException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault, cause);
    }

    public static ZClientException IO_ERROR(String msg, Throwable cause) {
        return new ZClientException(msg, IO_ERROR, SENDERS_FAULT, cause);
    }

    public static ZClientException CLIENT_ERROR(String msg, Throwable cause) {
        return new ZClientException(msg, CLIENT_ERROR, SENDERS_FAULT, cause);
    }

    public static ZClientException UPLOAD_SIZE_LIMIT_EXCEEDED(String msg, Throwable cause) {
        return new ZClientException(msg, UPLOAD_SIZE_LIMIT_EXCEEDED, SENDERS_FAULT, cause);
    }

    public static ZClientException UPLOAD_FAILED(String msg, Throwable cause) {
        return new ZClientException(msg, UPLOAD_FAILED, SENDERS_FAULT, cause);
    }

    public static ZClientException ZIMBRA_SHARE_PARSE_ERROR(String msg, Throwable cause) {
        return new ZClientException(msg, ZIMBRA_SHARE_PARSE_ERROR, SENDERS_FAULT, cause);
    }
}
