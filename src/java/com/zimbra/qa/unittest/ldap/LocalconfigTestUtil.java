/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.qa.unittest.ldap;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import com.zimbra.common.localconfig.KnownKey;

public class LocalconfigTestUtil {
    
    static void modifyLocalConfig(KnownKey key, String value) throws Exception {
        String keyName = key.key();
        
        Process process = null;
        try {
            String command = "/opt/zimbra/bin/zmlocalconfig -e " + keyName + "=" + value;
            System.out.println(command);
            process = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } 
        
        int exitCode;
        try {
            exitCode = process.waitFor();
            assertEquals(0, exitCode);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw e;
        } 
    }
    
    static void modifyLocalConfigTransient(KnownKey key, String value) {
        key.setDefault(value);
    }
}
