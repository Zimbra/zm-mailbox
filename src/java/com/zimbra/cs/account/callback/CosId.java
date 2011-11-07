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

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class CosId extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry)
    throws ServiceException {
        
        validateCosId(attrsToModify, attrName);
    }
    
    private void validateCosId(Map attrsToModify, String attrName) throws ServiceException {
        
        SingleValueMod mod = singleValueMod(attrsToModify, attrName);
        if (mod.unsetting())
            return;
        else {
            String cosId = mod.value();
            Provisioning prov = Provisioning.getInstance();
            /*
             * hmm, not sure if YCC(CalendarProvisioning) also requires that 
             * cos must exist when setting a cos id (e.g. zimbraDomainDefaultCOSId)
             * skip for now.  Hack to use idIsUUID() for the check.
             */
            if (prov.idIsUUID()) {
                Cos cos = prov.get(Key.CosBy.id, cosId);
                if (cos == null)
                    throw ServiceException.INVALID_REQUEST("cos id " + cosId + 
                            " does not point to a valid cos", null);
            }
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }
}
