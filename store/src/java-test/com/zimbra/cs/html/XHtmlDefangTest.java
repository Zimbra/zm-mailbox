package com.zimbra.cs.html;

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017, 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;

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

    @Test
    public void testDefang() {
        XHtmlDefang defang = new XHtmlDefang();
        String text = "<?xml version='1.0'\n" +
            "encoding='UTF-8'?><svg xmlns=\"http://www.w3.org/2000/svg\"\n" +
            "onload=\"alert('XSS in the attacchment')\"></svg>";
        StringReader reader = new StringReader(text);

        try {
            String sanitizedText = defang.defang(reader, true);
            Assert.assertTrue("Does not contain onload attribute.", sanitizedText.indexOf("onload") == -1);
        } catch (IOException e) {
            fail("No Exception should be thrown");
        }
    }

}
