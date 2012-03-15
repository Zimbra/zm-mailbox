/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.soap.mail;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.zimbra.soap.mail.message.SendMsgRequest;
import com.zimbra.soap.mail.type.Msg;
import com.zimbra.soap.mail.type.MsgToSend;

/**
 * Unit test for {@link SendMsgRequest}.
 *
 * @author ysasaki
 */
public final class SendMsgRequestTest {

    private static Marshaller marshaller;

    @BeforeClass
    public static void init() throws Exception {
        JAXBContext jaxb = JAXBContext.newInstance(SendMsgRequest.class);
        marshaller = jaxb.createMarshaller();
    }

    @Test
    public void marshal() throws Exception {
        SendMsgRequest req = new SendMsgRequest();
        MsgToSend msg = new MsgToSend();
        msg.setHeaders(ImmutableList.of(
                new Msg.Header("name1", "value1"),
                new Msg.Header("name2", "value2")));
        req.setMsg(msg);
        StringWriter writer = new StringWriter();
        marshaller.marshal(req, writer);
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<SendMsgRequest xmlns=\"urn:zimbraMail\">" +
                "<m>" +
                    "<header name=\"name1\">value1</header>" +
                    "<header name=\"name2\">value2</header>" +
                "</m>" +
            "</SendMsgRequest>";
        Assert.assertEquals(xml, writer.toString());
    }

}
