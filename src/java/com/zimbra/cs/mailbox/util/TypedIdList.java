/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.mailbox.util;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.mailbox.MailItem;

public final class TypedIdList implements Iterable<Map.Entry<MailItem.Type, List<Integer>>> {
    private final Map<MailItem.Type, List<Integer>> type2ids =
        new EnumMap<MailItem.Type, List<Integer>>(MailItem.Type.class);

    public TypedIdList() {

    }

    public TypedIdList(TypedIdList other) {
        this();
        if (other != null)
            add(other);
    }

    public void add(MailItem.Type type, Integer id) {
        if (id == null) {
            return;
        }
        List<Integer> items = type2ids.get(type);
        if (items == null) {
            type2ids.put(type, items = new ArrayList<Integer>(1));
        }
        items.add(id);
    }

    public void add(MailItem.Type type, List<Integer> ids) {
        if (ids == null || ids.size() == 0) {
            return;
        }
        List<Integer> items = type2ids.get(type);
        if (items == null) {
            type2ids.put(type, items = new ArrayList<Integer>(1));
        }
        items.addAll(ids);
    }

    public void add(TypedIdList other) {
        for (Map.Entry<MailItem.Type, List<Integer>> entry : other) {
            add(entry.getKey(), entry.getValue());
        }
    }

    public boolean remove(MailItem.Type type, Integer id) {
        if (id == null) {
            return false;
        }
        List<Integer> items = type2ids.get(type);
        if (items != null && items.remove(id)) {
            if (items.isEmpty()) {
                type2ids.remove(type);
            }
            return true;
        }
        return false;
    }

    public void remove(MailItem.Type type) {
        type2ids.remove(type);
    }

    public boolean contains(Integer id) {
        for (List<Integer> set : type2ids.values()) {
            if (set.contains(id)) {
                return true;
            }
        }
        return false;
    }

    public Set<MailItem.Type> types() {
        return type2ids.keySet();
    }

    public List<Integer> getIds(Set<MailItem.Type> types) {
        List<Integer> ids = null, typedIds;
        for (MailItem.Type type : types) {
            if ((typedIds = getIds(type)) == null) {
                continue;
            }
            if (ids == null) {
                ids = new ArrayList<Integer>(typedIds.size());
            }
            ids.addAll(typedIds);
        }
        return ids;
    }

    public List<Integer> getIds(MailItem.Type type) {
        return type2ids.get(type);
    }

    public List<Integer> getAll() {
        List<Integer> marked = new ArrayList<Integer>();
        for (List<Integer> set : type2ids.values()) {
            marked.addAll(set);
        }
        return marked;
    }

    public int size() {
        int size = 0;
        for (List<Integer> set : type2ids.values()) {
            size += set.size();
        }
        return size;
    }

    @Override
    public Iterator<Map.Entry<MailItem.Type, List<Integer>>> iterator() {
        return type2ids.entrySet().iterator();
    }

    public boolean isEmpty() {
        return type2ids.isEmpty();
    }

    public void clear() {
        type2ids.clear();
    }

    @Override
    public String toString() {
        return type2ids.toString();
    }

}
