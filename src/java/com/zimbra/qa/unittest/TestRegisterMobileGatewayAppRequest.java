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
import com.zimbra.soap.account.message.RegisterMobileGatewayAppRequest;
import com.zimbra.soap.account.type.ZmgDeviceSpec;

public class TestRegisterMobileGatewayAppRequest extends TestCase {

    private static final String USER = "user2";
    private ZMailbox mbox;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        mbox = TestUtil.getZMailbox(USER);
        super.setUp();
    }

    @Test
    public void testRegisterMobileGatewayAppRequest() throws ServiceException {
        ZmgDeviceSpec zmgDevice = new ZmgDeviceSpec("1234");
        zmgDevice.setRegistrationId("124h67");
        zmgDevice.setPushProvider("gcm");
        RegisterMobileGatewayAppRequest request = new RegisterMobileGatewayAppRequest(zmgDevice);
        mbox.invokeJaxb(request);
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
