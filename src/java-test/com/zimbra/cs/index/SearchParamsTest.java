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
package com.zimbra.cs.index;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link SearchParams}.
 *
 * @author ysasaki
 */
public final class SearchParamsTest {

    @Test
    public void parseLocale() {
        Assert.assertEquals(new Locale("da"), SearchParams.parseLocale("da"));
        Assert.assertEquals(new Locale("da", "DK"), SearchParams.parseLocale("da_DK"));
        Assert.assertEquals(new Locale("en"), SearchParams.parseLocale("en"));
        Assert.assertEquals(new Locale("en", "US", "MAC"), SearchParams.parseLocale("en_US-MAC"));
    }

}
