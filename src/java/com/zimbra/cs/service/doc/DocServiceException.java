/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2012 Zimbra, Inc.
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
package com.zimbra.cs.service.doc;

import com.zimbra.common.service.ServiceException;

@SuppressWarnings("serial")
public class DocServiceException extends ServiceException {

    public static final String ERROR = "doc.ERROR";

    private DocServiceException(String message, String code, boolean isReceiversFault, Argument... args) {
        super(message, code, isReceiversFault, args);
    }
    private DocServiceException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault, cause);
    }
    public static DocServiceException ERROR(String w) {
        return new DocServiceException("error: "+ w, ERROR, SENDERS_FAULT);
    }
    public static DocServiceException ERROR(String w, Throwable cause) {
        return new DocServiceException("error: "+ w, ERROR, SENDERS_FAULT, cause);
    }
    public static DocServiceException INVALID_PATH(String path) {
        return new DocServiceException("invalid path: "+ path, ERROR, SENDERS_FAULT);
    }
}
