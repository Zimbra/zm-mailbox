package com.zimbra.cs.mailbox.redis;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.redisson.api.RFuture;
import org.redisson.api.RList;
import org.redisson.api.SortOrder;
import org.redisson.api.mapreduce.RCollectionMapReduce;

public class RedissonRetryList<V> extends RedissonRetryExpirable<RList<V>> implements RList<V> {

    public RedissonRetryList(RedissonInitializer<RList<V>> listInitializer, RedissonRetryClient client) {
        super(listInitializer, client);
    }

    @Override
    public boolean isEmpty() {
        return runCommand(() -> redissonObject.isEmpty());
    }

    @Override
    public boolean add(V e) {
        return runCommand(() -> redissonObject.add(e));
    }

    @Override
    public void clear() {
        runCommand(() -> { redissonObject.clear(); return null; });
    }

    @Override
    public List<V> readAll() {
        return runCommand(() -> redissonObject.readAll());
    }

    @Override
    public void trim(int fromIndex, int toIndex) {
        runCommand(() -> { redissonObject.trim(fromIndex, toIndex); return null; });
    }

    @Override
    public int size() {
        return runCommand(() -> redissonObject.size());
    }

    @Override
    public boolean contains(Object o) {
        return runCommand(() -> redissonObject.contains(o));
    }

    @Override
    public Iterator<V> iterator() {
        return runCommand(() -> redissonObject.iterator());
    }

    @Override
    public Object[] toArray() {
        return runCommand(() -> redissonObject.toArray());
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return runCommand(() -> redissonObject.toArray(a));
    }

    @Override
    public boolean remove(Object o) {
        return runCommand(() -> redissonObject.remove(o));
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return runCommand(() -> redissonObject.containsAll(c));
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        return runCommand(() -> redissonObject.addAll(c));
    }

    @Override
    public boolean addAll(int index, Collection<? extends V> c) {
        return runCommand(() -> redissonObject.addAll(index, c));
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return runCommand(() -> redissonObject.removeAll(c));
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return runCommand(() -> redissonObject.retainAll(c));
    }

    @Override
    public V get(int index) {
        return runCommand(() -> redissonObject.get(index));
    }

    @Override
    public V set(int index, V element) {
        return runCommand(() -> redissonObject.set(index, element));
    }

    @Override
    public void add(int index, V element) {
        runCommand(() -> { redissonObject.add(index, element); return null;});
    }

    @Override
    public V remove(int index) {
        return runCommand(() -> redissonObject.remove(index));
    }

    @Override
    public int indexOf(Object o) {
        return runCommand(() -> redissonObject.indexOf(o));
    }

    @Override
    public int lastIndexOf(Object o) {
        return runCommand(() -> redissonObject.lastIndexOf(o));
    }

    @Override
    public ListIterator<V> listIterator() {
        return runCommand(() -> redissonObject.listIterator());
    }

    @Override
    public ListIterator<V> listIterator(int index) {
        return runCommand(() -> redissonObject.listIterator(index));
    }

    @Override
    public RFuture<List<V>> getAsync(int... indexes) {
        return runCommand(() -> redissonObject.getAsync(indexes));
    }

    @Override
    public RFuture<Integer> addAfterAsync(V elementToFind, V element) {
        return runCommand(() -> redissonObject.addAfterAsync(elementToFind, element));
    }

    @Override
    public RFuture<Integer> addBeforeAsync(V elementToFind, V element) {
        return runCommand(() -> redissonObject.addBeforeAsync(elementToFind, element));
    }

    @Override
    public RFuture<Boolean> addAllAsync(int index, Collection<? extends V> coll) {
        return runCommand(() -> redissonObject.addAllAsync(index, coll));
    }

    @Override
    public RFuture<Integer> lastIndexOfAsync(Object o) {
        return runCommand(() -> redissonObject.lastIndexOfAsync(o));
    }

    @Override
    public RFuture<Integer> indexOfAsync(Object o) {
        return runCommand(() -> redissonObject.indexOfAsync(o));
    }

    @Override
    public RFuture<Void> fastSetAsync(int index, V element) {
        return runCommand(() -> redissonObject.fastSetAsync(index, element));
    }

    @Override
    public RFuture<V> setAsync(int index, V element) {
        return runCommand(() -> redissonObject.setAsync(index, element));
    }

    @Override
    public RFuture<V> getAsync(int index) {
        return runCommand(() -> redissonObject.getAsync(index));
    }

    @Override
    public RFuture<List<V>> readAllAsync() {
        return runCommand(() -> redissonObject.readAllAsync());
    }

    @Override
    public RFuture<Void> trimAsync(int fromIndex, int toIndex) {
        return runCommand(() -> redissonObject.trimAsync(fromIndex, toIndex));
    }

