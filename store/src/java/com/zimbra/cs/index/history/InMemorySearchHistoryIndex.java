package com.zimbra.cs.index.history;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.zimbra.common.service.ServiceException;

/**
 * In-memory implementation of the search history index. Creates a prefix
 * map for fast searches. Should only be used for testing or debugging.
 */
public class InMemorySearchHistoryIndex implements ZimbraSearchHistory.SearchHistoryIndex {

    //map of prefixes to IDs of entries; used for searching
    SetMultimap<String, Integer> prefixMap = HashMultimap.create();

    //map of IDs to prefixes; used for deleting
    SetMultimap<Integer, String> idMap = HashMultimap.create();


    @Override
    public void add(int id, String entry) throws ServiceException {
        for (int i = 1; i <= entry.length(); i++) {
            String prefix = entry.substring(0, i);
            prefixMap.put(prefix, id);
            idMap.put(id, prefix);
        }
    }

    @Override
    public List<Integer> search(String prefix) throws ServiceException {
        return new ArrayList<Integer>(prefixMap.get(prefix));
    }

    @Override
    public void delete(Collection<Integer> ids) throws ServiceException {
        for (int id: ids) {
            Collection<String> prefixes = idMap.get(id);
            for (String prefix: prefixes) {
                prefixMap.remove(prefix, id);
            }
            idMap.removeAll(id);
        }
    }

    @Override
    public void deleteAll() throws ServiceException {
        prefixMap.clear();
        idMap.clear();
    }
}
