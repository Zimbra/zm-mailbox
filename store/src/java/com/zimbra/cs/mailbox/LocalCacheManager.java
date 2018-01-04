package com.zimbra.cs.mailbox;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;

import java.lang.ref.SoftReference;
import java.util.*;

public class LocalCacheManager implements CacheManager {
    /**
     * Maps account IDs (<code>String</code>s) to mailbox IDs
     * (<code>Integer</code>s).  <i>Every</i> mailbox in existence on the
     * server appears in this mapping.
     */
    private Map<String, Integer> mailboxIds;
    /**
     * Maps mailbox IDs ({@link Integer}s) to either
     * <ul>
     * <li>a loaded {@link Mailbox}, or
     * <li>a {@link SoftReference} to a loaded {@link Mailbox}, or
     * <li>a {@link MaintenanceContext} for the mailbox.
     * </ul>
     * Mailboxes are faulted into memory as needed, but may drop from memory when the SoftReference expires due to
     * memory pressure combined with a lack of outstanding references to the {@link Mailbox}.  Only one {@link Mailbox}
     * per user is cached, and only that {@link Mailbox} can process user requests.
     */
    private MailboxMap cache;

    public LocalCacheManager() {
        DbPool.DbConnection conn = null;
        synchronized (this) {
            try {
                conn = DbPool.getConnection();
                mailboxIds = DbMailbox.listMailboxes(conn, null);
                cache = createCache();
            }catch (ServiceException e) {
                ZimbraLog.mailbox.error("creating cache manager: ", e);
                throw new CacheManagerException("creating cache manager: ", e.getCause());
            } finally {
                DbPool.quietClose(conn);
            }
        }
    }

    @Override
    public Collection<Integer> getMailboxIds() {
        return mailboxIds.values();
    }

    public Set<String> getAccountIds(){
        return mailboxIds.keySet();
    }

    @Override
    public Set<Map.Entry<String, Integer>> getMailboxIdsByAccountId(){
        return mailboxIds.entrySet();
    }

    @Override
    public int getMailboxCount(){
        return mailboxIds.size();
    }

    @Override
    public Integer getMailboxKey(String accountId){
        return mailboxIds.get(accountId);
    }

    @Override
    public void removeMailboxId(String accountId){
        mailboxIds.remove(accountId);;
    }

    public MailboxMap createCache() {
        return new MailboxMap(LC.zimbra_mailbox_manager_hardref_cache.intValue());
    }

    @VisibleForTesting
    public void clearCache() {
        cache.clear();
        mailboxIds.clear();
    }

    public Object retrieveFromCache(int mailboxId, boolean trackGC, MailboxManager mailboxManager) throws MailServiceException {
        synchronized (mailboxManager) {
            Object cached = cache.get(mailboxId, trackGC);
            if (cached instanceof MailboxMaintenance) {
                MailboxMaintenance maintenance = (MailboxMaintenance) cached;
                if (!maintenance.canAccess()) {
                    if (mailboxManager.isMailboxLockedOut(maintenance.getAccountId())) {
                        throw MailServiceException.MAINTENANCE(mailboxId, "mailbox locked out for maintenance");
                    } else {
                        throw MailServiceException.MAINTENANCE(mailboxId);
                    }
                }
                if (maintenance.getMailbox() != null) {
                    return maintenance.getMailbox();
                }
            }
            // if we've retrieved NULL or a Mailbox or an accessible lock, return it
            return cached;
        }
    }

    @Override
    public int getMailboxCacheSize() {
        return cache.size();
    }

    @Override
    public Collection<Object> getAllLoadedMailboxes(){
        return cache.values();
    }

    @Override
    public Set<Map.Entry<Integer, Object>> getMailboxesById(){
        return cache.entrySet();
    }

    @Override
    public Object getMailbox(int mailboxId){
        return cache.get(mailboxId);
    }

    @Override
    public Mailbox cacheMailbox(Mailbox mailbox, MailboxManager mailboxManager) {
        cache.put(mailbox.getId(), mailbox);
        return mailbox;
    }

    @Override
    public void cacheMailbox(int mailboxId, MailboxMaintenance maint) {
        cache.put(mailboxId, maint);
    }

    @Override
    public void removeMailbox(int mailboxId) {
        cache.remove(mailboxId);
    }

