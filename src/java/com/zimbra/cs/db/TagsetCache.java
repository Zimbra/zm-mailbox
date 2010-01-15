/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.db;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.util.StringUtil;

class TagsetCache {
    private Set<Long> mTagsets = new HashSet<Long>();
    private String mName;
    
    TagsetCache(String name) {
        mName = name;
    }
    
    int size() {
        return mTagsets.size();
    }

    void addTagset(long tagset) {
        mTagsets.add(tagset);
    }
    
    void addTagsets(Collection<Long> tagsets) {
        mTagsets.addAll(tagsets);
    }

    /** Returns the set of all bitmasks for which <code>(tagset & mask) == value</code>,
     *  or <tt>null</tt> if <b>all</b> cached bitmasks match the given criteria. */
    Set<Long> getMatchingTagsets(long mask, long value) {
        Set<Long> matches = new HashSet<Long>();
        for (long tagset : mTagsets)
            if ((tagset & mask) == value)
                matches.add(tagset);
        return matches.size() == mTagsets.size() ? null : matches;
    }

    Set<Long> getAllTagsets() {
        return new HashSet<Long>(mTagsets);
    }

    /**
     * Applies a bitmask to all the tagsets in the collection, and adds the
     * resulting new tagsets. We do this when we know that the tag changed for one or
     * more items, but we don't have references to the items themselves.
     * <p>
     * The end result is that we add a bunch of new tagsets, some of which
     * may not actually exist for any items.  This is ok, since searches on
     * the bogus tagsets will never return data.  When the cache times out,
     * the bogus tagsets are removed.
     */
    void applyMask(long mask, boolean add) {
        Set<Long> newTagsets = new HashSet<Long>();
        for (long tags : mTagsets) {
            long newTags = add ? tags | mask : tags & ~mask;
            newTagsets.add(newTags);
        }
        addTagsets(newTagsets);
    }
    
    public String toString() {
        return "[TagsetCache " + mName + " (" + StringUtil.join(",", mTagsets) + ")]";
    }
}
