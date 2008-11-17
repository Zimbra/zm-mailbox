/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.im;

import com.zimbra.common.service.ServiceException;

/**
 * 
 */
public class IMServiceException extends ServiceException {
    private static final long serialVersionUID = 8303045261247860050L;
    
    public static final String INVALID_ADDRESS = "im.INVALID_ADDRESS";
    
    public static final String ADDR = "addr";
    
    IMServiceException(String message, String code, boolean isReceiversFault, Argument... args) {
        super(message, code, isReceiversFault, args);
    }
    
    public static IMServiceException INVALID_ADDRESS(String addr) {
        return new IMServiceException("address is invalid: "+addr, INVALID_ADDRESS, SENDERS_FAULT, new Argument(ADDR, addr, Argument.Type.STR));
    }
}
