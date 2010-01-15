/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
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
package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

public enum RightModifier {
    RM_DENY('-', AdminConstants.A_DENY),                   // the right is specifically denied
    RM_CAN_DELEGATE('+', AdminConstants.A_CAN_DELEGATE);   // the same right or part of the right can be delegated(granted) to others
    
    // urg, our soap intereface is already published with with the deny attribute, 
    // for backward compatibility, we map the modifier to soap instead of changing soap 
    // to take a modifier attribute
    private String mSoapAttrMapping;
    private char mModifier;
    
    private RightModifier(char modifier, String soapAttrMapping) {
        mModifier = modifier;
        mSoapAttrMapping = soapAttrMapping;
    }
    
    public static RightModifier fromChar(char c) throws ServiceException {
        
        if (RM_DENY.mModifier == c)
            return RM_DENY;
        else if (RM_CAN_DELEGATE.mModifier == c)
            return RM_CAN_DELEGATE;
        else
            return null;
    }
    
    public char getModifier() {
        return mModifier;
    }
    
    public String getSoapAttrMapping() {
        return mSoapAttrMapping;
    }
}
