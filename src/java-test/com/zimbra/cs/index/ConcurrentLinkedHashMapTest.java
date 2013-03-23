/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 VMware, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.util.concurrent.ConcurrentMap;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;

public class ConcurrentLinkedHashMapTest {
    private static Integer shouldEvictKey = null;
    private static final ConcurrentMap<Integer, Object> gal_searcher_cache = 
        new ConcurrentLinkedHashMap.Builder<Integer, Object>()
        .maximumWeightedCapacity(5)
        .listener(new EvictionListener<Integer, Object>() {
            @Override 
            public void onEviction(Integer key, Object value) {
                System.out.println("Evicted key=" + key + ", value=" + value);
                if (shouldEvictKey != null) {
                    Assert.assertEquals(key, shouldEvictKey);
                }
              }
            })
        .build();
    
    @Before
    public void setUp() throws Exception {
        shouldEvictKey = null;
    }
    
    @After
    public void tearDown() throws Exception {
        gal_searcher_cache.clear();   
    }
    
    @Test
    public void put() {
        Assert.assertTrue(gal_searcher_cache.size() == 0);
        gal_searcher_cache.put(1, new Integer(1));
        gal_searcher_cache.put(2, new Integer(2));
        gal_searcher_cache.put(3, new Integer(3));
        gal_searcher_cache.put(4, new Integer(4));
        gal_searcher_cache.put(5, new Integer(5));    
        Assert.assertTrue(gal_searcher_cache.size() == 5);
    }
    
    @Test
    public void replace() {
        Assert.assertTrue(gal_searcher_cache.size() == 0);
        Assert.assertNull(gal_searcher_cache.put(1, new Integer(1)));
        Assert.assertEquals(gal_searcher_cache.put(1, new Integer(2)), new Integer(1));
        Assert.assertEquals(gal_searcher_cache.put(1, new Integer(3)), new Integer(2));
        Assert.assertTrue(gal_searcher_cache.size() == 1);
    }
    
    @Test
    public void evict() {
        Assert.assertTrue(gal_searcher_cache.size() == 0);
        gal_searcher_cache.put(1, new Integer(1));
        gal_searcher_cache.put(2, new Integer(2));
        gal_searcher_cache.put(3, new Integer(3));
        gal_searcher_cache.put(4, new Integer(4));
        gal_searcher_cache.put(5, new Integer(5));
        
        //at this point the oldest entry should get evicted!!
        shouldEvictKey = new Integer(1);
        
        gal_searcher_cache.put(6, new Integer(6));    
        Assert.assertTrue(gal_searcher_cache.size() == 5);
    }
    
    @Test
    public void remove() {
        Assert.assertTrue(gal_searcher_cache.size() == 0);
        gal_searcher_cache.put(1, new Integer(1));
        Assert.assertEquals(gal_searcher_cache.get(1), new Integer(1));
        gal_searcher_cache.remove(1);
        Assert.assertNull(gal_searcher_cache.get(1));
        Assert.assertTrue(gal_searcher_cache.size() == 0); 
    }
    
    @Test
    public void clear() {
        Assert.assertTrue(gal_searcher_cache.size() == 0);
        gal_searcher_cache.put(1, new Integer(1));
        gal_searcher_cache.put(2, new Integer(2));
        gal_searcher_cache.put(3, new Integer(3));
        gal_searcher_cache.clear();
        Assert.assertTrue(gal_searcher_cache.size() == 0); 
    }
    
    
    
}
