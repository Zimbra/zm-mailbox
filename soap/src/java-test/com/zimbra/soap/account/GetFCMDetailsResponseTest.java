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
import com.zimbra.soap.account.message.GetFCMDetailsResponse;

/**
 * Unit test for {@link GetFCMDetailsResponse}, including the optional {@code pushStatus} element.
 */
public class GetFCMDetailsResponseTest {

    private static final String CONFIG = "{\"deviceJSON\":{\"android\":{}}}";

    @Test
    public void pushStatusXmlRoundTrip() throws Exception {
        Element elem = JaxbUtil.jaxbToElement(new GetFCMDetailsResponse(CONFIG, "PUSH_DISABLED"),
                XMLElement.mFactory);
        Assert.assertEquals("pushStatus element", "PUSH_DISABLED", elem.getElement("pushStatus").getText());

        GetFCMDetailsResponse resp = JaxbUtil.elementToJaxb(elem);
        Assert.assertEquals(CONFIG, resp.getDeviceConfig());
        Assert.assertEquals("PUSH_DISABLED", resp.getPushStatus());
    }

    @Test
    public void pushStatusJsonRoundTrip() throws Exception {
        Element elem = JaxbUtil.jaxbToElement(new GetFCMDetailsResponse(CONFIG, "ENABLED"), JSONElement.mFactory);

        GetFCMDetailsResponse resp = JaxbUtil.elementToJaxb(elem);
        Assert.assertEquals(CONFIG, resp.getDeviceConfig());
        Assert.assertEquals("ENABLED", resp.getPushStatus());
    }

    @Test
    public void pushStatusIsOptionalForOlderServers() throws Exception {
        Element elem = JaxbUtil.jaxbToElement(new GetFCMDetailsResponse(CONFIG), XMLElement.mFactory);
        Assert.assertNull("no pushStatus element when unset", elem.getOptionalElement("pushStatus"));

        GetFCMDetailsResponse resp = JaxbUtil.elementToJaxb(elem);
        Assert.assertNull(resp.getPushStatus());
        Assert.assertFalse(GetFCMDetailsResponse.class.getDeclaredField("pushStatus")
                .getAnnotation(XmlElement.class).required());
    }

    @Test
    public void setterUpdatesPushStatus() {
        GetFCMDetailsResponse resp = new GetFCMDetailsResponse(CONFIG);
        resp.setPushStatus("ENABLED");
        Assert.assertEquals("ENABLED", resp.getPushStatus());
    }
}
