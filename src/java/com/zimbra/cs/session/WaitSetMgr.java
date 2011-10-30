/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import com.zimbra.common.account.Key;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.admin.AdminDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.cs.util.Zimbra;

/**
 *
 */
public class WaitSetMgr {
    public static final String ALL_ACCOUNTS_ID_PREFIX = "AllWaitSet-";

    private static final int MAX_WAITSETS_PER_NONADMIN_ACCOUNT = LC.zimbra_waitset_max_per_account.intValueWithinRange(1,Integer.MAX_VALUE);
    private static final TimerTask sSweeper = new TimerTask() {
        @Override
        public void run() {
            try {
                WaitSetMgr.sweep();
            } catch (OutOfMemoryError e) {
                Zimbra.halt("out of memory", e);
            } catch (Throwable e) {
                if (e instanceof OutOfMemoryError)
                    Zimbra.halt("Caught out of memory error", e);
                ZimbraLog.session.warn("Caught exception in WaitSetMgr timer", e);
            }
        }
    };

    private static final HashMap<String, WaitSetBase> sWaitSets = new HashMap<String, WaitSetBase>();

    private static final HashMap<String /*AccountId*/, List<String /*WaitSetId*/>> sWaitSetsByAccountId = new HashMap<String, List<String>>();

    private static final int WAITSET_SWEEP_DELAY = 1000 * 60; // once every minute

    private static final int WAITSET_TIMEOUT = 1000 * 60 * 20; // 20min

    /**
     * Create a new WaitSet, optionally specifying an initial set of accounts
     * to start listening on
     *
     * WaitSets are stored in a serverwide cache and are stamped with a last-accessed-time,
     * therefore callers should *not* cache the WaitSet pointer beyond a few seconds they
     * should instead use the lookup() API to fetch WaitSets between calls
     *
     * @param ownerAccountId Account ID of the owner/creator
     * @param allowMultiple If FALSE, then the create fails if there is already a waitset for this account
     * @param defaultInterest
     * @param add
     * @return A Pair(WaitSetID, List<WaitSetError>)
     * @throws ServiceException
     */
    public static Pair<String, List<WaitSetError>> create(String ownerAccountId, boolean allowMultiple,
            Set<MailItem.Type> defaultInterest, boolean allAccts, List<WaitSetAccount> add) throws ServiceException {
        synchronized(sWaitSets) {
            if (!allowMultiple) {
                List<String> list = sWaitSetsByAccountId.get(ownerAccountId);
                if (list != null) {
                    if (list.size() >= MAX_WAITSETS_PER_NONADMIN_ACCOUNT) {
                        // find the least-recently-used
                        long oldestTime = Long.MAX_VALUE;
                        String oldestId = null;
                        for (String wsid : list) {
                            WaitSetBase ws = lookupInternal(wsid);
                            long time = ws.getLastAccessedTime();
                            if (time < oldestTime) {
                                oldestTime = time;
                                oldestId = wsid;
                            }
                        }
                        destroy(null, ownerAccountId, oldestId);
                    }
                }
            }

            // generate an appropriate ID for the new WaitSet
            String id;
            if (allAccts) {
//                id = ALL_ACCOUNTS_ID_PREFIX+sWaitSetNumber;
                id = ALL_ACCOUNTS_ID_PREFIX+LdapUtil.generateUUID();
            } else {
                id = "WaitSet-"+LdapUtil.generateUUID();
            }

            // create the proper kind of WaitSet
            WaitSetBase ws;
            List<WaitSetError> errors = null;
            if (allAccts) {
                AllAccountsWaitSet aws = AllAccountsWaitSet.create(ownerAccountId, id, defaultInterest);
                ws = aws;
                errors = new ArrayList<WaitSetError>();
            } else {
                SomeAccountsWaitSet sws = new SomeAccountsWaitSet(ownerAccountId, id, defaultInterest);
                errors = sws.addAccounts(add);
                MailboxManager.getInstance().addListener(sws);
                ws = sws;
            }

            // bookkeeping: update access time, add to static wait set maps
            ws.setLastAccessedTime(System.currentTimeMillis());
            sWaitSets.put(id, ws);
            List<String> list = sWaitSetsByAccountId.get(ownerAccountId);
            if (list == null) {
                list = new ArrayList<String>();
                sWaitSetsByAccountId.put(ownerAccountId, list);
            }
            list.add(id);

            // return!
            return new Pair<String, List<WaitSetError>>(id, errors);
        }
    }


