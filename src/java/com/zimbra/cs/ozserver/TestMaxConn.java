/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.ozserver;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.util.Zimbra;

public class TestMaxConn {
    private static TestClient[] mClients;

    static Log mLog = LogFactory.getLog(TestMaxConn.class);

    public static void main(String[] args) {
        Zimbra.toolSetup("TRACE", null, true);

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int n = Integer.parseInt(args[2]);
        mClients = new TestClient[n];
        for (int i = 0; i < n; i++) {
            try {
                mClients[i] = new TestClient(host, port, false);
            } catch (IOException ioe) {
                mLog.warn("connection number " + i + " excepted", ioe);
                return;
            }
        }
    }
}
