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
 *
 *
 */

package com.zimbra.cs.mailbox.redis;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.redisson.api.RFuture;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.SortOrder;
import org.redisson.api.mapreduce.RCollectionMapReduce;
import org.redisson.client.protocol.ScoredEntry;

public class RedissonRetryScoredSortedSet<V> extends RedissonRetryExpirable<RScoredSortedSet<V>> implements RScoredSortedSet<V> {

    public RedissonRetryScoredSortedSet(RedissonInitializer<RScoredSortedSet<V>> initializer, RedissonRetryClient client) {
        super(initializer, client);
    }

    @Override
    public V first() {
        return runCommand(() -> redissonObject.first());
    }

    @Override
    public V last() {
        return runCommand(() -> redissonObject.last());
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
    public boolean remove(Object o) {
        return runCommand(() -> redissonObject.remove(o));
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return runCommand(() -> redissonObject.containsAll(c));
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
    public <KOut, VOut> RCollectionMapReduce<V, KOut, VOut> mapReduce() {
        return runCommand(() -> redissonObject.mapReduce());
    }

    @Override
    public Collection<V> readAll() {
        return runCommand(() -> redissonObject.readAll());
    }

    @Override
    public RFuture<Collection<V>> readAllAsync() {
        return runCommand(() -> redissonObject.readAllAsync());
    }

    @Override
    public RFuture<V> pollLastFromAnyAsync(long timeout, TimeUnit unit, String... queueNames) {
        return runCommand(() -> redissonObject.pollLastFromAnyAsync(timeout, unit, queueNames));
    }

    @Override
    public RFuture<V> pollFirstFromAnyAsync(long timeout, TimeUnit unit, String... queueNames) {
        return runCommand(() -> redissonObject.pollFirstFromAnyAsync(timeout, unit, queueNames));
    }

    @Override
    public RFuture<V> pollFirstAsync(long timeout, TimeUnit unit) {
        return runCommand(() -> redissonObject.pollFirstAsync(timeout, unit));
    }

    @Override
    public RFuture<V> pollLastAsync(long timeout, TimeUnit unit) {
        return runCommand(() -> redissonObject.pollLastAsync(timeout, unit));
    }

    @Override
    public RFuture<Collection<V>> pollFirstAsync(int count) {
        return runCommand(() -> redissonObject.pollFirstAsync(count));
    }

    @Override
    public RFuture<Collection<V>> pollLastAsync(int count) {
        return runCommand(() -> redissonObject.pollLastAsync(count));
    }

    @Override
    public RFuture<V> pollFirstAsync() {
        return runCommand(() -> redissonObject.pollFirstAsync());
    }

    @Override
    public RFuture<V> pollLastAsync() {
        return runCommand(() -> redissonObject.pollLastAsync());
    }

    @Override
    public RFuture<V> firstAsync() {
        return runCommand(() -> redissonObject.firstAsync());
    }

    @Override
    public RFuture<V> lastAsync() {
        return runCommand(() -> redissonObject.lastAsync());
    }

    @Override
    public RFuture<Double> firstScoreAsync() {
        return runCommand(() -> redissonObject.firstScoreAsync());
    }

    @Override
    public RFuture<Double> lastScoreAsync() {
        return runCommand(() -> redissonObject.lastScoreAsync());
    }

    @Override
    public RFuture<Integer> addAllAsync(Map<V, Double> objects) {
        return runCommand(() -> redissonObject.addAllAsync(objects));
    }

    @Override
    public RFuture<Integer> removeRangeByScoreAsync(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive) {
        return runCommand(() -> redissonObject.removeRangeByScoreAsync(startScore, startScoreInclusive, endScore, endScoreInclusive));
    }

    @Override
    public RFuture<Integer> removeRangeByRankAsync(int startIndex, int endIndex) {
        return runCommand(() -> redissonObject.removeRangeByRankAsync(startIndex, endIndex));
    }

    @Override
    public RFuture<Integer> rankAsync(V o) {
        return runCommand(() -> redissonObject.rankAsync(o));
    }

    @Override
    public RFuture<Integer> revRankAsync(V o) {
        return runCommand(() -> redissonObject.revRankAsync(o));
    }

    @Override
    public RFuture<Double> getScoreAsync(V o) {
        return runCommand(() -> redissonObject.getScoreAsync(o));
    }

    @Override
    public RFuture<Boolean> addAsync(double score, V object) {
        return runCommand(() -> redissonObject.addAsync(score, object));
    }

    @Override
    public RFuture<Integer> addAndGetRankAsync(double score, V object) {
        return runCommand(() -> redissonObject.addAndGetRankAsync(score, object));
    }

    @Override
    public RFuture<Integer> addAndGetRevRankAsync(double score, V object) {
        return runCommand(() -> redissonObject.addAndGetRevRankAsync(score, object));
    }

    @Override
    public RFuture<Boolean> tryAddAsync(double score, V object) {
        return runCommand(() -> redissonObject.tryAddAsync(score, object));
    }

    @Override
    public RFuture<Boolean> removeAsync(V o) {
        return runCommand(() -> redissonObject.removeAsync(o));
    }

    @Override
    public RFuture<Integer> sizeAsync() {
        return runCommand(() -> redissonObject.sizeAsync());
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
    public RFuture<Boolean> removeAllAsync(Collection<?> c) {
        return runCommand(() -> redissonObject.removeAllAsync(c));
    }

    @Override
    public RFuture<Boolean> retainAllAsync(Collection<?> c) {
        return runCommand(() -> redissonObject.retainAllAsync(c));
    }

    @Override
    public RFuture<Double> addScoreAsync(V element, Number value) {
        return runCommand(() -> redissonObject.addScoreAsync(element, value));
    }

    @Override
    public RFuture<Integer> addScoreAndGetRevRankAsync(V object, Number value) {
        return runCommand(() -> redissonObject.addScoreAndGetRevRankAsync(object, value));
    }

    @Override
    public RFuture<Integer> addScoreAndGetRankAsync(V object, Number value) {
        return runCommand(() -> redissonObject.addScoreAndGetRankAsync(object, value));
    }

    @Override
    public RFuture<Collection<V>> valueRangeAsync(int startIndex, int endIndex) {
        return runCommand(() -> redissonObject.valueRangeAsync(startIndex, endIndex));
    }

    @Override
    public RFuture<Collection<V>> valueRangeReversedAsync(int startIndex, int endIndex) {
        return runCommand(() -> redissonObject.valueRangeReversedAsync(startIndex, endIndex));
    }

    @Override
    public RFuture<Collection<ScoredEntry<V>>> entryRangeAsync(int startIndex, int endIndex) {
        return runCommand(() -> redissonObject.entryRangeAsync(startIndex, endIndex));
    }

    @Override
    public RFuture<Collection<ScoredEntry<V>>> entryRangeReversedAsync(int startIndex, int endIndex) {
        return runCommand(() -> redissonObject.entryRangeReversedAsync(startIndex, endIndex));
    }

    @Override
    public RFuture<Collection<V>> valueRangeAsync(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive) {
        return runCommand(() -> redissonObject.valueRangeAsync(startScore, startScoreInclusive, endScore, endScoreInclusive));
    }

    @Override
    public RFuture<Collection<V>> valueRangeReversedAsync(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive) {
        return runCommand(() -> redissonObject.valueRangeReversedAsync(startScore, startScoreInclusive, endScore, endScoreInclusive));
    }

    @Override
    public RFuture<Collection<ScoredEntry<V>>> entryRangeAsync( double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive) {
        return runCommand(() -> redissonObject.entryRangeAsync(startScore, startScoreInclusive, endScore, endScoreInclusive));
    }

    @Override
    public RFuture<Collection<V>> valueRangeAsync(double startScore,
            boolean startScoreInclusive, double endScore,
            boolean endScoreInclusive, int offset, int count) {
        return runCommand(() -> redissonObject.valueRangeAsync(startScore, startScoreInclusive, endScore, endScoreInclusive, offset, count));
    }

    @Override
    public RFuture<Collection<V>> valueRangeReversedAsync(double startScore,
            boolean startScoreInclusive, double endScore,
            boolean endScoreInclusive, int offset, int count) {
        return runCommand(() -> redissonObject.valueRangeReversedAsync(startScore, startScoreInclusive, endScore, endScoreInclusive, offset, count));
    }

    @Override
    public RFuture<Collection<ScoredEntry<V>>> entryRangeAsync(
            double startScore, boolean startScoreInclusive, double endScore,
            boolean endScoreInclusive, int offset, int count) {
        return runCommand(() -> redissonObject.entryRangeAsync(startScore, startScoreInclusive, endScore, endScoreInclusive, offset, count));
    }

    @Override
    public RFuture<Collection<ScoredEntry<V>>> entryRangeReversedAsync(
            double startScore, boolean startScoreInclusive, double endScore,
            boolean endScoreInclusive) {
        return runCommand(() -> redissonObject.entryRangeReversedAsync(startScore, startScoreInclusive, endScore, endScoreInclusive));
    }

    @Override
    public RFuture<Collection<ScoredEntry<V>>> entryRangeReversedAsync(
            double startScore, boolean startScoreInclusive, double endScore,
            boolean endScoreInclusive, int offset, int count) {
        return runCommand(() -> redissonObject.entryRangeReversedAsync(startScore, startScoreInclusive, endScore, endScoreInclusive, offset, count));
    }

    @Override
    public RFuture<Integer> countAsync(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive) {
        return runCommand(() -> redissonObject.countAsync(startScore, startScoreInclusive, endScore, endScoreInclusive));
    }

    @Override
    public RFuture<Integer> intersectionAsync(String... names) {
        return runCommand(() -> redissonObject.intersectionAsync(names));
    }

    @Override
    public RFuture<Integer> intersectionAsync(
            org.redisson.api.RScoredSortedSet.Aggregate aggregate,
            String... names) {
        return runCommand(() -> redissonObject.intersectionAsync(aggregate, names));
    }

    @Override
    public RFuture<Integer> intersectionAsync(Map<String, Double> nameWithWeight) {
        return runCommand(() -> redissonObject.intersectionAsync(nameWithWeight));
    }

    @Override
    public RFuture<Integer> intersectionAsync(
            org.redisson.api.RScoredSortedSet.Aggregate aggregate,
            Map<String, Double> nameWithWeight) {
        return runCommand(() -> redissonObject.intersectionAsync(aggregate, nameWithWeight));
    }

    @Override
    public RFuture<Integer> unionAsync(String... names) {
        return runCommand(() -> redissonObject.unionAsync(names));
    }

    @Override
    public RFuture<Integer> unionAsync(
            org.redisson.api.RScoredSortedSet.Aggregate aggregate,
            String... names) {
        return runCommand(() -> redissonObject.unionAsync(aggregate, names));
    }

    @Override
    public RFuture<Integer> unionAsync(Map<String, Double> nameWithWeight) {
        return runCommand(() -> redissonObject.unionAsync(nameWithWeight));
    }

    @Override
    public RFuture<Integer> unionAsync(
            org.redisson.api.RScoredSortedSet.Aggregate aggregate,
            Map<String, Double> nameWithWeight) {
        return runCommand(() -> redissonObject.unionAsync(aggregate, nameWithWeight));
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
    public <T> RFuture<Collection<T>> readSortAsync(String byPattern,
            List<String> getPatterns, SortOrder order, int offset, int count) {
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
    public <T> RFuture<Collection<T>> readSortAlphaAsync(String byPattern,
            List<String> getPatterns, SortOrder order, int offset, int count) {
        return runCommand(() -> redissonObject.readSortAlphaAsync(byPattern, getPatterns, order, offset, count));
    }

    @Override
    public RFuture<Integer> sortToAsync(String destName, SortOrder order) {
        return runCommand(() -> redissonObject.sortToAsync(destName, order));
    }

    @Override
    public RFuture<Integer> sortToAsync(String destName, SortOrder order,
            int offset, int count) {
        return runCommand(() -> redissonObject.sortToAsync(destName, order));
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
    public RFuture<Integer> sortToAsync(String destName, String byPattern,
            List<String> getPatterns, SortOrder order) {
        return runCommand(() -> redissonObject.sortToAsync(destName, byPattern, getPatterns, order));
    }

    @Override
    public RFuture<Integer> sortToAsync(String destName, String byPattern,
            List<String> getPatterns, SortOrder order, int offset, int count) {
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
    public <T> Collection<T> readSort(String byPattern, List<String> getPatterns, SortOrder order, int offset, int count) {
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
    public <T> Collection<T> readSortAlpha(String byPattern, List<String> getPatterns, SortOrder order) {
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
    public V pollLastFromAny(long timeout, TimeUnit unit, String... queueNames) {
        return runCommand(() -> redissonObject.pollLastFromAny(timeout, unit, queueNames));
    }

    @Override
    public V pollFirstFromAny(long timeout, TimeUnit unit, String... queueNames) {
        return runCommand(() -> redissonObject.pollFirstFromAny(timeout, unit, queueNames));
    }

    @Override
    public V pollFirst(long timeout, TimeUnit unit) {
        return runCommand(() -> redissonObject.pollFirst(timeout, unit));
    }

    @Override
    public V pollLast(long timeout, TimeUnit unit) {
        return runCommand(() -> redissonObject.pollLast(timeout, unit));
    }

    @Override
    public Collection<V> pollFirst(int count) {
        return runCommand(() -> redissonObject.pollFirst(count));
    }

    @Override
    public Collection<V> pollLast(int count) {
        return runCommand(() -> redissonObject.pollLast(count));
    }

    @Override
    public V pollFirst() {
        return runCommand(() -> redissonObject.pollFirst());
    }

    @Override
    public V pollLast() {
        return runCommand(() -> redissonObject.pollLast());
    }

    @Override
    public Double firstScore() {
        return runCommand(() -> redissonObject.firstScore());
    }

    @Override
    public Double lastScore() {
        return runCommand(() -> redissonObject.lastScore());
    }

    @Override
    public int addAll(Map<V, Double> objects) {
        return runCommand(() -> redissonObject.addAll(objects));
    }

    @Override
    public int removeRangeByScore(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive) {
        return runCommand(() -> redissonObject.removeRangeByScore(startScore, startScoreInclusive, endScore, endScoreInclusive));
    }

    @Override
    public int removeRangeByRank(int startIndex, int endIndex) {
        return runCommand(() -> redissonObject.removeRangeByRank(startIndex, endIndex));
    }

    @Override
    public Integer rank(V o) {
        return runCommand(() -> redissonObject.rank(o));
    }

    @Override
    public Integer revRank(V o) {
        return runCommand(() -> redissonObject.revRank(o));
    }

    @Override
    public Double getScore(V o) {
        return runCommand(() -> redissonObject.getScore(o));
    }

    @Override
    public boolean add(double score, V object) {
        return runCommand(() -> redissonObject.add(score, object));
    }

    @Override
    public Integer addAndGetRank(double score, V object) {
        return runCommand(() -> redissonObject.addAndGetRank(score, object));
    }

    @Override
    public Integer addAndGetRevRank(double score, V object) {
        return runCommand(() -> redissonObject.addAndGetRevRank(score, object));
    }

    @Override
    public boolean tryAdd(double score, V object) {
        return runCommand(() -> redissonObject.tryAdd(score, object));
    }

    @Override
    public Iterator<V> iterator(String pattern) {
        return runCommand(() -> redissonObject.iterator(pattern));
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
    public Double addScore(V element, Number value) {
        return runCommand(() -> redissonObject.addScore(element, value));
    }

    @Override
    public Integer addScoreAndGetRank(V object, Number value) {
        return runCommand(() -> redissonObject.addScoreAndGetRank(object, value));
    }

    @Override
    public Integer addScoreAndGetRevRank(V object, Number value) {
        return runCommand(() -> redissonObject.addScoreAndGetRevRank(object, value));
    }

    @Override
    public Collection<V> valueRange(int startIndex, int endIndex) {
        return runCommand(() -> redissonObject.valueRange(startIndex, endIndex));
    }

    @Override
    public Collection<V> valueRangeReversed(int startIndex, int endIndex) {
        return runCommand(() -> redissonObject.valueRangeReversed(startIndex, endIndex));
    }

    @Override
    public Collection<ScoredEntry<V>> entryRange(int startIndex, int endIndex) {
        return runCommand(() -> redissonObject.entryRange(startIndex, endIndex));
    }

    @Override
    public Collection<ScoredEntry<V>> entryRangeReversed(int startIndex, int endIndex) {
        return runCommand(() -> redissonObject.entryRangeReversed(startIndex, endIndex));
    }

    @Override
    public Collection<V> valueRange(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive) {
        return runCommand(() -> redissonObject.valueRange(startScore, startScoreInclusive, endScore, endScoreInclusive));
    }

    @Override
    public Collection<V> valueRangeReversed(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive) {
        return runCommand(() -> redissonObject.valueRangeReversed(startScore, startScoreInclusive, endScore, endScoreInclusive));
    }

    @Override
    public Collection<ScoredEntry<V>> entryRange(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive) {
        return runCommand(() -> redissonObject.entryRange(startScore, startScoreInclusive, endScore, endScoreInclusive));
    }

    @Override
    public Collection<V> valueRange(double startScore,
            boolean startScoreInclusive, double endScore,
            boolean endScoreInclusive, int offset, int count) {
        return runCommand(() -> redissonObject.valueRange(startScore, startScoreInclusive, endScore, endScoreInclusive, offset, count));
    }

    @Override
    public Collection<V> valueRangeReversed(double startScore,
            boolean startScoreInclusive, double endScore,
            boolean endScoreInclusive, int offset, int count) {
        return runCommand(() -> redissonObject.valueRangeReversed(startScore, startScoreInclusive, endScore, endScoreInclusive, offset, count));
    }

    @Override
    public Collection<ScoredEntry<V>> entryRange(double startScore,
            boolean startScoreInclusive, double endScore,
            boolean endScoreInclusive, int offset, int count) {
        return runCommand(() -> redissonObject.entryRange(startScore, startScoreInclusive, endScore, endScoreInclusive, offset, count));
    }

    @Override
    public Collection<ScoredEntry<V>> entryRangeReversed(double startScore,
            boolean startScoreInclusive, double endScore,
            boolean endScoreInclusive) {
        return runCommand(() -> redissonObject.entryRangeReversed(startScore, startScoreInclusive, endScore, endScoreInclusive));
    }

    @Override
    public Collection<ScoredEntry<V>> entryRangeReversed(double startScore,
            boolean startScoreInclusive, double endScore,
            boolean endScoreInclusive, int offset, int count) {
        return runCommand(() -> redissonObject.entryRangeReversed(startScore, startScoreInclusive, endScore, endScoreInclusive, offset, count));
    }

    @Override
    public int count(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive) {
        return runCommand(() -> redissonObject.count(startScore, startScoreInclusive, endScore, endScoreInclusive));
    }

    @Override
    public int intersection(String... names) {
        return runCommand(() -> redissonObject.intersection(names));
    }

    @Override
    public int intersection( org.redisson.api.RScoredSortedSet.Aggregate aggregate, String... names) {
        return runCommand(() -> redissonObject.intersection(aggregate, names));
    }

    @Override
    public int intersection(Map<String, Double> nameWithWeight) {
        return runCommand(() -> redissonObject.intersection(nameWithWeight));
    }

    @Override
    public int intersection( org.redisson.api.RScoredSortedSet.Aggregate aggregate, Map<String, Double> nameWithWeight) {
        return runCommand(() -> redissonObject.intersection(aggregate, nameWithWeight));
    }

    @Override
    public int union(String... names) {
        return runCommand(() -> redissonObject.union(names));
    }

    @Override
    public int union(org.redisson.api.RScoredSortedSet.Aggregate aggregate, String... names) {
        return runCommand(() -> redissonObject.union(aggregate, names));
    }

    @Override
    public int union(Map<String, Double> nameWithWeight) {
        return runCommand(() -> redissonObject.union(nameWithWeight));
    }

    @Override
    public int union(org.redisson.api.RScoredSortedSet.Aggregate aggregate, Map<String, Double> nameWithWeight) {
        return runCommand(() -> redissonObject.union(aggregate, nameWithWeight));
    }

    @Override
    public RFuture<V> takeFirstAsync() {
        return runCommand(() -> redissonObject.takeFirstAsync());
    }

    @Override
    public RFuture<V> takeLastAsync() {
        return runCommand(() -> redissonObject.takeLastAsync());
    }

    @Override
    public Stream<V> stream() {
        return runCommand(() -> redissonObject.stream());
    }

    @Override
    public Stream<V> stream(String arg0) {
        return runCommand(() -> redissonObject.stream(arg0));
    }

    @Override
    public Stream<V> stream(int arg0) {
        return runCommand(() -> redissonObject.stream(arg0));
    }

    @Override
    public Stream<V> stream(String arg0, int arg1) {
        return runCommand(() -> redissonObject.stream(arg0, arg1));
    }

    @Override
    public V takeFirst() {
        return runCommand(() -> redissonObject.takeFirst());
    }

    @Override
    public V takeLast() {
        return runCommand(() -> redissonObject.takeLast());
    }
}
