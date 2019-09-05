/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;

public class SpoolingCache<K extends Serializable> implements Iterable<K> {
    final int memlimit;
    final List<K> memcache;
    File diskcache = null;
    private ObjectOutputStream oos = null;
    int size = 0;
    final List<CacheIterator> iterators = new ArrayList<CacheIterator>(3);

    private static final int DEFAULT_MEMORY_CACHE_ITEMS = 100000;

    public SpoolingCache() {
        this(DEFAULT_MEMORY_CACHE_ITEMS);
    }

    public SpoolingCache(int memoryItemLimit) {
        memlimit = memoryItemLimit;
        memcache = new ArrayList<K>(Math.min(memlimit, 1000));
    }

    public void add(K item) throws IOException {
        if (memcache.size() < memlimit) {
            memcache.add(item);
        } else {
            if (oos == null) {
                diskcache = File.createTempFile("scache", ".tmp", new File(LC.zimbra_tmp_directory.value()));
                oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(diskcache)));
            }
            oos.writeObject(item);
            oos.reset();
        }
        size++;
    }

    public void cleanup() {
        size = 0;
        memcache.clear();
        if (oos != null) {
            ByteUtil.closeStream(oos);
            oos = null;
        }
        if (diskcache != null) {
            diskcache.delete();
            diskcache = null;
        }
        if (!iterators.isEmpty()) {
            for (CacheIterator it : new ArrayList<CacheIterator>(iterators)) {
                it.cleanup();
            }
        }
    }

    private class CacheIterator implements Iterator<K> {
        private final int oldsize = size;
        private int index = 0;
        private ObjectInputStream ois = null;

        CacheIterator()  { }

        @SuppressWarnings("unchecked")
        @Override
        public K next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            } else if (index < memlimit) {
                return memcache.get(index++);
            } else {
                try {
                    if (ois == null) {
                        ois = new ObjectInputStream(new FileInputStream(diskcache));
                    }
                    K item = (K) ois.readObject();
                    index++;
                    return item;
                } catch (IOException e) {
                    throw diskcache == null ? new ConcurrentModificationException() : new RuntimeException("error reading from spool file");
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("could not deserialize spooled item");
                }
            }
        }

        @Override
        public boolean hasNext() {
            checkForComodification();

            if (index < size) {
                return true;
            } else {
                cleanup();
                return false;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        final void checkForComodification() {
            if (size != oldsize) {
                throw new ConcurrentModificationException();
            }
        }

        void cleanup() {
            if (ois != null) {
                ByteUtil.closeStream(ois);
                ois = null;
            }
            synchronized (iterators) {
                iterators.remove(this);
            }
        }
    }

    @Override
    public Iterator<K> iterator() {
        if (oos != null) {
            try {
                oos.flush();
            } catch (IOException e) {
                throw new RuntimeException("could not flush pending spool writes");
            }
        }

        CacheIterator it = new CacheIterator();
        synchronized (iterators) {
            iterators.add(it);
        }
        return it;
    }

    public int size() {
        return size;
    }

    @VisibleForTesting
    boolean isSpooled() {
        return diskcache != null;
    }
}
