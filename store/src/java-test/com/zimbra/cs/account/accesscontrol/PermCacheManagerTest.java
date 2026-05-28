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

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.accesscontrol.PermissionCache.CachedPermission;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.ldap.LdapUtil;

public class PermCacheManagerTest {
    
    // private static final AccessManager am = AccessManager.getInstance();
    
    // do tests in "atomic" blocks so the assertions will work well
    private static final Object lock = new Object();
    
    private class MockAccount extends Account {
        
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
        Thread mainThread;
        String id;
        MockAccount[] targets;
        MockAccount[] grantees;
        List<Right> rights;
        Random random = new Random(System.currentTimeMillis());
        
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
                                if (cachedPerm == CachedPermission.NOT_CACHED)
                                    continue;
                                
                                synchronized (lock) {
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
                    
                    synchronized (lock) {
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
                                   
                synchronized (lock) {
                    if (numIters % 2 == 0) {
                        synchronized (lock) {
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
            if (r.isCacheable())
                cacheableRights.add(r);
        }

        for (Right r : RightManager.getInstance().getAllAdminRights().values()) {
            if (r.isCacheable())
                cacheableRights.add(r);
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
                if (expectedPerm == CachedPermission.NOT_CACHED)
                    continue;
                
                // System.out.println("Testing " + expectedPerm.name());
                pcm.put(target, cacheKey, right, expectedPerm);
                cachedPerm = pcm.get(target, cacheKey, right);
                Assert.assertEquals(expectedPerm, cachedPerm);
                
                // verify other rights are not affected
                for (int otherRightIdx = 0; otherRightIdx < cacheableRights.size(); otherRightIdx++) {
                    if (otherRightIdx == rightIdx)
                        continue;
                    
                    Right otherRight = cacheableRights.get(otherRightIdx);
                    
                    // last right in CachedPermission
                    CachedPermission expectedPermForOtherRights;
                    if (otherRightIdx < rightIdx)
                        expectedPermForOtherRights = CachedPermission.DENIED;  // last cached perm for the right in the test
                    else
                        expectedPermForOtherRights = CachedPermission.NOT_CACHED;
                    
                    CachedPermission permOtherRight = pcm.get(target, cacheKey, otherRight);
                    Assert.assertEquals(expectedPermForOtherRights, permOtherRight);
                }
            }
        }
    }

    // @Test
    public void testMaxAge() throws Exception {
        int acl_cache_target_maxage = 1;
        LC.acl_cache_target_maxage.setDefault(String.valueOf(acl_cache_target_maxage));
        
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
        long waitFor = acl_cache_target_maxage * Constants.MILLIS_PER_MINUTE + 1000; // plus one second for the cusion
        System.out.println("Wait for " + waitFor + " msecs");
        Thread.sleep(waitFor);
        
        cachedPerm = pcm.get(target, cacheKey, right);
        Assert.assertEquals(CachedPermission.NOT_CACHED, cachedPerm);
    }
    
    // @Test
    public void testPermCacheManager() throws Exception {
        
        int acl_cache_target_maxsize = 10;
        int acl_cache_credential_maxsize = 10;
        int acl_cache_target_maxage = 1;
        
        // product default value
        /*
        int acl_cache_max_targets = 1024;               
        int acl_cache_max_entries_per_target = 512;
        int acl_cache_target_maxage = 15;
        */
        
        int numThreads = 10; // 100;
        
        float TARGET_FACTOR = 1.5F;  // multiple of cache target size
        
        LC.acl_cache_target_maxsize.setDefault(String.valueOf(acl_cache_target_maxsize));
        LC.acl_cache_credential_maxsize.setDefault(String.valueOf(acl_cache_credential_maxsize)); 
        LC.acl_cache_target_maxage.setDefault(String.valueOf(acl_cache_target_maxage));
        
        int numTargets = (int)(acl_cache_target_maxsize * TARGET_FACTOR);
        MockAccount[] targets = new MockAccount[numTargets];
        for (int i = 0; i < numTargets; i++) {
            String name = "T" + String.valueOf(i+1);
            targets[i] = new MockAccount(name);
        }
        
        int numGrantees = numTargets;  // assume everyone is accessing everyone's account
        MockAccount[] grantees = new MockAccount[numGrantees];
        for (int i = 0; i < numGrantees; i++) {
            String name = "G" + String.valueOf(i+1);
            grantees[i] = new MockAccount(name);
        }
        
        List<Right> rights = getAllCacheableRights();
            
        TestThread[] threads = new TestThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            String threadId = String.valueOf(i+1);
            threads[i] = new TestThread(Thread.currentThread(), threadId, targets, grantees, rights);
            threads[i].start();
        }

        Thread.currentThread().join();
    }
 
    @Test
    public void noOp() throws Exception {
    }

    @Test
    public void cacheGet_singlePermission_miss_returnsNotCached() throws Exception {
        MockAccount target = new MockAccount("target");
        MockAccount grantee = new MockAccount("grantee");
        Right right = User.R_loginAs;

        PermCacheManager pcm = PermCacheManager.getInstance();
        String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);

        CachedPermission result = pcm.get(target, cacheKey, right);

        Assert.assertEquals(CachedPermission.NOT_CACHED, result);
    }

