/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.account.ldap;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.account.ZAttrProvisioning.AutoProvMode;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.EagerAutoProvisionScheduler;
import com.zimbra.cs.account.ldap.entry.LdapEntry;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;
import com.zimbra.cs.ldap.SearchLdapOptions.StopIteratingException;
import com.zimbra.cs.util.Zimbra;

public class AutoProvisionEager extends AutoProvision {
    private EagerAutoProvisionScheduler scheduler;
    
    private AutoProvisionEager(LdapProv prov, Domain domain, EagerAutoProvisionScheduler scheduler) {
        super(prov, domain);
        this.scheduler = scheduler;
    }
    
    static void handleScheduledDomains(LdapProv prov, EagerAutoProvisionScheduler scheduler) {
        ZLdapContext zlc = null;
        
        try {
            // get scheduled domains on this server
            Server localServer = prov.getLocalServer();
            String[] scheduledDomains = localServer.getAutoProvScheduledDomains();
            
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.AUTO_PROVISION);
            
            for (String domainName : scheduledDomains) {
                if (scheduler.isShutDownRequested()) {
                    ZimbraLog.autoprov.info("eager auto provision aborted");
                    return;
                }
                
                try {
                    Domain domain = prov.get(DomainBy.name, domainName);
                    if (domain == null) {
                        ZimbraLog.autoprov.info("EAGER auto provision: no such domain " + domainName);
                        continue;
                    }
                    
                    // refresh the domain from LDAP master so we don't get into a race 
                    // condition if the domains was just enabled for EAGER mode on other node.
                    prov.reload(domain, true);
                    
                    if (!autoProvisionEnabled(domain)) {
                        /*
                         * remove it from the scheduled domains on the local server
                         */
                        
                        ZimbraLog.autoprov.info("Domain %s is scheduled for EAGER auto provision " +
                                "but EAGER mode is not enabled on the domain.  " +
                                "Removing domain %s from %s on server %s", 
                                domain.getName(), domain.getName(), 
                                Provisioning.A_zimbraAutoProvScheduledDomains, localServer.getName());
                        
                        // will trigger callback for AutoProvScheduledDomains.  If scheduled 
                        // domains become empty, the EAGER auto prov thread will be requested 
                        // to shutdown.
                        localServer.removeAutoProvScheduledDomains(domain.getName());
                        continue;
                    }
                    
                    ZimbraLog.autoprov.info("Auto provisioning accounts on domain %s", domainName);
                    AutoProvisionEager autoProv = new AutoProvisionEager(prov, domain, scheduler);
                    autoProv.handleBatch(zlc);
                } catch (Throwable t) {
                    if (t instanceof OutOfMemoryError) {
                        Zimbra.halt("Ran out of memory while auto provision accounts", t);
                    } else {
                        ZimbraLog.autoprov.warn("Unable to auto provision accounts for domain %s", domainName, t);
                    }
                }
            }
        } catch (ServiceException e) {
            // unable to get ldap context
            ZimbraLog.autoprov.warn("Unable to auto provision accounts", e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    private void handleBatch(ZLdapContext zlc) throws ServiceException {
        if (!autoProvisionEnabled(domain)) {
            throw ServiceException.FAILURE("EAGER auto provision is not enabled on domain " 
                    + domain.getName(), null);
        }

        try {
            if (!lockDomain(zlc)) {
                ZimbraLog.autoprov.info("EAGER auto provision unable to lock domain: skip domain " +
                        domain.getName() + " on server " + prov.getLocalServer().getName());
                return;
            }
            createAccountBatch();
        } finally {
            unlockDomain(zlc);
        }
    }

    @Override
    Account handle() throws ServiceException {
        throw new UnsupportedOperationException();
    }
    
    private static boolean autoProvisionEnabled(Domain domain) {
        return domain.getMultiAttrSet(Provisioning.A_zimbraAutoProvMode).contains(AutoProvMode.EAGER.name());
    }
    
    private void createAccountBatch() throws ServiceException {
        
        long polledAt = System.currentTimeMillis();
        
        List<ExternalEntry> entries = new ArrayList<ExternalEntry>();
        boolean hitSizeLimitExceededException = searchAccounts(entries);
        
        for (ExternalEntry entry : entries) {
            if (scheduler.isShutDownRequested()) {
                ZimbraLog.autoprov.info("eager auto provision aborted");
                return;
            }
            
            try {
                ZAttributes externalAttrs = entry.getAttrs();
                String acctZimbraName = mapName(externalAttrs, null);
                
                ZimbraLog.autoprov.info("auto creating account in EAGER mode: " + acctZimbraName);
                Account acct = createAccount(acctZimbraName, entry, null, AutoProvMode.EAGER);
            } catch (ServiceException e) {
                // log and continue with next entry
                ZimbraLog.autoprov.warn("unable to auto create account " + entry.getDN(), e);
            }
        }
        
        // Keep track of the last polled timestamp.
        // The next batch will fetch entries with createTimeStamp later than the last polled 
        // timestamp in this batch.
        // But don't update it if the search hit SizeLimitExceededException.  In that case 
        // we want to retain the current stamp so the next poll will still use this stamp.
        // Note: it is expected a AutoProvisionListener is configured on the domain.  
        //       The postCreate method of AutoProvisionListener should update the external 
        //       directory to indicate the entry was provisioned in Zimbra.  
        //       Also, the same assertion (e.g. (provisionNotes=provisioned-in-zimbra)) 
        //       should be included in zimbraAutoProvLdapSearchFilter.
        //
        //       See how TestLdapProvAutoProvision.eagerMode() does it.
        //
        if (!hitSizeLimitExceededException) {
            String lastPolledAt = DateUtil.toGeneralizedTime(new Date(polledAt));
            domain.setAutoProvLastPolledTimestampAsString(lastPolledAt);
        }
    }
    
    private boolean lockDomain(ZLdapContext zlc) throws ServiceException {
        Server localServer = prov.getLocalServer();
        
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().domainLockedForEagerAutoProvision();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLock, localServer.getId());
        
        boolean gotLock = prov.getHelper().testAndModifyEntry(zlc, ((LdapEntry)domain).getDN(), 
                filter, attrs, domain);
        
        // need to refresh the domain entry, because this modify is not done via the normal 
        // LdapProvisioning.modifyAttr path.  
        prov.reload(domain, true);
        
        return gotLock;
    }
    
    private void unlockDomain(ZLdapContext zlc) throws ServiceException {
        // clear the server id in the lock
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLock, "");
        
        prov.getHelper().modifyAttrs(zlc, ((LdapEntry)domain).getDN(), attrs, domain);
        
        // need to refresh the domain entry, because this modify is not done via the normal 
        // LdapProvisioning.modifyAttr path.  
        prov.reload(domain, true);
    }
    
    private boolean searchAccounts(final List<ExternalEntry> entries) 
    throws ServiceException {
        int maxResults = domain.getAutoProvBatchSize();
        String lastPolledAt = domain.getAutoProvLastPolledTimestampAsString();
        String[] returnAttrs = getAttrsToFetch();
        
        SearchLdapVisitor visitor = new SearchLdapVisitor(false) {
            @Override
            public void visit(String dn, IAttributes ldapAttrs)
            throws StopIteratingException {
                entries.add(new ExternalEntry(dn, (ZAttributes)ldapAttrs));
            }
        };

        boolean hitSizeLimitExceededException = 
                AutoProvision.searchAutoProvDirectory(prov, domain, null, null, 
                        lastPolledAt, returnAttrs, maxResults, visitor, true);
        
        return hitSizeLimitExceededException;
    }

}
