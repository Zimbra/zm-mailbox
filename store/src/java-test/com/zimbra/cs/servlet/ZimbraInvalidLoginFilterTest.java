/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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
package com.zimbra.cs.servlet;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(MockitoJUnitRunner.class)
public class ZimbraInvalidLoginFilterTest {

    private ZimbraInvalidLoginFilter zimbraInvalidLoginFilter;

    @Before
    public void setup() {
        zimbraInvalidLoginFilter = new ZimbraInvalidLoginFilter();
    }

    @Test
    public void getFirstExternalIpTest1() throws Exception {
        String origIPs = "193.3.142.123, 193.3.142.124";
        origIPs = Whitebox.invokeMethod(zimbraInvalidLoginFilter, "getFirstExternalIp", origIPs);
        assertEquals(origIPs, "193.3.142.123");
    }

    @Test
    public void getFirstExternalIpTest2() throws Exception {
        String origIPs = "193.3.142.123";
        origIPs = Whitebox.invokeMethod(zimbraInvalidLoginFilter, "getFirstExternalIp", origIPs);
        assertEquals(origIPs, "193.3.142.123");
    }
}
