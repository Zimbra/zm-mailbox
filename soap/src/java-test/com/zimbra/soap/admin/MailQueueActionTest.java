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
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.soap.admin.message.MailQueueActionRequest;
import com.zimbra.soap.admin.type.MailQueueAction;
import com.zimbra.soap.admin.type.MailQueueWithAction;
import com.zimbra.soap.admin.type.QueueQuery;
import com.zimbra.soap.admin.type.QueueQueryField;
import com.zimbra.soap.admin.type.ServerWithQueueAction;
import com.zimbra.soap.admin.type.ValueAttrib;

/**
 * Unit test for {@link MailQueueActionRequest}.
 * Mostly checking the handling of @XmlMixed in {@link MailQueueAction}
 * test xml files are hand generated and are not taken from real world data.
 */
public class MailQueueActionTest {

    private static final Logger LOG = LogManager.getLogger(MailQueueActionTest.class);
    
    private static Unmarshaller unmarshaller;
    private static Marshaller marshaller;

    static {
        Configurator.reconfigure();
        Configurator.setRootLevel(Level.INFO);
        Configurator.setLevel(LOG.getName(), Level.INFO);
    }

    @BeforeClass
    public static void init() throws Exception {
        JAXBContext jaxb = JAXBContext.newInstance(MailQueueActionRequest.class);
        unmarshaller = jaxb.createUnmarshaller();
        marshaller = jaxb.createMarshaller();
    }

    @Test
    public void unmarshallMailQueueActionRequestByIdTest()
    throws Exception {
        InputStream is = getClass().getResourceAsStream("MailQueueActionRequestIds.xml");
        MailQueueActionRequest result = (MailQueueActionRequest) unmarshaller.unmarshal(is);
        ServerWithQueueAction svr = result.getServer();
        Assert.assertEquals("server - 'name' attribute", "fun.example.test", svr.getName());
        MailQueueWithAction q = svr.getQueue();
        Assert.assertEquals("queue - 'name' attribute", "queueName", q.getName());
        MailQueueAction action = q.getAction();
        Assert.assertEquals("action - 'op' attribute", MailQueueAction.QueueAction.requeue, action.getOp());
        Assert.assertEquals("action - 'by' attribute", MailQueueAction.QueueActionBy.id, action.getBy());
        Assert.assertEquals("action - content", "id1,id2,id3", action.getIds());
        Assert.assertNull("action - query", action.getQuery());
    }

    @Test
    public void unmarshallMailQueueActionRequestByQueryTest()
    throws Exception {
        InputStream is = getClass().getResourceAsStream("MailQueueActionRequestQuery.xml");
        MailQueueActionRequest result = (MailQueueActionRequest) unmarshaller.unmarshal(is);
        ServerWithQueueAction svr = result.getServer();
        Assert.assertEquals("server - 'name' attribute", "fun.example.test", svr.getName());
        MailQueueWithAction q = svr.getQueue();
        Assert.assertEquals("queue - 'name' attribute", "queueName", q.getName());
        MailQueueAction action = q.getAction();
        Assert.assertEquals("action - 'op' attribute", MailQueueAction.QueueAction.requeue, action.getOp());
        Assert.assertEquals("action - 'by' attribute", MailQueueAction.QueueActionBy.query, action.getBy());
        QueueQuery query = action.getQuery();
        Assert.assertNotNull("action - query", query);
        Assert.assertNull("query - limit", query.getLimit());
        Assert.assertNull("query - offset", query.getOffset());
        QueueQueryField qqf = query.getFields().get(0);
        Assert.assertNotNull("field", qqf);
        Assert.assertEquals("field - 'name' attribute", "fieldName", qqf.getName());
        ValueAttrib match0 = qqf.getMatches().get(0);
        Assert.assertEquals("match - 'value' attribute", "matchValue", match0.getValue());

        Assert.assertNull("action - content", action.getIds());
    }

    @Test
    public void marshallMailQueueActionRequestByIdTest() throws Exception {
        MailQueueAction action = new MailQueueAction(MailQueueAction.QueueAction.requeue,
                MailQueueAction.QueueActionBy.id, "id1,id2,id3", null);
        MailQueueWithAction mqwa = new MailQueueWithAction("queueName", action);
        ServerWithQueueAction server = new ServerWithQueueAction("fun.example.test", mqwa);
        MailQueueActionRequest gsr = new MailQueueActionRequest(server);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.marshal(gsr, out);
        String xml = out.toString("UTF-8");
        if (LOG.isInfoEnabled())
            LOG.info("Xml:\n" + xml);
        Assert.assertTrue("Marshalled XML should end with 'MailQueueActionRequest>'",
                xml.endsWith("MailQueueActionRequest>"));
    }

    @Test
    public void marshallMailQueueActionRequestByQueryTest() throws Exception {
        QueueQuery qq = new QueueQuery(null, null);
        QueueQueryField qqf = new QueueQueryField("fieldName");
        qqf.addMatch(new ValueAttrib("matchValue"));
        qq.addField(qqf);
        MailQueueAction action = new MailQueueAction(MailQueueAction.QueueAction.requeue,
                MailQueueAction.QueueActionBy.id, null, qq);
        MailQueueWithAction mqwa = new MailQueueWithAction("queueName", action);
        ServerWithQueueAction server = new ServerWithQueueAction("fun.example.test", mqwa);
        MailQueueActionRequest gsr = new MailQueueActionRequest(server);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.marshal(gsr, out);
        String xml = out.toString("UTF-8");
        if (LOG.isInfoEnabled())
            LOG.info("Xml:\n" + xml);
        Assert.assertTrue("Marshalled XML should end with 'MailQueueActionRequest>'",
                xml.endsWith("MailQueueActionRequest>"));
    }
}