    public synchronized void cacheAccount(String accountId, int mailboxId) {
        mailboxIds.put(accountId.toLowerCase(), Integer.valueOf(mailboxId));
    }


    protected static class MailboxMap implements Map<Integer, Object> {
        final int mHardSize;
        final LinkedHashMap<Integer, Object> mHardMap;
        final HashMap<Integer, Object> mSoftMap;

        @SuppressWarnings("serial")
        MailboxMap(int hardSize) {
            hardSize = Math.max(hardSize, 0);
            mHardSize = hardSize;
            mSoftMap = new HashMap<Integer, Object>();
            mHardMap = new LinkedHashMap<Integer, Object>(mHardSize / 4, (float) .75, true) {
                @Override
                protected boolean removeEldestEntry(Entry<Integer, Object> eldest) {
                    if (size() <= mHardSize)
                        return false;

                    Object obj = eldest.getValue();
                    if (obj instanceof Mailbox)
                        obj = new SoftReference<Mailbox>((Mailbox) obj);
                    mSoftMap.put(eldest.getKey(), obj);
                    return true;
                }
            };
        }

        protected MailboxMap() {
            mHardSize = 0;
            mHardMap = null;
            mSoftMap = null;
        }

        @Override
        public void clear() {
            mHardMap.clear();
            mSoftMap.clear();
        }

        @Override
        public boolean containsKey(Object key) {
            return mHardMap.containsKey(key) || mSoftMap.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return mHardMap.containsValue(value) || mSoftMap.containsValue(value);
        }

        @Override
        public Set<Entry<Integer, Object>> entrySet() {
            Set<Entry<Integer, Object>> entries = new HashSet<Entry<Integer, Object>>(size());
            if (mHardSize > 0)
                entries.addAll(mHardMap.entrySet());
            entries.addAll(mSoftMap.entrySet());
            return entries;
        }

        @Override
        public Object get(Object key) {
            return get(key, false);
        }

        public Object get(Object key, boolean trackGC) {
            Object obj = mHardSize > 0 ? mHardMap.get(key) : null;
            if (obj == null) {
                obj = mSoftMap.get(key);
                if (obj instanceof SoftReference) {
                    obj = ((SoftReference<?>) obj).get();
                    if (trackGC && obj == null)
                        ZimbraLog.mailbox.debug("mailbox " + key + " has been GCed; reloading");
                }
            }
            return obj;
        }

        @Override
        public boolean isEmpty() {
            return mHardMap.isEmpty() && mSoftMap.isEmpty();
        }

        @Override
        public Set<Integer> keySet() {
            Set<Integer> keys = new HashSet<Integer>(size());
            if (mHardSize > 0)
                keys.addAll(mHardMap.keySet());
            keys.addAll(mSoftMap.keySet());
            return keys;
        }

        @Override
        public Object put(Integer key, Object value) {
            Object removed;
            if (mHardSize > 0) {
                removed = mHardMap.put(key, value);
                if (removed == null)
                    removed = mSoftMap.remove(key);
            } else {
                if (value instanceof Mailbox)
                    value = new SoftReference<Object>(value);
                removed = mSoftMap.put(key, value);
            }
            if (removed instanceof SoftReference)
                removed = ((SoftReference<?>) removed).get();
            return removed;
        }

        @Override
        public void putAll(Map<? extends Integer, ? extends Object> t) {
            for (Entry<? extends Integer, ? extends Object> entry : t.entrySet())
                put(entry.getKey(), entry.getValue());
        }

        @Override
        public Object remove(Object key) {
            Object removed = mHardSize > 0 ? mHardMap.remove(key) : null;
            if (removed == null) {
                removed = mSoftMap.remove(key);
                if (removed instanceof SoftReference)
                    removed = ((SoftReference<?>) removed).get();
            }
            return removed;
        }

        @Override
        public int size() {
            return mHardMap.size() + mSoftMap.size();
        }

        @Override
        public Collection<Object> values() {
            List<Object> values = new ArrayList<Object>(size());
            if (mHardSize > 0)
                values.addAll(mHardMap.values());
            for (Object o : mSoftMap.values()) {
                if (o instanceof SoftReference)
                    o = ((SoftReference<?>) o).get();
                values.add(o);
            }
            return values;
        }

        @Override
        public String toString() {
            return "<" + mHardMap.toString() + ", " + mSoftMap.toString() + ">";
        }
    }
}