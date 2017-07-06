package com.zimbra.common.soap;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;

import org.dom4j.io.SAXReader;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import com.zimbra.common.util.Constants;

import junit.framework.Assert;

public class W3cDomUtilTest {
    @Test
    public void testMakeDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = W3cDomUtil.makeDocumentBuilderFactory();
        Assert.assertTrue(dbf.isNamespaceAware());
        Assert.assertTrue(dbf.isIgnoringComments());
        Assert.assertFalse(dbf.isXIncludeAware());
        Assert.assertFalse(dbf.isExpandEntityReferences());
        Assert.assertTrue(dbf.getFeature(Constants.DISALLOW_DOCTYPE_DECL));
        Assert.assertFalse(dbf.getFeature(Constants.EXTERNAL_GENERAL_ENTITIES));
        Assert.assertFalse(dbf.getFeature(Constants.EXTERNAL_PARAMETER_ENTITIES));
        Assert.assertFalse(dbf.getFeature(Constants.LOAD_EXTERNAL_DTD));
    }

    @Test
    public void testMakeSAXParserFactory() throws XmlParseException, SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException {
        SAXParserFactory sf = W3cDomUtil.makeSAXParserFactory();
        Assert.assertTrue(sf.isNamespaceAware());
        Assert.assertFalse(sf.isXIncludeAware());
        Assert.assertFalse(sf.isValidating());
        Assert.assertTrue(sf.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING));
        Assert.assertTrue(sf.getFeature(Constants.DISALLOW_DOCTYPE_DECL));
        Assert.assertFalse(sf.getFeature(Constants.EXTERNAL_GENERAL_ENTITIES));
        Assert.assertFalse(sf.getFeature(Constants.EXTERNAL_PARAMETER_ENTITIES));
        Assert.assertFalse(sf.getFeature(Constants.LOAD_EXTERNAL_DTD));
    }

    @Test
    public void testGetDom4jSAXReaderWhichUsesSecureProcessing() throws XmlParseException, SAXException {
        SAXReader reader = W3cDomUtil.getDom4jSAXReaderWhichUsesSecureProcessing();
        Assert.assertTrue(reader.getXMLReader().getFeature(Constants.DISALLOW_DOCTYPE_DECL));
        Assert.assertFalse(reader.getXMLReader().getFeature(Constants.EXTERNAL_GENERAL_ENTITIES));
        Assert.assertFalse(reader.getXMLReader().getFeature(Constants.EXTERNAL_PARAMETER_ENTITIES));
    }

    @Test
    public void testMakeTransformerFactory() {
        TransformerFactory factory = W3cDomUtil.makeTransformerFactory();
        Assert.assertEquals("", factory.getAttribute(XMLConstants.ACCESS_EXTERNAL_DTD));
        Assert.assertEquals("", factory.getAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET));
    }
}