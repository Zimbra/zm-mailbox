package com.zimbra.cs.mailbox.calendar.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

public class LocalCalendarDataCache implements CalendarDataCache {
    public static final int FOLDER_NOT_FOUND = -1;
    protected SummaryLRU map;


    public LocalCalendarDataCache(int capacity) {
        map = new SummaryLRU(capacity);
    }

    @VisibleForTesting
    public void flush() {
        map.clear();
    }

    @Override
    public CalendarData get(Key key) throws ServiceException {
        return map.get(key);
    }

    public int getFolderForItem(String accountId, int itemId) {
        return map.getFolderForItem(accountId, itemId);
    }

    @Override
    public void put(Key key, CalendarData value) throws ServiceException {
        map.put(key, value);
    }

    @Override
    public void remove(Key key) throws ServiceException {
        map.remove(key);
    }

    @Override
    public void remove(Set<Key> keys) throws ServiceException {
        for (Key key: keys) {
            map.remove(key);
        }
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        Set<Key> keysToRemove = new HashSet<>();
        for (Key key: map.keySet()) {
            if (Objects.equal(mbox.getAccountId(), key.getAccountId())) {
                keysToRemove.add(key);
            }
        }
        remove(keysToRemove);
    }

    public void removeAccount(String accountId) {
        map.removeAccount(accountId);
    }

    public int size() {
        return map.size();
    }



    @SuppressWarnings("serial")
    private static class SummaryLRU extends LinkedHashMap<CalendarDataCache.Key, CalendarData> {
        private final int mMaxAllowed;

        // map that keeps track of which calendar folders are cached for each account
        // This map is updated every time a calendar folder is added, removed, or aged out
        // of the LRU.
        private final Map<String /* account id */, Set<Integer> /* folder ids */> mAccountFolders;

        private SummaryLRU(int capacity) {
            super(capacity + 1, 1.0f, true);
            mMaxAllowed = Math.max(capacity, 1);
            mAccountFolders = new HashMap<String, Set<Integer>>();
        }

        @Override
        public void clear() {
            super.clear();
            mAccountFolders.clear();
        }

        @Override
        public CalendarData put(CalendarDataCache.Key key, CalendarData value) {
            CalendarData prevVal = super.put(key, value);
            if (prevVal == null)
                registerWithAccount(key);
            return prevVal;
        }

        @Override
        public void putAll(Map<? extends CalendarDataCache.Key, ? extends CalendarData> t) {
            super.putAll(t);
            for (CalendarDataCache.Key key : t.keySet()) {
                registerWithAccount(key);
            }
        }

        @Override
        public CalendarData remove(Object key) {
            CalendarData prevVal = super.remove(key);
            if (prevVal != null && key instanceof CalendarDataCache.Key) {
                CalendarDataCache.Key k = (CalendarDataCache.Key) key;
                deregisterFromAccount(k);
            }
            return prevVal;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<CalendarDataCache.Key, CalendarData> eldest) {
            boolean remove = size() > mMaxAllowed;
            if (remove)
                deregisterFromAccount(eldest.getKey());
            return remove;
        }

        private void registerWithAccount(CalendarDataCache.Key key) {
            String accountId = key.getAccountId();
            int folderId = key.getFolderId();
            Set<Integer> folders = mAccountFolders.get(accountId);
            if (folders == null) {
                folders = new HashSet<Integer>();
                mAccountFolders.put(accountId, folders);
            }
            folders.add(folderId);
        }

        private void deregisterFromAccount(CalendarDataCache.Key key) {
            String accountId = key.getAccountId();
            int folderId = key.getFolderId();
            Set<Integer> folders = mAccountFolders.get(accountId);
            if (folders != null) {
                folders.remove(folderId);
                // If no folders are cached for the account, drop the account entry from the map to save memory.
                if (folders.isEmpty())
                    mAccountFolders.remove(accountId);
            }
        }

        public int getFolderForItem(String accountId, int itemId) {
            int retval = FOLDER_NOT_FOUND;
            Set<Integer> folders = mAccountFolders.get(accountId);
            if (folders != null) {
                for (int folderId : folders) {
                    Key key = new Key(accountId, folderId);
                    CalendarData calData = get(key);
                    if (calData != null) {
                        CalendarItemData ci = calData.getCalendarItemData(itemId);
                        if (ci != null) {
                            retval = folderId;
                            break;
                        }
                    }
                }
            }
            return retval;
        }

        /**
         * Toss all folders of the account from the LRU.
         * @param mboxId
         */
        public void removeAccount(String accountId) {
            Set<Integer> folders = mAccountFolders.get(accountId);
            if (folders != null) {
                // Get a copy of the folder list to avoid ConcurrentModificationException on mMboxFolders.
                Integer[] fids = folders.toArray(new Integer[0]);
                for (int folderId : fids) {
                    Key key = new Key(accountId, folderId);
                    remove(key);
                }
            }
        }
    }
}