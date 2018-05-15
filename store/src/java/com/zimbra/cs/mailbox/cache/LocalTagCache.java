package com.zimbra.cs.mailbox.cache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.zimbra.cs.mailbox.Tag;

public class LocalTagCache implements TagCache {

    private Map<String, Tag> tagNameMap;
    private Map<Integer, Tag> tagIdMap;

    public LocalTagCache() {
        tagNameMap = new ConcurrentHashMap<String, Tag>();
        tagIdMap = new ConcurrentHashMap<Integer, Tag>();
    }

    @Override
    public void put(Tag tag) {
       tagNameMap.put(tag.getName().toLowerCase(), tag);
       tagIdMap.put(tag.getId(), tag);
    }

    @Override
    public Tag remove(int tagId) {
        return tagIdMap.remove(tagId);
    }

    @Override
    public Tag remove(String tagName) {
        return tagNameMap.remove(tagName);
    }

    @Override
    public Collection<Tag> values() {
        return tagIdMap.values();
    }

    @Override
    public Tag get(int tagId) {
        return tagIdMap.get(tagId);
    }

    @Override
    public Tag get(String tagName) {
        return tagNameMap.get(tagName.toLowerCase());
    }

    @Override
    public boolean contains(String tagName) {
        return tagNameMap.containsKey(tagName.toLowerCase());
    }

    @Override
    public int size() {
        return tagNameMap.size();
    }
}
