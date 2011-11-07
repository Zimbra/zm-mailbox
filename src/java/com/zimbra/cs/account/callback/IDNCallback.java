/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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