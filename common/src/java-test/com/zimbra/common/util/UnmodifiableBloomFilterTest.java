/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;


public class UnmodifiableBloomFilterTest {

    @Rule public TestName testName = new TestName();
    @Rule public MethodRule watchman = new ZTestWatchman();
    protected static UnmodifiableBloomFilter<String> bloomFilter  =  UnmodifiableBloomFilter
        .createFilterFromFile("common/src/java-test/common-passwords.txt");


    @Before
    public void setUp() {
        assertTrue(bloomFilter.isInitialized());
        assertFalse(bloomFilter.isDisabled());
    }

    @Test
    public void testMightContain() {
        assertTrue(bloomFilter.isInitialized());
        assertTrue(bloomFilter.mightContain("test123"));
        assertTrue(bloomFilter.mightContain("hunter2"));
    }

    @Test
    public void testMightContainFalse() {
        assertTrue(bloomFilter.isInitialized());
        assertFalse(bloomFilter.mightContain("not-in-the-test-file"));
    }

    @Test
    public void testCreateFilterFromMissingFile() {
        UnmodifiableBloomFilter<String> missingFileFilter = UnmodifiableBloomFilter
            .createFilterFromFile("src/java-test/fake-file-not-found");
        // expect to immediately initialize
        assertTrue(missingFileFilter.isInitialized());
        assertTrue(missingFileFilter.isDisabled());
        assertFalse(missingFileFilter.mightContain("test123"));
    }

    @Test
    public void testCreateFilterFromEmptySpecifiedFile() {
        UnmodifiableBloomFilter<String> noFileFilter = UnmodifiableBloomFilter
            .createFilterFromFile("");
        // expect to immediately consider empty file as initialized
        assertTrue(noFileFilter.isInitialized());
        assertTrue(noFileFilter.isDisabled());
        assertFalse(noFileFilter.mightContain("test123"));
    }

    @Test
    public void testCreateFilterFromNullSpecifiedFile() {
        UnmodifiableBloomFilter<String> noFileFilter = UnmodifiableBloomFilter
            .createFilterFromFile(null);
        // expect to immediately consider null file as initialized
        assertTrue(noFileFilter.isInitialized());
        assertTrue(noFileFilter.isDisabled());
        assertFalse(noFileFilter.mightContain("test123"));
    }

    @Test
    public void testMightContainLazyLoad() {
        UnmodifiableBloomFilter<String> lazyFilter = UnmodifiableBloomFilter
            .createLazyFilterFromFile("common/src/java-test/common-passwords.txt");
        // expect to initialize on demand
        assertFalse(lazyFilter.isInitialized());
        assertFalse(lazyFilter.isDisabled());
        assertTrue(lazyFilter.mightContain("test123"));
        assertTrue(lazyFilter.mightContain("hunter2"));
        assertTrue(lazyFilter.isInitialized());
        assertFalse(lazyFilter.isDisabled());
    }

    @Test
    public void testCreateLazyFilterFromMissingFile() {
        UnmodifiableBloomFilter<String> missingFileFilter = UnmodifiableBloomFilter
            .createLazyFilterFromFile("src/java-test/fake-file-not-found");
        // expect to initialize on demand
        assertFalse(missingFileFilter.isInitialized());
        assertFalse(missingFileFilter.mightContain("test123"));
        assertTrue(missingFileFilter.isInitialized());
        // file not found results in disabled instance
        assertTrue(missingFileFilter.isDisabled());
    }

    @Test
    public void testCreateLazyFilterFromEmptySpecifiedFile() {
        UnmodifiableBloomFilter<String> noFileFilter = UnmodifiableBloomFilter
            .createLazyFilterFromFile("");
        // expect to immediately consider empty file as initialized
        assertTrue(noFileFilter.isInitialized());
        assertTrue(noFileFilter.isDisabled());
        assertFalse(noFileFilter.mightContain("test123"));
    }

    @Test
    public void testCreateLazyFilterFromNullSpecifiedFile() {
        UnmodifiableBloomFilter<String> noFileFilter = UnmodifiableBloomFilter
            .createLazyFilterFromFile(null);
        // expect to immediately consider null file as initialized
        assertTrue(noFileFilter.isInitialized());
        assertTrue(noFileFilter.isDisabled());
        assertFalse(noFileFilter.mightContain("test123"));
    }

}
