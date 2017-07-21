/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.localconfig;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.python.google.common.base.Strings;

public class LocalConfigTest {

    private static LocalConfig localConfig;

    @BeforeClass
    public static void init() throws Exception {
        if (Strings.isNullOrEmpty(System.getProperty("zimbra.config"))) {
            System.setProperty("zimbra.config", "../store/src/java-test/localconfig-test.xml");
        }
        localConfig = LocalConfig.getInstance();
    }
    
    @Rule
    public TestName TEST_NAME = new TestName();
    
    private String getTestName() {
        return TEST_NAME.getMethodName();
    }
    
    private String keyName(String suffix) {
        return getTestName() + "-" + suffix;
    }
    
    private String get(String key) throws ConfigException {
        return localConfig.get(key);
    }
    
    private String get(KnownKey key) throws ConfigException {
        return get(key.key());
    }
    
    private void assertEquals(String expected, KnownKey key) {
        try {
            String actual = get(key);
            Assert.assertEquals(expected, actual);
        } catch (ConfigException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }
    
    private void assertRecursive(KnownKey key) {
        boolean caught = false;
        try {
            get(key);
        } catch (ConfigException e) {
            Assert.assertTrue(e.getMessage().contains("recursive expansion of key"));
            caught = true;
        }
        Assert.assertTrue(caught);
    }
    
    private void assertNullKey(KnownKey key) {
        boolean caught = false;
        try {
            get(key);
        } catch (ConfigException e) {
            Assert.assertTrue(e.getMessage().contains("null valued key"));
            caught = true;
        }
        Assert.assertTrue(caught);
    }
    
    @Test
    public void multipleSimple() throws Exception {
        KnownKey a = new KnownKey(keyName("a"), String.format("${%s} ${%s}", keyName("b"), keyName("b")));
        KnownKey b = new KnownKey(keyName("b"), "123");
        
        assertEquals("123 123", a);
    }
    
    @Test
    public void multipleDeep() throws Exception {
        KnownKey a = new KnownKey(keyName("a"), String.format("${%s} ${%s}", keyName("b"), keyName("b")));
        KnownKey b = new KnownKey(keyName("b"), String.format("${%s} ${%s}", keyName("c"), keyName("d")));
        KnownKey c = new KnownKey(keyName("c"), String.format("${%s} ${%s}", keyName("d"), keyName("d")));
        KnownKey d = new KnownKey(keyName("d"), "123");
        
        assertEquals("123 123 123 123 123 123", a);
        assertEquals("123 123 123", b);
        assertEquals("123 123", c);
        assertEquals("123", d);
    }
    
    @Test
    public void recursiveContainsSelf() {
        KnownKey a = new KnownKey(keyName("a"), String.format("${%s}", keyName("a")));
        
        assertRecursive(a);
    }
    
    @Test
    public void recursiveContainsMutual() {
        KnownKey a = new KnownKey(keyName("a"), String.format("${%s}", keyName("b")));
        KnownKey b = new KnownKey(keyName("b"), String.format("${%s}", keyName("a")));
        
        assertRecursive(a);
        assertRecursive(b);
    }
    
    @Test
    public void recursiveContainsLoop() {
        KnownKey a = new KnownKey(keyName("a"), String.format("${%s}", keyName("b")));
        KnownKey b = new KnownKey(keyName("b"), String.format("${%s}", keyName("c")));
        KnownKey c = new KnownKey(keyName("c"), String.format("${%s}", keyName("a")));
        
        assertRecursive(a);
        assertRecursive(b);
        assertRecursive(c);
    }
    
    @Test
    public void recursiveContainsSubLoop() {
        KnownKey a = new KnownKey(keyName("a"), String.format("${%s}", keyName("b")));
        KnownKey b = new KnownKey(keyName("b"), String.format("${%s}", keyName("c")));
        KnownKey c = new KnownKey(keyName("c"), String.format("${%s}", keyName("d")));
        KnownKey d = new KnownKey(keyName("d"), String.format("hello ${%s}", keyName("b")));
        
        assertRecursive(a);
        assertRecursive(b);
        assertRecursive(c);
        assertRecursive(d);
    }

    
    @Test
    public void indirect() throws Exception {
        KnownKey a = new KnownKey(keyName("a"), "$");
        KnownKey b = new KnownKey(keyName("b"), String.format("${%s}{%s}", keyName("a"), keyName("a")));
        
        assertEquals("$", a);
        assertEquals("$", b);
        
        KnownKey c = new KnownKey(keyName("c"), "${");
        KnownKey d = new KnownKey(keyName("d"), String.format("${%s}%s}", keyName("c"), keyName("c")));
        
        assertEquals("${", c);
        assertEquals("${", d);
        
        KnownKey e = new KnownKey(keyName("e"), String.format("${%s", keyName("e")));
        KnownKey f = new KnownKey(keyName("f"), String.format("${%s}}", keyName("e")));
        
        assertEquals(String.format("${%s", keyName("e")), e);
        assertEquals(String.format("${%s", keyName("e")), f);
    }
    
    @Test
    public void indirectBad() throws Exception {
        KnownKey a = new KnownKey(keyName("a"), "${");
        KnownKey b = new KnownKey(keyName("b"), String.format("${%s}%s${%s}", keyName("a"), keyName("a"), keyName("c")));
        KnownKey c = new KnownKey(keyName("c"), "}");
        
        assertEquals("${", a);
        assertNullKey(b);
        assertEquals("}", c);
    }

    @Test
    public void indirectRecursiveContainsSelf() throws Exception {
        KnownKey a = new KnownKey(keyName("a"), "${");
        KnownKey b = new KnownKey(keyName("b"), String.format("${%s}%s}", keyName("a"), keyName("b")));
        
        assertEquals("${", a);
        assertRecursive(b);
    }
    
    @Test
    public void indirectRecursiveContainsMutual() throws Exception {
        KnownKey a = new KnownKey(keyName("a"), "${");
        KnownKey b = new KnownKey(keyName("b"), String.format("${%s}%s}", keyName("a"), keyName("c")));
        KnownKey c = new KnownKey(keyName("c"), String.format("${%s}", keyName("b")));
        
        assertEquals("${", a);
        assertRecursive(b);
        assertRecursive(c);
    }
    
    @Test
    public void indirectRecursiveContainsLoop() throws Exception {
        KnownKey a = new KnownKey(keyName("a"), "${");
        KnownKey b = new KnownKey(keyName("b"), String.format("${%s}%s}", keyName("a"), keyName("c")));
        KnownKey c = new KnownKey(keyName("c"), String.format("${%s}%s}", keyName("a"), keyName("d")));
        KnownKey d = new KnownKey(keyName("d"), String.format("${%s}%s}", keyName("a"), keyName("b")));
        
        assertEquals("${", a);
        assertRecursive(b);
        assertRecursive(c);
        assertRecursive(d);
    }
}
