/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.common.soap;

import com.zimbra.common.service.ServiceException;

public class XmlParseException
extends ServiceException {

    private static final long serialVersionUID = 2012769501847268691L;

    protected XmlParseException(String message, Throwable cause) {
        super(message, PARSE_ERROR, SENDERS_FAULT, cause);
    }

    public static XmlParseException PARSE_ERROR(String message, Throwable cause) {
        return new XmlParseException("parse error: " + message, cause);
    }
}
