/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Date;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class GalCallback extends AttributeCallback {
    
    private String oldValue = null;
    private String newValue = null;

    @Override
    public void preModify(CallbackContext context, String attrName, Object value,
            Map attrsToModify, Entry entry) 
    throws ServiceException {
        
        if (entry != null) {
            oldValue = entry.getAttr(attrName, true);
        }
        SingleValueMod mod = singleValueMod(attrsToModify, attrName);
        newValue = mod.value();
        if (attrName.equals("zimbraGalLdapFilter")) {
            if (mod.unsetting())
                return;
            if ("ad".equalsIgnoreCase(newValue)) {
                attrsToModify.put(Provisioning.A_zimbraGalLdapGroupHandlerClass, 
                        com.zimbra.cs.account.grouphandler.ADGroupHandler.class.getCanonicalName());
            }
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        if (entry instanceof Domain) {
            try {
                if (!StringUtil.equal(oldValue, newValue)) {
                    ((Domain) entry).setGalDefinitionLastModifiedTime(new Date());
                }
            } catch (ServiceException e) {
                ZimbraLog.misc.warn("Unable to set zimbraGalDefinitionLastModifiedTime " + e);
            }
        }
    }
    
}
