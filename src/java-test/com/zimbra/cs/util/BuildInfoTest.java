/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.util;

import org.junit.Assert;
import org.junit.Test;

public class BuildInfoTest {

    @Test
    public void compare() throws Exception {
        Assert.assertEquals(1,
                BuildInfo.Version.compare("5.0.10", "5.0.9"));
        Assert.assertEquals(0,
                BuildInfo.Version.compare("5.0.10", "5.0.10"));
        Assert.assertEquals(-9,
                BuildInfo.Version.compare("5.0", "5.0.9"));
        Assert.assertEquals(2,
                BuildInfo.Version.compare("5.0.10_RC1", "5.0.10_BETA3"));
        Assert.assertEquals(1,
                BuildInfo.Version.compare("5.0.10_GA", "5.0.10_RC2"));
        Assert.assertEquals(1,
                BuildInfo.Version.compare("5.0.10", "5.0.10_RC2"));
    }

}
