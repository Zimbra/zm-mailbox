/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.admin;

import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.soap.admin.message.GetServerRequest;
import com.zimbra.soap.admin.type.ServerSelector;

/**
 * Unit test for {@link GetServerRequest}.
 * Mostly checking that JAXB can cope with immutable objects like {@link ServerSelector}
 */
public class GetServerTest {

    private static Unmarshaller unmarshaller;
    private static Marshaller marshaller;

    @BeforeClass
    public static void init() throws Exception {
        JAXBContext jaxb = JAXBContext.newInstance(GetServerRequest.class);
        unmarshaller = jaxb.createUnmarshaller();
        marshaller = jaxb.createMarshaller();
    }

    @Test
    public void unmarshallGetServerRequest() throws Exception {
        GetServerRequest result = (GetServerRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("GetServerRequest.xml"));
        ServerSelector svrSel = result.getServer();
        Assert.assertEquals("server - 'by' attribute", ServerSelector.ServerBy.name, svrSel.getBy());
        Assert.assertEquals("server - value", "fun.example.test", svrSel.getKey());
    }

    @Test
    public void marshallGetServerRequest() throws Exception {
        ServerSelector svrSel = new ServerSelector(ServerSelector.ServerBy.name, "fun.example.net");
        GetServerRequest gsr = new GetServerRequest();
        gsr.setServer(svrSel);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.marshal(gsr, out);
        String xml = out.toString("UTF-8");
        Assert.assertTrue("Marshalled XML should end with 'GetServerRequest>'", xml.endsWith("GetServerRequest>"));
    }
}
