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

public class PairTest {

    @Test
    public void testEquals() {
        Assert.assertEquals(new Pair<String,String>("foo", "bar"), new Pair<String,String>("foo", "bar"));
        Assert.assertEquals(new Pair<String,String>("foo", null) , new Pair<String,String>("fo" + 'o', null));
    }
    
    @Test
    public void testNotEquals() {
        Assert.assertFalse(new Pair<String,String>(null, "bar").equals(new Pair<String,String>(null, "foo")));
        Assert.assertFalse(new Pair<String,String>("foo", "bar").equals(new Pair<String,Integer>("foo", 8)));
        Assert.assertFalse(new Pair<String,String>(null, "bar").equals(new Pair<Integer,String>(0, "bar")));
    }
}
