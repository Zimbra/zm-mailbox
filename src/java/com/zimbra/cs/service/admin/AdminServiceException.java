/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