    @Override
    public RFuture<Void> fastRemoveAsync(int index) {
        return runCommand(() -> redissonObject.fastRemoveAsync(index));
    }

    @Override
    public RFuture<V> removeAsync(int index) {
        return runCommand(() -> redissonObject.removeAsync(index));
    }

    @Override
    public RFuture<Boolean> retainAllAsync(Collection<?> c) {
        return runCommand(() -> redissonObject.retainAllAsync(c));
    }

    @Override
    public RFuture<Boolean> removeAllAsync(Collection<?> c) {
        return runCommand(() -> redissonObject.removeAllAsync(c));
    }

    @Override
    public RFuture<Boolean> containsAsync(Object o) {
        return runCommand(() -> redissonObject.containsAsync(o));
    }

    @Override
    public RFuture<Boolean> containsAllAsync(Collection<?> c) {
        return runCommand(() -> redissonObject.containsAllAsync(c));
    }

    @Override
    public RFuture<Boolean> removeAsync(Object o) {
        return runCommand(() -> redissonObject.removeAsync(o));
    }

    @Override
    public RFuture<Integer> sizeAsync() {
        return runCommand(() -> redissonObject.sizeAsync());
    }

    @Override
    public RFuture<Boolean> addAsync(V e) {
        return runCommand(() -> redissonObject.addAsync(e));
    }

    @Override
    public RFuture<Boolean> addAllAsync(Collection<? extends V> c) {
        return runCommand(() -> redissonObject.addAllAsync(c));
    }

    @Override
    public RFuture<List<V>> readSortAsync(SortOrder order) {
        return runCommand(() -> redissonObject.readSortAsync(order));
    }

