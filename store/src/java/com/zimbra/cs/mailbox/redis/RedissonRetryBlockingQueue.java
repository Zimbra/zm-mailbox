/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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
package com.zimbra.cs.mailbox.redis;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RBlockingQueue;
import org.redisson.api.RFuture;

public class RedissonRetryBlockingQueue<V> extends RedissonRetryExpirable<RBlockingQueue<V>> implements RBlockingQueue<V> {

    public RedissonRetryBlockingQueue(RedissonInitializer<RBlockingQueue<V>> bucketInitializer, RedissonRetryClient client) {
        super(bucketInitializer, client);
    }

    @Override
    public boolean add(V e) {
        return runCommand(() -> redissonObject.add(e));
    }

    @Override
    public boolean offer(V e) {
        return runCommand(() -> redissonObject.offer(e));
    }

    @Override
    public void put(V e) {
        runCommand(() -> { redissonObject.put(e); return null; });
    }

    @Override
    public boolean offer(V e, long timeout, TimeUnit unit) {
        return runCommand(() -> redissonObject.offer(e, timeout, unit));
    }

    @Override
    public V take() {
        return runCommand(() -> redissonObject.take());
    }

    @Override
    public V poll(long timeout, TimeUnit unit) {
        return runCommand(() -> redissonObject.poll(timeout, unit));
    }

    @Override
    public int remainingCapacity() {
        return runCommand(() -> redissonObject.remainingCapacity());
    }

    @Override
    public boolean remove(Object o) {
        return runCommand(() -> redissonObject.remove(o));
    }

    @Override
    public boolean contains(Object o) {
        return runCommand(() -> redissonObject.contains(o));
    }

    @Override
    public int drainTo(Collection<? super V> c) {
        return runCommand(() -> redissonObject.drainTo(c));
    }

    @Override
    public int drainTo(Collection<? super V> c, int maxElements) {
        return runCommand(() -> redissonObject.drainTo(c, maxElements));
    }

    @Override
    public V remove() {
        return runCommand(() -> redissonObject.remove());
    }

    @Override
    public V poll() {
        return runCommand(() -> redissonObject.poll());
    }

    @Override
    public V element() {
        return runCommand(() -> redissonObject.element());
    }

    @Override
    public V peek() {
        return runCommand(() -> redissonObject.peek());
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
    public boolean containsAll(Collection<?> c) {
        return runCommand(() -> redissonObject.containsAll(c));
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        return runCommand(() -> redissonObject.addAll(c));
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
    public void clear() {
        runCommand(() -> { redissonObject.clear(); return null; });
    }

    @Override
    public V pollLastAndOfferFirstTo(String queueName) {
        return runCommand(() -> redissonObject.pollLastAndOfferFirstTo(queueName));
    }

    @Override
    public List<V> readAll() {
        return runCommand(() -> redissonObject.readAll());
    }

    @Override
    public RFuture<V> peekAsync() {
        return runCommand(() -> redissonObject.peekAsync());
    }

    @Override
    public RFuture<V> pollAsync() {
        return runCommand(() -> redissonObject.pollAsync());
    }

    @Override
    public RFuture<Boolean> offerAsync(V e) {
        return runCommand(() -> redissonObject.offerAsync(e));
    }

    @Override
    public RFuture<V> pollLastAndOfferFirstToAsync(String queueName) {
        return runCommand(() -> redissonObject.pollLastAndOfferFirstToAsync(queueName));
    }

    @Override
    public RFuture<List<V>> readAllAsync() {
        return runCommand(() -> redissonObject.readAllAsync());
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
    public RFuture<V> pollFromAnyAsync(long timeout, TimeUnit unit, String... queueNames) {
        return runCommand(() -> redissonObject.pollFromAnyAsync(timeout, unit, queueNames));
    }

    @Override
    public RFuture<Integer> drainToAsync(Collection<? super V> c, int maxElements) {
        return runCommand(() -> redissonObject.drainToAsync(c, maxElements));
    }

    @Override
    public RFuture<Integer> drainToAsync(Collection<? super V> c) {
        return runCommand(() -> redissonObject.drainToAsync(c));
    }

    @Override
    public RFuture<V> pollLastAndOfferFirstToAsync(String queueName, long timeout, TimeUnit unit) {
        return runCommand(() -> redissonObject.pollLastAndOfferFirstToAsync(queueName, timeout, unit));
    }

    @Override
    public RFuture<V> takeLastAndOfferFirstToAsync(String queueName) {
        return runCommand(() -> redissonObject.takeLastAndOfferFirstToAsync(queueName));
    }

    @Override
    public RFuture<V> pollAsync(long timeout, TimeUnit unit) {
        return runCommand(() -> redissonObject.pollAsync(timeout, unit));
    }

    @Override
    public RFuture<V> takeAsync() {
        return runCommand(() -> redissonObject.takeAsync());
    }

    @Override
    public RFuture<Void> putAsync(V e) {
        return runCommand(() -> redissonObject.putAsync(e));
    }

    @Override
    public V pollFromAny(long timeout, TimeUnit unit, String... queueNames) {
        return runCommand(() -> redissonObject.pollFromAny(timeout, unit, queueNames));
    }

    @Override
    public V pollLastAndOfferFirstTo(String queueName, long timeout, TimeUnit unit) {
        return runCommand(() -> redissonObject.pollLastAndOfferFirstTo(queueName, timeout, unit));
    }

    @Override
    public V takeLastAndOfferFirstTo(String queueName) {
        return runCommand(() -> redissonObject.takeLastAndOfferFirstTo(queueName));
    }

}
