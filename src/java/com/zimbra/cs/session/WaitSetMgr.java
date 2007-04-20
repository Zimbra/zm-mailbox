/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.session;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.session.WaitSet.WaitSetAccount;
import com.zimbra.cs.session.WaitSet.WaitSetError;
import com.zimbra.cs.util.Zimbra;

/**
 * 
 */
public class WaitSetMgr {
    private static int sWaitSetNumber = 1;

    private static final HashMap<String, WaitSet> sWaitSets = new HashMap<String, WaitSet>();
    
    private static final int WAITSET_SWEEP_DELAY = 1000 * 60; // once every minute
    
    private static final int WAITSET_TIMEOUT = 1000 * 60 * 20; // 20min
    
    /**
     * Create a new WaitSet, optionally specifying an initial set of accounts
     * to start listening wait on
     * 
     * WaitSets are stored in a serverwide cache and are stamped with a last-accessed-time,
     * therefore callers should *not* cache the WaitSet pointer beyond a few seconds they
     * should instead use the lookup() API to fetch WaitSets between calls
     * 
     * @param defaultInterest
     * @param add
     * @return A Pair(WaitSetID, List<WaitSetError>)
     * @throws ServiceException 
     */
    public static Pair<String, List<WaitSetError>> create(int defaultInterest, boolean allAccts, List<WaitSetAccount> add) throws ServiceException {
        synchronized(sWaitSets) {
            String id = "WaitSet"+sWaitSetNumber;
            sWaitSetNumber++;
            WaitSet ws = new WaitSet(id, defaultInterest);
            if (allAccts)
                ws.setIncludeAllAccounts(true);
            
            ws.setLastAccessedTime(System.currentTimeMillis());
            sWaitSets.put(id, ws);
            
            MailboxManager.getInstance().addListener(ws);
            
            List<WaitSetError> errors = null;
            if (!allAccts) {
                errors = ws.addAccounts(add);
            } else {
                errors = ws.addAllAccounts();
            }
            return new Pair<String, List<WaitSetError>>(id, errors);
        }
    }

    /**
     * Destroy the referenced WaitSet.  
     * 
     * @param id
     * @throws ServiceException
     */
    public static void destroy(String id) throws ServiceException {
        synchronized(sWaitSets) {
            WaitSet ws = lookup(id);
            if (ws == null)
                throw MailServiceException.NO_SUCH_WAITSET(id);
            assert(!Thread.holdsLock(ws));
            
            MailboxManager.getInstance().removeListener(ws);
            
            sWaitSets.remove(id);
            HashMap<String, WaitSetAccount> toCleanup = ws.destroy();
            if (toCleanup != null) {
                assert(!Thread.holdsLock(ws));
                for (WaitSetAccount wsa: toCleanup.values()) {
                    WaitSetSession session = wsa.getSession();
                    if (session != null) {
                        session.doCleanup();
                        wsa.ref = null;
                    }
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
    public static WaitSet lookup(String id) {
        synchronized(sWaitSets) {
            WaitSet toRet = sWaitSets.get(id);
            if (toRet != null) {
                assert(!Thread.holdsLock(toRet));
                synchronized(toRet) { 
                    toRet.setLastAccessedTime(System.currentTimeMillis());
                }
            }
            return toRet;
        }
    }
    
    public static void shutdown() {
        sSweeper.cancel();
    }

    
    public static void startup() {
        Zimbra.sTimer.schedule(sSweeper, WAITSET_SWEEP_DELAY, WAITSET_SWEEP_DELAY);
    }


    private static final TimerTask sSweeper = new TimerTask() { 
        public void run() { 
            WaitSetMgr.sweep();
        }
    };
    
    /**
     * Called by timer in order to timeout unused WaitSets
     */
    private static void sweep() {
        int activeSets = 0;
        int activeSessions = 0;
        int removed = 0;
        int withCallback = 0;
        synchronized(sWaitSets) {
            long cutoffTime = System.currentTimeMillis() - WAITSET_TIMEOUT;
            
            for (Iterator<WaitSet> iter = sWaitSets.values().iterator(); iter.hasNext();) {
                WaitSet ws = iter.next();
                assert(!Thread.holdsLock(ws)); // must never lock WS before sWaitSets or deadlock

                HashMap<String, WaitSetAccount> toCleanup = null;
                
                synchronized(ws) {
                    // only timeout if no cb AND if not accessed for a timeout
                    if (ws.getCb() == null && ws.getLastAccessedTime() < cutoffTime) {
                        iter.remove();
                        toCleanup = ws.destroy();
                        removed++;
                    } else {
                        if (ws.getCb() != null)
                            withCallback++;
                        activeSets++;
                        activeSessions+=ws.getSessions().size();
                    }
                }

                // cleanup w/o WaitSet lock held
                if (toCleanup != null) {
                    assert(!Thread.holdsLock(ws));
                    for (WaitSetAccount wsa : toCleanup.values()) {
                        WaitSetSession session = wsa.getSession();
                        if (session != null) {
                            session.doCleanup();
                            wsa.ref = null;
                        }
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
}
