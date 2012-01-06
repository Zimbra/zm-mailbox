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
package com.zimbra.qa.unittest.prov;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.zimbra.cs.account.NamedEntry;

public class Verify {
    
    public static void verifySameId(NamedEntry entry1, NamedEntry entry2) 
    throws Exception {
        assertNotNull(entry1);
        assertNotNull(entry2);
        assertEquals(entry1.getId(), entry2.getId());
    }
    
    public static void verifySameEntry(NamedEntry entry1, NamedEntry entry2) 
    throws Exception {
        verifySameId(entry1, entry2);
        assertEquals(entry1.getName(), entry2.getName());
    }
    
    // verify list contains all the entries
    // if checkCount == true, verify the count matches too
    public static void verifyEntries(List<NamedEntry> list, NamedEntry[] entries, 
            boolean checkCount) throws Exception {
        try {
            if (checkCount)
                assertEquals(list.size(), entries.length);
        
            Set<String> ids = new HashSet<String>();
            for (NamedEntry entry : list)
                ids.add(entry.getId());
            
            for (NamedEntry entry : entries) {
                assertTrue(ids.contains(entry.getId()));
                ids.remove(entry.getId());
            }
            
            // make sure all ids in list is present is entries
            if (checkCount)
                assertEquals(ids.size(), 0);
         
        } catch (AssertionError e) {
            System.out.println();
            System.out.println("===== verifyEntries failed =====");
            System.out.println("Message: " + e.getMessage());
            
            System.out.println();
            System.out.println("list contains " + list.size() + " entries:");
            for (NamedEntry entry : list) {
                System.out.println("    " + entry.getName());
            }
            
            System.out.println();
            System.out.println("entries contains " + entries.length + " entries:");
            for (NamedEntry entry : entries) {
                System.out.println("    " + entry.getName());
            }
            
            System.out.println();
            throw e;
        }
    }
    
    // verify list of NamedEntry contains all the ids
    // if checkCount == true, verify the count matches too
    public static void verifyEntriesById(List<NamedEntry> list, String[] names, 
            boolean checkCount) 
    throws Exception {
        Set<String> idsInList = new HashSet<String>();
        for (NamedEntry entry : list)
            idsInList.add(entry.getId());
        
        verifyEntries(idsInList, names, checkCount);
    }
    
    // verify list of NamedEntry contains all the names
    // if checkCount == true, verify the count matches too
    public static void verifyEntriesByName(List<NamedEntry> list, String[] names, 
            boolean checkCount) 
    throws Exception {
        Set<String> namesInList = new HashSet<String>();
        for (NamedEntry entry : list)
            namesInList.add(entry.getName());
        
        verifyEntries(namesInList, names, checkCount);
    }
    
    // verify list contains all the names
    // if checkCount == true, verify the count matches too
    public static void verifyEntries(Set<String> list, String[] names, 
            boolean checkCount) 
    throws Exception {
        try {
            if (checkCount) {
                assertEquals(names.length, list.size());
            }
            
            for (String name : names) {
                assertTrue(list.contains(name));
            }
        } catch (AssertionError e) {
            System.out.println();
            System.out.println("===== verifyEntries failed =====");
            System.out.println("Message: " + e.getMessage());
            
            System.out.println();
            System.out.println("list contains " + list.size() + " entries:");
            for (String name : list) {
                System.out.println("    " + name);
            }
            
            System.out.println();
            System.out.println("entries contains " + names.length + " entries:");
            for (String name : names) {
                System.out.println("    " + name);
            }
            
            System.out.println();
            throw e;
        }
    }
    
    public static void verifyEquals(Set<String> expected, Set<String> actual) 
    throws Exception {
        try {
            assertEquals(expected.size(), actual.size());
            
            for (String entry : expected) {
                assertTrue(actual.contains(entry));
            }
        } catch (AssertionError e) {
            dump(e, expected, actual);
            throw e;
        }
    }
    
