/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.im.provider;

import java.util.HashMap;

import org.jivesoftware.util.PropertyProvider;

import com.zimbra.common.util.ZimbraLog;

public class IMLocalProperties implements PropertyProvider {
    
    private HashMap<String, String> sPropertyMap = new HashMap<String, String>();
    
    public IMLocalProperties() {
        sPropertyMap.put("provider.user.className", ZimbraUserProvider.class.getName());
        sPropertyMap.put("provider.auth.className", ZimbraAuthProvider.class.getName());
        sPropertyMap.put("provider.group.className", ZimbraGroupProvider.class.getName());
        sPropertyMap.put("connectionProvider.className", ZimbraConnectionProvider.class.getName());
        sPropertyMap.put("routingTableImpl.className", ZimbraRoutingTableImpl.class.getName());
        
        // do not allow users to update IM passwd via XMPP
        sPropertyMap.put("register.password", "false");
    }        

    public String get(String key) {
        String retVal = sPropertyMap.get(key);
        ZimbraLog.im.info("IMLocalProperties.get("+key+") = "+retVal);
        return retVal;
    }

    public String put(String key, String value) {
        throw new UnsupportedOperationException();
    }

    public String remove(String key) {
        throw new UnsupportedOperationException();
    }

}