    @Override
    public RFuture<List<V>> readSortAsync(SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSortAsync(order, offset, count));
    }

    @Override
    public RFuture<List<V>> readSortAsync(String byPattern, SortOrder order) {
        return runCommand(() -> redissonObject.readSortAsync(byPattern, order));
    }

    @Override
    public RFuture<List<V>> readSortAsync(String byPattern, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSortAsync(byPattern, order, offset, count));
    }

    @Override
    public <T> RFuture<Collection<T>> readSortAsync(String byPattern, List<String> getPatterns, SortOrder order) {
        return runCommand(() -> redissonObject.readSortAsync(byPattern, getPatterns, order));
    }

    @Override
    public <T> RFuture<Collection<T>> readSortAsync(String byPattern, List<String> getPatterns, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSortAsync(byPattern, getPatterns, order, offset, count));
    }

    @Override
    public RFuture<Integer> sortToAsync(String destName, SortOrder order) {
        return runCommand(() -> redissonObject.sortToAsync(destName, order));
    }

    @Override
    public RFuture<Integer> sortToAsync(String destName, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.sortToAsync(destName, order, offset, count));
    }

    @Override
    public RFuture<Integer> sortToAsync(String destName, String byPattern, SortOrder order) {
        return runCommand(() -> redissonObject.sortToAsync(destName, byPattern, order));
    }

    @Override
    public RFuture<Integer> sortToAsync(String destName, String byPattern, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.sortToAsync(destName, byPattern, order, offset, count));
    }

    @Override
    public RFuture<Integer> sortToAsync(String destName, String byPattern, List<String> getPatterns, SortOrder order) {
        return runCommand(() -> redissonObject.sortToAsync(destName, byPattern, getPatterns, order));
    }

    @Override
    public RFuture<Integer> sortToAsync(String destName, String byPattern, List<String> getPatterns, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.sortToAsync(destName, byPattern, getPatterns, order, offset, count));
    }

    @Override
    public List<V> readSort(SortOrder order) {
        return runCommand(() -> redissonObject.readSort(order));
    }

    @Override
    public List<V> readSort(SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSort(order, offset, count));
    }

    @Override
    public List<V> readSort(String byPattern, SortOrder order) {
        return runCommand(() -> redissonObject.readSort(byPattern, order));
    }

    @Override
    public List<V> readSort(String byPattern, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSort(byPattern, order, offset, count));
    }

    @Override
    public <T> Collection<T> readSort(String byPattern, List<String> getPatterns, SortOrder order) {
        return runCommand(() -> redissonObject.readSort(byPattern, getPatterns, order));
    }

    @Override
    public <T> Collection<T> readSort(String byPattern, List<String> getPatterns, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSort(byPattern, getPatterns, order, offset, count));
    }

    @Override
    public int sortTo(String destName, SortOrder order) {
        return runCommand(() -> redissonObject.sortTo(destName, order));
    }

    @Override
    public int sortTo(String destName, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.sortTo(destName, order, offset, count));
    }

    @Override
    public int sortTo(String destName, String byPattern, SortOrder order) {
        return runCommand(() -> redissonObject.sortTo(destName, byPattern, order));
    }

    @Override
    public int sortTo(String destName, String byPattern, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.sortTo(destName, byPattern, order, offset, count));
    }

    @Override
    public int sortTo(String destName, String byPattern, List<String> getPatterns, SortOrder order) {
        return runCommand(() -> redissonObject.sortTo(destName, byPattern, getPatterns, order));
    }

    @Override
    public int sortTo(String destName, String byPattern, List<String> getPatterns, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.sortTo(destName, byPattern, getPatterns, order, offset, count));
    }

    @Override
    public List<V> get(int... indexes) {
        return runCommand(() -> redissonObject.get(indexes));
    }

    @Override
    public <KOut, VOut> RCollectionMapReduce<V, KOut, VOut> mapReduce() {
        return runCommand(() -> redissonObject.mapReduce());
    }

    @Override
    public int addAfter(V elementToFind, V element) {
        return runCommand(() -> redissonObject.addAfter(elementToFind, element));
    }

    @Override
    public int addBefore(V elementToFind, V element) {
        return runCommand(() -> redissonObject.addBefore(elementToFind, element));
    }

    @Override
    public void fastSet(int index, V element) {
        runCommand(() -> { redissonObject.fastSet(index, element); return null; });

    }

    @Override
    public RList<V> subList(int fromIndex, int toIndex) {
        return runCommand(() -> redissonObject.subList(fromIndex, toIndex));
    }

    @Override
    public void fastRemove(int index) {
        runCommand(() -> { redissonObject.fastRemove(index); return null; });
    }

    @Override
    public RFuture<Boolean> addAsync(int index, V element) {
        return runCommand(() -> redissonObject.addAsync(index, element));
    }

    @Override
    public RFuture<Boolean> removeAsync(Object o, int count) {
        return runCommand(() -> redissonObject.removeAsync(o, count));
    }

    @Override
    public RFuture<List<V>> readSortAlphaAsync(SortOrder order) {
        return runCommand(() -> redissonObject.readSortAlphaAsync(order));
    }

    @Override
    public RFuture<List<V>> readSortAlphaAsync(SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSortAlphaAsync(order, offset, count));
    }

    @Override
    public RFuture<List<V>> readSortAlphaAsync(String byPattern, SortOrder order) {
        return runCommand(() -> redissonObject.readSortAlphaAsync(byPattern, order));
    }

    @Override
    public RFuture<List<V>> readSortAlphaAsync(String byPattern, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSortAlphaAsync(byPattern, order, offset, count));
    }

    @Override
    public <T> RFuture<Collection<T>> readSortAlphaAsync(String byPattern, List<String> getPatterns, SortOrder order) {
        return runCommand(() -> redissonObject.readSortAlphaAsync(byPattern, getPatterns, order));
    }

    @Override
    public <T> RFuture<Collection<T>> readSortAlphaAsync(String byPattern, List<String> getPatterns, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSortAlphaAsync(byPattern, getPatterns, order, offset, count));
    }

    @Override
    public List<V> readSortAlpha(SortOrder order) {
        return runCommand(() -> redissonObject.readSortAlpha(order));
    }

    @Override
    public List<V> readSortAlpha(SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSortAlpha(order, offset, count));
    }

    @Override
    public List<V> readSortAlpha(String byPattern, SortOrder order) {
        return runCommand(() -> redissonObject.readSortAlpha(byPattern, order));
    }

    @Override
    public List<V> readSortAlpha(String byPattern, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSortAlpha(byPattern, order, offset, count));
    }

    @Override
    public <T> Collection<T> readSortAlpha(String byPattern, List<String> getPatterns, SortOrder order) {
        return runCommand(() -> redissonObject.readSortAlpha(byPattern, getPatterns, order));
    }

    @Override
    public <T> Collection<T> readSortAlpha(String byPattern, List<String> getPatterns, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSortAlpha(byPattern, getPatterns, order, offset, count));
    }

    @Override
    public boolean remove(Object o, int count) {
        return runCommand(() -> redissonObject.remove(o, count));
    }

    @Override
    public RFuture<List<V>> rangeAsync(int arg0) {
        return runCommand(() -> redissonObject.rangeAsync(arg0));
    }

    @Override
    public RFuture<List<V>> rangeAsync(int arg0, int arg1) {
        return runCommand(() -> redissonObject.rangeAsync(arg0, arg1));
    }

    @Override
    public List<V> range(int arg0) {
        return runCommand(() -> redissonObject.range(arg0));
    }

    @Override
    public List<V> range(int arg0, int arg1) {
        return runCommand(() -> redissonObject.range(arg0, arg1));
    }
}
