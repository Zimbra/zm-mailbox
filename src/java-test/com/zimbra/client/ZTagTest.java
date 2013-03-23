/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 VMware, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
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
