/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 VMware, Inc.
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
package com.zimbra.common.soap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.python.google.common.collect.Lists;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.dom4j.DocumentException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element.ElementFactory;
import com.zimbra.common.soap.Element.XMLElement;

/**
 */
public class ElementTest {

    @Rule
    public TestName testName = new TestName();

    private static final Logger LOG = Logger.getLogger(ElementTest.class);
    private static final int maxiter = 50000;
    private static final int maxiterInThreads = 1000;
    private static final int numThreads = 30;
    private static final ByteArrayInputStream getInfoRespBais = fileToBais("GetInfoResponseSOAP.xml");
    private static final ByteArrayInputStream getInfoReqBais = fileToBais("GetInfoRequestSOAP.xml");

    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        LOG.setLevel(Level.INFO);
    }

    private void logInfo(String format, Object ... objects) {
        if (LOG.isInfoEnabled()) {
            LOG.info("[[[  " + testName.getMethodName() + "  ]]] :" + String.format(format, objects));
        }
    }

    private void logNewAndLegacyElements(Element elem, Element legacyElem) {
        String newString = elem.toString();
        String legacyString = legacyElem.toString();
        if ((newString != null) && newString.equals(legacyString)) {
            return;
        }
        logInfo("\n\nElement toString() value:\n%1$s\n\nLegacy Element toString() value:\n%2$s",
                newString, legacyString);
    }

    @Test
    public void prettyPrintSafeXml() {
        prettyPrintSafe(new Element.XMLElement("dummy"));
    }

    @Test
    public void prettyPrintSafeJson() {
        prettyPrintSafe(new Element.JSONElement("dummy"));
    }

    private void prettyPrintSafe(Element element) {
        element.addElement("password").addText("secret");
        element.addElement("pfxPassword").addText("secret");
        element.addElement("a").addAttribute("n", "pfxPassword").addText("secret");
        element.addElement("a").addAttribute("n", "hostPwd").addText("secret");
        element.addElement("a").addAttribute("n", "webexZimlet_pwd1").addText("secret");
        element.addElement("dummy2").
                addAttribute("password", "secret").
                addAttribute("pass", "secret").
                addAttribute("pwd", "secret");
        element.addElement("prop").addAttribute("name", "passwd").addText("secret");
        String elementStr = element.prettyPrint(true);
        Assert.assertFalse("Sensitive values have not been masked\n" + elementStr, elementStr.contains("secret"));
    }

    @Test
    public void getPathElementList() {
        Element e = XMLElement.mFactory.createElement("parent");
        e.addElement("child");
        Assert.assertEquals(1, e.getPathElementList(new String[] { "child" } ).size());
        Assert.assertEquals(0, e.getPathElementList(new String[] { "bogus" } ).size());
    }

    private static final String xmlContainingMixed = "<a><b foo=\"bar\">doo<c/>wop</b></a>";
    @Test
    public void flatten() throws Exception {
        Element a = Element.parseXML(xmlContainingMixed);
        Assert.assertEquals("toplevel is <a>", "a", a.getName());
        Assert.assertEquals("<a> has no attrs", 0, a.listAttributes().size());
        Assert.assertEquals("<a> has 1 child", 1, a.listElements().size());

        Element b = a.listElements().get(0);
        Assert.assertEquals("child is <b>", "b", b.getName());
        Assert.assertEquals("<b> has 1 attr", 1, b.listAttributes().size());
        Assert.assertEquals("<b> attr foo=bar", "bar", b.getAttribute("foo"));
        Assert.assertEquals("<b> has no children", 0, b.listElements().size());
        Assert.assertEquals("<b>'s contents are flattened", "doo<c/>wop", b.getText());
    }

    /** Cache one DocumentFactory per thread to avoid unnecessarily recreating
     *  them for every XML parse. */
    private static final ThreadLocal<org.dom4j.DocumentFactory> mDocumentFactory =
            new ThreadLocal<org.dom4j.DocumentFactory>() {
        @Override
        protected synchronized org.dom4j.DocumentFactory initialValue() {
            return new org.dom4j.DocumentFactory();
        }
    };

    /**
     * Used to be {@code Element.parseXML (InputStream is, ElementFactory factory)}
     */
    public static Element parseXMLusingDom4j(InputStream is, ElementFactory factory)
    throws org.dom4j.DocumentException {
        return Element.convertDOM(Element.getSAXReader(mDocumentFactory.get()).read(is).getRootElement(), factory);
    }

    private static final String testXml =
            "<xml xmlns=\"urn:zimbra\">\n<a fred=\"woof y&lt;6\"></a>\n<b/><b/>\n<text>R &amp; B</text></xml>";
    @Test
    public void parseTestXml() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(testXml.getBytes());
        Element legacyElem = parseXMLusingDom4j(bais, Element.XMLElement.mFactory);
        bais.reset();
        Element elem = Element.parseXML(bais);
        logNewAndLegacyElements(elem, legacyElem);
        Assert.assertEquals("toString value unchanged", legacyElem.toString(), elem.toString());
    }

    private static final String nsTestXml =
            "<xml xmlns=\"urn:zimbra\" xmlns:admin=\"urn:zimbraAdmin\">\n<e attr=\"aVal\">text</e>\n<admin:b/>\n</xml>";
    @Test
    public void parseNamespaceTestXml() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(nsTestXml.getBytes());
        Element legacyElem = parseXMLusingDom4j(bais, Element.XMLElement.mFactory);
        bais.reset();
        Element elem = Element.parseXML(bais);
        logNewAndLegacyElements(elem, legacyElem);
        Assert.assertEquals("toString value unchanged", legacyElem.toString(), elem.toString());
    }

    private static final String brokenXml = "<xml xmlns=\"urn:zimbra\">\n<a fred=\"woof\"></a>\n<b/>\n</xmlbroken>";
    @Test
    public void parseBrokenXmlUsingJAXP() {
        ByteArrayInputStream bais = new ByteArrayInputStream(brokenXml.getBytes());
        try {
            Element.parseXML(bais);
            Assert.fail("XmlParseException should have been thrown");
        } catch (Exception e) {
            // Expecting XmlParseException with mesage something like :
            //     "parse error: Fatal Error: Problem on line 4 of document :
            //         The end-tag for element type \"xml\" must end with a '>' delimiter.",
            Assert.assertTrue("XmlParseException should have been thrown", e instanceof XmlParseException); 
        }
    }

    /**
     * Validate that the new {@code Element.parseXML} produces the same Element structure as the old one
     */
    @Test
    public void getInfoRequestSOAP()
    throws DocumentException, XmlParseException {
        getInfoReqBais.reset();
        Element legacyElem = parseXMLusingDom4j(getInfoReqBais, Element.XMLElement.mFactory);
        getInfoReqBais.reset();
        Element elem = Element.parseXML(getInfoReqBais);
        logNewAndLegacyElements(elem, legacyElem);
        Assert.assertEquals("toString value unchanged", legacyElem.toString(), elem.toString());
    }

    /**
     * Validate that the new {@code Element.parseXML} produces the same Element structure as the old one
     */
    @Test
    public void getInfoResponseSOAP()
    throws DocumentException, XmlParseException {
        getInfoRespBais.reset();
        Element legacyElem = parseXMLusingDom4j(getInfoRespBais, Element.XMLElement.mFactory);
        getInfoRespBais.reset();
        Element elem = Element.parseXML(getInfoRespBais);
        logNewAndLegacyElements(elem, legacyElem);
        Assert.assertEquals("toString value unchanged", legacyElem.toString(), elem.toString());
    }

    @Test
    public void parseTestXmlFromFile()
    throws URISyntaxException, DocumentException, FileNotFoundException, ServiceException {
        getInfoRespBais.reset();
        Element legacyElem = parseXMLusingDom4j(getInfoRespBais, Element.XMLElement.mFactory);
        URL xmlUrl = ElementTest.class.getResource("GetInfoResponseSOAP.xml");
        File xmlFile = new File(xmlUrl.toURI());
        Element elem = W3cDomUtil.parseXML(xmlFile);
        logNewAndLegacyElements(elem, legacyElem);
        Assert.assertEquals("toString value unchanged", legacyElem.toString(), elem.toString());
    }

    /**
     * Validate that the new {@code Element.parseXML} produces the same Element structure as the old one
     * when the input text is XHTML
     */
    @Test
    public void xhtmlToElement()
    throws DocumentException, XmlParseException {
        ByteArrayInputStream bais = toBais(ElementTest.class.getResourceAsStream("xhtml.html"));
        Element legacyElem = parseXMLusingDom4j(bais, Element.XMLElement.mFactory);
        bais.reset();
        Element elem = Element.parseXML(bais);
        logNewAndLegacyElements(elem, legacyElem);

        Assert.assertEquals("top node name", "html", elem.getName());
        Assert.assertEquals("Number of sub elements", 0, elem.listElements().size());

        Assert.assertEquals("Legacy top node name", "html", legacyElem.getName());
        Assert.assertEquals("Legacy Number of sub elements", 0, legacyElem.listElements().size());
        // Disabling for Helix.  New style is same as Both are in IronMaiden but old style
        // differs in white space handling - assuming IronMaiden fix not present.
        // Assert.assertEquals("toString value", legacyElem.toString(), elem.toString());
    }

    /**
     * Validate that the new {@code Element.parseXML} produces the same Element structure as the old one
     * when the input text contains a sub-element which is xhtml and that in turn contains CDATA
     *
     * Note: Original test data contained attributes xml:lang="en" and lang="en" but the new code and
     * the old code swapped the order of the attributes.  This should not make any effective difference
     * and DOM does not make any guarantees with regard to attribute order, so xml:lang attribute removed to keep test
     * for success simple.
     */
    @Test
    public void wrappedXhtmlWithCdata()
    throws DocumentException, XmlParseException {
        ByteArrayInputStream bais = toBais(ElementTest.class.getResourceAsStream("wrappedXhtml.xml"));
        Element elem = Element.parseXML(bais);
        logInfo("\n\nElement toString() value:\n%1$s", elem.toString());

        Assert.assertEquals("top node name", "xml", elem.getName());
        Assert.assertEquals("Number of sub elements", 2, elem.listElements().size());
        Assert.assertEquals("Number of 'fun' sub elements", 1, elem.listElements("fun").size());
        Assert.assertEquals("Number of 'wrapper' sub elements", 1, elem.listElements("wrapper").size());
        Element wrapperElem = elem.listElements("wrapper").get(0);
        Assert.assertEquals("Number of sub elements for wrapper", 1, wrapperElem.listElements().size());
        // top level element of xhtml is an Element
        Element htmlElem = wrapperElem.listElements("html").get(0);
        // Zero sub-elements - i.e. all children flattened into the text
        Assert.assertEquals("Number of sub elements for html", 0, htmlElem.listElements().size());
    }

    /**
     * Validate that the new {@code Element.parseXML} produces the same Element structure as the old one
     * when the input text contains a sub-element which is xhtml and that in turn contains CDATA
     *
     * Note: Original test data contained attributes xml:lang="en" and lang="en" but the new code and
     * the old code swapped the order of the attributes.  This should not make any effective difference
     * and DOM does not make any guarantees with regard to attribute order, so xml:lang attribute removed to keep test
     * for success simple.
     */
    // Enable for comparison @Test
    public void legacyWrappedXhtmlWithCdata()
    throws DocumentException {
        ByteArrayInputStream bais = toBais(ElementTest.class.getResourceAsStream("wrappedXhtml.xml"));
        Element legacyElem = parseXMLusingDom4j(bais, Element.XMLElement.mFactory);
        Assert.assertEquals("Legacy top node name", "xml", legacyElem.getName());
        Assert.assertEquals("Legacy Number of sub elements", 2, legacyElem.listElements().size());
        Assert.assertEquals("Legacy Number of 'fun' sub elements", 1, legacyElem.listElements("fun").size());
        Assert.assertEquals("Legacy Number of 'wrapper' sub elements", 1, legacyElem.listElements("wrapper").size());
        Element lWrapperElem = legacyElem.listElements("wrapper").get(0);
        // Zero sub-elements - i.e. all children flattened into the text - Compare with newer code which goes
        // one level deeper
        Assert.assertEquals("Legacy Number of sub elements for wrapper", 0, lWrapperElem.listElements().size());
    }

    private static final String wrappedXhtmlSingleElem = "<xml>\n" +
                                "<fun/>\n" +
                                "<wrapper>\n" +
                                "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\">" +
                                "<body></body>" +
                                "</html>\n" +
                                "</wrapper>\n" +
                                "<work/>\n" +
                                "</xml>\n";

    /**
     * input text contains xhtml which only has one sub-element - i.e. it isn't mixed text
     */
    @Test
    public void wrappedXhtmlWithSingleElem()
    throws DocumentException, XmlParseException {
        logInfo("Source TEXT:\n%1$s", wrappedXhtmlSingleElem);
        ByteArrayInputStream bais = new ByteArrayInputStream(wrappedXhtmlSingleElem.getBytes());
        Element legacyElem = parseXMLusingDom4j(bais, Element.XMLElement.mFactory);
        bais.reset();
        Element elem = Element.parseXML(bais);
        logNewAndLegacyElements(elem, legacyElem);

        Assert.assertEquals("top node name", "xml", elem.getName());
        Assert.assertEquals("Number of sub elements", 3, elem.listElements().size());
        Assert.assertEquals("Number of 'fun' sub elements", 1, elem.listElements("fun").size());
        Assert.assertEquals("Number of 'wrapper' sub elements", 1, elem.listElements("wrapper").size());
        Assert.assertEquals("Number of 'work' sub elements", 1, elem.listElements("work").size());
        Element wrapperElem = elem.listElements("wrapper").get(0);
        Assert.assertEquals("Number of sub elements for wrapper", 1, wrapperElem.listElements().size());
        // top level element of xhtml is an Element
        Element htmlElem = wrapperElem.listElements("html").get(0);
        // Zero sub-elements - i.e. all children flattened into the text
        Assert.assertEquals("Number of sub elements for html", 0, htmlElem.listElements().size());
    }

    /**
     * Old behavior when the input text contains xhtml which only has one sub-element - i.e. it isn't mixed text
     */
    // Enable for comparison @Test
    public void legacyWrappedXhtmlWithSingleElem()
    throws DocumentException {
        logInfo("Source TEXT:\n%1$s", wrappedXhtmlSingleElem);
        ByteArrayInputStream bais = new ByteArrayInputStream(wrappedXhtmlSingleElem.getBytes());
        Element legacyElem = parseXMLusingDom4j(bais, Element.XMLElement.mFactory);

        Assert.assertEquals("top node name", "xml", legacyElem.getName());
        Assert.assertEquals("Number of sub elements", 3, legacyElem.listElements().size());
        Assert.assertEquals("Number of 'fun' sub elements", 1, legacyElem.listElements("fun").size());
        Assert.assertEquals("Number of 'wrapper' sub elements", 1, legacyElem.listElements("wrapper").size());
        Assert.assertEquals("Number of 'work' sub elements", 1, legacyElem.listElements("work").size());
        Element lWrapperElem = legacyElem.listElements("wrapper").get(0);
        // Zero sub-elements - i.e. all children flattened into the text - Compare with newer code which goes
        // one level deeper
        Assert.assertEquals("Number of sub elements for wrapper", 0, lWrapperElem.listElements().size());
    }

    private static final String wrappedXhtmlTextContentOnly = "<xml>\n" +
                                "<fun/>\n" +
                                "<wrapper>\n" +
                                "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\">" +
                                "Text only" +
                                "</html>\n" +
                                "</wrapper>\n" +
                                "</xml>\n";

    /**
     * Validate that the new {@code Element.parseXML} produces the same Element structure as the old one
     * when the input text contains xhtml which only contains text
     */
    @Test
    public void wrappedXhtmlWithTextContentOnly()
    throws DocumentException, XmlParseException {
        logInfo("Source TEXT:\n%1$s", wrappedXhtmlTextContentOnly);
        ByteArrayInputStream bais = new ByteArrayInputStream(wrappedXhtmlTextContentOnly.getBytes());
        Element legacyElem = parseXMLusingDom4j(bais, Element.XMLElement.mFactory);
        bais.reset();
        Element elem = Element.parseXML(bais);
        logNewAndLegacyElements(elem, legacyElem);
        Assert.assertEquals("toString value unchanged", legacyElem.toString(), elem.toString());

        Assert.assertEquals("top node name", "xml", elem.getName());
        Assert.assertEquals("Number of sub elements", 2, elem.listElements().size());
        Assert.assertEquals("Number of 'fun' sub elements", 1, elem.listElements("fun").size());
        Assert.assertEquals("Number of 'wrapper' sub elements", 1, elem.listElements("wrapper").size());
        Element wrapperElem = elem.listElements("wrapper").get(0);
        // XHTML contains only text, so we treat it as an element
        Assert.assertEquals("Number of sub elements for wrapper", 1, wrapperElem.listElements().size());

        Assert.assertEquals("top node name", "xml", legacyElem.getName());
        Assert.assertEquals("Number of sub elements", 2, legacyElem.listElements().size());
        Assert.assertEquals("Number of 'fun' sub elements", 1, legacyElem.listElements("fun").size());
        Assert.assertEquals("Number of 'wrapper' sub elements", 1, legacyElem.listElements("wrapper").size());
        Element lWrapperElem = legacyElem.listElements("wrapper").get(0);
        // XHTML contains only text, so we treat it as an element
        Assert.assertEquals("Number of sub elements for wrapper", 1, lWrapperElem.listElements().size());
    }

    /**
     * Validate entity references are not expanded. (security issue)
     */
    @Test
    public void parseXmlWithEntityReference()
    throws XmlParseException {
        ByteArrayInputStream bais = toBais(ElementTest.class.getResourceAsStream("entityRef.xml"));
        Element elem = Element.parseXML(bais);
        // Expect :    <root>&lt;i/>text</root>
        logInfo("       Element value:\n%1$s", elem.toString());
        Assert.assertEquals("root elem name", "root", elem.getName());
//        Assert.assertEquals("root elem content", "<i/>text", elem.getText());  // this is the case if entity ref expansion was allowed
        Assert.assertEquals("root elem content", "", elem.getText());
    }

    /**
     * Validate entity references are not evaluated. (security issue)
     */
    @Test
    public void parseXmlWithEntityReferenceToNonExistentFile() {
        ByteArrayInputStream bais = toBais(ElementTest.class.getResourceAsStream("refNonExistentFileInEntity.xml"));
        try {
            Element elem = Element.parseXML(bais);
            logInfo("       Element value:\n%1$s", elem.toString());
        } catch (XmlParseException e) {
            // Before fix to Bug 79719 would get an error like:
            //    parse error: /tmp/not/there/non-existent.xml (No such file or directory)
            LOG.info("Exception thrown from parseXML", e);
            Assert.fail("Unexpected exception thrown.  Shouldn't have looked for non-existent file err=" +
                        e.getMessage());
        }
    }

    /**
     * Validate that entity expansion is not taken to extreme. (denial of service security issue)
     */
    @Test
    public void parseXmlWithExcessiveEntityExpansion() {
        ByteArrayInputStream bais = toBais(ElementTest.class.getResourceAsStream("recursiveEntity.xml"));
        try {
            Element.parseXML(bais);
            Assert.fail("Should have failed as there are too many recursive entity expansions");
        } catch (XmlParseException e) {
            // Should be an error like:
            //    parse error: Fatal Error: Problem on line 19 of document : The parser has encountered more than
            //    "100,000" entity expansions in this document; this is the limit imposed by the application.
        }
    }

    private static final String xmlCdata = "<xml>\n" +
            "<cdatawrap><![CDATA[if (a>b) a++;\n]]>\n</cdatawrap>\n" +
            "<elem/>\n" +
            "</xml>";

    /**
     * Validate that the new {@code Element.parseXML} produces the same Element structure as the old one
     * when the input text contains CDATA
     */
    @Test
    public void cdata()
    throws DocumentException, XmlParseException {
        ByteArrayInputStream bais = new ByteArrayInputStream(xmlCdata.getBytes());
        Element legacyElem = parseXMLusingDom4j(bais, Element.XMLElement.mFactory);
        bais.reset();
        Element elem = Element.parseXML(bais);
        logNewAndLegacyElements(elem, legacyElem);
        Assert.assertEquals("toString value unchanged", legacyElem.toString(), elem.toString());
    }

    private static ByteArrayInputStream fileToBais(String resFilename) {
        InputStream is = ElementTest.class.getResourceAsStream(resFilename);
        if (is == null) {
            Assert.fail("Unable to process resource " + resFilename);
        }
        return toBais(is);
    }

    /** Ensure that we can reset the input stream */
    private static ByteArrayInputStream toBais(InputStream is) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int cnt;
        byte[] data = new byte[2048];
        try {
            while ((cnt = is.read(data, 0, data.length)) != -1) {
              buffer.write(data, 0, cnt);
            }
            buffer.flush();
        } catch (IOException e) {
            return null;
        }
        return new ByteArrayInputStream(buffer.toByteArray());
    }

    public class ExerciseNewParse extends Thread {
       private ByteArrayInputStream bais;

       public ExerciseNewParse(ByteArrayInputStream bais) {
          this.bais = bais;
       }

       @Override
       public void run() {
            try {
                iter_NEW(bais, maxiterInThreads);
            } catch (XmlParseException e) {
                Assert.fail("Unexpected failure - " + e.getMessage());
            }
       }
    }

    public class ExerciseDom4JParse extends Thread {
       private ByteArrayInputStream bais;

       public ExerciseDom4JParse(ByteArrayInputStream bais) {
          this.bais = bais;
       }

       @Override
       public void run() {
            try {
                iter_OLD(bais, maxiterInThreads);
            } catch (DocumentException e) {
                Assert.fail("Unexpected failure - " + e.getMessage());
            }
       }
    }

    public void iter_OLD(ByteArrayInputStream bais, int maxiter) throws DocumentException {
        for (int i = 0;i< maxiter; i++) {
            bais.reset();
            parseXMLusingDom4j(bais, Element.XMLElement.mFactory);
        }
    }

    public void iter_NEW(ByteArrayInputStream bais, int maxiter) throws XmlParseException {
        for (int i = 0;i< maxiter; i++) {
            bais.reset();
            Element.parseXML(bais);
        }
    }

    public void iter_ThreadedDom4jParse(ByteArrayInputStream bais) {
        List<ExerciseDom4JParse> threads = Lists.newArrayList();
        for (int i = 0;i< numThreads; i++) {
            threads.add(new ExerciseDom4JParse(bais));
        }
        for (ExerciseDom4JParse thread : threads) {
            thread.run();
        }
    }

    public void iter_ThreadedNewParse(ByteArrayInputStream bais) {
        List<ExerciseNewParse> threads = Lists.newArrayList();
        for (int i = 0;i< numThreads; i++) {
            threads.add(new ExerciseNewParse(bais));
        }
        for (ExerciseNewParse thread : threads) {
            thread.run();
        }
    }

    /*   Speed tests */
    // Enable for performance comparison @Test
    public void iterNamespaceTest_OLD() throws Exception {
        iter_OLD(new ByteArrayInputStream(nsTestXml.getBytes()), maxiter);
    }

    // Enable for performance comparison @Test
    public void iterNamespaceTest_NEW() throws Exception {
        iter_NEW(new ByteArrayInputStream(nsTestXml.getBytes()), maxiter);
    }

    // Enable for performance comparison @Test
    public void iterGetInfoRequestSOAP_OLD() throws DocumentException {
        iter_OLD(getInfoReqBais, maxiter);
    }

    // Enable for performance comparison @Test
    public void iterGetInfoRequestSOAP_NEW() throws XmlParseException {
        iter_NEW(getInfoReqBais, maxiter);
    }

    // Enable for performance comparison @Test
    public void iterGetInfoResponseSOAP_OLD() throws DocumentException {
        iter_OLD(getInfoRespBais, maxiter);
    }

    // Enable for performance comparison @Test
    public void iterGetInfoResponseSOAP_NEW() throws XmlParseException {
        iter_NEW(getInfoRespBais, maxiter);
    }

    // Enable for performance comparison @Test
    public void iterThreadedGetInfoResponseSOAP_OLD() throws XmlParseException {
        iter_ThreadedDom4jParse(getInfoRespBais);
    }

    // Enable for performance comparison @Test
    public void iterThreadedGetInfoResponseSOAP_NEW() throws XmlParseException {
        iter_ThreadedNewParse(getInfoRespBais);
    }

    // Enable for performance comparison @Test
    public void iterThreadedGetInfoRequestSOAP_OLD() throws XmlParseException {
        iter_ThreadedDom4jParse(getInfoReqBais);
    }

    // Enable for performance comparison @Test
    public void iterThreadedGetInfoRequestSOAP_NEW() throws XmlParseException {
        iter_ThreadedNewParse(getInfoReqBais);
    }
}
