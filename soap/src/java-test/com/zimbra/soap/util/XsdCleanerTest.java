package com.zimbra.soap.util;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;

import org.junit.Test;

import com.zimbra.common.util.Constants;

import junit.framework.Assert;

public class XsdCleanerTest {
    @Test
    public void testMakeDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = XsdCleaner.makeDocumentBuilderFactory();
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
    public void testMakeTransformerFactory() {
        TransformerFactory factory = XsdCleaner.makeTransformerFactory();
        Assert.assertEquals("", factory.getAttribute(XMLConstants.ACCESS_EXTERNAL_DTD));
        Assert.assertEquals("", factory.getAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET));
    }
}