    public static void verifyEquals(List<String> expected, List<String> actual) 
    throws Exception {
        try {
            assertEquals(expected.size(), actual.size());
            
            for (int i = 0; i < expected.size(); i++) {
                assertEquals(expected.get(i), actual.get(i));
            }
        } catch (AssertionError e) {
            dump(e, expected, actual);
            throw e;
        }
    }
    
    private static void dump(AssertionError e, Collection<String> expected, Collection<String> actual) {
        System.out.println();
        System.out.println("===== verifyEquals failed =====");
        System.out.println("Message: " + e.getMessage());
        
        System.out.println();
        System.out.println(String.format("expected (size=%d)", expected.size()));
        for (String str : expected) {
            System.out.println("    " + str);
        }
        
        System.out.println();
        System.out.println(String.format("actual (size=%d)", actual.size()));
        for (String str : actual) {
            System.out.println("    " + str);
        }
        
        System.out.println();
    }
    
    public static void verifyEquals(Set<String> expected, List<NamedEntry> actual) {
        try {
            assertEquals(expected.size(), actual.size());
            
            for (NamedEntry entry : actual) {
                assertTrue(expected.contains(entry.getName()));
            }
        } catch (AssertionError e) {
            System.out.println();
            System.out.println("===== verifyEquals failed =====");
            System.out.println("Message: " + e.getMessage());
            
            System.out.println();
            System.out.println(String.format("expected (size=%d)", expected.size()));
            for (String name : expected)
                System.out.println("    " + name);
            
            System.out.println();
            System.out.println(String.format("actual (size=%d)", actual.size()));
            for (NamedEntry entry : actual)
                System.out.println("    " + entry.getName());
            
            System.out.println();
            throw e;
        }
    }
    
    public static void verifyEquals(List<? extends NamedEntry> expected, List<? extends NamedEntry> actual,
            boolean orderMatters) {
        try {
            if (expected == null) {
                expected = new ArrayList<NamedEntry>();
            }
            
            int size = expected.size();
            
            assertEquals(expected.size(), actual.size());
            
            List<String> expectedIds = Lists.newArrayList();
            List<String> expectedNames = Lists.newArrayList();
            for (NamedEntry entry : expected) {
                expectedIds.add(entry.getId());
                expectedNames.add(entry.getName());
            }
            
            List<String> actualIds = Lists.newArrayList();
            List<String> actualNames = Lists.newArrayList();
            for (NamedEntry entry : actual) {
                actualIds.add(entry.getId());
                actualNames.add(entry.getName());
            }

            for (int i = 0; i < size; i++) {
                if (orderMatters) {
                    assertEquals(expectedIds.get(i), actualIds.get(i));
                    assertEquals(expectedNames.get(i), actualNames.get(i));
                } else {
                    assertTrue(actualIds.contains(expectedIds.get(i)));
                    assertTrue(actualNames.contains(expectedNames.get(i)));
                }
            }

        } catch (AssertionError e) {
            System.out.println();
            System.out.println("===== verifyEquals failed =====");
            System.out.println("Message: " + e.getMessage());
            
            System.out.println();
            System.out.println(String.format("expected (size=%d)", expected.size()));
            for (NamedEntry entry : expected) {
                System.out.println("    " + entry.getName() + " (" + entry.getId() + ")");
            }
            
            System.out.println();
            System.out.println(String.format("actual (size=%d)", actual.size()));
            for (NamedEntry entry : actual) {
                System.out.println("    " + entry.getName() + " (" + entry.getId() + ")");
            }
            
            System.out.println();
            throw e;
        }
    }
    
    public static String makeResultStr(Object... objs) {
        StringBuffer sb = new StringBuffer();
        for (Object obj : objs) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            if (obj != null) {
                if (obj instanceof Collection) {
                    for (Object o : (Collection) obj) {
                        sb.append("(");
                        sb.append(o.toString());
                        sb.append(")");
                    }
                } else {
                    sb.append(obj.toString());
                }
            } else {
                sb.append("null");
            }
        }
        return sb.toString();
    }

}
