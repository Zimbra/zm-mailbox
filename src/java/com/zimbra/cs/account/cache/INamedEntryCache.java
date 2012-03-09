/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.cache;

import java.util.List;

import com.zimbra.cs.account.NamedEntry;

public interface INamedEntryCache<E extends NamedEntry> extends IEntryCache {
    public void clear();
    public void remove(String name, String id);
    public void remove(E entry);
    public void put(E entry);
    public void replace(E entry);
    public void put(List<E> entries, boolean clear);
    public E getById(String key);
    public E getByName(String key);
}
