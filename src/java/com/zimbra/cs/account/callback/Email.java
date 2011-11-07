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
package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.AttributeInfo;

/**
 * Callback for validating attributes that should've been declared
 * email in zimbra-attrs.xml but had been declared as string.  
 * 
 * To avoid LDAP upgrade complication, we use this callback for
 * validating the format.  If the attr had been declared as 
 * email, the validation would have happened in AttributeInfo.checkValue. 
 *
 */
public class Email extends AttributeCallback {


    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry)
            throws ServiceException {
        
        SingleValueMod mod = singleValueMod(attrsToModify, attrName);
        if (mod.unsetting())
            return;
        
        AttributeInfo.validEmailAddress(mod.value(), false);
    }
    
    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }


}
