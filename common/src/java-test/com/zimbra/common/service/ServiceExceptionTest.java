/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.service;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.service.ServiceException.Argument;

public class ServiceExceptionTest {

    @Test
    public void testArgumentEquals() {
        Argument arg1a = new Argument("1", "one", Argument.Type.STR);
        Argument arg1b = new Argument("1", "one", Argument.Type.STR);
        Argument arg1c = new Argument("1", "two", Argument.Type.STR);
        Argument arg2 = new Argument("2", "one", Argument.Type.STR);
        
        Assert.assertFalse(arg1a.equals(null));
        Assert.assertTrue(arg1a.equals(arg1b));
        Assert.assertFalse(arg1a.equals(arg1c));
        Assert.assertFalse(arg1a.equals(arg2));
    }
}
