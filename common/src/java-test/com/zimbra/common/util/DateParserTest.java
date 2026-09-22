
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

import java.util.Date;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies DateParser does not throw when timezone is null (covers the NPE fix).
 */
public class DateParserTest {

    @Test
    public void testFormatWithNullTimezone() {
        DateParser dp = new DateParser("yyyyMMdd");
        dp.setTimezone(null); // previously could cause NPE
        String out = dp.format(new Date());
        assertNotNull("Formatted string should not be null", out);
    }

    @Test
    public void testParseWithNullTimezone() {
        DateParser dp = new DateParser("yyyyMMdd");
        dp.setTimezone(null); // previously could cause NPE
        // parse a valid date string for the pattern
        Date d = dp.parse("20200101");
        assertNotNull("Parsed date should not be null", d);
    }
}