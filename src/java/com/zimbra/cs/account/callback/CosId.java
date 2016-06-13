/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
