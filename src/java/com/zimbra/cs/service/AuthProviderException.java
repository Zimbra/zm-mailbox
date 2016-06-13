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

package com.zimbra.cs.service;

import com.zimbra.common.service.ServiceException;

@SuppressWarnings("serial")
public class AuthProviderException extends ServiceException {
    
    private boolean mCanIgnore;
    
    // auth data is not present for the auth provider
    public static final String NO_AUTH_DATA        = "authprovider.NO_AUTH_DATA";
    
    // auth method is not supported by the auth provider
    public static final String NOT_SUPPORTED       = "authprovider.NOT_SUPPORTED";
    
    // internal auth provider error
    public static final String FAILURE             = "authprovider.FAILURE";
    
    private AuthProviderException(String message, String code, boolean isReceiversFault) {
        super(message, code, isReceiversFault);
        setCanIgnore(true);
    }
    
    private AuthProviderException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault, cause);
        setCanIgnore(true);
    }
    
    private void setCanIgnore(boolean canIgnore) {
        mCanIgnore = canIgnore;
    }
    
    public boolean canIgnore() {
        return mCanIgnore;
    }
    
    public static AuthProviderException NO_AUTH_DATA() {
        return new AuthProviderException("no auth token", NO_AUTH_DATA, SENDERS_FAULT, null);
    }
    
    public static AuthProviderException NOT_SUPPORTED() {
        return new AuthProviderException("not suported", NOT_SUPPORTED, SENDERS_FAULT, null);
    }
    
    public static AuthProviderException FAILURE(String message) {
        return new AuthProviderException("failure:" + message, FAILURE, SENDERS_FAULT, null);
    }
}
