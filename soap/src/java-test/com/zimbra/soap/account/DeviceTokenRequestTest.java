/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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
package com.zimbra.soap.account;

import javax.xml.bind.annotation.XmlElement;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.AddDeviceTokenRequest;
import com.zimbra.soap.account.message.RemoveDeviceTokenRequest;

/**
 * Unit test for {@link AddDeviceTokenRequest} and {@link RemoveDeviceTokenRequest}.
 */
public class DeviceTokenRequestTest {

    private static AddDeviceTokenRequest newAddRequest() {
        AddDeviceTokenRequest req = new AddDeviceTokenRequest();
        req.setDeviceToken("fcm-token-123");
        req.setDeviceId("device-abc");
        req.setDeviceType("MOBILE");
        req.setPushProvider("FCM");
        req.setAppId("com.zimbra.mobile");
        req.setAppVersion("2.0.0");
        req.setDeviceOSName("Android");
        req.setDeviceOSVersion("14");
        return req;
    }

    private static void checkAddRequest(AddDeviceTokenRequest req, String tag) {
        Assert.assertEquals(tag + "deviceToken", "fcm-token-123", req.getDeviceToken());
        Assert.assertEquals(tag + "deviceId", "device-abc", req.getDeviceId());
        Assert.assertEquals(tag + "deviceType", "MOBILE", req.getDeviceType());
        Assert.assertEquals(tag + "pushProvider", "FCM", req.getPushProvider());
        Assert.assertEquals(tag + "appId", "com.zimbra.mobile", req.getAppId());
        Assert.assertEquals(tag + "appVersion", "2.0.0", req.getAppVersion());
        Assert.assertEquals(tag + "deviceOSName", "Android", req.getDeviceOSName());
        Assert.assertEquals(tag + "deviceOSVersion", "14", req.getDeviceOSVersion());
    }

    private static boolean isRequired(Class<?> clazz, String field) throws Exception {
        return clazz.getDeclaredField(field).getAnnotation(XmlElement.class).required();
    }

    @Test
    public void addDeviceTokenRequestXmlRoundTrip() throws Exception {
        Element elem = JaxbUtil.jaxbToElement(newAddRequest(), XMLElement.mFactory);
        Assert.assertEquals("deviceId element", "device-abc", elem.getElement("deviceId").getText());
        AddDeviceTokenRequest req = JaxbUtil.elementToJaxb(elem);
        checkAddRequest(req, "XML round tripped ");
    }

    @Test
    public void addDeviceTokenRequestJsonRoundTrip() throws Exception {
        Element elem = JaxbUtil.jaxbToElement(newAddRequest(), JSONElement.mFactory);
        AddDeviceTokenRequest req = JaxbUtil.elementToJaxb(elem);
        checkAddRequest(req, "JSON round tripped ");
    }

    @Test
    public void addDeviceTokenRequestRequiredFields() throws Exception {
        for (String field : new String[] {"deviceToken", "deviceId", "deviceType", "pushProvider", "appId"}) {
            Assert.assertTrue(field + " is required", isRequired(AddDeviceTokenRequest.class, field));
        }
    }

    @Test
    public void removeDeviceTokenRequestWithDeviceIdRoundTrip() throws Exception {
        RemoveDeviceTokenRequest orig = new RemoveDeviceTokenRequest("fcm-token-123", "device-abc");
        for (Element.ElementFactory factory : new Element.ElementFactory[] {XMLElement.mFactory, JSONElement.mFactory}) {
            RemoveDeviceTokenRequest req = JaxbUtil.elementToJaxb(JaxbUtil.jaxbToElement(orig, factory));
            Assert.assertEquals("deviceToken", "fcm-token-123", req.getDeviceToken());
            Assert.assertEquals("deviceId", "device-abc", req.getDeviceId());
        }
    }

    @Test
    public void removeDeviceTokenRequestWithoutDeviceId() throws Exception {
        Assert.assertFalse("deviceId is optional", isRequired(RemoveDeviceTokenRequest.class, "deviceId"));
        Element elem = JaxbUtil.jaxbToElement(new RemoveDeviceTokenRequest("fcm-token-123"), XMLElement.mFactory);
        Assert.assertNull("no deviceId element", elem.getOptionalElement("deviceId"));
        RemoveDeviceTokenRequest req = JaxbUtil.elementToJaxb(elem);
        Assert.assertEquals("deviceToken", "fcm-token-123", req.getDeviceToken());
        Assert.assertNull("deviceId absent", req.getDeviceId());
    }
}
