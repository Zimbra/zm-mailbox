/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.cs.account.AttributeManager;


public class TestBuildInfo  {

    @Test
    public void testInVersion() throws Exception {
        AttributeManager am = AttributeManager.getInstance();

        assertTrue(am.inVersion("zimbraId", "0"));
        assertTrue(am.inVersion("zimbraId", "5.0.10"));

        assertFalse(am.inVersion("zimbraZimletDomainAvailableZimlets", "5.0.9"));
        assertTrue(am.inVersion("zimbraZimletDomainAvailableZimlets", "5.0.10"));
        assertTrue(am.inVersion("zimbraZimletDomainAvailableZimlets", "5.0.11"));
        assertTrue(am.inVersion("zimbraZimletDomainAvailableZimlets", "5.5"));
        assertTrue(am.inVersion("zimbraZimletDomainAvailableZimlets", "6"));
    }

}
