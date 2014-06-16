/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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
