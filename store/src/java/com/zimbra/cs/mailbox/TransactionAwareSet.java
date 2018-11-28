package com.zimbra.cs.mailbox;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public abstract class TransactionAwareSet<E> extends TransactionAware<Set<E>, TransactionAwareSet.SetChange> implements Set<E> {

    public TransactionAwareSet(TransactionCacheTracker cacheTracker, String name) {
        super(cacheTracker, name);
    }

    @Override
    public int size() {
        return getLocalCache().size();
    }

    @Override
    public boolean isEmpty() {
        return getLocalCache().isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return getLocalCache().contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return getLocalCache().iterator();
    }

    @Override
    public Object[] toArray() {
        return getLocalCache().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return getLocalCache().toArray(a);
    }

    @Override
    public boolean add(E e) {
        boolean val = getLocalCache().add(e);
        addChange(new SetAddOp(e));
        return val;
    }

    @Override
    public boolean remove(Object o) {
        boolean val = getLocalCache().remove(o);
        addChange(new SetRemoveOp(o));
        return val;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return getLocalCache().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean val = getLocalCache().addAll(c);
        addChange(new SetAddAllOp(c));
        return val;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean val = getLocalCache().retainAll(c);
        addChange(new SetRetainAllOp(c));
        return val;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean val = getLocalCache().removeAll(c);
        addChange(new SetRemoveAllOp(c));
        return val;
    }

    @Override
    public void clear() {
        getLocalCache().clear();
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

}
