package com.zimbra.cs.db;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.zimbra.cs.util.StringUtil;

class TagsetCache {
    private Set mTagsets = new HashSet();
    private String mName;
    
    TagsetCache(String name) {
        mName = name;
    }

    void addTagset(long tagset) {
        addTagset(new Long(tagset));
    }
    
    void addTagset(Long tagset) {
        if (tagset == null) {
            throw new IllegalArgumentException("tagset cannot be null");
        }
        mTagsets.add(tagset);
    }
    
    void addTagsets(Collection /* Long */ tagsets) {
        // Iterate the collection instead of just calling addAll(), to
        // make sure all the values passed in are Longs.
        Iterator i = tagsets.iterator();
        while (i.hasNext()) {
            addTagset((Long) i.next());
        }
    }
    
    Set getTagsets(long mask) {
        Set matches = new HashSet();
        Iterator i = mTagsets.iterator();
        while (i.hasNext()) {
            Long tags = (Long) i.next();
            if ((tags.longValue() & mask) > 0) {
                matches.add(tags);
            }
        }
        return matches;
    }
    
    Set getAllTagsets() {
        return new HashSet(mTagsets);
    }
    
    /**
     * Applies a bitmask to all the tagsets in the collection, and adds the
     * resulting new tagsets. We do this when we know that the tag changed for one or
     * more items, but we don't have references to the items themselves.
     * <p>
     * 
     * The end result is that we add a bunch of new tagsets, some of which
     * may not actually exist for any items.  This is ok, since searches on
     * the bogus tagsets will never return data.  When the cache times out,
     * the bogus tagsets are removed.
     */
    void applyMask(long mask, boolean add) {
        Iterator i = mTagsets.iterator();
        Set newTagsets = new HashSet();
        while (i.hasNext()) {
            long tags = ((Long) i.next()).longValue();
            long newTags = add ? tags | mask : tags & ~mask;
            newTagsets.add(new Long(newTags));
        }
        addTagsets(newTagsets);
    }
    
    public String toString() {
        return "[TagsetCache " + mName + " (" + StringUtil.join(",", mTagsets) + ")]";
    }
}
