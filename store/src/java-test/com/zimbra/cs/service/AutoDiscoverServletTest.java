/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.service;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.util.Constants;


/**
 * @author zimbra
 *
 */
public class AutoDiscoverServletTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * Test method for {@link com.zimbra.cs.service.AutoDiscoverServlet#isEwsClient(java.lang.String)}.
     */
    @Test
    public void testIsEwsClient() {
        // Active Sync
        boolean result = AutoDiscoverServlet.isEwsClient("http://schemas.microsoft.com/exchange/autodiscover/mobilesync/responseschema/2006");
        Assert.assertTrue(!result);

        // Ews CLient
        result = AutoDiscoverServlet.isEwsClient("http://schemas.microsoft.com/exchange/autodiscover/outlook/responseschema/2006a");
        Assert.assertTrue(result);
    }

    @Test
    public void testMakeDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = AutoDiscoverServlet.makeDocumentBuilderFactory();
        Assert.assertTrue(dbf.isNamespaceAware());
        Assert.assertTrue(dbf.getFeature(Constants.DISALLOW_DOCTYPE_DECL));
        Assert.assertFalse(dbf.getFeature(Constants.EXTERNAL_GENERAL_ENTITIES));
        Assert.assertFalse(dbf.getFeature(Constants.EXTERNAL_PARAMETER_ENTITIES));
        Assert.assertFalse(dbf.getFeature(Constants.LOAD_EXTERNAL_DTD));
    }

    @Test
    public void testMakeTransformerFactory() {
        TransformerFactory factory = AutoDiscoverServlet.makeTransformerFactory();
        Assert.assertEquals("", factory.getAttribute(XMLConstants.ACCESS_EXTERNAL_DTD));
        Assert.assertEquals("", factory.getAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET));
    }
}
