/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

package com.zimbra.cs.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


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

}
