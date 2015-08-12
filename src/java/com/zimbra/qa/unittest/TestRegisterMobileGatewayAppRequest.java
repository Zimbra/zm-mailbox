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
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.account.message.RegisterMobileGatewayAppRequest;
import com.zimbra.soap.account.type.ZmgDeviceSpec;

public class TestRegisterMobileGatewayAppRequest extends TestCase {

    private static final String USER = "user2";
    private ZMailbox mbox;
    private Account account;

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
        ZmgDeviceSpec zmgDevice = new ZmgDeviceSpec("1234", "124h67", "gcm");
        zmgDevice.setOSName("os_name");
        zmgDevice.setOSVersion("7.0");
        zmgDevice.setMaxPayloadSize(512);
        RegisterMobileGatewayAppRequest request = new RegisterMobileGatewayAppRequest(zmgDevice);
        mbox.invokeJaxb(request);
        account = Provisioning.getInstance().getAccountByName(USER);
        assertTrue(account.isPrefZmgPushNotificationEnabled());
    }

    @Test
    public void testRegisterMobileGatewayAppRequestWithNoDevice() {
        checkException(null);
    }

    @Test
    public void testRegisterMobileGatewayAppRequestWithNoAppId() {
        ZmgDeviceSpec zmgDevice = new ZmgDeviceSpec(null, "124h67", "gcm");
        zmgDevice.setOSName("os_name");
        zmgDevice.setOSVersion("7.0");
        zmgDevice.setMaxPayloadSize(512);
        checkException(zmgDevice);
    }

    @Test
    public void testRegisterMobileGatewayAppRequestWithNoToken() {
        ZmgDeviceSpec zmgDevice = new ZmgDeviceSpec("1234", null, "gcm");
        zmgDevice.setOSName("os_name");
        zmgDevice.setOSVersion("7.0");
        zmgDevice.setMaxPayloadSize(512);
        checkException(zmgDevice);
    }

    @Test
    public void testRegisterMobileGatewayAppRequestWithNoProvider() {
        ZmgDeviceSpec zmgDevice = new ZmgDeviceSpec("1234", "124h67", null);
        zmgDevice.setOSName("os_name");
        zmgDevice.setOSVersion("7.0");
        zmgDevice.setMaxPayloadSize(512);
        checkException(zmgDevice);
    }

    private void checkException(ZmgDeviceSpec zmgDevice) {
        RegisterMobileGatewayAppRequest request = new RegisterMobileGatewayAppRequest(zmgDevice);
        boolean caughtInvalidRequest = false;
        try {
            mbox.invokeJaxb(request);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode())) {
                caughtInvalidRequest = true;
            }
        }
        assertTrue(caughtInvalidRequest);
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
