/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.HashSet;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.zimbra.common.localconfig.LC;

@Ignore
public class ServerThrottleTest {

    String ip = "146.126.106.1";
    String acctId = "abc-123";

    
    public void throttleIpCount() {
        int numReqs = 100;
        ServerThrottle throttle = new ServerThrottle("test");
        throttle.setIpReqsPerSecond(numReqs);
        long time = System.currentTimeMillis() + 10000000;
        // add timestamps far enough in the future that they don't get pruned.
        // this tests basic functions

        Assert.assertFalse(throttle.isIpThrottled(ip));

        for (int i = 0; i < numReqs; i++) {
            throttle.addIpReq(ip, time);
        }

        Assert.assertTrue(throttle.isIpThrottled(ip));
        Assert.assertFalse(throttle.isIpThrottled(ip + "foo"));
        Assert.assertFalse(throttle.isAccountThrottled(acctId));
    }

    @Test
    public void throttleIpIgnore() {
        int numReqs = 100;
        ServerThrottle throttle = new ServerThrottle("test");
        throttle.setIpReqsPerSecond(numReqs);
        long time = System.currentTimeMillis() + 10000000;
        Assert.assertFalse(throttle.isIpThrottled(ip));

        for (int i = 0; i < numReqs; i++) {
            throttle.addIpReq(ip, time);
        }

        Assert.assertTrue(throttle.isIpThrottled(ip));
        throttle.addIgnoredIp(ip);
        Assert.assertFalse(throttle.isIpThrottled(ip));
    }

    @Test
    public void throttleIpTime() {
        int numReqs = 1;
        ServerThrottle throttle = new ServerThrottle("test");
        throttle.setIpReqsPerSecond(numReqs);

        Assert.assertFalse(throttle.isIpThrottled(ip));
        // on a really slow system this might fail; if there is more than 1
        // second pause in execution here
        Assert.assertTrue(throttle.isIpThrottled(ip));
        Assert.assertFalse(throttle.isIpThrottled(ip + "foo"));
        Assert.assertFalse(throttle.isAccountThrottled(acctId));

        try {
            Thread.sleep(1001);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertFalse(throttle.isIpThrottled(ip));
        Assert.assertFalse(throttle.isIpThrottled(ip + "foo"));
        Assert.assertFalse(throttle.isAccountThrottled(acctId));
    }

    @Test
    public void throttleAcctCount() {
        int numReqs = 100;
        ServerThrottle throttle = new ServerThrottle("test");
        throttle.setAcctReqsPerSecond(numReqs);
        long time = System.currentTimeMillis() + 10000000;
        // add timestamps far enough in the future that they don't get pruned.
        // this tests basic functions

        Assert.assertFalse(throttle.isAccountThrottled(acctId));

        for (int i = 0; i < numReqs; i++) {
            throttle.addAcctReq(acctId, time);
        }

        Assert.assertTrue(throttle.isAccountThrottled(acctId));
        Assert.assertFalse(throttle.isAccountThrottled(acctId + "foo"));
        Assert.assertFalse(throttle.isIpThrottled(ip));
    }

    @Test
    public void throttleAcctTime() {
        int numReqs = 1;
        ServerThrottle throttle = new ServerThrottle("test");
        throttle.setAcctReqsPerSecond(numReqs);

        Assert.assertFalse(throttle.isAccountThrottled(acctId));
        // on a really slow system this might fail; if there is more than 1
        // second pause in execution here
        Assert.assertTrue(throttle.isAccountThrottled(acctId));
        Assert.assertFalse(throttle.isAccountThrottled(acctId + "foo"));
        Assert.assertFalse(throttle.isIpThrottled(ip));

        try {
            Thread.sleep(1001);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertFalse(throttle.isAccountThrottled(acctId));
        Assert.assertFalse(throttle.isAccountThrottled(acctId + "foo"));
        Assert.assertFalse(throttle.isIpThrottled(ip));
    }

    @Test
    public void throttleIpWhitelist() {
        int numReqs = 100;
        ServerThrottle throttle = new ServerThrottle("test");
        throttle.setIpReqsPerSecond(numReqs);
        long time = System.currentTimeMillis() + 10000000;
        Assert.assertFalse(throttle.isIpThrottled(ip));

        for (int i = 0; i < numReqs; i++) {
            throttle.addIpReq(ip, time);
        }

        Assert.assertTrue(throttle.isIpThrottled(ip));
        throttle.addWhitelistIp(ip);
        Assert.assertFalse(throttle.isIpThrottled(ip));
    }

    @Test
    public void throttleAcctWhitelist() {
        int numReqs = 100;
        ServerThrottle throttle = new ServerThrottle("test");
        throttle.setAcctReqsPerSecond(numReqs);
        long time = System.currentTimeMillis() + 10000000;
        // add timestamps far enough in the future that they don't get pruned.
        // this tests basic functions

        Assert.assertFalse(throttle.isAccountThrottled(acctId, ip));

        for (int i = 0; i < numReqs; i++) {
            throttle.addAcctReq(acctId, time);
        }

        //add null to ip list just to be sure varargs is handled the way it should be
        Assert.assertTrue(throttle.isAccountThrottled(acctId, ip, null));
        Assert.assertTrue(throttle.isAccountThrottled(acctId, ip));
        Assert.assertTrue(throttle.isAccountThrottled(acctId, null, ip));
        Assert.assertTrue(throttle.isAccountThrottled(acctId, null, null, ip));
        Assert.assertFalse(throttle.isIpThrottled(ip));

        throttle.addWhitelistIp(ip);
        //add null to ip list just to be sure varargs is handled the way it should be
        Assert.assertFalse(throttle.isAccountThrottled(acctId, null, ip));
        Assert.assertFalse(throttle.isAccountThrottled(acctId, ip));
        Assert.assertFalse(throttle.isAccountThrottled(acctId, ip, null));
        Assert.assertFalse(throttle.isAccountThrottled(acctId, null, null, ip));
    }

    @Test
    public void testUnknownHost() {
        LC.zimbra_attrs_directory.setDefault(System.getProperty("user.dir") + "/conf/attrs");
        try {
            ServerThrottle.configureThrottle("IMAP", 1, 1, Sets.newHashSet("nosuchhost", "www.zimbra.com"), new HashSet<String>());
        } catch (Exception e) {
            e.printStackTrace();
        }
        }
}