    @Test
    public void cachePut_singlePermission_allowed_storesAndRetrieves() throws Exception {
        MockAccount target = new MockAccount("target");
        MockAccount grantee = new MockAccount("grantee");
        Right right = User.R_loginAs;

        PermCacheManager pcm = PermCacheManager.getInstance();
        String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);

        pcm.put(target, cacheKey, right, CachedPermission.ALLOWED);
        CachedPermission result = pcm.get(target, cacheKey, right);

        Assert.assertEquals(CachedPermission.ALLOWED, result);
    }

    @Test
    public void cachePut_singlePermission_denied_storesAndRetrieves() throws Exception {
        MockAccount target = new MockAccount("target");
        MockAccount grantee = new MockAccount("grantee");
        Right right = User.R_loginAs;

        PermCacheManager pcm = PermCacheManager.getInstance();
        String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);

        pcm.put(target, cacheKey, right, CachedPermission.DENIED);
        CachedPermission result = pcm.get(target, cacheKey, right);

        Assert.assertEquals(CachedPermission.DENIED, result);
    }

    @Test
    public void cachePut_singlePermission_noMatchingAcl_storesAndRetrieves() throws Exception {
        MockAccount target = new MockAccount("target");
        MockAccount grantee = new MockAccount("grantee");
        Right right = User.R_loginAs;

        PermCacheManager pcm = PermCacheManager.getInstance();
        String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);

        pcm.put(target, cacheKey, right, CachedPermission.NO_MATCHING_ACL);
        CachedPermission result = pcm.get(target, cacheKey, right);

        Assert.assertEquals(CachedPermission.NO_MATCHING_ACL, result);
    }

    @Test
    public void cacheMultiplePermissions_differentRights_independentlyStored() throws Exception {
        MockAccount target = new MockAccount("target");
        MockAccount grantee1 = new MockAccount("grantee1");
        MockAccount grantee2 = new MockAccount("grantee2");
        Right right1 = User.R_loginAs;
        Right right2 = User.R_viewFreeBusy;

        PermCacheManager pcm = PermCacheManager.getInstance();
        String cacheKey1 = PermissionCache.buildCacheKey(grantee1, right1, false);
        String cacheKey2 = PermissionCache.buildCacheKey(grantee2, right2, false);

        pcm.put(target, cacheKey1, right1, CachedPermission.ALLOWED);
        pcm.put(target, cacheKey2, right2, CachedPermission.DENIED);

        Assert.assertEquals(CachedPermission.ALLOWED, pcm.get(target, cacheKey1, right1));
        Assert.assertEquals(CachedPermission.DENIED, pcm.get(target, cacheKey2, right2));
    }

    @Test
    public void cacheInvalidateAll_afterPut_cacheClearedCompletelyAndReturnsMiss() throws Exception {
        MockAccount target = new MockAccount("target");
        MockAccount grantee = new MockAccount("grantee");
        Right right = User.R_loginAs;

        PermCacheManager pcm = PermCacheManager.getInstance();
        String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);

        // Store a value
        pcm.put(target, cacheKey, right, CachedPermission.ALLOWED);
        Assert.assertEquals(CachedPermission.ALLOWED, pcm.get(target, cacheKey, right));

        // Invalidate entire cache
        pcm.invalidateCache();

        // Verify cache miss
        Assert.assertEquals(CachedPermission.NOT_CACHED, pcm.get(target, cacheKey, right));
    }

    @Test
    public void cacheInvalidateTarget_afterPut_targetCacheClearedReturnsMiss() throws Exception {
        MockAccount target1 = new MockAccount("target1");
        MockAccount target2 = new MockAccount("target2");
        MockAccount grantee = new MockAccount("grantee");
        Right right = User.R_loginAs;

        PermCacheManager pcm = PermCacheManager.getInstance();
        String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);

        // Store values for two different targets
        pcm.put(target1, cacheKey, right, CachedPermission.ALLOWED);
        pcm.put(target2, cacheKey, right, CachedPermission.DENIED);

        Assert.assertEquals(CachedPermission.ALLOWED, pcm.get(target1, cacheKey, right));
        Assert.assertEquals(CachedPermission.DENIED, pcm.get(target2, cacheKey, right));

        // Invalidate only target1
        pcm.invalidateCache(target1);

        // Verify target1 cache is cleared
        Assert.assertEquals(CachedPermission.NOT_CACHED, pcm.get(target1, cacheKey, right));
        // Verify target2 cache is NOT affected (if target can't be inherited from)
        // Otherwise entire cache is cleared
    }

    @Test
    public void cachePutMultipleWithSameCredential_sameTargetDifferentRights_allIndependentlyStored() throws Exception {
        MockAccount target = new MockAccount("target");
        MockAccount grantee = new MockAccount("grantee");
        Right right1 = User.R_loginAs;
        Right right2 = User.R_viewFreeBusy;
        Right right3 = User.R_sendAs;

        PermCacheManager pcm = PermCacheManager.getInstance();
        String cacheKey = PermissionCache.buildCacheKey(grantee, right1, false);

        // Store different permissions for different rights with same credential
        pcm.put(target, cacheKey, right1, CachedPermission.ALLOWED);
        pcm.put(target, cacheKey, right2, CachedPermission.DENIED);
        pcm.put(target, cacheKey, right3, CachedPermission.NO_MATCHING_ACL);

        // Verify all are stored independently
        Assert.assertEquals(CachedPermission.ALLOWED, pcm.get(target, cacheKey, right1));
        Assert.assertEquals(CachedPermission.DENIED, pcm.get(target, cacheKey, right2));
        Assert.assertEquals(CachedPermission.NO_MATCHING_ACL, pcm.get(target, cacheKey, right3));
    }

    @Test
    public void cacheWorkflow_putGetInvalidateGet_stateTransitionsCorrectly() throws Exception {
        MockAccount target = new MockAccount("target");
        MockAccount grantee = new MockAccount("grantee");
        Right right = User.R_loginAs;

        PermCacheManager pcm = PermCacheManager.getInstance();
        String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);

        // Initial miss
        Assert.assertEquals(CachedPermission.NOT_CACHED, pcm.get(target, cacheKey, right));

        // Store ALLOWED
        pcm.put(target, cacheKey, right, CachedPermission.ALLOWED);
        Assert.assertEquals(CachedPermission.ALLOWED, pcm.get(target, cacheKey, right));

        // Invalidate specific target
        pcm.invalidateCache(target);
        Assert.assertEquals(CachedPermission.NOT_CACHED, pcm.get(target, cacheKey, right));

        // Store DENIED (recovery after invalidation)
        pcm.put(target, cacheKey, right, CachedPermission.DENIED);
        Assert.assertEquals(CachedPermission.DENIED, pcm.get(target, cacheKey, right));
    }

    @Test
    public void cacheMultipleTargets_invalidateAll_allTargetsClearedIndependently() throws Exception {
        MockAccount target1 = new MockAccount("target1");
        MockAccount target2 = new MockAccount("target2");
        MockAccount target3 = new MockAccount("target3");
        MockAccount grantee = new MockAccount("grantee");
        Right right = User.R_loginAs;

        PermCacheManager pcm = PermCacheManager.getInstance();
        String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);

        // Store for all targets
        pcm.put(target1, cacheKey, right, CachedPermission.ALLOWED);
        pcm.put(target2, cacheKey, right, CachedPermission.DENIED);
        pcm.put(target3, cacheKey, right, CachedPermission.NO_MATCHING_ACL);

        // Verify all stored
        Assert.assertEquals(CachedPermission.ALLOWED, pcm.get(target1, cacheKey, right));
        Assert.assertEquals(CachedPermission.DENIED, pcm.get(target2, cacheKey, right));
        Assert.assertEquals(CachedPermission.NO_MATCHING_ACL, pcm.get(target3, cacheKey, right));

        // Invalidate all
        pcm.invalidateCache();

        // Verify all cleared
        Assert.assertEquals(CachedPermission.NOT_CACHED, pcm.get(target1, cacheKey, right));
        Assert.assertEquals(CachedPermission.NOT_CACHED, pcm.get(target2, cacheKey, right));
        Assert.assertEquals(CachedPermission.NOT_CACHED, pcm.get(target3, cacheKey, right));
    }

    @Test
    public void cacheMultipleCredentialsPerTarget_independentlyStored() throws Exception {
        MockAccount target = new MockAccount("target");
        MockAccount grantee1 = new MockAccount("grantee1");
        MockAccount grantee2 = new MockAccount("grantee2");
        Right right = User.R_loginAs;

        PermCacheManager pcm = PermCacheManager.getInstance();
        String cacheKey1 = PermissionCache.buildCacheKey(grantee1, right, false);
        String cacheKey2 = PermissionCache.buildCacheKey(grantee2, right, false);

        // Store different values for same right with different credentials
        pcm.put(target, cacheKey1, right, CachedPermission.ALLOWED);
        pcm.put(target, cacheKey2, right, CachedPermission.DENIED);

        // Verify independent retrieval
        Assert.assertEquals(CachedPermission.ALLOWED, pcm.get(target, cacheKey1, right));
        Assert.assertEquals(CachedPermission.DENIED, pcm.get(target, cacheKey2, right));
    }

    @Test
    public void cacheAllCacheableRights_allStored_independentlyRetrieved() throws Exception {
        MockAccount target = new MockAccount("target");
        MockAccount grantee = new MockAccount("grantee");

        PermCacheManager pcm = PermCacheManager.getInstance();
        List<Right> cacheableRights = getAllCacheableRights();

        // Store one permission per cacheable right
        for (int i = 0; i < cacheableRights.size(); i++) {
            Right right = cacheableRights.get(i);
            String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);

            // Alternate between ALLOWED and DENIED for variety
            CachedPermission perm = (i % 2 == 0) ? CachedPermission.ALLOWED : CachedPermission.DENIED;
            pcm.put(target, cacheKey, right, perm);
        }

        // Verify all retrieved correctly
        for (int i = 0; i < cacheableRights.size(); i++) {
            Right right = cacheableRights.get(i);
            String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);

            CachedPermission expected = (i % 2 == 0) ? CachedPermission.ALLOWED : CachedPermission.DENIED;
            Assert.assertEquals(expected, pcm.get(target, cacheKey, right));
        }
    }
}
