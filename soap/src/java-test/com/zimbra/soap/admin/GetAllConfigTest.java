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

import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.KeyValuePair;
import com.zimbra.soap.admin.message.GetAllConfigResponse;
import com.zimbra.soap.admin.type.Attr;

/**
 * Unit test for {@link GetAllConfigResponse}.
 * com.zimbra.soap.admin.WSDLAdminTest.getAllConfigTest currently failing
 * due to what looks like metro issue : http://java.net/jira/browse/JAX_WS-807
 * This test uses a capture of the response which appeared to cause issues
 * to make sure that JAXB unmarshalling is ok.
 */
public class GetAllConfigTest {

    private static final Logger LOG = LogManager.getLogger(GetAllConfigTest.class);

    private static Unmarshaller unmarshaller;

    static {
        Configurator.reconfigure();
        Configurator.setRootLevel(Level.INFO);
        Configurator.setLevel(LOG.getName(), Level.INFO);
    }

    @BeforeClass
    public static void init() throws Exception {
        JAXBContext jaxb = JAXBContext.newInstance(GetAllConfigResponse.class);
        unmarshaller = jaxb.createUnmarshaller();
    }

    @Test
    public void unmarshallGetAllConfigResponseTest()
    throws Exception {
        InputStream is = getClass().getResourceAsStream("GetAllConfigResponse.xml");
        Element elem = Element.parseXML(is);
        List<KeyValuePair> kvps = elem.listKeyValuePairs();
        is.close();
        is = getClass().getResourceAsStream("GetAllConfigResponse.xml");
        GetAllConfigResponse resp = (GetAllConfigResponse) unmarshaller.unmarshal(is);
        Assert.assertNotNull("Response", resp);
        List<Attr> attrs = resp.getAttrs();
        LOG.info("unmarshallGetAllConfigResponseTest:KVPS from elem=" + kvps.size() + " from jaxb=" + attrs.size());
        Assert.assertTrue("Have some attrs", attrs.size() > 20);
        Assert.assertEquals("Number of attrs from elem and from jaxb agree", kvps.size(), attrs.size());
    }
}
