/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.cs.server;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.cs.server.ServerThrottle;

public class ServerThrottleTest {

    String ip = "146.126.106.1";
    String acctId = "abc-123";

    @Test
    public void throttleIpCount() {
        int numReqs = 100;
        ServerThrottle throttle = new ServerThrottle();
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
        ServerThrottle throttle = new ServerThrottle();
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
        ServerThrottle throttle = new ServerThrottle();
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
        ServerThrottle throttle = new ServerThrottle();
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
        ServerThrottle throttle = new ServerThrottle();
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
}
