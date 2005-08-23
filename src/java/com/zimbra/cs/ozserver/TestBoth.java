/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.ozserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.zimbra.cs.util.Zimbra;

class TestBoth {

    private static final int CLIENT_THREADS = 2;
    
    static class TestClientThread extends Thread {
        public TestClientThread(int num) {
            super("TestClientThread-" + num);
            setDaemon(true);
        }

        public void run() {
            while (true) {
                try {
                    TestClient.test();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        
    }
    
    public static void main(String[] args) throws IOException {
        Zimbra.toolSetup("DEBUG");
        TestServer.main(null);
        for (int i = 0; i < CLIENT_THREADS; i++) {
            new TestClientThread(i).start();
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        br.readLine();
        TestServer.shutdown();
    }
}
