/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.instrument;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author pfnguyen
 *
 */
public class JavaAgent {
    private static Instrumentation instrumentation;
    private static Map<Class<?>,List<Field>> fieldMapping =
        new HashMap<Class<?>,List<Field>>();
    public static void premain(String args, Instrumentation i) {
        instrumentation = i;
    }
    
    /**
     * Checks to see whether this application has been run with the
     * correct -javaagent commandline
     * @return true if this agent has been loaded by the JVM
     */
    public static boolean isEnabled() {
        return instrumentation != null;
    }

    /**
     * @returns -1 on error, otherwise the deep estimate of the object's size.
     */
    public static long deeplyInspectObjectSize(Object in) {
        return _deeplyInspectObjectSize(in, new IdentityHashSet());
    }
    
    private static long _deeplyInspectObjectSize(Object in, IdentityHashSet seen) {
        if (in == null)
            return 0;
        Class<?> inClass = in.getClass();
        long estimate = instrumentation.getObjectSize(in);
        List<Field> fields = getNonPrimitiveFields(inClass);
        for (Field field : fields) {
            Object fieldValue = null;
            try {
                fieldValue = field.get(in);
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
                return -1;
            }
            // primitives are accounted by getObjectSize()
            // avoid doubly counting duplicate references we've already seen
            if (!seen.contains(fieldValue)) {
                estimate += _deeplyInspectObjectSize(fieldValue, seen);
                seen.add(fieldValue);
            }
        }
        return estimate;
    }
    /**
     * get all fields for a given Class, inspects all super classes.
     * Automatically discounts static and primitive fields.
     */
    private static List<Field> getNonPrimitiveFields(Class<?> clazz) {
        if (clazz.isPrimitive() || clazz.isArray())
            return Arrays.<Field>asList();

        // cache to avoid repeated reflection
        List<Field> cached = fieldMapping.get(clazz);
        if (cached != null)
            return cached;

        Field[] fields = clazz.getDeclaredFields();
        ArrayList<Field> fieldList = new ArrayList<Field>(
                Arrays.asList(fields));
        for (Iterator<Field> i = fieldList.iterator(); i.hasNext();) {
            Field f = i.next();
            if (Modifier.isStatic(f.getModifiers())
                    || f.getType().isPrimitive()) {
                i.remove();
            } else {
                f.setAccessible(true);
            }
        }
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null)
            fieldList.addAll(getNonPrimitiveFields(superClass));

        // cache to avoid repeated reflection
        fieldMapping.put(clazz, fieldList);

        return fieldList;
    }
    
    private static class IdentityHashSet extends AbstractSet<Object>implements Set<Object> {
        private IdentityHashMap<Object,Object> backing = new IdentityHashMap<Object,Object>();
        private final static Object STUB = new Object();
        @Override
        public boolean add(Object o) {
            return backing.put(o, STUB) == null;
        }

        @Override
        public void clear() {
            backing.clear();
        }

        @Override
        public boolean contains(Object o) {
            return backing.containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            return backing.remove(o) == STUB;
        }

        @Override
        public Iterator<Object> iterator() {
            return backing.keySet().iterator();
        }

        @Override
        public int size() {
            return backing.size();
        }
        
    }
}
