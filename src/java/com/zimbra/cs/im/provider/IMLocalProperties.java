/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
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
        sPropertyMap.put("provider.user.className", "com.zimbra.cs.im.provider.ZimbraUserProvider");
        sPropertyMap.put("provider.auth.className", "com.zimbra.cs.im.provider.ZimbraAuthProvider");
        sPropertyMap.put("provider.group.className", "com.zimbra.cs.im.provider.ZimbraGroupProvider");
        sPropertyMap.put("connectionProvider.className", "com.zimbra.cs.im.provider.ZimbraConnectionProvider");
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
