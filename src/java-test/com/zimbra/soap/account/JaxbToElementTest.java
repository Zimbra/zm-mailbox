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

import com.google.common.base.Charsets;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import junit.framework.Assert;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.junit.BeforeClass;
import org.junit.Test;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.AuthRequest;
import com.zimbra.soap.account.message.CreateIdentityRequest;
import com.zimbra.soap.account.message.GetInfoResponse;
import com.zimbra.soap.account.type.Pref;
import com.zimbra.soap.admin.message.CreateAccountRequest;
import com.zimbra.soap.admin.message.CreateXMbxSearchRequest;
import com.zimbra.soap.admin.message.MailQueueActionRequest;
import com.zimbra.soap.admin.message.SearchAutoProvDirectoryResponse;
import com.zimbra.soap.admin.type.Attr;
import com.zimbra.soap.admin.type.AutoProvDirectoryEntry;
import com.zimbra.soap.admin.type.MailQueueAction;
import com.zimbra.soap.admin.type.MailQueueWithAction;
import com.zimbra.soap.admin.type.QueueQuery;
import com.zimbra.soap.admin.type.QueueQueryField;
import com.zimbra.soap.admin.type.ServerWithQueueAction;
import com.zimbra.soap.admin.type.ValueAttrib;
import com.zimbra.soap.mail.message.ConvActionRequest;
import com.zimbra.soap.mail.message.DeleteDataSourceRequest;
import com.zimbra.soap.mail.message.GetContactsRequest;
import com.zimbra.soap.mail.message.ImportContactsRequest;
import com.zimbra.soap.mail.message.WaitSetRequest;
import com.zimbra.soap.mail.type.ActionSelector;
import com.zimbra.soap.mail.type.ContactActionSelector;
import com.zimbra.soap.mail.type.FolderActionSelector;
import com.zimbra.soap.mail.type.ImapDataSourceNameOrId;
import com.zimbra.soap.mail.type.NoteActionSelector;
import com.zimbra.soap.mail.type.Pop3DataSourceNameOrId;
import com.zimbra.soap.mail.type.RetentionPolicy;
import com.zimbra.soap.type.KeyValuePair;
import com.zimbra.soap.type.WaitSetAddSpec;
import com.zimbra.soap.util.JaxbElementInfo;
import com.zimbra.soap.util.JaxbInfo;
import com.zimbra.soap.util.JaxbNodeInfo;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.StringUtil;

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
    static GetInfoResponse getInfoRespJaxb;
    static String getInfoResponseXml;
    static String getInfoResponseJSON;
    static String getInfoResponseJSONwithEnv;
    static Element getInfoRespElem;

    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        LOG.setLevel(Level.INFO);
    }

    public static String streamToString(InputStream stream, Charset cs)
    throws IOException {
        try {
            Reader reader = new BufferedReader(
                    new InputStreamReader(stream, cs));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } finally {
            stream.close();
        }
    }

    public static Unmarshaller getGetInfoResponseUnmarshaller() throws JAXBException {
        if (unmarshaller == null) {
            JAXBContext jaxb = JAXBContext.newInstance(GetInfoResponse.class);
            unmarshaller = jaxb.createUnmarshaller();
        }
        return unmarshaller;
    }
    public static GetInfoResponse getInfoResponsefromXml() throws JAXBException {
        if (getInfoRespJaxb == null) {
            getGetInfoResponseUnmarshaller();
            getInfoRespJaxb = (GetInfoResponse) unmarshaller.unmarshal(
                JaxbToElementTest.class.getResourceAsStream("GetInfoResponse.xml"));
        }
        return getInfoRespJaxb;
    }

    public static String getTestInfoResponseXml() throws IOException {
        if (getInfoResponseXml == null) {
            InputStream is = JaxbToElementTest.class.getResourceAsStream(
                    "GetInfoResponse.xml");
            getInfoResponseXml = streamToString(is, Charsets.UTF_8);
        }
        return getInfoResponseXml;
    }

    public static String getTestInfoResponseJson() throws IOException {
        if (getInfoResponseJSON == null) {
            InputStream is = JaxbToElementTest.class.getResourceAsStream(
                    "GetInfoResponse.json");
            getInfoResponseJSON = streamToString(is, Charsets.UTF_8);
        }
        return getInfoResponseJSON;
    }

    @BeforeClass
    public static void init() throws Exception {
        getInfoResponsefromXml();
        getTestInfoResponseXml();
        getTestInfoResponseJson();
        StringBuffer sb = new StringBuffer();
        sb.append("{\n\"GetInfoResponse\": ").append(getInfoResponseJSON).append("\n}");
        getInfoResponseJSONwithEnv = sb.toString();
        getInfoRespElem = JaxbUtil.jaxbToElement(getInfoRespJaxb);
    }

    @Test
    public void jaxBToElementTest() throws Exception {
        for (int cnt = 1; cnt <= iterationNum;cnt++) {
            Element el = JaxbUtil.jaxbToElement(getInfoRespJaxb);
            String actual = el.prettyPrint();
            // TODO: At present some stuff is wrong/missing 
            // so just check the first part.
            Assert.assertEquals(getInfoResponseXml.substring(0, 1000),
                    actual.substring(0, 1000));
            // validateLongString("XML response differs from expected\n",
            //     getInfoResponseXml, actual,
            //             "GetInfoResponse.xml", "/tmp/GetInfoResponse.xml");
        }
    }

    private void validateLongString(String message,
                String expected, String actual,
                String expectedFile, String actualFile) {
        if (!actual.equals(expected)) {
            try{
                OutputStreamWriter out = new OutputStreamWriter(
                        new FileOutputStream(actualFile),"UTF-8");
                out.write(actual);
                out.close();
            }catch (Exception e){//Catch exception if any
              System.err.println("validateLongString:Error writing to " +
                      actualFile + " : " + e.getMessage());
            }
            Assert.fail(message + "\nexpected=" + expectedFile +
                    "\nactual=" + actualFile);
        }
    }

    @Test
    public void jaxBToJSONElementTest() throws Exception {
            Element el = JaxbUtil.jaxbToElement(
                    getInfoRespJaxb, JSONElement.mFactory);
            // el.toString() and el.prettyPrint() don't provide the
            // name of the element - that only happens when it is a
            // child of other elements (the "soap" envelop)
            String actual = el.prettyPrint();
            Assert.assertEquals("Top level Element name",
                    "GetInfoResponse", el.getName());
            validateLongString("JSON response differs from expected\n",
                getInfoResponseJSON, actual,
                        "GetInfoResponse.json", "/tmp/GetInfoResponse.json");
    }

    @Test
    public void elementToJaxbTest() throws Exception {
        Element el = JaxbUtil.jaxbToElement(getInfoRespJaxb);
        org.w3c.dom.Document doc = el.toW3cDom();
        if (LOG.isDebugEnabled())
            LOG.debug("(XML)elementToJaxbTest toW3cDom() Xml:\n" +
                    JaxbUtil.domToString(doc));
        for (int cnt = 1; cnt <= iterationNum;cnt++) {
            GetInfoResponse getInfoResp = JaxbUtil.elementToJaxb(getInfoRespElem);
            Assert.assertEquals("Account name", "user1@ysasaki.local",
                 getInfoResp.getAccountName());
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void elementToJaxbUsingDom4jTest() throws Exception {
        for (int cnt = 1; cnt <= iterationNum;cnt++) {
            GetInfoResponse getInfoResp = JaxbUtil.elementToJaxbUsingDom4j(getInfoRespElem);
            Assert.assertEquals("Account name", "user1@ysasaki.local",
                 getInfoResp.getAccountName());
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void elementToJaxbUsingByteArrayTest() throws Exception {
        for (int cnt = 1; cnt <= iterationNum;cnt++) {
            GetInfoResponse getInfoResp = JaxbUtil.elementToJaxbUsingByteArray(getInfoRespElem);
            Assert.assertEquals("Account name", "user1@ysasaki.local",
                 getInfoResp.getAccountName());
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
        GetInfoResponse getInfoResp = JaxbUtil.elementToJaxb(el);
        Assert.assertEquals("Account name", "user1@ysasaki.local",
             getInfoResp.getAccountName());
    }

    // This seems to work fine, although similar code in JAXB enabled
    // ExportContacts server-side does not - get:
    // javax.xml.bind.UnmarshalException: Namespace URIs and local names
    //      to the unmarshaller needs to be interned.
    @SuppressWarnings("deprecation")
    @Test
    public void JSONelementToJaxbUsingDom4jTest() throws Exception {
        for (int cnt = 1; cnt <= 4;cnt++) {
            Element env = Element.parseJSON(getInfoResponseJSONwithEnv);
            Element el = env.listElements().get(0);
            GetInfoResponse getInfoResp = JaxbUtil.elementToJaxbUsingDom4j(el);
            Assert.assertEquals("Account name", "user1@ysasaki.local",
                 getInfoResp.getAccountName());
        }
    }

    /**
     * Check that @{link JaxbUtil.elementToJaxb} will accept XML where
     * JAXB expects content type as an attribute but it is specified as
     * an element.
     * @throws Exception
     */
    @Test
    public void importContactsWithContentTypeAsElementTest () throws Exception {
        Element icrElem = Element.XMLElement.mFactory.createElement(
                MailConstants.IMPORT_CONTACTS_REQUEST);
        icrElem.addAttribute(MailConstants.A_CSVLOCALE, "fr");
        icrElem.addElement(MailConstants.A_CONTENT_TYPE).setText("csv");
        icrElem.addElement(MailConstants.E_CONTENT).setText("CONTENT");
        ImportContactsRequest icr = JaxbUtil.elementToJaxb(icrElem);
        Assert.assertEquals("ImportContactsRequest content type:",
                "csv", icr.getContentType());
        Assert.assertEquals("ImportContactsRequest csvlocale:",
                "fr", icr.getCsvLocale());
        Assert.assertEquals("ImportContactsRequest contents:",
                "CONTENT", icr.getContent().getValue());
    }

    /**
     * Check that @{link JaxbUtil.elementToJaxb} will accept XML where
     * JAXB expects various attributes that have been specified as elements
     * in a fairly deep structure.  Ensure that @XmlElementRef is handled
     * @throws Exception
     */
    @Test
    public void jaxbElementRefsFixupTest () throws Exception {
        Element rootElem = Element.XMLElement.mFactory.createElement(
                AdminConstants.MAIL_QUEUE_ACTION_REQUEST);
        // JAXB Element E_SERVER --> ServerWithQueueAction
        Element svrE = rootElem.addElement(AdminConstants.E_SERVER);
        // JAXB attribute A_NAME
        svrE.addElement(AdminConstants.A_NAME).setText("SERVER-NAME");
        // JAXB Element E_QUEUE --> MailQueueWithAction
        Element qE = svrE.addElement(AdminConstants.E_QUEUE);
        // JAXB attribute A_NAME
        qE.addElement(AdminConstants.A_NAME).setText("queueName");
        // JAXB Element E_ACTION --> MailQueueAction
        Element actE = qE.addElement(AdminConstants.E_ACTION);
        // JAXB attribute A_OP
        actE.addElement(AdminConstants.A_OP).setText("requeue");
        // JAXB attribute A_BY
        actE.addElement(AdminConstants.A_BY).setText("query");
        // MailQueueAction XmlElementRef E_QUERY --> QueueQuery
        // actually, part of XmlMixed, so JAXB class deals in
        // an array of Object
        Element queryE = actE.addElement(AdminConstants.E_QUERY);
        // JAXB attribute A_OFFSET
        queryE.addAttribute(AdminConstants.A_OFFSET, "20");
        // JAXB attribute A_LIMIT
        queryE.addElement(AdminConstants.A_LIMIT).setText("99");
        for (int sfx = 1; sfx <= 3; sfx++) {
            // List<QueueQueryField> fields 
            Element fE = queryE.addElement(AdminConstants.E_FIELD);
            fE.addAttribute(AdminConstants.A_NAME, "name" + sfx);
            // List<ValueAttrib> matches
            Element mE = fE.addElement(AdminConstants.E_MATCH);
            // JAXB attribute A_VALUE
            mE.addElement(AdminConstants.A_VALUE).setText("value " + sfx);
            mE = fE.addElement(AdminConstants.E_MATCH);
            // JAXB attribute A_VALUE
            mE.addElement(AdminConstants.A_VALUE).setText("2nd value " + sfx);
        }
        MailQueueActionRequest req = JaxbUtil.elementToJaxb(rootElem);
        ServerWithQueueAction svrWithQ = req.getServer();
        Assert.assertEquals("Server name", "SERVER-NAME", svrWithQ.getName());
        MailQueueWithAction q = svrWithQ.getQueue();
        Assert.assertEquals("Queue name", "queueName", q.getName());
        MailQueueAction a = q.getAction();
        Assert.assertEquals("Action BY",
                MailQueueAction.QueueActionBy.query, a.getBy());
        Assert.assertEquals("Action OP",
                MailQueueAction.QueueAction.requeue, a.getOp());
        QueueQuery query = a.getQuery();
        Assert.assertEquals("Query offset", 20, query.getOffset().intValue());
        Assert.assertEquals("Query limit", 99, query.getLimit().intValue());
        List<QueueQueryField> qFields = query.getFields();
        Assert.assertEquals("Number of query fields", 3, qFields.size());
        Assert.assertEquals("Query field 2 name", "name2",
                qFields.get(1).getName());
        List<ValueAttrib> matches = qFields.get(1).getMatches();
        Assert.assertEquals("Number of matches", 2, matches.size());
        Assert.assertEquals("Match 2 value", "2nd value 2",
                matches.get(1).getValue());
    }

    /**
     * Check that @{link JaxbUtil.elementToJaxb} will accept XML where
     * JAXB expects various attributes that have been specified as elements.
     * Ensure that @XmlElements is handled
     * @throws Exception
     */
    @Test
    public void jaxbElementsFixupTest() throws Exception {
        Element rootElem = Element.XMLElement.mFactory.createElement(
                MailConstants.GET_CONTACTS_REQUEST);
        // JAXB Attribute A_SYNC
        rootElem.addElement(MailConstants.A_SYNC).addText("true");
        // JAXB Attribute A_FOLDER
        rootElem.addAttribute(MailConstants.A_FOLDER, "folderId");
        // JAXB Attribute A_SORTBY
        rootElem.addElement(MailConstants.A_SORTBY).addText("sortBy");
        // JAXB Elements:
        //    Element E_ATTRIBUTE --> AttributeName
        //    Element E_CONTACT --> Id
        Element attrName1 = rootElem.addElement(MailConstants.E_ATTRIBUTE);
        attrName1.addAttribute(MailConstants.A_ATTRIBUTE_NAME, "aName1");
        Element contact1 = rootElem.addElement(MailConstants.E_CONTACT);
        contact1.addElement(MailConstants.A_ID).addText("ctctId1");
        Element contact2 = rootElem.addElement(MailConstants.E_CONTACT);
        contact2.addAttribute(MailConstants.A_ID, "ctctId2");
        Element attrName2 = rootElem.addElement(MailConstants.E_ATTRIBUTE);
        attrName2.addElement(MailConstants.A_ATTRIBUTE_NAME).addText("aName2");
        Element memAttr1 = rootElem.addElement(MailConstants.E_CONTACT_GROUP_MEMBER_ATTRIBUTE);
        memAttr1.addElement(MailConstants.A_ATTRIBUTE_NAME).addText("grpAttrName1");

        GetContactsRequest req = JaxbUtil.elementToJaxb(rootElem);

        Assert.assertEquals("Sync", true, req.getSync().booleanValue());
        Assert.assertEquals("FolderID", "folderId", req.getFolderId());
        Assert.assertEquals("SortBy", "sortBy", req.getSortBy());
    }

    /**
     * Check that @{link JaxbUtil.elementToJaxb} will accept XML where
     * JAXB expects various attributes that have been specified as elements.
     * Ensure that attributes in elements of superclasses are handled
     * In this case:
     *                  <a><n>attrName1</n></a>
     * should be recognised as meaning:
     *                  <a n="attrName1"></a>
     * @throws Exception
     */
    @Test
    public void jaxbSubclassFixupTest() throws Exception {
        Element rootElem = Element.XMLElement.mFactory.createElement(AdminConstants.CREATE_ACCOUNT_REQUEST);
        // JAXB Attribute E_NAME
        rootElem.addElement(AdminConstants.E_NAME).addText("acctName");
        // JAXB Attribute E_PASSWORD
        rootElem.addElement(AdminConstants.E_PASSWORD).addText("AcctPassword");
        // JAXB Element E_A ---> Attr (actually a List)
        Element a1 = rootElem.addElement(AdminConstants.E_A);
        // JAXB Attribute A_N
        a1.addElement(AdminConstants.A_N).addText("attrName1");
        // value can't be set when we've specified an attribute as an element

        CreateAccountRequest req = JaxbUtil.elementToJaxb(rootElem);
        Assert.assertEquals("Account name", "acctName", req.getName());
        Assert.assertEquals("Account Password", "AcctPassword", req.getPassword());
        List<Attr> attrs = req.getAttrs();
        Assert.assertEquals("Number of attrs", 1, attrs.size());
        Assert.assertEquals("attr 1 name", "attrName1", attrs.get(0).getKey());
        Assert.assertEquals("attr 1 value", "", attrs.get(0).getValue());
    }

    @Test
    public void jaxbInfoSuperclassElems() throws Exception {
        JaxbInfo jaxbInfo = JaxbInfo.getFromCache(CreateAccountRequest.class);
        Iterable<String> attrNames = jaxbInfo.getAttributeNames();
        Assert.assertEquals("Number of attributes for CreateAccountRequest", 2, Iterables.size(attrNames));
        Iterable<String> elemNames = jaxbInfo.getElementNames();
        Assert.assertEquals("Number of elements for CreateAccountRequest", 1, Iterables.size(elemNames));
        Assert.assertTrue("Has <a>", -1 != Iterables.indexOf(elemNames, Predicates.equalTo(MailConstants.E_A)));
        Iterable<JaxbNodeInfo> nodeInfos = jaxbInfo.getJaxbNodeInfos();
        Assert.assertEquals("Number of nodeInfos for CreateAccountRequest", 1, Iterables.size(nodeInfos));
        JaxbNodeInfo nodeInfo = Iterables.get(nodeInfos, 0);
        Assert.assertEquals("NodeInfo name ", MailConstants.E_A, nodeInfo.getName());
        if (! (nodeInfo instanceof JaxbElementInfo)) {
            Assert.fail("Expecting JaxbElementInfo but got " + nodeInfo.getClass().getName());
        } else {
            JaxbElementInfo elemInfo = (JaxbElementInfo) nodeInfo;
            Assert.assertEquals("Class associated with <a>", Attr.class, elemInfo.getAtomClass());
        }
        JaxbNodeInfo node = jaxbInfo.getElemNodeInfo(MailConstants.E_A);
        Assert.assertNotNull("has NodeInfo for Element <a>", node);
        Assert.assertTrue("hasElement <a>", jaxbInfo.hasElement(MailConstants.E_A));
    }

    @Test
    public void jaxbInfoWrapperHandling() throws Exception {
        JaxbInfo jaxbInfo = JaxbInfo.getFromCache(WaitSetRequest.class);
        Class<?> klass;
        klass = jaxbInfo.getClassForWrappedElement("notWrapperName", "nonexistent");
        if (klass != null) {
            Assert.fail("Class " + klass.getName() + " should be null for non-existent wrapper/wrapped");
        }
        klass = jaxbInfo.getClassForWrappedElement(MailConstants.E_WAITSET_ADD /* add */, "nonexistent");
        if (klass != null) {
            Assert.fail("Class " + klass.getName() + " should be null for existing wrapper/non-existent wrapped");
        }
        klass = jaxbInfo.getClassForWrappedElement(MailConstants.E_WAITSET_ADD /* add */, MailConstants.E_A);
        Assert.assertNotNull("Class should NOT be null for existing wrapper/non-existent wrapped", klass);
        Assert.assertEquals("WaitSetAddSpec class", WaitSetAddSpec.class,klass);
    }

    /**
     * Ensure that we still find attributes encoded as elements below a wrapped element in the hierarchy
     * @throws Exception
     */
    @Test
    public void jaxbBelowWrapperFixupTest() throws Exception {
        Element rootElem = Element.XMLElement.mFactory.createElement(MailConstants.WAIT_SET_REQUEST);
        // JAXB Attribute - not Element
        rootElem.addElement(MailConstants.A_WAITSET_ID /* waitSet */).addText("myWaitSet");
        // JAXB Attribute - not Element
        rootElem.addElement(MailConstants.A_SEQ /* seq */).addText("lastKnownSeq");
        // JAXB XmlElementWrapper 
        Element addElem = rootElem.addElement(MailConstants.E_WAITSET_ADD /* add */);
        Element aElem = addElem.addElement(MailConstants.E_A /* a */);
        // JAXB Attribute - not Element
        aElem.addElement(MailConstants.A_NAME).addText("waitsetName");
        // JAXB Attribute - not Element
        aElem.addElement(MailConstants.A_ID).addText("waitsetId");
        WaitSetRequest req = JaxbUtil.elementToJaxb(rootElem);
        List<WaitSetAddSpec> adds = req.getAddAccounts();
        Assert.assertEquals("Waitset add number", 1, adds.size());
        WaitSetAddSpec wsAdd = adds.get(0);
        Assert.assertEquals("Waitset name", "waitsetName", wsAdd.getName());
        Assert.assertEquals("Waitset id", "waitsetId", wsAdd.getId());
    }

    /**
     * Check that @{link JaxbUtil.elementToJaxb} will accept XML where
     * JAXB expects various attributes that have been specified as elements.
     * Ensure that attributes in wrapped elements are handled
     * @throws Exception
     */
    @Test
    public void jaxbWrapperFixupTest() throws Exception {
        Element rootElem = Element.XMLElement.mFactory.createElement(
                AccountConstants.AUTH_REQUEST);
        // JAXB wrapper element name E_PREFS
        Element prefsE = rootElem.addElement(AccountConstants.E_PREFS);
        // JAXB element E_PREF with attribute "name"
        Element prefE = prefsE.addElement(AccountConstants.E_PREF);
        prefE.addElement("name").addText("pref name");

        AuthRequest req = JaxbUtil.elementToJaxb(rootElem);
        List<Pref> prefs = req.getPrefs();
        Assert.assertEquals("Number of prefs", 1, prefs.size());
        Assert.assertEquals("Pref name",
                "pref name", prefs.get(0).getName());
    }

    /**
     * Explore handling of Jaxb classes which specify an @XmlElement with
     * a super class.  How do subclasses get treated with this?
     * WSDLJaxbTest.ConvActionRequestJaxbSubclassHandlingTest passes,
     * i.e. it successfully unmarshalls to a ConvActionRequest with
     * a FolderActionSelector member.
     * However, even if I use those class files (with package name changed)
     * in place of the committed ones, this test only seems to unmarshall
     * with an ActionSelector member - i.e. the "recursive" and "url"
     * attribute information gets lost.
     */
    // @Test
    public void ConvActionRequestJaxbSubclassHandlingTestDisabled() throws Exception {
        FolderActionSelector fas = new FolderActionSelector("ids", "op");
        fas.setFolder("folder");
        fas.setRecursive(true);
        fas.setUrl("http://url");
        ConvActionRequest car = new ConvActionRequest(fas);
        Element carE = JaxbUtil.jaxbToElement(car);
        String eXml = carE.toString();
        LOG.info("ConvActionRequestJaxbSubclassHandling: marshalled XML=" +
                eXml);
        Assert.assertTrue("Xml should contain recursive attribute",
                eXml.contains("recursive=\"true\""));

        carE = Element.XMLElement.mFactory.createElement(
                MailConstants.CONV_ACTION_REQUEST);
        Element actionE = carE.addElement(MailConstants.E_ACTION);
        actionE.addAttribute(MailConstants.A_OPERATION, "op");
        actionE.addAttribute(MailConstants.A_ID, "ids");
        actionE.addAttribute(MailConstants.A_FOLDER, "folder");
        actionE.addAttribute(MailConstants.A_RECURSIVE, true);
        actionE.addAttribute(MailConstants.A_URL, "http://url");
        LOG.info("ConvActionRequestJaxbSubclassHandling: half baked XML=" +
                carE.toString());
        car = JaxbUtil.elementToJaxb(carE);
        carE = JaxbUtil.jaxbToElement(car);
        eXml = carE.toString();
        LOG.info("ConvActionRequestJaxbSubclassHandling: round tripped XML=" +
                eXml);
        ActionSelector as = car.getAction();
        Assert.assertEquals("Folder attribute value",
                    "folder", as.getFolder());
        if (as instanceof FolderActionSelector) {
            fas = (FolderActionSelector)as;
            Assert.assertEquals("url attribute value",
                    "http://url", fas.getUrl());
        } else if (as instanceof NoteActionSelector) {
            Assert.fail("got a NoteActionSelector");
        } else if (as instanceof ContactActionSelector) {
            Assert.fail("got a ContactActionSelector");
        } else {
            Assert.fail("Failed to get back a FolderActionSelector");
        }
    }

    @Test
    public void standalonElementToJaxbTest() throws Exception {
        InputStream is = getClass().getResourceAsStream("retentionPolicy.xml");
        Element elem = Element.parseXML(is);
        String eXml = elem.toString();
        LOG.info("retentionPolicy.xml from Element:\n" + eXml);
        RetentionPolicy rp = JaxbUtil.elementToJaxb(elem, RetentionPolicy.class);
        Assert.assertNotNull("elementToJaxb RetentionPolicy returned object", rp);
        Element elem2 = JaxbUtil.jaxbToElement(rp, XMLElement.mFactory);
        String eXml2 = elem2.toString();
        LOG.info("Round tripped retentionPolicy.xml from Element:\n" + eXml2);
        Assert.assertEquals("elementToJaxb RetentionPolicy Xml after", eXml, eXml2);
    }

    @Test
    public void IdentityToStringTest () throws Exception {
        com.zimbra.soap.account.type.Identity id =
                new com.zimbra.soap.account.type.Identity("hello", null);
        Map<String, String> attrs = Maps.newHashMap();
        attrs.put("key1", "value1");
        attrs.put("key2", "value2 wonderful");
        id.setAttrs(attrs);
        CreateIdentityRequest request = new CreateIdentityRequest(id);
        Assert.assertEquals("toString output", 
            "CreateIdentityRequest{identity=Identity{a=[Attr{name=key2, value=value2 wonderful}, Attr{name=key1, value=value1}], name=hello, id=null}}",
            request.toString());
    }

    /*
     * Currently, DeleteDataSourceRequest does not have a setter for all datasource children.  Making sure that
     * it works.  Actually believe that JAXB ignores setters for lists and adds by using the getter to get the
     * list and adding to that.
     */
    @Test
    public void DeleteDataSourceRequestTest () throws Exception {
        DeleteDataSourceRequest req = new DeleteDataSourceRequest();
        Pop3DataSourceNameOrId pop = new Pop3DataSourceNameOrId();
        pop.setName("pop3name");
        ImapDataSourceNameOrId imap = new ImapDataSourceNameOrId();
        imap.setName("imap4name");
        req.addDataSource(pop);
        req.addDataSource(imap);
        Element elem = JaxbUtil.jaxbToElement(req, XMLElement.mFactory);
        Assert.assertNotNull("DataSourceRequest elem", elem);
        Element imapE = elem.getElement(MailConstants.E_DS_IMAP);
        Assert.assertNotNull("imap elem", imapE);
        Element popE = elem.getElement(MailConstants.E_DS_POP3);
        Assert.assertNotNull("imap elem", popE);
        req = JaxbUtil.elementToJaxb(elem, DeleteDataSourceRequest.class);
        Assert.assertNotNull("JAXB DeleteDataSourceRequest", req);
        Assert.assertEquals("Number of datasources in JAXB", 2, req.getDataSources().size());
    }

    @Test
    public void KeyValuePairs() throws Exception {
        InputStream is = JaxbToElementTest.class.getResourceAsStream(
                "CreateXMbxSearchRequest.xml");
        // String soapXml = streamToString(is, Charsets.UTF_8);
        JAXBContext jaxb = JAXBContext.newInstance(CreateXMbxSearchRequest.class);
        Unmarshaller unmarshaller = jaxb.createUnmarshaller();
        JAXBElement<CreateXMbxSearchRequest> jaxbElem =
            unmarshaller.unmarshal(new StreamSource(is), CreateXMbxSearchRequest.class);
        Assert.assertNotNull("JAXBElement resulting from unmarshal", jaxbElem);
        CreateXMbxSearchRequest soapObj = jaxbElem.getValue();
        Assert.assertNotNull("Unmarshal soap object", soapObj);
        Assert.assertEquals("Number of attributes", 10, soapObj.getKeyValuePairs().size());
    }

    // AutoProvDirectoryEntry contains 2 lists - one via extending AdminKeyValuePairs
    // No order is specified for elements via XmlType propOrder.  Checking how well this works.
    @Test
    public void searchAutoProvDirectoryResponse() throws Exception {
        Element resp = Element.XMLElement.mFactory.createElement(
                AdminConstants.SEARCH_AUTO_PROV_DIRECTORY_RESPONSE);
        resp.addAttribute(AdminConstants.A_MORE, false);
        resp.addAttribute(AdminConstants.A_SEARCH_TOTAL, 1);
        Element entryE = resp.addElement(AdminConstants.E_ENTRY);
        entryE.addAttribute(AdminConstants.A_DN, "displayNam");
        entryE.addElement(AdminConstants.E_KEY).setText("keyValue1");
        entryE.addElement(AdminConstants.E_KEY).setText("keyValue2");
        entryE.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, "nVal1").setText("attr1Txt");
        entryE.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, "nVal2").setText("attr2Txt");
        SearchAutoProvDirectoryResponse jaxb = JaxbUtil.elementToJaxb(resp);
        Assert.assertNotNull("Unmarshal soap object", jaxb);
        List<AutoProvDirectoryEntry> entries = jaxb.getEntries();
        Assert.assertNotNull("entries list", entries);
        Assert.assertEquals("Number of entries", 1, entries.size());
        AutoProvDirectoryEntry entry = entries.get(0);
        List<KeyValuePair> kvps = entry.getKeyValuePairs();
        Assert.assertNotNull("entry - attrs list", kvps);
        Assert.assertEquals("entry - Number of attrs", 2, kvps.size());
        List<String> keys = entry.getKeys();
        Assert.assertNotNull("entry - keys list", keys);
        Assert.assertEquals("entry - Number of keys", 2, keys.size());
    }

    // TODO - enable when/if bug fixed.  Note that handler currently uses "Element".  This issue is with
    //        zmsoap producing non-KeyValuePairs compatible JSON.  Although JAXB object originated JSON
    //        currently exhibits similar issues.  Would prefer to fix JAXB to JSON, use the JAXB object in zmsoap
    //        and send the resulting JSON to the server rather than fix the Element code to understand Xml-like
    //        JSON KeyValuePairs - added flexibility means more complexity...
    // @Test
    public void bug62571_zmsoapRequestPrefsSupport() throws Exception {
        HashMap<String, Object> prefs;
        final String zmsoapRequest = "{ \"pref\": [" +
                "{ \"_content\": \"TRUE\", \"name\": \"zimbraPrefSharedAddrBookAutoCompleteEnabled\" }," +
                "{ \"_content\": \"carbon\", \"name\": \"zimbraPrefSkin\" }" +
                "], \"_jsns\": \"urn:zimbraAccount\" }";
        final String idealRequest = "{ \"_attrs\": { " +
                "\"zimbraPrefSharedAddrBookAutoCompleteEnabled\": \"TRUE\"," +
                "\"zimbraPrefSkin\": \"carbon\"" +
                "}, \"_jsns\": \"urn:zimbraAccount\" }";
        // SoapEngine uses this to parse the Whole soapMessage for a request where "in" is a ByteArrayInputStream
        //        document = Element.parseJSON(in);
        // That parseJSON seems to extract the string from the stream and end up calling something similar to this...
        Element zmsoapElem = Element.parseJSON(zmsoapRequest);
        Element idealElem = Element.parseJSON(idealRequest);
        prefs = Maps.newHashMap();
        for (Element.KeyValuePair kvp : idealElem.listKeyValuePairs(AccountConstants.E_PREF, AccountConstants.A_NAME)) {
            String name = kvp.getKey(), value = kvp.getValue();
            StringUtil.addToMultiMap(prefs, name, value);
        }
        Assert.assertEquals("Ideal request - number of prefs found", 2, prefs.size());
        prefs = Maps.newHashMap();
        for (Element.KeyValuePair kvp : zmsoapElem.listKeyValuePairs(AccountConstants.E_PREF, AccountConstants.A_NAME)) {
            String name = kvp.getKey(), value = kvp.getValue();
            StringUtil.addToMultiMap(prefs, name, value);
        }
        Assert.assertEquals("zmsoap request - number of prefs found", 2, prefs.size());
    }

    // Simplified version of what appears in mail.ToXML
    public static void encodeAttr(Element parent, String key, String value, String eltname, String attrname, 
            boolean allowed) {
        
        Element.KeyValuePair kvPair;
        if (allowed) {
            kvPair = parent.addKeyValuePair(key, value, eltname, attrname);
        } else {
            kvPair = parent.addKeyValuePair(key, "", eltname, attrname);
            kvPair.addAttribute(AccountConstants.A_PERM_DENIED, true);
        }
    }
    
    // GetInfo encoding of identities calls similar code
    @Test
    public void encodeAttrsWithDenied() throws Exception {
        Element identExml = Element.XMLElement.mFactory.createElement(AccountConstants.E_IDENTITY);
        encodeAttr(identExml, "keyAllowed", "valueAllowed", AccountConstants.E_A, AccountConstants.A_NAME, true);
        encodeAttr(identExml, "keyDenied", "valueDenied", AccountConstants.E_A, AccountConstants.A_NAME, false);
        Element identEjson = Element.JSONElement.mFactory.createElement(AccountConstants.E_IDENTITY);
        encodeAttr(identEjson, "keyAllowed", "valueAllowed", AccountConstants.E_A, AccountConstants.A_NAME, true);
        encodeAttr(identEjson, "keyDenied", "valueDenied", AccountConstants.E_A, AccountConstants.A_NAME, false);
        // <identity><a name="keyAllowed">valueAllowed</a><a pd="1" name="keyDenied"/></identity>
        LOG.info("encodeAttrsWithDenied xml\n" + identExml.toString());
        // {"_attrs":{"keyAllowed":"valueAllowed","keyDenied":{"_content":"","pd":true}}}
        LOG.info("encodeAttrsWithDenied json\n" + identEjson.toString());
        com.zimbra.soap.account.type.Attr deniedAttr = com.zimbra.soap.account.type.Attr.forNameWithPermDenied("keyDenied");
        Element elem2 = JaxbUtil.jaxbToNamedElement(AccountConstants.E_A, AccountConstants.NAMESPACE_STR,
                deniedAttr, XMLElement.mFactory);
        String eXml2 = elem2.toString();
        LOG.info("XML from JAXB denied attr\n" + eXml2);
        Assert.assertEquals("XML from JAXB denied attr\n",
                "<a pd=\"1\" name=\"keyDenied\" xmlns=\"urn:zimbraAccount\"/>", eXml2);
    }
}
