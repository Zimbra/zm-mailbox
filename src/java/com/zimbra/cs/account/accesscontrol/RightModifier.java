/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

public enum RightModifier {
    // the right is specifically denied
    RM_DENY('-', AdminConstants.A_DENY),
    
    // the same right or part of the right can be delegated(granted) to others
    RM_CAN_DELEGATE('+', AdminConstants.A_CAN_DELEGATE),
    
    // the grant affects sub domains only
    //   - for domain rights only
    //   - can only be granted on domain targets
    //   - affect sub domains only, *not* the domain on which the right is granted
    //   - does not work with the +/- modifier.  i.e., grants made with the * modifier 
    //     cannot be delegated and cannot be negated with a - modifier.
    RM_SUBDOMAIN('*', AdminConstants.A_SUB_DOMAIN); 
    
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
        else if (RM_SUBDOMAIN.mModifier == c)
            return RM_SUBDOMAIN;
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
