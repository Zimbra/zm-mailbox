/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util;

import junit.framework.Assert;

import org.junit.Test;

public class TripleTest {

    @Test
    public void testEquals() {
        Assert.assertEquals(new Triple<String,String,String>("foo", "bar", "baz"), new Triple<String,String,String>("foo", "bar", "baz"));
        Assert.assertEquals(new Triple<String,String,String>("foo", null, "baz") , new Triple<String,String,String>("fo" + 'o', null, "baz"));
    }

    @Test
    public void testNotEquals() {
        Assert.assertFalse(new Triple<String,String,String>(null, "bar", "baz").equals(new Triple<String,String,String>(null, "foo", "bar")));
        Assert.assertFalse(new Triple<String,String,String>("foo", "bar", "baz").equals(new Triple<String,String,Integer>("foo", "bar", 8)));
        Assert.assertFalse(new Triple<String,String,String>(null, "bar", "baz").equals(new Triple<Integer,String,String>(0, "bar", "baz")));
    }
}
