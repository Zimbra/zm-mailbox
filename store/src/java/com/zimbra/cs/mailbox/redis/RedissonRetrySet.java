package com.zimbra.cs.mailbox.redis;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.redisson.api.RCountDownLatch;
import org.redisson.api.RFuture;
import org.redisson.api.RLock;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RSet;
import org.redisson.api.SortOrder;
import org.redisson.api.mapreduce.RCollectionMapReduce;

public class RedissonRetrySet<V> extends RedissonRetryExpirable<RSet<V>> implements RSet<V> {

    public RedissonRetrySet(RedissonInitializer<RSet<V>> initializer, RedissonRetryClient client) {
        super(initializer, client);
    }

    @Override
    public int size() {
        return runCommand(() -> redissonObject.size());
    }

    @Override
    public boolean isEmpty() {
        return runCommand(() -> redissonObject.isEmpty());
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
    public boolean add(V e) {
        return runCommand(() -> redissonObject.add(e));
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
    public boolean retainAll(Collection<?> c) {
        return runCommand(() -> redissonObject.retainAll(c));
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return runCommand(() -> redissonObject.removeAll(c));
    }

    @Override
    public void clear() {
        runCommand(() -> { redissonObject.clear(); return null; });
    }

    @Override
    public RFuture<Set<V>> removeRandomAsync(int amount) {
        return runCommand(() -> redissonObject.removeRandomAsync(amount));
    }

    @Override
    public RFuture<V> removeRandomAsync() {
        return runCommand(() -> redissonObject.removeRandomAsync());
    }

    @Override
    public RFuture<V> randomAsync() {
        return runCommand(() -> redissonObject.randomAsync());
    }

    @Override
    public RFuture<Set<V>> randomAsync(int count) {
        return runCommand(() -> redissonObject.randomAsync(count));
    }

    @Override
    public RFuture<Boolean> moveAsync(String destination, V member) {
        return runCommand(() -> redissonObject.moveAsync(destination, member));
    }

    @Override
    public RFuture<Set<V>> readAllAsync() {
        return runCommand(() -> redissonObject.readAllAsync());
    }

    @Override
    public RFuture<Integer> unionAsync(String... names) {
        return runCommand(() -> redissonObject.unionAsync(names));
    }

    @Override
    public RFuture<Set<V>> readUnionAsync(String... names) {
        return runCommand(() -> redissonObject.readUnionAsync(names));
    }

    @Override
    public RFuture<Integer> diffAsync(String... names) {
        return runCommand(() -> redissonObject.diffAsync(names));
    }

    @Override
    public RFuture<Set<V>> readDiffAsync(String... names) {
        return runCommand(() -> redissonObject.readDiffAsync(names));
    }

    @Override
    public RFuture<Integer> intersectionAsync(String... names) {
        return runCommand(() -> redissonObject.intersectionAsync(names));
    }

    @Override
    public RFuture<Set<V>> readIntersectionAsync(String... names) {
        return runCommand(() -> redissonObject.readIntersectionAsync(names));
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
    public RFuture<Set<V>> readSortAsync(SortOrder order) {
        return runCommand(() -> redissonObject.readSortAsync(order));
    }

    @Override
    public RFuture<Set<V>> readSortAsync(SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSortAsync(order, offset, count));
    }

    @Override
    public RFuture<Set<V>> readSortAsync(String byPattern, SortOrder order) {
        return runCommand(() -> redissonObject.readSortAsync(byPattern, order));
    }

    @Override
    public RFuture<Set<V>> readSortAsync(String byPattern, SortOrder order, int offset, int count) {
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
    public RFuture<Set<V>> readSortAlphaAsync(SortOrder order) {
        return runCommand(() -> redissonObject.readSortAlphaAsync(order));
    }

    @Override
    public RFuture<Set<V>> readSortAlphaAsync(SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSortAlphaAsync(order, offset, count));
    }

    @Override
    public RFuture<Set<V>> readSortAlphaAsync(String byPattern, SortOrder order) {
        return runCommand(() -> redissonObject.readSortAlphaAsync(byPattern, order));
    }

    @Override
    public RFuture<Set<V>> readSortAlphaAsync(String byPattern, SortOrder order, int offset, int count) {
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
    public Set<V> readSort(SortOrder order) {
        return runCommand(() -> redissonObject.readSort(order));
    }

    @Override
    public Set<V> readSort(SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSort(order, offset, count));
    }

    @Override
    public Set<V> readSort(String byPattern, SortOrder order) {
        return runCommand(() -> redissonObject.readSort(byPattern, order));
    }

    @Override
    public Set<V> readSort(String byPattern, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSort(byPattern, order, offset, count));
    }

    @Override
    public <T> Collection<T> readSort(String byPattern, List<String> getPatterns, SortOrder order) {
        return runCommand(() -> redissonObject.readSort(byPattern, getPatterns, order));
    }

    @Override
    public <T> Collection<T> readSort(String byPattern,
            List<String> getPatterns, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSort(byPattern, getPatterns, order, offset, count));
    }

