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

import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.account.message.AddZmgDeviceRequest;
import com.zimbra.soap.account.message.AddZmgDeviceResponse;
import com.zimbra.soap.account.type.ZmgDeviceSpec;

public class TestAddZmgDevice extends TestCase {

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
    public void testAddZmgDevice() throws ServiceException {
        ZmgDeviceSpec zmgDevice = new ZmgDeviceSpec("1234");
        zmgDevice.setRegistrationId("124h67");
        zmgDevice.setPushProvider("gcm");
        AddZmgDeviceRequest request = new AddZmgDeviceRequest(zmgDevice);
        AddZmgDeviceResponse response = mbox.invokeJaxb(request);
        List<String> notificationEnabled = mbox.getAccountInfo(true).getAttrs()
            .get("zimbraPrefZmgPushNotificationEnabled");
        String result = response.getMessage();
        assertEquals("1", result);
        assertEquals("TRUE", notificationEnabled.get(0));
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
