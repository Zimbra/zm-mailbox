/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
