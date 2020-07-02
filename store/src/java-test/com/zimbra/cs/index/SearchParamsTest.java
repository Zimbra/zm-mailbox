/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016, 2018 Synacor, Inc.
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
    
    
    public void testIsSortByReadFlag() {
        Assert.assertFalse(SearchParams.isSortByReadFlag(SortBy.DATE_ASC));
        Assert.assertFalse(SearchParams.isSortByReadFlag(SortBy.DATE_DESC));
        Assert.assertTrue(SearchParams.isSortByReadFlag(SortBy.READ_ASC));
        Assert.assertTrue(SearchParams.isSortByReadFlag(SortBy.READ_DESC));
    }

}
