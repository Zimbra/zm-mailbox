/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
        Assert.assertTrue("Marshalled XML should contain '</GetServerRequest>'", xml.indexOf("</GetServerRequest>") > 0);
    }
}
