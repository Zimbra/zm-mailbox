/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WebClientLogoffUrlRegistryTest {

    @Before
    public void setUp() {
        WebClientLogoffUrlRegistry.clearForTest();
    }

    @After
    public void tearDown() {
        WebClientLogoffUrlRegistry.clearForTest();
    }

    @Test
    public void buildWithNoRegisteredPathReturnsOnlyLC() {
        String result = WebClientLogoffUrlRegistry.build("https://mail.acme.com/service/extension/samllogout");
        assertFalse(result.contains("mail.acme.com"));
    }

    @Test
    public void buildWithRegisteredPathMatchIncludesUrl() {
        WebClientLogoffUrlRegistry.register("/service/extension/samllogout");

        String result = WebClientLogoffUrlRegistry.build("https://mail.acme.com/service/extension/samllogout");
        assertTrue(result.contains("https://mail.acme.com/service/extension/samllogout"));
    }

    @Test
    public void buildWithRegisteredPathNoMatchExcludesUrl() {
        WebClientLogoffUrlRegistry.register("/service/extension/samllogout");

        String result = WebClientLogoffUrlRegistry.build("https://corporate.acme.com/goodbye.html");
        assertFalse(result.contains("corporate.acme.com"));
    }

    @Test
    public void buildWithNullUrlDoesNotThrow() {
        WebClientLogoffUrlRegistry.register("/service/extension/samllogout");

        String result = WebClientLogoffUrlRegistry.build(null);
        assertNotNull(result);
    }

    @Test
    public void buildWithEmptyUrlDoesNotThrow() {
        WebClientLogoffUrlRegistry.register("/service/extension/samllogout");

        String result = WebClientLogoffUrlRegistry.build("");
        assertNotNull(result);
    }

    @Test
    public void buildWithMalformedUrlDoesNotThrow() {
        WebClientLogoffUrlRegistry.register("/service/extension/samllogout");

        String result = WebClientLogoffUrlRegistry.build("not a valid url %%%");
        assertNotNull(result);
        assertFalse(result.contains("not a valid url"));
    }

    @Test
    public void buildWithMultipleRegisteredPaths() {
        WebClientLogoffUrlRegistry.register("/service/extension/samllogout");
        WebClientLogoffUrlRegistry.register("/service/extension/oidclogout");

        String result1 = WebClientLogoffUrlRegistry.build("https://mail.acme.com/service/extension/samllogout");
        assertTrue(result1.contains("samllogout"));

        String result2 = WebClientLogoffUrlRegistry.build("https://mail.acme.com/service/extension/oidclogout");
        assertTrue(result2.contains("oidclogout"));
    }

    @Test
    public void buildWithDifferentHostnamesSamePathAllMatch() {
        WebClientLogoffUrlRegistry.register("/service/extension/samllogout");

        String result1 = WebClientLogoffUrlRegistry.build("https://domain1.com/service/extension/samllogout");
        assertTrue(result1.contains("domain1.com"));

        String result2 = WebClientLogoffUrlRegistry.build("https://domain2.com/service/extension/samllogout");
        assertTrue(result2.contains("domain2.com"));
    }

    @Test
    public void registerNullPathDoesNotThrow() {
        WebClientLogoffUrlRegistry.register(null);
        String result = WebClientLogoffUrlRegistry.build("https://mail.acme.com/service/extension/samllogout");
        assertNotNull(result);
    }
}
