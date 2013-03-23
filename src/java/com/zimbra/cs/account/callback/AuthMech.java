/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 VMware, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.auth.AuthMechanism;

public class AuthMech extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry)
            throws ServiceException {
        // TODO Auto-generated method stub
        
        String authMech;
        
        SingleValueMod mod = singleValueMod(attrName, attrValue);
        if (mod.setting()) {
            authMech = mod.value();
            
            boolean valid = false;
            
            if (authMech == null) {
                valid = true;
            } else if (authMech.startsWith(AuthMechanism.AuthMech.custom.name())) {
                valid = true;
            } else {
                try {
                    AuthMechanism.AuthMech mech = AuthMechanism.AuthMech.fromString(authMech);
                    valid = true;
                } catch (ServiceException e) {
                    ZimbraLog.account.error("invalud auth mech", e);
                }
            }
           
            if (!valid) {
                throw ServiceException.INVALID_REQUEST("invalud value: " + authMech, null);
            }
        }

    }

    
    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }

}
