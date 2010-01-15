/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import com.zimbra.common.service.ServiceException;

/**
 * 
 */
@SuppressWarnings("serial")
public class AdminServiceException extends ServiceException {
    public static final String NO_SUCH_WAITSET = "admin.NO_SUCH_WAITSET";
    
    private AdminServiceException(String message, String code, boolean isReceiversFault, Throwable cause, Argument... args) {
        super(message, code, isReceiversFault, cause, args);
    }
    
    public static AdminServiceException NO_SUCH_WAITSET(String id) {
        return new AdminServiceException("No such waitset: "+id, NO_SUCH_WAITSET, SENDERS_FAULT,
            null, new Argument("id", id, Argument.Type.STR));
    }
    
}
