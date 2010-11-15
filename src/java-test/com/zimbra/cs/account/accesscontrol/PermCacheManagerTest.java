/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.accesscontrol;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.accesscontrol.PermissionCache.CachedPermission;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.account.ldap.LdapUtil;

public class PermCacheManagerTest {
    
    private static final AccessManager am = AccessManager.getInstance();
    
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
        Right[] rights;
        Random random = new Random(System.currentTimeMillis());
        
        TestThread(Thread mainThread, String id, MockAccount[] targets, MockAccount[] grantees, Right[] rights) {
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
                        for (int k = 0; k < rights.length; k++) {
                            Right right = rights[k];
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
                    long numCacheOperations = numOpers * numResults * rights.length * grantees.length;
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
    
    // @Test
    public void testCachedPerms() throws Exception {
        
        MockAccount target = new MockAccount("target");
        MockAccount grantee = new MockAccount("grantee");
        
        Right[] rights = new Right[] {
                User.R_invite,
                User.R_viewFreeBusy,
                User.R_sendAs,
                User.R_loginAs,
                User.R_sendToDistList,
                User.R_viewDistList,
                Admin.R_adminLoginAs};
        
        PermCacheManager pcm = PermCacheManager.getInstance();
        
        // last right in CachedPermission
        CachedPermission expectedPermForOtherRights = null; 
        
        for (Right right : rights) {
            System.out.println("Testing " + right.getName());
            
            String cacheKey = PermissionCache.buildCacheKey(grantee, right, false);
            
            for (CachedPermission cachedPerm : CachedPermission.values()) {
                if (cachedPerm == CachedPermission.NOT_CACHED)
                    continue;
                
                pcm.put(target, cacheKey, right, cachedPerm);
                CachedPermission perm = pcm.get(target, cacheKey, right);
                Assert.assertEquals(cachedPerm, perm);
                
                // verify other rights are not affected
                if (expectedPermForOtherRights != null) {
                    for (Right otherRight : rights) {
                        if (otherRight == right)
                            continue;
                        
                        CachedPermission permOtherRight = pcm.get(target, cacheKey, otherRight);
                        Assert.assertEquals(expectedPermForOtherRights, permOtherRight);
                    }
                }
            }
            
            expectedPermForOtherRights = CachedPermission.DENIED; 
        }
    }

    
    @Test
    public void testPermCacheManager() throws Exception {
        
        int acl_cache_max_targets = 10;
        int acl_cache_max_entries_per_target = 10;

        
        // product default value
        /*
        int acl_cache_max_targets = 1024;               
        int acl_cache_max_entries_per_target = 512;
        */
        
        int numThreads = 10; // 100;
        
        float TARGET_FACTOR = 1.5F;  // multiple of cache target size
        
        LC.acl_cache_max_targets.setDefault(String.valueOf(acl_cache_max_targets));
        LC.acl_cache_max_entries_per_target.setDefault(String.valueOf(acl_cache_max_entries_per_target)); 
        
        int numTargets = (int)(acl_cache_max_targets * TARGET_FACTOR);
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
        
        Right[] rights = new Right[] {
                User.R_invite,
                User.R_viewFreeBusy,
                User.R_sendAs,
                User.R_loginAs,
                User.R_sendToDistList,
                User.R_viewDistList,
                Admin.R_adminLoginAs
        };
            
        TestThread[] threads = new TestThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            String threadId = String.valueOf(i+1);
            threads[i] = new TestThread(Thread.currentThread(), threadId, targets, grantees, rights);
            threads[i].start();
        }

        Thread.currentThread().join();
    }
    
}
