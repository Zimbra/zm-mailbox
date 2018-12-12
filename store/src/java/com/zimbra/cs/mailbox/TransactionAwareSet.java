/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

package com.zimbra.cs.mailbox;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.zimbra.common.util.ZimbraLog;

public abstract class TransactionAwareSet<E> extends TransactionAware<Set<E>, TransactionAwareSet.SetChange> implements Set<E> {

    public TransactionAwareSet(String name, TransactionCacheTracker cacheTracker, Getter<Set<E>, ?> getter) {
        super(name, cacheTracker, getter);
    }

    @Override
    public int size() {
        return data().size();
    }

    @Override
    public boolean isEmpty() {
        return data().isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return data().contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return data().iterator();
    }

    @Override
    public Object[] toArray() {
        return data().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return data().toArray(a);
    }

    @Override
    public boolean add(E e) {
        boolean val = data().add(e);
        addChange(new SetAddOp(e));
        return val;
    }

    @Override
    public boolean remove(Object o) {
        boolean val = data().remove(o);
        addChange(new SetRemoveOp(o));
        return val;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return data().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean val = data().addAll(c);
        addChange(new SetAddAllOp(c));
        return val;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean val = data().retainAll(c);
        addChange(new SetRetainAllOp(c));
        return val;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean val = data().removeAll(c);
        addChange(new SetRemoveAllOp(c));
        return val;
    }

    @Override
    public void clear() {
        data().clear();
        addChange(new SetClearOp());

    }

    public static abstract class SetChange extends TransactionAware.Change {

        public SetChange(ChangeType changeType) {
            super(changeType);
        }
    }

    public class SetAddOp extends SetChange {
        private E value;

        public SetAddOp(E value) {
            super(ChangeType.SET_ADD);
            this.value = value;
        }

        public E getValue() {
            return value;
        }

        @Override
        public String toString() {
            return toStringHelper().add("val", value).toString();
        }
    }

    public class SetAddAllOp extends SetChange {
        private Collection<? extends E> values;

        public SetAddAllOp(Collection<? extends E> values) {
            super(ChangeType.SET_ADD_ALL);
            this.values = values;
        }

        public Collection<? extends E> getValues() {
            return values;
        }

        @Override
        public String toString() {
            return toStringHelper().add("vals", values).toString();
        }
    }

    public class SetRemoveOp extends SetChange {

        private Object value;

        public SetRemoveOp(Object value) {
            super(ChangeType.REMOVE);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public String toString() {
            return toStringHelper().add("val", value).toString();
        }
    }

    public class SetRemoveAllOp extends SetChange {
        private Collection<?> values;

        public SetRemoveAllOp(Collection<?> values) {
            super(ChangeType.SET_REMOVE_ALL);
            this.values = values;
        }

        public Collection<?> getValues() {
            return values;
        }

        @Override
        public String toString() {
            return toStringHelper().add("vals", values).toString();
        }
    }

    public class SetRetainAllOp extends SetChange {
        private Collection<?> values;

        public SetRetainAllOp(Collection<?> values) {
            super(ChangeType.SET_RETAIN_ALL);
            this.values = values;
        }

        public Collection<?> getValues() {
            return values;
        }

        @Override
        public String toString() {
            return toStringHelper().add("vals", values).toString();
        }
    }

    public class SetClearOp extends SetChange {

        public SetClearOp() {
            super(ChangeType.CLEAR);
        }
    }

    @FunctionalInterface
    protected static interface SetLoader<E> {
        public Set<E> loadSet();
    }

    protected static class GreedySetGetter<E> extends GreedyGetter<Set<E>> {

        private SetLoader<E> loader;

        public GreedySetGetter(String objectName, SetLoader<E> loader) {
            super(objectName);
            this.loader = loader;
        }

        @Override
        protected Set<E> loadObject() {
            if (ZimbraLog.cache.isTraceEnabled()) {
                ZimbraLog.cache.trace("fetching set %s", objectName);
            }
            return loader.loadSet();
        }
    }
}
