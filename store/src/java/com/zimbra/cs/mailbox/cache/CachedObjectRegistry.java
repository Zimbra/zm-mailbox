/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.MoreObjects;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.TransactionAware.Getter;

public class CachedObjectRegistry {

    private Map<CachedObjectKey, WeakCachedValue> objects;
    private Mailbox mbox;
    private String accountId;
    private ReferenceQueue<Getter<?,?>> gcQueue;

    public CachedObjectRegistry(Mailbox mbox) {
        this.accountId = mbox.getAccountId();
        this.mbox = mbox;
        this.objects = new ConcurrentHashMap<>();
        this.gcQueue = new ReferenceQueue<>();
    }

    public void addObject(CachedObjectKey key, Getter<?,?> object) {
        if (ZimbraLog.cache.isTraceEnabled()) {
            ZimbraLog.cache.trace("adding %s=%s to cache registry", key, object);
        }
        objects.put(key, new WeakCachedValue(key, object));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("accountId", accountId)
                .add("size", objects.size()).toString();
    }

    public void invalidate(CachedObjectKey key) {
        WeakCachedValue cachedGetter = objects.remove(key);
        if (cachedGetter != null && cachedGetter.get() != null) {
            if (ZimbraLog.cache.isTraceEnabled()) {
                ZimbraLog.cache.trace("removing %s=%s from cache registry", key, cachedGetter.get());
            }
            cachedGetter.get().clearCache(false);
        }
        removeGarbageCollectedKeys();
    }

    private void removeGarbageCollectedKeys() {
        WeakCachedValue value = null;
        int removed = 0;
        while ((value = (WeakCachedValue) gcQueue.poll()) != null) {
            WeakCachedValue val = objects.remove(value.key);
            if (val != null) {
                removed++;
                if (ZimbraLog.cache.isTraceEnabled()) {
                    ZimbraLog.cache.trace("removing %s from cache registry (value was gc'd)", value.key);
                }
            }
        }
        if (removed > 0 && ZimbraLog.cache.isDebugEnabled()) {
            ZimbraLog.cache.debug("removed %d gc'd objects from cache registry for %s", removed, accountId);
        }
    }

    public static enum CachedObjectKeyType {
        MAILITEM, MAILBOX;
    }

    public static class CachedObjectKey extends Pair<CachedObjectKeyType, Integer> {

        public CachedObjectKey(CachedObjectKeyType keyType, Integer key) {
            super(keyType, key);
        }
    }

    private class WeakCachedValue extends WeakReference<Getter<?,?>> {

        private CachedObjectKey key;

        private WeakCachedValue(CachedObjectKey key, Getter<?,?> value) {
            super(value, gcQueue);
            this.key = key;
        }
    }
}
