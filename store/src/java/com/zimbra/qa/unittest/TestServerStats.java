/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.qa.unittest;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.soap.admin.message.GetServerStatsRequest;
import com.zimbra.soap.admin.message.GetServerStatsResponse;

import junit.framework.TestCase;

public class TestServerStats
extends TestCase {

    private SoapProvisioning prov;
    private List<Socket> sockets = Lists.newArrayList();
    
    public void setUp()
    throws Exception {
        prov = TestUtil.newSoapProvisioning();
    }
    
    /**
     * Confirms that the server is returning the correct values for the number of protocol
     * handler threads and client connections.
     */
    public void testThreadsAndConnections()
    throws Exception {
        Server server = Provisioning.getInstance().getLocalServer();
        verifyThreadsAndConnections(ZimbraPerf.RTS_POP_THREADS, ZimbraPerf.RTS_POP_CONN, server.getPop3BindPort());
        verifyThreadsAndConnections(ZimbraPerf.RTS_IMAP_THREADS, ZimbraPerf.RTS_IMAP_CONN, server.getImapBindPort());
        verifyThreadsAndConnections(ZimbraPerf.RTS_LMTP_THREADS, ZimbraPerf.RTS_LMTP_CONN, server.getLmtpBindPort());
    }
    
    private void verifyThreadsAndConnections(String threadStatName, String connStatName, int port)
    throws Exception {
        int originalConnections = getStatValue(connStatName);
        
        Socket socket1 = connect(port);
        Socket socket2 = connect(port);
        int numConnections = originalConnections + 2;
        waitForValue(connStatName, numConnections);
        
        int numThreads = getStatValue(threadStatName);
        assertTrue("Unexpected thread count: " + numThreads, numThreads > 0);
        
        socket1.close();
        socket2.close();
        waitForValue(connStatName, originalConnections);
    }
    
    /**
     * Checks for a value repeatedly, to give the server time to update stats.
     */
    private void waitForValue(String statName, int expected)
    throws Exception {
        int val = 0;
        for (int i = 1; i <= 20; i++) {
            val = getStatValue(statName);
            if (val == expected) {
                return;
            }
            Thread.sleep(50);
        }

        assertEquals(statName, expected, val);
    }
    
    private int getStatValue(String statName)
    throws Exception {
        GetServerStatsResponse stats = prov.invokeJaxb(new GetServerStatsRequest(statName));
        String s = stats.getValue(statName);
        if (StringUtil.isNullOrEmpty(s)) {
            return 0;
        }
        return Integer.parseInt(stats.getValue(statName));
    }
    
    private Socket connect(int port)
    throws ServiceException, IOException {
        Server server = Provisioning.getInstance().getLocalServer();
        Socket socket = new Socket(server.getName(), port);
        sockets.add(socket);
        return socket;
    }
    
    public void tearDown()
    throws IOException {
        for (Socket socket : sockets) {
            if (!socket.isClosed()) {
                socket.close();
            }
        }
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestServerStats.class);
    }
}
