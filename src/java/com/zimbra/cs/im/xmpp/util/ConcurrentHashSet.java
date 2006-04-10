/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * Part of the Zimbra Collaboration Suite Server.
 *
 * The Original Code is Copyright (C) Jive Software. Used with permission
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class implements the <tt>Set</tt> interface, backed by a ConcurrentHashMap instance.
 *
 * @author Matt Tucker
 */
public class ConcurrentHashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable,
        java.io.Serializable
{

    private transient ConcurrentHashMap<E,Object> map;

    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();

    /**
     * Constructs a new, empty set; the backing <tt>ConcurrentHashMap</tt> instance has
     * default initial capacity (16) and load factor (0.75).
     */
    public ConcurrentHashSet() {
	    map = new ConcurrentHashMap<E,Object>();
    }

    /**
     * Constructs a new set containing the elements in the specified
     * collection.  The <tt>ConcurrentHashMap</tt> is created with default load factor
     * (0.75) and an initial capacity sufficient to contain the elements in
     * the specified collection.
     *
     * @param c the collection whose elements are to be placed into this set.
     * @throws NullPointerException   if the specified collection is null.
     */
    public ConcurrentHashSet(Collection<? extends E> c) {
	    map = new ConcurrentHashMap<E,Object>(Math.max((int) (c.size()/.75f) + 1, 16));
	    addAll(c);
    }

    /**
     * Constructs a new, empty set; the backing <tt>ConcurrentHashMap</tt> instance has
     * the specified initial capacity and the specified load factor.
     *
     * @param initialCapacity the initial capacity of the hash map.
     * @param loadFactor the load factor of the hash map.
     * @throws IllegalArgumentException if the initial capacity is less
     *      than zero, or if the load factor is nonpositive.
     */
    public ConcurrentHashSet(int initialCapacity, float loadFactor) {
	    map = new ConcurrentHashMap<E,Object>(initialCapacity, loadFactor, 16);
    }

    /**
     * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
     * the specified initial capacity and default load factor, which is
     * <tt>0.75</tt>.
     *
     * @param      initialCapacity   the initial capacity of the hash table.
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero.
     */
    public ConcurrentHashSet(int initialCapacity) {
	    map = new ConcurrentHashMap<E,Object>(initialCapacity);
    }

    public Iterator<E> iterator() {
	    return map.keySet().iterator();
    }

    public int size() {
	    return map.size();
    }

    public boolean isEmpty() {
	    return map.isEmpty();
    }

    public boolean contains(Object o) {
	    return map.containsKey(o);
    }

    public boolean add(E o) {
	    return map.put(o, PRESENT)==null;
    }

    public boolean remove(Object o) {
	    return map.remove(o)==PRESENT;
    }

    public void clear() {
	    map.clear();
    }

    public Object clone() {
        try {
            ConcurrentHashSet<E> newSet = (ConcurrentHashSet<E>)super.clone();
            newSet.map.putAll(map);
            return newSet;
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
	    s.defaultWriteObject();

        // Write out size
        s.writeInt(map.size());

        // Write out all elements in the proper order.
        for (Iterator i=map.keySet().iterator(); i.hasNext(); )
            s.writeObject(i.next());
        }

    /**
     * Reconstitute the <tt>HashSet</tt> instance from a stream (that is,
     * deserialize it).
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException
    {
	    s.defaultReadObject();

        map = new ConcurrentHashMap<E, Object>();

        // Read in size
        int size = s.readInt();

        // Read in all elements in the proper order.
        for (int i=0; i<size; i++) {
            E e = (E) s.readObject();
            map.put(e, PRESENT);
        }
    }
}