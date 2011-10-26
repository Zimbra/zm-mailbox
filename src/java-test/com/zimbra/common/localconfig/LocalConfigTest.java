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

package com.zimbra.common.localconfig;

import org.junit.Assert;
import org.junit.Test;

public class LocalConfigTest {
    
    private String get(String key) throws ConfigException {
        return LocalConfig.getInstance().get(key);
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
        KnownKey a = new KnownKey("a", "${b} ${b}");
        KnownKey b = new KnownKey("b", "123");
        
        assertEquals("123 123", a);
    }
    
    @Test
    public void multipleDeep() throws Exception {
        KnownKey a = new KnownKey("a", "${b} ${b}");
        KnownKey b = new KnownKey("b", "${c} ${d}");
        KnownKey c = new KnownKey("c", "${d} ${d}");
        KnownKey d = new KnownKey("d", "123");
        
        assertEquals("123 123 123 123 123 123", a);
        assertEquals("123 123 123", b);
        assertEquals("123 123", c);
        assertEquals("123", d);
    }
    
    @Test
    public void recursiveContainsSelf() {
        KnownKey a = new KnownKey("a", "${a}");
        
        assertRecursive(a);
    }
    
    @Test
    public void recursiveContainsMutual() {
        KnownKey a = new KnownKey("a", "${b}");
        KnownKey b = new KnownKey("b", "${a}");
        
        assertRecursive(a);
        assertRecursive(b);
    }
    
    @Test
    public void recursiveContainsLoop() {
        KnownKey a = new KnownKey("a", "${b}");
        KnownKey b = new KnownKey("b", "${c}");
        KnownKey c = new KnownKey("c", "${a}");
        
        assertRecursive(a);
        assertRecursive(b);
        assertRecursive(c);
    }
    
    @Test
    public void recursiveContainsSubLoop() {
        KnownKey a = new KnownKey("a", "${b}");
        KnownKey b = new KnownKey("b", "${c}");
        KnownKey c = new KnownKey("c", "${d}");
        KnownKey d = new KnownKey("d", "hello ${b}");
        
        assertRecursive(a);
        assertRecursive(b);
        assertRecursive(c);
        assertRecursive(d);
    }

    
    @Test
    public void indirect() throws Exception {
        KnownKey a = new KnownKey("a", "$");
        KnownKey b = new KnownKey("b", "${a}{a}");
        
        assertEquals("$", a);
        assertEquals("$", b);
        
        KnownKey c = new KnownKey("c", "${");
        KnownKey d = new KnownKey("d", "${c}c}");
        
        assertEquals("${", c);
        assertEquals("${", d);
        
        KnownKey e = new KnownKey("e", "${e");
        KnownKey f = new KnownKey("f", "${e}}");
        
        assertEquals("${e", e);
        assertEquals("${e", f);
    }
    
    @Test
    public void indirectBad() throws Exception {
        KnownKey a = new KnownKey("a", "${");
        KnownKey b = new KnownKey("b", "${a}a${c}");
        KnownKey c = new KnownKey("c", "}");
        
        assertEquals("${", a);
        assertNullKey(b);
        assertEquals("}", c);
    }

    @Test
    public void indirectRecursiveContainsSelf() throws Exception {
        KnownKey a = new KnownKey("a", "${");
        KnownKey b = new KnownKey("b", "${a}b}");
        
        assertEquals("${", a);
        assertRecursive(b);
    }
    
    @Test
    public void indirectRecursiveContainsMutual() throws Exception {
        KnownKey a = new KnownKey("a", "${");
        KnownKey b = new KnownKey("b", "${a}c}");
        KnownKey c = new KnownKey("c", "${b}");
        
        assertEquals("${", a);
        assertRecursive(b);
        assertRecursive(c);
    }
    
    @Test
    public void indirectRecursiveContainsLoop() throws Exception {
        KnownKey a = new KnownKey("a", "${");
        KnownKey b = new KnownKey("b", "${a}c}");
        KnownKey c = new KnownKey("c", "${a}d}");
        KnownKey d = new KnownKey("d", "${a}b}");
        
        assertEquals("${", a);
        assertRecursive(b);
        assertRecursive(c);
        assertRecursive(d);
    }
}
