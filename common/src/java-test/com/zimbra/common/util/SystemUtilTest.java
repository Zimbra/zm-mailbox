/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.util;

import org.junit.Assert;
import org.junit.Test;

public class SystemUtilTest {

    @Test
    public void coalesce() {
        Assert.assertEquals(1, (int) SystemUtil.coalesce(null, 1));
        Assert.assertEquals(null, SystemUtil.coalesce(null, null, null, null));
        Assert.assertEquals(2, (int) SystemUtil.coalesce(2, 3));
        Assert.assertEquals("good", SystemUtil.coalesce("good", "bad", "ugly"));
    }
}
