package com.zimbra.cs.html;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Test;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import com.zimbra.common.soap.XmlParseException;
import com.zimbra.common.util.Constants;

import junit.framework.Assert;

public class XHtmlDefangTest {
    @Test
    public void testMakeSAXParserFactory() throws XmlParseException, SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException {
        SAXParserFactory sf = XHtmlDefang.makeSAXParserFactory();
        Assert.assertTrue(sf.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING));
        Assert.assertTrue(sf.getFeature(Constants.DISALLOW_DOCTYPE_DECL));
        Assert.assertFalse(sf.getFeature(Constants.EXTERNAL_GENERAL_ENTITIES));
        Assert.assertFalse(sf.getFeature(Constants.EXTERNAL_PARAMETER_ENTITIES));
        Assert.assertFalse(sf.getFeature(Constants.LOAD_EXTERNAL_DTD));
    }

}
