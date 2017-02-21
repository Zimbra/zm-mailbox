/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.AttributeManager.IDNType;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.IDNUtil;

/**
 * Convert unicode address to ASCII (ACE)
 *
 */
public class IDNCallback extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object value,
            Map attrsToModify, Entry entry) 
    throws ServiceException {
        
        MultiValueMod mod = multiValueMod(attrsToModify, attrName);
        IDNType idnType = AttributeManager.idnType(AttributeManager.getInstance(), attrName);
        
        if (mod.adding() || mod.replacing()) {
            Set<String> asciiValues = new HashSet<String>();
            List<String> addrs = mod.values();
            for (String addr : addrs) {
                if (addr == null || addr.equals("")) continue;
                
                String asciiName;
                if (addr.charAt(0) == '@') {
                    // meant for catchall addresses
                    asciiName = "@" + IDNUtil.toAsciiDomainName(addr.substring(1));
                } else {
                    asciiName = IDNUtil.toAscii(addr, idnType);
                }
                
                asciiValues.add(asciiName);
            }
            
            String aName = (mod.adding()?"+":"") + attrName;
            attrsToModify.remove(aName);
            attrsToModify.put(aName, asciiValues.toArray(new String[asciiValues.size()]));
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }
}