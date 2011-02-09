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
package com.zimbra.soap.account;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import junit.framework.Assert;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import org.junit.BeforeClass;
import org.junit.Test;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.GetInfoResponse;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.JSONElement;

/**
 * Unit test for {@link GetInfoResponse} which exercises
 * translation to and from Element
 *
 * @author Gren Elliot
 */
public class JaxbToElementTest {
    private static final Logger LOG = Logger.getLogger(JaxbToElementTest.class);
    private static Unmarshaller unmarshaller;
    // one run with iterationNum = 80000:
    //     elementToJaxbTest time="30.013" (using w3c dom document)
    //     elementToJaxbUsingDom4jTest time="41.165"
    //     elementToJaxbUsingByteArrayTest time="122.265"
    private static int iterationNum = 2;
    static GetInfoResponse getInfoResp;
    static String getInfoResponseXml;
    static String getInfoResponseJSON;
    static String getInfoResponseJSONwithEnv;
    static Element getInfoRespElem;

    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        LOG.setLevel(Level.INFO);
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n");
        }
        is.close();
        return sb.toString();
    }

    @BeforeClass
    public static void init() throws Exception {
        JAXBContext jaxb = JAXBContext.newInstance(GetInfoResponse.class);
        unmarshaller = jaxb.createUnmarshaller();
        getInfoResp = (GetInfoResponse) unmarshaller.unmarshal(
            JaxbToElementTest.class.getResourceAsStream("GetInfoResponse.xml"));
        InputStream is = JaxbToElementTest.class.getResourceAsStream(
                "GetInfoResponse.xml");
        getInfoResponseXml = convertStreamToString(is);
        is = JaxbToElementTest.class.getResourceAsStream(
                "GetInfoResponse.json");
        getInfoResponseJSON = convertStreamToString(is);
        StringBuffer sb = new StringBuffer();
        sb.append("{\n\"GetInfoResponse\": ").append(getInfoResponseJSON).append("\n}");
        getInfoResponseJSONwithEnv = sb.toString();
        getInfoRespElem = JaxbUtil.jaxbToElement(getInfoResp);
    }

    @Test
    public void jaxBToElementTest() throws Exception {
        for (int cnt = 1; cnt <= iterationNum;cnt++) {
            Element el = JaxbUtil.jaxbToElement(getInfoResp);
            String actual = el.prettyPrint();
            // TODO: At present the order varies a little and zimlets plus
            //       other things are missing - so just check the first part.
            Assert.assertEquals(getInfoResponseXml.substring(0, 100),
                    actual.substring(0, 100));
        }
    }

    @Test
    public void jaxBToJSONElementTest() throws Exception {
            Element el = JaxbUtil.jaxbToElement(
                    getInfoResp, JSONElement.mFactory);
            // el.toString() and el.prettyPrint() don't provide the
            // name of the element - that only happens when it is a
            // child of other elements (the "soap" envelop)
            String actual = el.prettyPrint();
            Assert.assertEquals("Top level Element name", "GetInfoResponse", el.getName());
            // The test file has one extra line ending. 
            Assert.assertEquals(getInfoResponseJSON.substring(0, 27390),
                    actual);
    }

    @Test
    public void elementToJaxbTest() throws Exception {
        Element el = JaxbUtil.jaxbToElement(getInfoResp);
        org.w3c.dom.Document doc = el.toW3cDom();
        if (LOG.isDebugEnabled())
            LOG.debug("(XML)elementToJaxbTest toW3cDom() Xml:\n" +
                    JaxbUtil.domToString(doc));
        for (int cnt = 1; cnt <= iterationNum;cnt++) {
            getInfoResp = JaxbUtil.elementToJaxb(getInfoRespElem);
        }
    }

    @Test
    public void elementToJaxbUsingDom4jTest() throws Exception {
        for (int cnt = 1; cnt <= iterationNum;cnt++) {
            getInfoResp = JaxbUtil.elementToJaxbUsingDom4j(getInfoRespElem);
        }
    }

    @Test
    public void elementToJaxbUsingByteArrayTest() throws Exception {
        for (int cnt = 1; cnt <= iterationNum;cnt++) {
            getInfoResp = JaxbUtil.elementToJaxbUsingByteArray(getInfoRespElem);
        }
    }

    @Test
    public void JSONelementToJaxbTest() throws Exception {
        Element env = Element.parseJSON(getInfoResponseJSONwithEnv);
        Element el = env.listElements().get(0);
        org.w3c.dom.Document doc = el.toW3cDom();
        if (LOG.isDebugEnabled())
            LOG.debug("JSONelementToJaxbTest toW3cDom Xml:\n" +
                    JaxbUtil.domToString(doc));
        getInfoResp = JaxbUtil.elementToJaxb(el);
        Assert.assertEquals("Account name", "user1@ysasaki.local",
             getInfoResp.getAccountName());
    }

    // This seems to work fine, although similar code in JAXB enabled
    // ExportContacts server-side does not - get:
    // javax.xml.bind.UnmarshalException: Namespace URIs and local names
    //      to the unmarshaller needs to be interned.
    @Test
    public void JSONelementToJaxbUsingDom4jTest() throws Exception {
        for (int cnt = 1; cnt <= 4;cnt++) {
            Element env = Element.parseJSON(getInfoResponseJSONwithEnv);
            Element el = env.listElements().get(0);
            getInfoResp = JaxbUtil.elementToJaxbUsingDom4j(el);
            Assert.assertEquals("Account name", "user1@ysasaki.local",
                 getInfoResp.getAccountName());
        }
    }
}