    /**
     * Destroy the referenced WaitSet.
     * 
     * @param zsc ZimbraSoapContext or permission checking.  If null, permission checking is skipped
     * @param requestingAcctId
     * @param id
     * @throws ServiceException
     */
    public static void destroy(ZimbraSoapContext zsc, String requestingAcctId, String id) throws ServiceException {
        synchronized(sWaitSets) {
            WaitSetBase ws = lookupInternal(id);
            if (ws == null) {
                throw MailServiceException.NO_SUCH_WAITSET(id);
            }
            assert(!Thread.holdsLock(ws));

            // skip permission checking if zsc is null
            if (zsc != null) {
                if (id.startsWith(WaitSetMgr.ALL_ACCOUNTS_ID_PREFIX)) {
                    checkRightForAllAccounts(zsc);
                } else {
                    checkRightForOwnerAccount(ws, requestingAcctId);
                }
            }
            
            //remove from the by-id map
            List<String> list = sWaitSetsByAccountId.get(ws.getOwnerAccountId());
            assert(list != null);
            list.remove(id);
            if (list.size() == 0) {
                sWaitSetsByAccountId.remove(ws.getOwnerAccountId());
            }

            // remove the wait set
            sWaitSets.remove(id);

            Map<String, WaitSetAccount> toCleanup = ws.destroy();
            if (toCleanup != null) {
                assert(!Thread.holdsLock(ws));
                for (WaitSetAccount wsa: toCleanup.values()) {
                    wsa.cleanupSession();
                }
            }
        }
    }

    /**
     * Find an active waitset.
     *
     * WaitSets are stored in a serverwide cache and are stamped with a last-accessed-time,
     * therefore callers should *not* cache the WaitSet pointer beyond a few seconds they
     * should instead use the lookup() API to fetch WaitSets between calls
     *
     * @param id
     * @return
     */
    public static IWaitSet lookup(String id) {
        return lookupInternal(id);
    }

    /**
     * WaitSets that are targeted at "all accounts" can be used across server restarts
     * (they are simply re-created)
     *
     * @param ownerAccountId Account ID of the owner/creator
     */
    public static IWaitSet lookupOrCreateForAllAccts(String ownerAccountId, String id,
            Set<MailItem.Type> defaultInterests, String lastKnownSeqNo) throws ServiceException {
        synchronized(sWaitSets) {
            if (!id.startsWith(ALL_ACCOUNTS_ID_PREFIX)) {
                throw ServiceException.INVALID_REQUEST("Called WaitSetMgr.lookupOrCreate but wasn't an 'All-' waitset ID", null);
            }

            IWaitSet toRet = lookup(id);
            if (toRet == null) {
                // oops, it's gone!  Try to re-create it given the last known sequence number
                AllAccountsWaitSet ws = AllAccountsWaitSet.createWithSeqNo(ownerAccountId, id, defaultInterests, lastKnownSeqNo);
                toRet = ws;
                ws.setLastAccessedTime(System.currentTimeMillis());

                // add the set to the two hashmaps
                sWaitSets.put(id, ws);
                List<String> list = sWaitSetsByAccountId.get(ownerAccountId);
                if (list == null) {
                    list = new ArrayList<String>();
                    sWaitSetsByAccountId.put(ownerAccountId, list);
                }
                list.add(id);

            }
            assert(toRet instanceof AllAccountsWaitSet);
            return toRet;
        }
    }

    public static void shutdown() {
        sSweeper.cancel();
    }


    public static void startup() {
        Zimbra.sTimer.schedule(sSweeper, WAITSET_SWEEP_DELAY, WAITSET_SWEEP_DELAY);
    }

