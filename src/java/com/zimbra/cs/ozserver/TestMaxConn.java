/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.ozserver;

import java.io.IOException;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

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
