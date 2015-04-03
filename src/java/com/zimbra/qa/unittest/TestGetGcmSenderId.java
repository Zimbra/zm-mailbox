/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.account.message.GetGcmSenderIdRequest;
import com.zimbra.soap.account.message.GetGcmSenderIdResponse;

public class TestGetGcmSenderId extends TestCase {

    private static final String USER = "user2";
    private ZMailbox mbox;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testGetGcmSenderId() throws ServiceException {

        TestUtil.setConfigAttr(Provisioning.A_zimbraGCMSenderId, "123456");

        mbox = TestUtil.getZMailbox(USER);
        GetGcmSenderIdRequest request = new GetGcmSenderIdRequest();
        GetGcmSenderIdResponse response = mbox.invokeJaxb(request);
        String senderId = response.getSenderId();
        assertEquals("123456", senderId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