    @Override
    public Set<V> readSortAlpha(SortOrder order) {
        return runCommand(() -> redissonObject.readSortAlpha(order));
    }

    @Override
    public Set<V> readSortAlpha(SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSortAlpha(order, offset, count));
    }

    @Override
    public Set<V> readSortAlpha(String byPattern, SortOrder order) {
        return runCommand(() -> redissonObject.readSortAlpha(byPattern, order));
    }

    @Override
    public Set<V> readSortAlpha(String byPattern, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSortAlpha(byPattern, order, offset, count));
    }

    @Override
    public <T> Collection<T> readSortAlpha(String byPattern,List<String> getPatterns, SortOrder order) {
        return runCommand(() -> redissonObject.readSortAlpha(byPattern, getPatterns, order));
    }

    @Override
    public <T> Collection<T> readSortAlpha(String byPattern, List<String> getPatterns, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSortAlpha(byPattern, getPatterns, order, offset, count));
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
    public RLock getLock(V value) {
        return runCommand(() -> redissonObject.getLock(value));
    }

    @Override
    public Iterator<V> iterator(int count) {
        return runCommand(() -> redissonObject.iterator(count));
    }

    @Override
    public Iterator<V> iterator(String pattern, int count) {
        return runCommand(() -> redissonObject.iterator(pattern, count));
    }

    @Override
    public Iterator<V> iterator(String pattern) {
        return runCommand(() -> redissonObject.iterator(pattern));
    }

    @Override
    public <KOut, VOut> RCollectionMapReduce<V, KOut, VOut> mapReduce() {
        return runCommand(() -> redissonObject.mapReduce());
    }

    @Override
    public Set<V> removeRandom(int amount) {
        return runCommand(() -> redissonObject.removeRandom(amount));
    }

    @Override
    public V removeRandom() {
        return runCommand(() -> redissonObject.removeRandom());
    }

    @Override
    public V random() {
        return runCommand(() -> redissonObject.random());
    }

    @Override
    public Set<V> random(int count) {
        return runCommand(() -> redissonObject.random(count));
    }

    @Override
    public boolean move(String destination, V member) {
        return runCommand(() -> redissonObject.move(destination, member));
    }

    @Override
    public Set<V> readAll() {
        return runCommand(() -> redissonObject.readAll());
    }

    @Override
    public int union(String... names) {
        return runCommand(() -> redissonObject.union(names));
    }

    @Override
    public Set<V> readUnion(String... names) {
        return runCommand(() -> redissonObject.readUnion(names));
    }

    @Override
    public int diff(String... names) {
        return runCommand(() -> redissonObject.diff(names));
    }

    @Override
    public Set<V> readDiff(String... names) {
        return runCommand(() -> redissonObject.readDiff(names));
    }

    @Override
    public int intersection(String... names) {
        return runCommand(() -> redissonObject.intersection(names));
    }

    @Override
    public Set<V> readIntersection(String... names) {
        return runCommand(() -> redissonObject.readIntersection(names));
    }

    @Override
    public RCountDownLatch getCountDownLatch(V arg0) {
        return runCommand(() -> redissonObject.getCountDownLatch(arg0));
    }

    @Override
    public RLock getFairLock(V arg0) {
        return runCommand(() -> redissonObject.getFairLock(arg0));
    }

    @Override
    public RPermitExpirableSemaphore getPermitExpirableSemaphore(V arg0) {
        return runCommand(() -> redissonObject.getPermitExpirableSemaphore(arg0));
    }

    @Override
    public RReadWriteLock getReadWriteLock(V arg0) {
        return runCommand(() -> redissonObject.getReadWriteLock(arg0));
    }

    @Override
    public RSemaphore getSemaphore(V arg0) {
        return runCommand(() -> redissonObject.getSemaphore(arg0));
    }

    @Override
    public Stream<V> stream(int arg0) {
        return runCommand(() -> redissonObject.stream(arg0));
    }

    @Override
    public Stream<V> stream(String arg0) {
        return runCommand(() -> redissonObject.stream(arg0));
    }

    @Override
    public Stream<V> stream(String arg0, int arg1) {
        return runCommand(() -> redissonObject.stream(arg0, arg1));
    }
}
