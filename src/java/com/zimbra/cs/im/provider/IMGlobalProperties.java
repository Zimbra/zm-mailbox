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

import org.jivesoftware.util.JiveProperties;
import org.jivesoftware.util.PropertyProvider;

public class IMGlobalProperties implements PropertyProvider {
    
    private HashMap<String, String> mProvMap = new HashMap<String, String>();
    private JiveProperties mJiveProps;
    
    public IMGlobalProperties() {
        mProvMap.put("xmpp.socket.ssl.keystore", "tomcat/conf/keystore");
        mProvMap.put("xmpp.socket.ssl.keypass", "zimbra");
        mProvMap.put("xmpp.socket.ssl.truststore", "tomcat/conf/keystore");
        mProvMap.put("xmpp.socket.ssl.trustpass", "zimbra");
        mProvMap.put("xmpp.socket.blocking", "false");
//        provMap.put("", "");
    }
    
    public String get(String key) {
        synchronized(this) {
            if (mProvMap.containsKey(key)) {
                return mProvMap.get(key);
            } else {
                if (mJiveProps == null)
                    mJiveProps = JiveProperties.getInstance();
            }
        }
        return mJiveProps.get(key);
    }

    public String put(String key, String value) {
        synchronized(this) {
            if (mProvMap.containsKey(key)) {
                throw new UnsupportedOperationException("Cannot write to provisioning-mapped props yet");
            }
            if (mJiveProps == null)
                mJiveProps = JiveProperties.getInstance();
        }
        return mJiveProps.put(key, value);
    }

    public String remove(String key) {
        synchronized(this) {
            if (mProvMap.containsKey(key)) {
                throw new UnsupportedOperationException("Cannot write to provisioning-mapped props yet");
            }
            if (mJiveProps == null)
                mJiveProps = JiveProperties.getInstance();
        }
        return mJiveProps.remove(key);
    }
}
