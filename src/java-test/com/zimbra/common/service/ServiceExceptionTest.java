/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
