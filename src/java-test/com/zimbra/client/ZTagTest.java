/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.client;

import junit.framework.Assert;

import org.junit.Test;


public class ZTagTest {

    @Test
    public void testColor() throws Exception {
        // 4451821 is equivalent long value for cyan
        ZTag.Color color = ZTag.Color.fromString("4451821");
        Assert.assertEquals(color.name(), "cyan");
        color = ZTag.Color.fromString("blue");
        Assert.assertEquals(color.name(), "blue");
        color = ZTag.Color.fromString("0x5b9bf2");
        Assert.assertEquals(color.name(), "orange");
    }
}
