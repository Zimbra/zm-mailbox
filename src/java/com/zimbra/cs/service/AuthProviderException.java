/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
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
}
