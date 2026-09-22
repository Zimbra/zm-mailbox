/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.stats.Counter;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.accesscontrol.PermissionCache.CachedPermission;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PermCacheManagerTest {

    private static Right cacheableRightA;

    private static Right cacheableRightB;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        RightManager rm = RightManager.getInstance();
        // Pick two distinct cacheable user rights with distinct cache indices.
        List<Right> cacheable = new ArrayList<Right>();
        for (Right r : rm.getAllUserRights().values()) {
            if (r.isCacheable()) {
                cacheable.add(r);
            }
        }
        for (Right r : rm.getAllAdminRights().values()) {
            if (r.isCacheable()) {
                cacheable.add(r);
            }
        }
        Assert.assertTrue("need at least 2 cacheable rights for tests", cacheable.size() >= 2);
        cacheableRightA = cacheable.get(0);
        // find a right with a different cache index than A
        for (int i = 1; i < cacheable.size(); i++) {
            if (cacheable.get(i).getCacheIndex() != cacheableRightA.getCacheIndex()) {
                cacheableRightB = cacheable.get(i);
                break;
            }
        }
        Assert.assertNotNull("need two distinct cache indices", cacheableRightB);
    }

    /** Reset the singleton's shared hit-rate Counter and clear the whole cache before each test. */
    @Before
    public void resetSingletonState() throws Exception {
        PermCacheManager pcm = PermCacheManager.getInstance();
        pcm.invalidateCache();
        Field f = PermCacheManager.class.getDeclaredField("hitRate");
        f.setAccessible(true);
        Counter counter = (Counter) f.get(pcm);
        counter.reset();
    }
    
    // private static final AccessManager am = AccessManager.getInstance();
    
    // do tests in "atomic" blocks so the assertions will work well
    private static final Object LOCK = new Object();

    private final class MockAccount extends Account {

        private String id = LdapUtil.generateUUID();

        private String name;
        
        private MockAccount(String name) {
            super(name, null, null, null, null);
            this.name = name;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        @Override 
        public String getName() {
            return name;
        }
        
        @Override
        public boolean isIsAdminAccount() {
            return false;
        }
        
        @Override 
        public boolean isIsDelegatedAdminAccount() {
            return false;
        }
    }
    
    private class TestThread extends Thread {
        private Thread mainThread;

        private String id;

        private MockAccount[] targets;

        private MockAccount[] grantees;

        private List<Right> rights;

        private Random random = new Random(System.currentTimeMillis());
        
        TestThread(Thread mainThread, String id, MockAccount[] targets, MockAccount[] grantees, List<Right> rights) {
            this.mainThread = mainThread;
            this.id = id;
            this.targets = targets;
            this.grantees = grantees;
            this.rights = rights;
        }
        
        public void run() {
            try {
                execute();
            } catch (Exception e) {
                System.out.println("Thread " + id + " encountered exception");
                e.printStackTrace();
                mainThread.interrupt();
                System.exit(1);
            }
        }
        
        public void execute() throws Exception {
            PermCacheManager pcm = PermCacheManager.getInstance();
            
            int numIters = 0;
            while (true) {
                numIters++;
                for (int i = 0; i < targets.length; i++) {
                    // System.out.println("Thread " + id + " testing target " + targets[i].getName());
                    long memBefore = calculateMemoryUsage();
                    long startTime = System.currentTimeMillis();
                    
                    for (int j = 0; j < grantees.length; j++) {
                        for (int k = 0; k < rights.size(); k++) {
                            Right right = rights.get(k);
                            String cacheKey = PermissionCache.buildCacheKey(grantees[j], right, false);
                            
                            for (CachedPermission cachedPerm : CachedPermission.values()) {
                                if (cachedPerm == CachedPermission.NOT_CACHED) {
                                    continue;
                                }

                                synchronized (LOCK) {
                                    pcm.put(targets[i], cacheKey, right, cachedPerm);
                                    CachedPermission perm = pcm.get(targets[i], cacheKey, right);
                                    
                                    if (cachedPerm != perm) {
                                        System.out.println();
                                    }
                                    Assert.assertEquals(cachedPerm, perm);
                                }
                            }
                        }
                    }
                    
                    synchronized (LOCK) {
                        pcm.invalidateCache(targets[i]);
                    }
                    
                    long memAfter = calculateMemoryUsage();
                    long endTime = System.currentTimeMillis();
                    long elapsedTime = endTime - startTime;
                    
                    int numOpers = 2; // one put, one get
                    int numResults = CachedPermission.values().length - 1;  // not testing NO_CACHED
                    long numCacheOperations = numOpers * numResults * rights.size() * grantees.length;
                    float mSecsPerCacheOper = elapsedTime / numCacheOperations;
                    System.out.println("Thread " + id + " iter = " + numIters + ": Target " + i + 
                            ": mem delta = " + (memAfter - memBefore) + "K" + 
                            ", mem in use = " + (memAfter / 1024) + "M" +
                            ", time = " + elapsedTime + "ms" + 
                            ", num opers = " + numCacheOperations +
                            ", time per cache oper = " + mSecsPerCacheOper + "ms");
                    
                }
                                   
                synchronized (LOCK) {
                    if (numIters % 2 == 0) {
                        synchronized (LOCK) {
                            System.out.println("Thread " + id + " iter = " + numIters + " invalidating entire cache");
                            pcm.invalidateCache();
                        }
                    }
                }
                
                /*
                if (numIters % 1 == 0) {
                    System.out.println("Thread " + id + " done " + numIters + " iterations: " + calculateMemoryUsage());
                }
                */
            }
        }
    }
    
    /*
     * returns memory usage in Kbytes
     */
    private static long calculateMemoryUsage() {
        System.gc(); System.gc(); System.gc(); System.gc();
        System.gc(); System.gc(); System.gc(); System.gc();
        System.gc(); System.gc(); System.gc(); System.gc();
        System.gc(); System.gc(); System.gc(); System.gc();
        long bytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        return bytes / (1024);
    }
    
    private List<Right> getAllCacheableRights() throws Exception {

        List<Right> cacheableRights = new ArrayList<Right>();
        /*
        cacheableRights.add(User.R_invite);
        cacheableRights.add(User.R_viewFreeBusy);
        cacheableRights.add(User.R_sendAs);
        cacheableRights.add(User.R_loginAs);
        cacheableRights.add(User.R_sendToDistList);
        cacheableRights.add(User.R_viewDistList);
        cacheableRights.add(Admin.R_adminLoginAs);
        */
        
        for (Right r : RightManager.getInstance().getAllUserRights().values()) {
            if (r.isCacheable()) {
                cacheableRights.add(r);
            }
        }

        for (Right r : RightManager.getInstance().getAllAdminRights().values()) {
            if (r.isCacheable()) {
                cacheableRights.add(r);
            }
        }
        
        return cacheableRights;
    }

    // @Test
    // must be first test - avoid having to call Counter.reset() for the test
    public void testHitRate() throws Exception {
       
        MockAccount target = new MockAccount("target");
        MockAccount grantee = new MockAccount("grantee");
        Right right = User.R_loginAs;
        
        PermCacheManager pcm = PermCacheManager.getInstance();
        String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);
        
        CachedPermission cachedPerm;
        
        cachedPerm = pcm.get(target, cacheKey, right);
        Assert.assertEquals(CachedPermission.NOT_CACHED, cachedPerm);
        Assert.assertEquals(0.0, PermissionCache.getHitRate(), 0);
        
        CachedPermission expectedPerm = CachedPermission.ALLOWED;
        pcm.put(target, cacheKey, right, expectedPerm);
        cachedPerm = pcm.get(target, cacheKey, right);
        Assert.assertEquals(expectedPerm, cachedPerm);
        Assert.assertEquals(50.0, PermissionCache.getHitRate(), 0);
        
        for (int i = 0; i < 8; i++) {
            cachedPerm = pcm.get(target, cacheKey, right);
            Assert.assertEquals(expectedPerm, cachedPerm);
        }
        
        Assert.assertEquals(90.0, PermissionCache.getHitRate(), 0);
    }
    
    // @Test
    public void testCachedPerms() throws Exception {
        
        MockAccount target = new MockAccount("target");
        MockAccount grantee = new MockAccount("grantee");

        List<Right> cacheableRights = getAllCacheableRights();
        
        PermCacheManager pcm = PermCacheManager.getInstance();
        
        for (int rightIdx = 0; rightIdx < cacheableRights.size(); rightIdx++) {
            Right right = cacheableRights.get(rightIdx);
            // System.out.println("Testing " + right.getName());
            
            String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);
            
            CachedPermission cachedPerm = pcm.get(target, cacheKey, right);
            Assert.assertEquals(CachedPermission.NOT_CACHED, cachedPerm);
            
            for (CachedPermission expectedPerm : CachedPermission.values()) {
                if (expectedPerm == CachedPermission.NOT_CACHED) {
                    continue;
                }

                // System.out.println("Testing " + expectedPerm.name());
                pcm.put(target, cacheKey, right, expectedPerm);
                cachedPerm = pcm.get(target, cacheKey, right);
                Assert.assertEquals(expectedPerm, cachedPerm);
                
                // verify other rights are not affected
                for (int otherRightIdx = 0; otherRightIdx < cacheableRights.size(); otherRightIdx++) {
                    if (otherRightIdx == rightIdx) {
                        continue;
                    }

                    Right otherRight = cacheableRights.get(otherRightIdx);

                    // last right in CachedPermission
                    CachedPermission expectedPermForOtherRights;
                    if (otherRightIdx < rightIdx) {
                        // last cached perm for the right in the test
                        expectedPermForOtherRights = CachedPermission.DENIED;
                    } else {
                        expectedPermForOtherRights = CachedPermission.NOT_CACHED;
                    }
                    
                    CachedPermission permOtherRight = pcm.get(target, cacheKey, otherRight);
                    Assert.assertEquals(expectedPermForOtherRights, permOtherRight);
                }
            }
        }
    }

    // @Test
    public void testMaxAge() throws Exception {
        int aclCacheTargetMaxage = 1;
        LC.acl_cache_target_maxage.setDefault(String.valueOf(aclCacheTargetMaxage));
        
        MockAccount target = new MockAccount("target");
        MockAccount grantee = new MockAccount("grantee");
        Right right = User.R_loginAs;
        
        PermCacheManager pcm = PermCacheManager.getInstance();
        String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);
        
        CachedPermission expectedPerm = CachedPermission.NO_MATCHING_ACL;
        CachedPermission cachedPerm;
        
        pcm.put(target, cacheKey, right, expectedPerm);
        cachedPerm = pcm.get(target, cacheKey, right);
        Assert.assertEquals(expectedPerm, cachedPerm);
        
        // wait for TTL 
        long waitFor = aclCacheTargetMaxage * Constants.MILLIS_PER_MINUTE + 1000; // plus one second for the cusion
        System.out.println("Wait for " + waitFor + " msecs");
        Thread.sleep(waitFor);
        
        cachedPerm = pcm.get(target, cacheKey, right);
        Assert.assertEquals(CachedPermission.NOT_CACHED, cachedPerm);
    }
    
    // @Test
    public void testPermCacheManager() throws Exception {
        
        int aclCacheTargetMaxsize = 10;
        int aclCacheCredentialMaxsize = 10;
        int aclCacheTargetMaxage = 1;
        
        // product default value
        /*
        int acl_cache_max_targets = 1024;               
        int acl_cache_max_entries_per_target = 512;
        int acl_cache_target_maxage = 15;
        */
        
        int numThreads = 10; // 100;
        
        float targetFactor = 1.5F;  // multiple of cache target size

        LC.acl_cache_target_maxsize.setDefault(String.valueOf(aclCacheTargetMaxsize));
        LC.acl_cache_credential_maxsize.setDefault(String.valueOf(aclCacheCredentialMaxsize));
        LC.acl_cache_target_maxage.setDefault(String.valueOf(aclCacheTargetMaxage));

        int numTargets = (int) (aclCacheTargetMaxsize * targetFactor);
        MockAccount[] targets = new MockAccount[numTargets];
        for (int i = 0; i < numTargets; i++) {
            String name = "T" + String.valueOf(i + 1);
            targets[i] = new MockAccount(name);
        }
        
        int numGrantees = numTargets;  // assume everyone is accessing everyone's account
        MockAccount[] grantees = new MockAccount[numGrantees];
        for (int i = 0; i < numGrantees; i++) {
            String name = "G" + String.valueOf(i + 1);
            grantees[i] = new MockAccount(name);
        }
        
        List<Right> rights = getAllCacheableRights();
            
        TestThread[] threads = new TestThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            String threadId = String.valueOf(i + 1);
            threads[i] = new TestThread(Thread.currentThread(), threadId, targets, grantees, rights);
            threads[i].start();
        }

        Thread.currentThread().join();
    }
 
    @Test
    public void noOp() throws Exception {
    }

    private double currentHitRate() throws Exception {
        Field f = PermCacheManager.class.getDeclaredField("hitRate");
        f.setAccessible(true);
        Counter counter = (Counter) f.get(PermCacheManager.getInstance());
        return counter.getAverage();
    }

    /**
     * get() on a target that has never been cached must return NOT_CACHED (kills L172/L173
     * NULL_RETURN + VoidMethodCall on the miss path) and must register a miss in the hit rate
     * (kills L172 VoidMethodCallMutator on updateHitRate(false)).
     */
    @Test
    public void getOnUncachedTargetReturnsNotCached() throws Exception {
        PermCacheManager pcm = PermCacheManager.getInstance();
        MockAccount target = new MockAccount("uncachedTarget");
        String cacheKey = PermissionCache.buildCacheKey(new MockAccount("g"), cacheableRightA, false);

        CachedPermission perm = pcm.get(target, cacheKey, cacheableRightA);
        Assert.assertEquals(CachedPermission.NOT_CACHED, perm);
        // one miss recorded -> average 0.0 (kills updateHitRate(false) removal + ternary negate)
        Assert.assertEquals(0.0, currentHitRate(), 0.0);
    }

    /**
     * put() then get() must return the EXACT permission stored, for every CachedPermission value.
     * Kills the CachedPerms.get/put bit-math mutations (L271,L275,L276,L287,L291,L292), the inner
     * PermCache.get NULL_RETURN/NEGATE (L232,L234), put VoidMethodCall (L183,L243), and getPermCache
     * createIfNotExist branch (L142).
     */
    @Test
    public void putThenGetReturnsExactPermissionForEveryValue() throws Exception {
        PermCacheManager pcm = PermCacheManager.getInstance();
        MockAccount target = new MockAccount("roundTripTarget");
        String cacheKey = PermissionCache.buildCacheKey(new MockAccount("g"), cacheableRightA, false);

        for (CachedPermission expected : CachedPermission.values()) {
            if (expected == CachedPermission.NOT_CACHED) {
                continue;
            }
            pcm.put(target, cacheKey, cacheableRightA, expected);
            CachedPermission actual = pcm.get(target, cacheKey, cacheableRightA);
            Assert.assertEquals("round trip for " + expected, expected, actual);
        }
    }

    /**
     * A put for one right must not corrupt the cache slot for another right in the same byte array.
     * Kills CachedPerms bit-clear/mask mutations (L291 byteForRight &= MASKS_CLEAR, L292 |=) and the
     * index/mod math (L271,L273,L287,L289).
     */
    @Test
    public void putForOneRightDoesNotAffectAnother() throws Exception {
        PermCacheManager pcm = PermCacheManager.getInstance();
        MockAccount target = new MockAccount("twoRightsTarget");
        String cacheKey = PermissionCache.buildCacheKey(new MockAccount("g"), cacheableRightA, false);

        pcm.put(target, cacheKey, cacheableRightA, CachedPermission.ALLOWED);
        pcm.put(target, cacheKey, cacheableRightB, CachedPermission.DENIED);

        Assert.assertEquals(CachedPermission.ALLOWED, pcm.get(target, cacheKey, cacheableRightA));
        Assert.assertEquals(CachedPermission.DENIED, pcm.get(target, cacheKey, cacheableRightB));

        // overwrite A; B must remain DENIED
        pcm.put(target, cacheKey, cacheableRightA, CachedPermission.NO_MATCHING_ACL);
        Assert.assertEquals(CachedPermission.NO_MATCHING_ACL, pcm.get(target, cacheKey, cacheableRightA));
        Assert.assertEquals(CachedPermission.DENIED, pcm.get(target, cacheKey, cacheableRightB));
    }

    /**
     * get() for a credential that was never put, on a target bucket that DOES exist, must return
     * NOT_CACHED (kills inner PermCache.get NULL_RETURN L233/L234 and L232 NEGATE) and record a miss.
     */
    @Test
    public void getUnknownCredentialInExistingBucketReturnsNotCached() throws Exception {
        PermCacheManager pcm = PermCacheManager.getInstance();
        MockAccount target = new MockAccount("bucketTarget");
        String knownKey = PermissionCache.buildCacheKey(new MockAccount("g1"), cacheableRightA, false);
        String unknownKey = PermissionCache.buildCacheKey(new MockAccount("g2"), cacheableRightA, false);

        pcm.put(target, knownKey, cacheableRightA, CachedPermission.ALLOWED);
        // bucket exists, but this credential was never cached
        CachedPermission perm = pcm.get(target, unknownKey, cacheableRightA);
        Assert.assertEquals(CachedPermission.NOT_CACHED, perm);
    }

    /**
     * A single hit (put then get of a real value) must yield hit rate 100.0; a single miss must yield
     * 0.0. Kills updateHitRate ternary negate (L162), increment removal (L162 VoidMethodCall) and the
     * L177 NEGATE/VoidMethodCall on the hit path (NOT_CACHED != perm).
     */
    @Test
    public void hitRateReflectsHitAndMissDistinctly() throws Exception {
        PermCacheManager pcm = PermCacheManager.getInstance();
        MockAccount target = new MockAccount("hitRateTarget");
        String cacheKey = PermissionCache.buildCacheKey(new MockAccount("g"), cacheableRightA, false);

        // exactly one hit
        pcm.put(target, cacheKey, cacheableRightA, CachedPermission.ALLOWED);
        Assert.assertEquals(CachedPermission.ALLOWED, pcm.get(target, cacheKey, cacheableRightA));
        Assert.assertEquals(100.0, currentHitRate(), 0.0);

        // now a miss -> average of {100, 0} = 50.0
        MockAccount missTarget = new MockAccount("missTarget");
        Assert.assertEquals(CachedPermission.NOT_CACHED, pcm.get(missTarget, cacheKey, cacheableRightA));
        Assert.assertEquals(50.0, currentHitRate(), 0.0);
    }

    /**
     * invalidateCache() (no arg) must clear ALL targets so subsequent gets are NOT_CACHED.
     * Kills L94 VoidMethodCall (targetCache.clear removed).
     */
    @Test
    public void invalidateCacheClearsAllTargets() throws Exception {
        PermCacheManager pcm = PermCacheManager.getInstance();
        MockAccount targetA = new MockAccount("clearA");
        MockAccount targetB = new MockAccount("clearB");
        String cacheKey = PermissionCache.buildCacheKey(new MockAccount("g"), cacheableRightA, false);

        pcm.put(targetA, cacheKey, cacheableRightA, CachedPermission.ALLOWED);
        pcm.put(targetB, cacheKey, cacheableRightA, CachedPermission.DENIED);
        Assert.assertEquals(CachedPermission.ALLOWED, pcm.get(targetA, cacheKey, cacheableRightA));
        Assert.assertEquals(CachedPermission.DENIED, pcm.get(targetB, cacheKey, cacheableRightA));

        pcm.invalidateCache();

        Assert.assertEquals(CachedPermission.NOT_CACHED, pcm.get(targetA, cacheKey, cacheableRightA));
        Assert.assertEquals(CachedPermission.NOT_CACHED, pcm.get(targetB, cacheKey, cacheableRightA));
    }

    /**
     * invalidateCache(target) for a NON-inheritable target (an Account) must reset ONLY that target's
     * bucket, leaving other targets cached. Kills L124 NEGATE (invalidateAll branch) and L125
     * VoidMethodCall (the targeted reset). If the conditional were negated, the whole cache would be
     * cleared and targetB would also become NOT_CACHED.
     */
    @Test
    public void invalidateCacheForAccountResetsOnlyThatTarget() throws Exception {
        PermCacheManager pcm = PermCacheManager.getInstance();
        MockAccount targetA = new MockAccount("invA");
        MockAccount targetB = new MockAccount("invB");
        String cacheKey = PermissionCache.buildCacheKey(new MockAccount("g"), cacheableRightA, false);

        pcm.put(targetA, cacheKey, cacheableRightA, CachedPermission.ALLOWED);
        pcm.put(targetB, cacheKey, cacheableRightA, CachedPermission.DENIED);

        pcm.invalidateCache(targetA);

        // targetA reset (L125 reset() executed) -> NOT_CACHED
        Assert.assertEquals(CachedPermission.NOT_CACHED, pcm.get(targetA, cacheKey, cacheableRightA));
        // targetB untouched because account is non-inheritable -> still DENIED (L124 not negated)
        Assert.assertEquals(CachedPermission.DENIED, pcm.get(targetB, cacheKey, cacheableRightA));
    }

    /**
     * getMaxPermArraySize must equal (maxCacheIndex / RIGHTS_PER_BYTE) + 1. Computed independently
     * from Right.getMaxCacheIndex() so any MathMutator on L266 (/ -> *, + -> -) changes the production
     * result but not this expectation. Also verifies the array is big enough to index the max right.
     */
    @Test
    public void getMaxPermArraySizeExact() throws Exception {
        int maxIndex = Right.getMaxCacheIndex();
        int expected = (maxIndex / 2) + 1;
        Assert.assertEquals(expected, PermCacheManager.CachedPerms.getMaxPermArraySize());
        // the largest cache index must be addressable within the array
        Assert.assertTrue(maxIndex / 2 < PermCacheManager.CachedPerms.getMaxPermArraySize());
    }

    /**
     * Direct test of CachedPerms.get/put bit packing on a hand-built byte array. Drives the index/mod
     * math (L271,L273,L287,L289), the mask selection (L275/L277/L279 in get, L291/L292 in put) and the
     * NOT_CACHED default (L282). Uses both rights so both nibbles of a byte are exercised.
     */
    @Test
    public void cachedPermsBitPackingDirect() throws Exception {
        int size = PermCacheManager.CachedPerms.getMaxPermArraySize();
        byte[] perms = new byte[size];

        // initially every right reads NOT_CACHED
        Assert.assertEquals(CachedPermission.NOT_CACHED,
                PermCacheManager.CachedPerms.get(perms, cacheableRightA));

        PermCacheManager.CachedPerms.put(perms, cacheableRightA, CachedPermission.ALLOWED);
        Assert.assertEquals(CachedPermission.ALLOWED,
                PermCacheManager.CachedPerms.get(perms, cacheableRightA));

        PermCacheManager.CachedPerms.put(perms, cacheableRightB, CachedPermission.NO_MATCHING_ACL);
        Assert.assertEquals(CachedPermission.NO_MATCHING_ACL,
                PermCacheManager.CachedPerms.get(perms, cacheableRightB));
        // A unaffected by B's write
        Assert.assertEquals(CachedPermission.ALLOWED,
                PermCacheManager.CachedPerms.get(perms, cacheableRightA));

        // overwrite A to DENIED; clear-mask (L291) must wipe the old ALLOWED bits first
        PermCacheManager.CachedPerms.put(perms, cacheableRightA, CachedPermission.DENIED);
        Assert.assertEquals(CachedPermission.DENIED,
                PermCacheManager.CachedPerms.get(perms, cacheableRightA));
        Assert.assertEquals(CachedPermission.NO_MATCHING_ACL,
                PermCacheManager.CachedPerms.get(perms, cacheableRightB));
    }

    /**
     * getCacheKey: a NamedEntry target must be keyed by getId() (L155 NEGATE / L156 EMPTY_RETURN).
     * Two accounts with the SAME name but different ids must map to different buckets, so caching for
     * one does not leak into the other. If getCacheKey returned getLabel() (name) or "", the two would
     * collide and the second get would wrongly return the first's value.
     */
    @Test
    public void getCacheKeyUsesIdNotName() throws Exception {
        PermCacheManager pcm = PermCacheManager.getInstance();
        MockAccount a1 = new MockAccount("samename");
        MockAccount a2 = new MockAccount("samename");
        Assert.assertNotEquals(a1.getId(), a2.getId());
        String cacheKey = PermissionCache.buildCacheKey(new MockAccount("g"), cacheableRightA, false);

        pcm.put(a1, cacheKey, cacheableRightA, CachedPermission.ALLOWED);
        // a2 has same name but different id -> distinct bucket -> not cached
        Assert.assertEquals(CachedPermission.NOT_CACHED, pcm.get(a2, cacheKey, cacheableRightA));
        // a1 still returns its value
        Assert.assertEquals(CachedPermission.ALLOWED, pcm.get(a1, cacheKey, cacheableRightA));
    }
}
