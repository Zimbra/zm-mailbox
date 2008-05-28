/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.mailbox.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.mailbox.MailItem;

public final class TypedIdList implements Iterable<Map.Entry<Byte,List<Integer>>> {
    private Map<Byte,List<Integer>> mIds = new HashMap<Byte,List<Integer>>(4);

    public TypedIdList()  {}
    public TypedIdList(TypedIdList other) {
        this();
        if (other != null)
            add(other);
    }

    public void add(byte type, Integer id) {
        if (id == null)
            return;
        List<Integer> items = mIds.get(type);
        if (items == null)
            mIds.put(type, items = new ArrayList<Integer>(1));
        items.add(id);
    }

    public void add(byte type, List<Integer> ids) {
        if (ids == null || ids.size() == 0)
            return;
        List<Integer> items = mIds.get(type);
        if (items == null)
            mIds.put(type, items = new ArrayList<Integer>(1));
        items.addAll(ids);
    }

    public void add(TypedIdList other) {
        for (Map.Entry<Byte,List<Integer>> entry : other)
            add(entry.getKey(), entry.getValue());
    }

    public boolean remove(byte type, Integer id) {
        if (id == null)
            return false;
        List<Integer> items = mIds.get(type);
        if (items != null && items.remove(id)) {
            if (items.isEmpty())
                mIds.remove(type);
            return true;
        }
        return false;
    }

    public void remove(byte type) {
        mIds.remove(type);
    }

    public boolean contains(Integer id) {
        for (List<Integer> set : mIds.values())
            if (set.contains(id))
                return true;
        return false;
    }

    public Set<Byte> types() {
        return mIds.keySet();
    }

    /** list of types included in this map, see @link{MailItem#typeToBitmask} */
    public int getTypesMask() {
        int mask = 0;
        for (byte b : types())
            mask |= MailItem.typeToBitmask(b);
        return mask;
    }

    public List<Integer> getIds(byte... types) {
        List<Integer> ids = null, typedIds;
        for (byte b : types) {
            if ((typedIds = getIds(b)) == null)
                continue;
            if (ids == null)
                ids = new ArrayList<Integer>(typedIds.size());
            ids.addAll(typedIds);
        }
        return ids;
    }

    public List<Integer> getIds(byte type) {
        return mIds.get(type);
    }

    public List<Integer> getAll() {
        List<Integer> marked = new ArrayList<Integer>();
        for (List<Integer> set : mIds.values())
            marked.addAll(set);
        return marked;
    }

    public int size() {
        int size = 0;
        for (List<Integer> set : mIds.values())
            size += set.size();
        return size;
    }

    public Iterator<Map.Entry<Byte, List<Integer>>> iterator() {
        return mIds.entrySet().iterator();
    }

    public boolean isEmpty() {
        return mIds.isEmpty();
    }

    public void clear() {
        mIds.clear();
    }

    @Override public String toString() {
        if (isEmpty())
            return "<empty>";

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Byte,List<Integer>> entry : mIds.entrySet())
            sb.append(sb.length() == 0 ? "" : ",").append(MailItem.getNameForType(entry.getKey())).append('=').append(entry.getValue());
        return sb.toString();
    }
}