    public static List<IWaitSet> getAll() {
        synchronized(sWaitSets) {
            List<IWaitSet> toRet = new ArrayList<IWaitSet>(sWaitSets.size());
            toRet.addAll(sWaitSets.values());
            return toRet;
        }
    }


    private static WaitSetBase lookupInternal(String id) {
        synchronized(sWaitSets) {
            WaitSetBase toRet = sWaitSets.get(id);
            if (toRet != null) {
                assert(!Thread.holdsLock(toRet));
                synchronized(toRet) {
                    toRet.setLastAccessedTime(System.currentTimeMillis());
                }
            }
            return toRet;
        }
    }

    /**

     /** Called by timer in order to timeout unused WaitSets */
    private static void sweep() {
        int activeSets = 0;
        int activeSessions = 0;
        int removed = 0;
        int withCallback = 0;
        synchronized(sWaitSets) {
            long cutoffTime = System.currentTimeMillis() - WAITSET_TIMEOUT;

            for (Iterator<WaitSetBase> iter = sWaitSets.values().iterator(); iter.hasNext();) {
                WaitSetBase ws = iter.next();
                assert(!Thread.holdsLock(ws)); // must never lock WS before sWaitSets or deadlock

                Map<String, WaitSetAccount> toCleanup = null;

                synchronized(ws) {
                    // only timeout if no cb AND if not accessed for a timeout
                    if (ws.getCb() == null && ws.getLastAccessedTime() < cutoffTime) {
                        //remove from the by-id map
                        List<String> list = sWaitSetsByAccountId.get(ws.getOwnerAccountId());
                        assert(list != null);
                        list.remove(ws.getWaitSetId());
                        if (list.size() == 0) {
                            sWaitSetsByAccountId.remove(ws.getOwnerAccountId());
                        }

                        // remove
                        iter.remove();
                        toCleanup = ws.destroy();
                        removed++;
                    } else {
                        if (ws.getCb() != null) {
                            withCallback++;
                        }
                        activeSets++;
                        activeSessions+=ws.countSessions();
                    }
                }

                // cleanup w/o WaitSet lock held
                if (toCleanup != null) {
                    assert(!Thread.holdsLock(ws));
                    for (WaitSetAccount wsa : toCleanup.values()) {
                        wsa.cleanupSession();
                    }
                }
            }
        }
        if (removed > 0) {
            ZimbraLog.session.info("WaitSet sweeper timing out %d WaitSets due to inactivity", removed);
        }

        if (activeSets > 0) {
            ZimbraLog.session.info("WaitSet sweeper: %d active WaitSets (%d accounts) - %d sets with blocked callbacks",
                activeSets, activeSessions, withCallback);
        }
    }
    
    /*
     * ensure that the authenticated account is allowed to create/destroy/access a waitset on
     * all accounts
     */
    public static void checkRightForAllAccounts(ZimbraSoapContext zsc) throws ServiceException {
        AdminDocumentHandler.checkRight(zsc, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);
    }
    
    /*
     * ensure that the authenticated account must be able to access the additionally specified 
     * account in order to add/delete it to/from a waitset 
     */
    public static void checkRightForAdditionalAccount(String acctId, ZimbraSoapContext zsc) 
    throws ServiceException {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.id, acctId);
        if (acct == null)
            throw ServiceException.DEFEND_ACCOUNT_HARVEST(acctId);
            
        if (!AccessManager.getInstance().canAccessAccount(zsc.getAuthToken(), acct, zsc.isUsingAdminPrivileges()))
            throw ServiceException.PERM_DENIED("cannot access account " + acct.getName());
    }
    
    /*
     * ensure that the requesting account must be the owner(creator) of the waitset
     */
    public static void checkRightForOwnerAccount(IWaitSet ws, String requestingAcctId) throws ServiceException {
        if (!ws.getOwnerAccountId().equals(requestingAcctId)) {
            throw ServiceException.PERM_DENIED("Not owner(creator) of waitset");
        }
    }

}
