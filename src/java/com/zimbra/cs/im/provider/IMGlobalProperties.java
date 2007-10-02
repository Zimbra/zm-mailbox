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

import org.jivesoftware.util.JiveProperties;
import org.jivesoftware.util.PropertyProvider;

import com.zimbra.common.localconfig.LC;

public class IMGlobalProperties implements PropertyProvider {
    
    private HashMap<String, String> mProvMap = new HashMap<String, String>();
    private JiveProperties mJiveProps;
    
    public IMGlobalProperties() {
        mProvMap.put("xmpp.socket.ssl.keystore", LC.mailboxd_keystore.value());
        mProvMap.put("xmpp.socket.ssl.keypass", LC.mailboxd_keystore_password.value());
        mProvMap.put("xmpp.socket.ssl.truststore", LC.mailboxd_keystore.value());
        mProvMap.put("xmpp.socket.ssl.trustpass", LC.mailboxd_truststore_password.value());
        mProvMap.put("xmpp.socket.blocking", "false");
        mProvMap.put("xmpp.server.certificate.verify", "false");
        
//        if (true) { // UNCOMMENT ME TO DISABLE TLS FOR DEBUGGING!
//            mProvMap.put("xmpp.client.tls.policy", "disabled");
//        }
                
//      mProvMap.put("xmpp.server.read.timeout", Integer.toString(60 * 60 * 1000));        
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
