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

package com.zimbra.cs.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MemoryUnitUtilTest {

    @Test(expected = NumberFormatException.class)
    public void convertToBytesNumberFormatExceptionForStringWithSpecialCharacters() {
        MemoryUnitUtil memoryUnitUtil = new MemoryUnitUtil();
        memoryUnitUtil.convertToBytes("abc%@");
    }

    @Test(expected = NumberFormatException.class)
    public void convertToBytesNullThrowingNumberFormatException() {
        MemoryUnitUtil memoryUnitUtil = new MemoryUnitUtil();
        memoryUnitUtil.convertToBytes("null");
    }

    @Test(expected = NumberFormatException.class)
    public void convertToBytes1TBThrowingNumberFormatException() {
        MemoryUnitUtil memoryUnitUtil = new MemoryUnitUtil();
        memoryUnitUtil.convertToBytes("1TB");
    }

    @Test
    public void validMemoryTest() {
        assertTrue(MemoryUnitUtil.isMemoryUnit("512MB"));
    }

    @Test
    public void invalidMemoryTest() {
        assertFalse(MemoryUnitUtil.isMemoryUnit("12GH"));
        assertFalse(MemoryUnitUtil.isMemoryUnit(""));
        assertFalse(MemoryUnitUtil.isMemoryUnit(null));
        assertFalse(MemoryUnitUtil.isMemoryUnit("ABC@"));
    }

    @Test
    public void constructorTest() {
        MemoryUnitUtil memoryUnitUtil = new MemoryUnitUtil(1024);
        assertEquals(1024, memoryUnitUtil.getMULTIPLIER());
    }

    @Test
    public void convertToBytesTest() {
        MemoryUnitUtil memoryUnitUtil = new MemoryUnitUtil();
        long actual = memoryUnitUtil.convertToBytes("1024KB");
        long actualformemoryPattern = memoryUnitUtil.convertToBytes("10");
        long actualformemoryPatternB = memoryUnitUtil.convertToBytes("10B");
        long actualformemoryPatternb = memoryUnitUtil.convertToBytes("10b");
        long actualformemoryPatternk = memoryUnitUtil.convertToBytes("10k");
        long actualformemoryPattern512KB = memoryUnitUtil.convertToBytes("512KB");
        long actualformemoryPattern20MB = memoryUnitUtil.convertToBytes("20MB");
        long actualformemoryPattern20m = memoryUnitUtil.convertToBytes("20m");
        assertEquals(Long.parseLong("1048576"), actual);
        assertEquals(Long.parseLong("10"), actualformemoryPattern);
        assertEquals(Long.parseLong("10"), actualformemoryPatternB);
        assertEquals(Long.parseLong("10"), actualformemoryPatternb);
        assertEquals(Long.parseLong("10240"), actualformemoryPatternk);
        assertEquals(Long.parseLong("20971520"), actualformemoryPattern20MB);
        assertEquals(Long.parseLong("20971520"), actualformemoryPattern20m);
        assertEquals(Long.parseLong("524288"), actualformemoryPattern512KB);
    }

    @Test
    public void convertToBytesConvertingBIGValueTest() {
        MemoryUnitUtil memoryUnitUtil = new MemoryUnitUtil();
        long actualforHIGHValue1GB = memoryUnitUtil.convertToBytes("1GB");
        assertEquals(Long.parseLong("1073741824"), actualforHIGHValue1GB);
        long actualvalueHIGH1TB = memoryUnitUtil.convertToBytes("1T");
        assertEquals(Long.parseLong("1"), actualvalueHIGH1TB);
    }
}
