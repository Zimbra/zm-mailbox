package com.zimbra.cs.index.history;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.history.ZimbraSearchHistory.SearchHistoryEntry;
import com.zimbra.cs.index.history.ZimbraSearchHistory.SearchHistoryMetadataParams;

/**
 * In-memory implementation of the search history index.
 * Should only be used for testing or debugging.
 */
public class InMemorySearchHistoryMetadata implements ZimbraSearchHistory.SearchHistoryMetadata {

    //ids of search entries
    private Map<String, Integer> idMap = new HashMap<String, Integer>();
    //running list of search entries
    private List<EntryInfo> history = new LinkedList<EntryInfo>();
    //used for groupBy functionality
    private HashMultimap<String, EntryInfo> buckets = HashMultimap.create();

    @Override
    public List<SearchHistoryEntry> search(SearchHistoryMetadataParams params)
            throws ServiceException {
        int numResults = params.getNumResults();
        long maxAge = params.getMaxAge();

        //distinct search string filter is constructed first
        Predicate<EntryInfo> filter = new Predicate<EntryInfo>() {
            private Set<Integer> seen = new HashSet<Integer>();

            @Override
            public boolean test(EntryInfo info) {
                boolean isFirstOccurrence = !seen.contains(info.getID());
                seen.add(info.getID());
                return isFirstOccurrence;
            }
        };

        //specific IDs from an index search are the second filter
        if (params.hasIds()) {
            Set<Integer> ids = new HashSet<Integer>(params.getIds());
            ZimbraLog.search.debug("constructing idFilter with ids=%s", Joiner.on(",").join(ids));
            Predicate<EntryInfo> idFilter = new Predicate<EntryInfo>() {

                @Override
                public boolean test(EntryInfo info) {
                    if (!ids.contains(info.getID())) {
                        ZimbraLog.search.debug("rejecting '%s' (id=%s, outside requested ID set)", info.getSearchString(), info.getID());
                        return false;
                    } else {
                        return true;
                    }
                }
            };
            filter = filter.and(idFilter);
        }

        //max age filter
        if (maxAge > 0) {
            long now = System.currentTimeMillis();
            ZimbraLog.search.debug("constructing ageFilter with age=%s", maxAge);
            Predicate<EntryInfo> ageFilter = new Predicate<EntryInfo>() {

                @Override
                public boolean test(EntryInfo info) {
                    long age = now - info.getTimestamp();
                    if (age < maxAge) {
                        return true;
                    } else {
                        ZimbraLog.search.debug("rejecting '%s' (age=%s ms)", info.getSearchString(), age);
                        return false;
                    }
                }
            };
            filter = filter.and(ageFilter);
        }

        //result count filter is last
        if (numResults > 0) {
            ZimbraLog.search.debug("constructing numResultsFilter with n=%s", numResults);
            Predicate<EntryInfo> numResultsFilter = new Predicate<EntryInfo>() {
                int numResultsSeen = 0;
                @Override
                public boolean test(EntryInfo info) {
                    numResultsSeen++;
                    if (numResultsSeen <= numResults) {
                        return true;
                    } else {
                        ZimbraLog.search.debug("rejecting '%s' (result #%s, max=%s)", info.getSearchString(), numResultsSeen, numResults);
                        return false;
                    }
                }
            };
            filter = filter.and(numResultsFilter);
        }

        List<SearchHistoryEntry> results = new ArrayList<SearchHistoryEntry>();
        history.stream().filter(filter)
        .map(info -> info.toSearchHistoryEntry())
        .forEach(results::add);
        return results;
    }

    @Override
    public boolean contains(String query) throws ServiceException {
        return idMap.containsKey(query);
    }

    @Override
    public void logSearch(String searchString, long timestamp) throws ServiceException {
        int id = idMap.get(searchString);
        EntryInfo info = new EntryInfo(id, searchString, timestamp);
        history.add(0, info);
        buckets.put(searchString, info);
    }

    private List<Integer> deleteByPartition(int index) {
        List<EntryInfo> toKeep = history.subList(0, index);
        List<EntryInfo> toDelete = history.subList(index, history.size());
        ZimbraLog.search.debug("marking %s entries to delete past index %s", toDelete.size(), index);
        for (EntryInfo info: toDelete) {
            buckets.get(info.getSearchString()).removeIf(item -> item.timestamp == info.getTimestamp());
        }
        //we don't want to delete entries from the index if they still exist in the remaining history,
        //so we filter by that before returning
        List<Integer> idsToDelete = new ArrayList<Integer>();
        toDelete.stream().filter(info -> buckets.get(info.searchString).isEmpty())
        .map(info -> info.id)
        .forEach(idsToDelete::add);
        history = toKeep;
        return idsToDelete;
    }

    @Override
    public Collection<Integer> deleteByAge(long maxAgeMillis) throws ServiceException {
        ZimbraLog.search.debug("purging search history with max age=%s", maxAgeMillis);
        int idx = 0;
        long now = System.currentTimeMillis();
        for (EntryInfo info: history) {
            if (now - info.timestamp > maxAgeMillis) {
                return deleteByPartition(idx);
            }
            idx++;
        }
        return Collections.emptyList();
    }

    @Override
    public void deleteAll() throws ServiceException {
        idMap.clear();
        history.clear();
        buckets.clear();
    }

    @Override
    public int getCount(String searchString, long maxAge)
            throws ServiceException {
        Set<EntryInfo> instances = buckets.get(searchString);
        if(maxAge > 0) {
            //exclude expired entries
            long now = System.currentTimeMillis();
            return (int) instances.stream().filter(i -> now - i.timestamp < maxAge).count();
        } else {
            return instances.size();
        }
    }

    @Override
    public void initNewSearch(int id, String searchString) throws ServiceException {
        ZimbraLog.search.debug("added new search history entry %s with id %s", searchString, id);
        idMap.put(searchString, id);
    }

    private static class EntryInfo {
        private int id;
        private String searchString;
        private Long timestamp;
        public EntryInfo(int id, String searchString, long timestamp) {
            this.id = id;
            this.searchString = searchString;
            this.timestamp = timestamp;
        }

        public String getSearchString() { return searchString; }
        public long getTimestamp() { return timestamp; }
        public int getID() { return id; }

        private SearchHistoryEntry toSearchHistoryEntry() {
            return new SearchHistoryEntry(id, searchString);
        }

    }

}
