/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
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
