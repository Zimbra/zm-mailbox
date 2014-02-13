/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

@Ignore //long running test; kept for dev purposes only
public class TestNoOp extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = TestNoOp.class.getSimpleName();
    private static final int THREAD_COUNT = 5;
    private static final int LOOP_COUNT = 10;
    private static final int THREAD_TIMEOUT = 30000;

    @Override
    public void setUp() throws Exception {
        cleanUp();
    }

    @Override
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }

    @Test
    public void testNoOpMany() throws ServiceException {
        Set<Thread> threads = new HashSet<Thread>();
        final Set<Exception> exceptions = new HashSet<Exception>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            Thread t = new Thread() {
                @Override
                public void run() {
                    ZMailbox mbox = null;
                    try {
                        mbox = TestUtil.getZMailbox(USER_NAME);
                    } catch (ServiceException e) {
                        exceptions.add(e);
                        return;
                    }
                    for (int j = 0; j < LOOP_COUNT; j++) {
                        try {
                            mbox.noOp(1000);
                        } catch (ServiceException se) {
                            exceptions.add(se);
                        }
                    }
                }
            };
            threads.add(t);
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            try {
                //note this test fails with normal timeout min limit in NoOp.java
                t.join(THREAD_TIMEOUT);
            } catch (InterruptedException e) {
            }
            Assert.assertFalse(t.isAlive());
        }
        if (!exceptions.isEmpty()) {
            for (Exception e : exceptions) {
                ZimbraLog.test.error("Exception during test", e);
            }
            Assert.fail(exceptions.size() + " exceptions during test");
        }

    }
}
