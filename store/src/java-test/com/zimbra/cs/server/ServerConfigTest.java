/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.server;

import junit.framework.Assert;

import org.junit.Test;

public class ServerConfigTest {

    private static final String SINGLE_IP = "1.2.3.4";
    private static final String SINGLE_HOSTNAME = "host.example.com";

    private static final String MULTI_ADDR0 = "1.2.3.4";
    private static final String MULTI_ADDR1 = "host2.example.com";
    private static final String MULTI_ADDR2 = "8.9.10.11";

    @Test
    public void singleIp() {
        String[] single = {SINGLE_IP};
        String[] get = ServerConfig.getAddrListCsv(single);
        Assert.assertEquals(1, get.length);
        Assert.assertEquals(SINGLE_IP, get[0]);
    }

    @Test
    public void singleHostname() {
        String[] single = {SINGLE_HOSTNAME};
        String[] get = ServerConfig.getAddrListCsv(single);
        Assert.assertEquals(1, get.length);
        Assert.assertEquals(SINGLE_HOSTNAME, get[0]);
    }

    @Test
    public void empty() {
        String[] empty = {};
        String[] get = ServerConfig.getAddrListCsv(empty);
        Assert.assertEquals(0, get.length);
    }


    @Test
    public void multiAddrsSingleString() {
        String[] multi = {MULTI_ADDR0 + "," + MULTI_ADDR1 + "," + MULTI_ADDR2};
        Assert.assertEquals(1, multi.length);
        String[] get = ServerConfig.getAddrListCsv(multi);
        Assert.assertEquals(3, get.length);
        Assert.assertEquals(MULTI_ADDR0, get[0]);
        Assert.assertEquals(MULTI_ADDR1, get[1]);
        Assert.assertEquals(MULTI_ADDR2, get[2]);
    }
}
