/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest.prov;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import com.zimbra.common.localconfig.KnownKey;

public class LocalconfigTestUtil {
    
    public static void modifyLocalConfig(KnownKey key, String value) throws Exception {
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
    
    public static void modifyLocalConfigTransient(KnownKey key, String value) {
        key.setDefault(value);
    }
}
