/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.ldap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Alias;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Provisioning.CacheEntryBy;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.servlet.ZimbraServlet;


class RenameDomain {
 
    private static final Log sRenameDomainLog = LogFactory.getLog("zimbra.provisioning.renamedomain");
    
    private DirContext mDirCtxt;
    private LdapProvisioning mProv;
    private Domain mOldDomain;
    private String mNewDomainName;
    
    RenameDomain(DirContext dirCtxt, LdapProvisioning prov, Domain oldDomain, String newDomainName) {
        mDirCtxt = dirCtxt;
        mProv = prov;
        mOldDomain = oldDomain;
        mNewDomainName = newDomainName;
    }
    
    private RenameDomainVisitor getVisitor(RenamePhase phase) {
        return new RenameDomainVisitor(mDirCtxt, mProv, mOldDomain.getName(), mNewDomainName, phase);
    }
        
    
    public void execute() throws ServiceException {
        String oldDomainName = mOldDomain.getName();
        String oldDomainId = mOldDomain.getId();
           
        RenameInfo renameInfo = beginRenameDomain();
        RenamePhase startingPhase = renameInfo.phase();
        RenamePhase phase = RenamePhase.PHASE_FIX_FOREIGN_DL_MEMBERS;
            
        /*
         * 1. create the new domain
         */ 
        Domain newDomain = createNewDomain();
            
        /*
         * 2. move all accounts, DLs, and aliases
         */ 
        RenameDomainVisitor visitor;
        String searchBase = mProv.mDIT.domainDNToAccountSearchDN(((LdapDomain)mOldDomain).getDN());
        int flags = 0;
            
        // first phase, go thru DLs and accounts and their aliases that are in the old domain into the new domain
        phase = RenamePhase.PHASE_RENAME_ENTRIES;
        if (phase.ordinal() >= startingPhase.ordinal()) {
            // don't need to setPhase for the first first, it was set or got from beginRenameDomain
            visitor = getVisitor(phase);
            flags = Provisioning.SA_ACCOUNT_FLAG + Provisioning.SA_CALENDAR_RESOURCE_FLAG + Provisioning.SA_DISTRIBUTION_LIST_FLAG;
            mProv.searchObjects(null, null, searchBase, flags, visitor, 0);
        }
            
        // second phase, go thru aliases that have not been moved yet, by now aliases left in the domain should be aliases with target in other domains
        phase = RenamePhase.PHASE_FIX_FOREIGN_ALIASES;
        if (phase.ordinal() >= startingPhase.ordinal()) {
            renameInfo.setPhase(phase);
            renameInfo.write(mProv, mOldDomain);
            visitor = getVisitor(phase);
            flags = Provisioning.SA_ALIAS_FLAG;
            mProv.searchObjects(null, null, searchBase, flags, visitor, 0);
        }
            
        // third phase, go thru DLs and accounts in the *new* domain, rename the addresses in all DLs
        //     - the addresses to be renamed are: the DL/account's main address and all the aliases that were moved to the new domain
        //     - by now the DLs to modify should be those in other domains, because members of DLs in the old domain (now new domain) 
        //       have been updated in first pass.
        phase = RenamePhase.PHASE_FIX_FOREIGN_DL_MEMBERS;
        if (phase.ordinal() >= startingPhase.ordinal()) {
            renameInfo.setPhase(phase);
            renameInfo.write(mProv, mOldDomain);
            visitor = getVisitor(phase);
            searchBase = mProv.mDIT.domainDNToAccountSearchDN(((LdapDomain)newDomain).getDN());
            flags = Provisioning.SA_ACCOUNT_FLAG + Provisioning.SA_CALENDAR_RESOURCE_FLAG + Provisioning.SA_DISTRIBUTION_LIST_FLAG;
            mProv.searchObjects(null, null, searchBase, flags, visitor, 0);
        }
        
        /*
         * 3. Delete the old domain
         */ 
        mProv.deleteDomain(oldDomainId);
            
        /*
         * 4. restore zimbraId to the id of the old domain and erase rename info
         */ 
        LdapProvisioning.flushDomainCache(mProv, newDomain.getId());
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, oldDomainId);
        attrs.put(Provisioning.A_zimbraDomainRenameInfo, "");
        mProv.modifyAttrsInternal(newDomain, mDirCtxt, attrs);  // skip callback
        
        /*
         * 5. activate the new domain
         */
        mProv.modifyDomainStatus(newDomain, Provisioning.DOMAIN_STATUS_ACTIVE);
        
    }
    
    public static enum RenamePhase {
        /*
         * Note: the following text is written in zimbraDomainRenameInfo - change would require migration!!
         */
        PHASE_RENAME_ENTRIES,
        PHASE_FIX_FOREIGN_ALIASES,
        PHASE_FIX_FOREIGN_DL_MEMBERS;
        
        public static RenamePhase fromString(String s) throws ServiceException {
            try {
                return RenamePhase.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.FAILURE("unknown phase: "+s, e);
            }
        }
        
        public String toString() {
            return name().substring(6);  // skip the "PHASE_"
        }
    }
        
    private static class RenameInfo {
        
        /*
         * values are written in ldap attr, change would require migration!
         */
        private static final String SRC  = "SRC";
        private static final String DEST = "DEST";
        private static final char COLON = ':';
        private static final char COMMA = ',';
        
        /*
         * if this is the source domain, mDestDomainName contains the dest domain name, mSrcDomainName is null
         * if this is the dest domain, mSrcDomainName contains the src domain name, mDestDomainName is null
         */
        private String mSrcDomainName;
        private String mDestDomainName;
        private RenamePhase mPhase;
        private boolean mIsSrc;  // convenient var so we don't need to check mSrcDomainName/mDestDomainName when determining whether thsi is for src or desc
        
        private RenameInfo(String srcDomainName, String destDomainName, RenamePhase phase) {
            mSrcDomainName = srcDomainName;
            mDestDomainName = destDomainName;
            mPhase = phase;
            mIsSrc = (srcDomainName == null);
        }
        
        String srcDomainName() { return mSrcDomainName; }
        String destDomainName() { return mDestDomainName; } 
        RenamePhase phase() { return mPhase; }
        
        public void setPhase(RenamePhase phase) throws ServiceException {
            mPhase = phase;
        }
        
        private String encodeSrc() {
            return SRC + COMMA + mPhase.toString() + COLON + mDestDomainName;
        }
        
        private String encodeDest() {
            return DEST + COLON + mSrcDomainName;
        }
        
        static RenameInfo load(Domain domain, boolean expectingSrc) throws ServiceException {
            /*
             * rename info is stored in zimbraDomainRenameInfo. 
             * The format is:
             *     On the source(old) domain:
             *         SOURCE,{phase}:{destination-domain-name}
             *     On the destination(new) domain:
             *         DEST:{source-domain-name}
             */
            
            String renameInfo = domain.getAttr(Provisioning.A_zimbraDomainRenameInfo);
            if (StringUtil.isNullOrEmpty(renameInfo))
                return null;
            
            int idx = renameInfo.indexOf(COLON);
            if (idx == -1)
                throw ServiceException.FAILURE("invalid value in " + Provisioning.A_zimbraDomainRenameInfo + ": " + renameInfo + " missing " + COLON, null);
            String statusPart = renameInfo.substring(0, idx);
            String domainName = renameInfo.substring(idx);
            if (StringUtil.isNullOrEmpty(domainName))
                throw ServiceException.FAILURE("invalid value in " + Provisioning.A_zimbraDomainRenameInfo + ": " + renameInfo + " missing domain name", null);
            
            idx = statusPart.indexOf(COMMA);
            String srcOrDest = statusPart;
            RenamePhase phase = null;
            if (idx != -1) {
                srcOrDest = statusPart.substring(0, idx);
                phase = RenamePhase.fromString(statusPart.substring(idx));
            }
                
            if (srcOrDest.equals(SRC)) {
                if (!expectingSrc)
                    throw ServiceException.FAILURE("invalid value in " + Provisioning.A_zimbraDomainRenameInfo + ": " + renameInfo + " missing " + DEST + " keyword", null);
                if (phase == null)
                    throw ServiceException.FAILURE("invalid value in " + Provisioning.A_zimbraDomainRenameInfo + ": " + renameInfo + " missing phase info for source domain" , null);
                return new RenameInfo(null, domainName, phase);
            } else {
                if (expectingSrc)
                    throw ServiceException.FAILURE("invalid value in " + Provisioning.A_zimbraDomainRenameInfo + ": " + renameInfo + " missing " + SRC + " keyword", null);
                return new RenameInfo(domainName, null, phase);
            }
        }
        
        public void write(LdapProvisioning prov, Domain domain) throws ServiceException {
            HashMap<String, Object> attrs = new HashMap<String, Object>();
            
            String renameInfoStr;
            
            if (mIsSrc)
                renameInfoStr = encodeSrc();
            else
                renameInfoStr = encodeDest();
            attrs.put(Provisioning.A_zimbraDomainRenameInfo, renameInfoStr);
            prov.modifyAttrs(domain, attrs);
        }
    }
    
    private RenameInfo beginRenameDomain() throws ServiceException {
        String oldDomainName = mOldDomain.getName();
        
        // see if the domain is currently shutdown and/or being renamed
        boolean domainIsShutdown = mOldDomain.isShutdown();
        RenameInfo renameInfo = RenameInfo.load(mOldDomain, true);
        
        if (domainIsShutdown && renameInfo == null)
            throw ServiceException.INVALID_REQUEST("domain " + oldDomainName + " is shutdown without rename domain info", null);
        
        if (renameInfo != null && !renameInfo.destDomainName().equals(mNewDomainName))
            throw ServiceException.INVALID_REQUEST("domain " + oldDomainName + " was being renamed to " + renameInfo.destDomainName() + 
                                                   " it cannot be renamed to " + mNewDomainName + " until the previous rename is finished", null);
      
        // okay, this is either a new rename or a restart of a previous rename that did not finish   
        
        // mark domain shutdown and rejecting mails
        // mProv.modifyDomainStatus(mOldDomain, Provisioning.DOMAIN_STATUS_SHUTDOWN);
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_SHUTDOWN);
        attrs.put(Provisioning.A_zimbraMailStatus, Provisioning.MAIL_STATUS_DISABLED);
        mProv.modifyAttrs(mOldDomain, attrs, false, false);  // skip callback
                
        RenamePhase phase = RenamePhase.PHASE_RENAME_ENTRIES;
        
        if (renameInfo == null) {
            // new rename
            renameInfo = new RenameInfo(null, mNewDomainName, phase);
            renameInfo.write(mProv, mOldDomain);
        } else {
            // restart of a previous rename that did not finish
            phase = renameInfo.phase();
        }
        
        mProv.flushDomainCacheOnAllServers(mOldDomain.getId());
        
        return renameInfo;
    }
   

    private Domain createNewDomain() throws ServiceException {
        
        // Get existing domain attributes
        // make a copy, we don't want to step over our old domain object
        Map<String, Object> domainAttrs = new HashMap<String, Object>(mOldDomain.getAttrs(false));
        
        // remove attributes that are not needed for createDomain
        domainAttrs.remove(Provisioning.A_o);
        domainAttrs.remove(Provisioning.A_dc);
        domainAttrs.remove(Provisioning.A_objectClass);
        domainAttrs.remove(Provisioning.A_zimbraId);  // use a new zimbraId so getDomainById of the old domain will not return this half baked domain
        domainAttrs.remove(Provisioning.A_zimbraDomainName);
        domainAttrs.remove(Provisioning.A_zimbraMailStatus);
        
        // the new domain is created shutdown and rejecting mails
        domainAttrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_SHUTDOWN);
        domainAttrs.put(Provisioning.A_zimbraMailStatus, Provisioning.MAIL_STATUS_DISABLED);
        
        Domain newDomain = null;
        try {
            newDomain = mProv.createDomain(mNewDomainName, domainAttrs);
        } catch (AccountServiceException e) {
            if (e.getCode().equals(AccountServiceException.DOMAIN_EXISTS)) {
                newDomain = mProv.get(Provisioning.DomainBy.name, mNewDomainName);
                if (newDomain == null)  // this should not happen
                    throw ServiceException.FAILURE("failed to load existing domain " + mNewDomainName, null);
                
                // the new domain already exists, make sure it is the one that was being renamed to
                RenameInfo renameInfo = RenameInfo.load(newDomain, false);
                if (!renameInfo.srcDomainName().equals(mOldDomain.getName()))
                    throw ServiceException.INVALID_REQUEST("domain " + mNewDomainName + " was being renamed from " + renameInfo.srcDomainName() + 
                            " it cannot be renamed from " + mOldDomain.getName() + " until the previous rename is finished" , null);
                return newDomain;  // all is well
            } else
                throw e;
                
        } 
        
        RenameInfo renameInfo = new RenameInfo(mOldDomain.getName(), null, null);
        renameInfo.write(mProv, newDomain);
        return newDomain;
    }
    
    static class RenameDomainVisitor implements NamedEntry.Visitor {
    
        private DirContext mDirCtxt;
        private LdapProvisioning mProv;
        private String mOldDomainName;
        private String mNewDomainName;
        private RenamePhase mPhase;
    
        private static final Set<String> sAddrContainsDomainOnly;
        
        static {
            sAddrContainsDomainOnly = new HashSet<String>();
            
            sAddrContainsDomainOnly.add(Provisioning.A_zimbraMailCatchAllAddress);
            sAddrContainsDomainOnly.add(Provisioning.A_zimbraMailCatchAllCanonicalAddress);
            sAddrContainsDomainOnly.add(Provisioning.A_zimbraMailCatchAllForwardingAddress);
        }
    
        private static final String[] sDLAttrsNeedRename = {Provisioning.A_mail, 
                                                            Provisioning.A_zimbraMailAlias,
                                                            Provisioning.A_zimbraMailForwardingAddress,
                                                            Provisioning.A_zimbraMailDeliveryAddress, // ?
                                                            Provisioning.A_zimbraMailCanonicalAddress,
                                                            Provisioning.A_zimbraMailCatchAllAddress,
                                                            Provisioning.A_zimbraMailCatchAllCanonicalAddress,
                                                            Provisioning.A_zimbraMailCatchAllForwardingAddress};
    
        private static final String[] sAcctAttrsNeedRename = {Provisioning.A_mail, 
                                                              Provisioning.A_zimbraMailAlias,
                                                              Provisioning.A_zimbraMailForwardingAddress,
                                                              Provisioning.A_zimbraMailDeliveryAddress, // ?
                                                              Provisioning.A_zimbraMailCanonicalAddress,
                                                              Provisioning.A_zimbraMailCatchAllAddress,
                                                              Provisioning.A_zimbraMailCatchAllCanonicalAddress,
                                                              Provisioning.A_zimbraMailCatchAllForwardingAddress};
    
    
        private static boolean addrContainsDomainOnly(String addr) {
            return (sAddrContainsDomainOnly.contains(addr));
        }
    
        private RenameDomainVisitor(DirContext dirCtxt, LdapProvisioning prov, String oldDomainName, String newDomainName, RenamePhase phase) {
            mDirCtxt = dirCtxt;
            mProv = prov;
            mOldDomainName = oldDomainName;
            mNewDomainName = newDomainName;
            mPhase = phase;
        }
    
        public void visit(NamedEntry entry) throws ServiceException {
            debug("(" + mPhase.toString() + ") visiting " + entry.getName());
        
            if (mPhase == RenamePhase.PHASE_RENAME_ENTRIES) {
                if (entry instanceof DistributionList)
                    handleEntry(entry, true);  // PHASE_RENAME_ENTRIES
                else if (entry instanceof Account)
                    handleEntry(entry, false); // PHASE_RENAME_ENTRIES
            } else if (mPhase == RenamePhase.PHASE_FIX_FOREIGN_ALIASES) {
                if (entry instanceof Alias)
                    handleForeignAlias(entry); // PHASE_FIX_FOREIGN_ALIASES
                else
                    assert(false);  // by now there should only be foreign aliases in the old domain
            } else if (mPhase == RenamePhase.PHASE_FIX_FOREIGN_DL_MEMBERS) {
                handleForeignDLMembers(entry);
            }
        }
    
        private void handleEntry(NamedEntry entry, boolean isDL) {
            LdapEntry ldapEntry = (LdapEntry)entry;
            String[] parts = EmailUtil.getLocalPartAndDomain(entry.getName());
            
            String newDn = null;
        
            try {
                newDn = (isDL)?mProv.mDIT.distributionListDNRename(ldapEntry.getDN(), parts[0], mNewDomainName):
                               mProv.mDIT.accountDNRename(ldapEntry.getDN(), parts[0], mNewDomainName);
            } catch (NamingException e) {
                warn(e, "handleEntry", "cannot get new DN, entry not handled", "entry=[%s]", entry.getName());
                return;
            } catch (ServiceException e) {
                warn(e, "handleEntry", "cannot get new DN, entry not handled", "entry=[%s]", entry.getName());
                return;
            }
        
            // Step 1. move the all aliases of the entry that are in the old domain to the new domain 
            String[] aliases = (isDL)?((DistributionList)entry).getAliases():((Account)entry).getAliases();
            handleAliases(entry, aliases, newDn);
         
            // Step 2. move the entry to the new domain and fixup all the addr attrs that contain the old domain
            String oldDn = ((LdapEntry)entry).getDN();
            if (isDL) 
                handleDistributionList(entry, oldDn, newDn);
            else
                handleAccount(entry, oldDn, newDn);
        }
        
        /*
         * 
         */
        private void handleAliases(NamedEntry targetEntry, String[] aliases, String newTargetDn) {
            LdapEntry ldapEntry = (LdapEntry)targetEntry;
            String oldDn = ldapEntry.getDN();
        
            // move aliases in the old domain if there are any
            for (int i=0; i<aliases.length; i++) {
            
                // for dl, the main dl addr is also in the zimbraMailAlias.  To be consistent witha account,
                // we don't move that when we move aliases; and will move it when we move the entry itself
                if (aliases[i].equals(targetEntry.getName()))
                    continue;
            
                String[] parts = EmailUtil.getLocalPartAndDomain(aliases[i]);
                if (parts == null) {
                    assert(false);
                    warn("moveEntry", "encountered invalid alias address", "alias=[%s], entry=[%s]", aliases[i], targetEntry.getName());
                    continue;
                }
                String aliasLocal = parts[0];
                String aliasDomain = parts[1];
                if (aliasDomain.equals(mOldDomainName)) {
                    // move the alias
                    // ug, aliasDN and aliasDNRename also throw ServiceExeption - declared vars outside the try block so we can log it in the catch blocks
                    String oldAliasDn = "";  
                    String newAliasDn = "";
                    try {
                        oldAliasDn = mProv.mDIT.aliasDN(oldDn, mOldDomainName, aliasLocal, mOldDomainName);
                        newAliasDn = mProv.mDIT.aliasDNRename(newTargetDn, mNewDomainName, aliasLocal+"@"+mNewDomainName);
                        if (!oldAliasDn.equals(newAliasDn))
                            LdapUtil.renameEntry(mDirCtxt, oldAliasDn, newAliasDn);
                    } catch (NamingException e) {
                        // log the error and continue
                        warn(e, "moveEntry", "alias not moved", "alias=[%s], entry=[%s], oldAliasDn=[%s], newAliasDn=[%s]", aliases[i], targetEntry.getName(), oldAliasDn, newAliasDn);
                    } catch (ServiceException e) {
                        // log the error and continue
                        warn(e, "moveEntry", "alias not moved", "alias=[%s], entry=[%s], oldAliasDn=[%s], newAliasDn=[%s]", aliases[i], targetEntry.getName(), oldAliasDn, newAliasDn);
                    }
                }
            }
        }
    
        private void handleDistributionList(NamedEntry entry, String oldDn, String newDn) {
        
            NamedEntry refreshedEntry = entry;
        
            if (!oldDn.equals(newDn)) {
                // move the entry
                try {
                    LdapUtil.renameEntry(mDirCtxt, oldDn, newDn);
                } catch (NamingException e) {
                    warn(e, "moveDistributionList", "renameEntry", "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
                }
            
                // refresh for the new DN
                // do not catch here, if we can't refresh - we can't modify, just let it throw and proceed to the next entry
                try {
                    refreshedEntry = mProv.get(Provisioning.DistributionListBy.id, entry.getId());
                } catch (ServiceException e) {
                    warn(e, "moveDistributionList", "getDistributionListById, entry not modified", "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
                    // if we can't refresh - we can't modify, just return and proceed to the next entry
                    return;
                }
            }
        
            // modify the entry in the new domain
            Map<String, Object> fixedAttrs = fixupAddrs(entry, sDLAttrsNeedRename);
        
            // modify the entry
            try {
                mProv.modifyAttrsInternal(refreshedEntry, mDirCtxt, fixedAttrs);
            } catch (ServiceException e) {
                warn(e, "moveDistributionList", "modifyAttrsInternal", "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
            }
        }
    
        private void handleAccount(NamedEntry entry, String oldDn, String newDn) {
        
            NamedEntry refreshedEntry = entry;
            Map<String, Object> fixedAttrs = fixupAddrs(entry, sAcctAttrsNeedRename);
        
            if (!oldDn.equals(newDn)) {
                // move the entry
            
                /*
                 * for accounts, we need to first crate the entry in the new domain, because it may have sub entries
                 * (identities/datasources, signatures).  We create the account entry in the new domain using the fixed adddr attrs.
                 */
                Attributes attributes = new BasicAttributes(true);
                LdapUtil.mapToAttrs(fixedAttrs, attributes);
                try {
                    LdapUtil.createEntry(mDirCtxt, newDn, attributes, "renameDomain-createAccount");
                } catch (NameAlreadyBoundException e) {
                    warn(e, "moveAccount", "createEntry", "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
                } catch (ServiceException e) {
                    warn(e, "moveAccount", "createEntry", "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
                }
                    
                try {
                    // move over all identities/sources/signatures etc. doesn't throw an exception, just logs
                    LdapUtil.moveChildren(mDirCtxt, oldDn, newDn);
                } catch (ServiceException e) {
                    warn(e, "moveAccount", "moveChildren", "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
                }
                
                try {
                    LdapUtil.unbindEntry(mDirCtxt, oldDn);
                } catch (NamingException e) {
                    warn(e, "moveAccount", "unbindEntry", "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
                }
            } else {
                // didn't need to move the account entry, still need to fixup the addr attrs
         
                try {
                    mProv.modifyAttrsInternal(refreshedEntry, mDirCtxt, fixedAttrs);
                } catch (ServiceException e) {
                    warn(e, "moveAccount", "modifyAttrsInternal", "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
                }
            }
        }
    
        private Map<String, Object> fixupAddrs(NamedEntry entry, String[] attrsNeedRename)  {

            // replace the addr attrs
            Map<String, Object> attrs = entry.getAttrs(false);
            for (String attr : attrsNeedRename) {
                boolean addrCanBeDomainOnly = addrContainsDomainOnly(attr);
                
                String[] values = entry.getMultiAttr(attr, false);
                if (values.length > 0) {
                    Set<String> newValues = new HashSet<String>();
                    for (int i=0; i<values.length; i++) {
                        String newValue = convertToNewAddr(values[i], mOldDomainName, mNewDomainName, addrCanBeDomainOnly);
                        if (newValue != null)
                            newValues.add(newValue);
                    }
            
                    // replace the attr with the new values
                    // if there is any address format error and the address cannot be converted, 
                    // whatever the current value will be carried to the new entry.
                    if (newValues.size() > 0)
                        attrs.put(attr, newValues.toArray(new String[newValues.size()]));
                }
            }
        
            return attrs;
        }

        /*
         * given an email address, and old domain name and a new domain name, returns:
         *   - if the domain of the address is the same as the old domain, returns localpart-of-addr@new-domain
         *   - otherwise returns the email addr as is.
         *   
         *   Note, for some addresses they can be in the @domain format (no local part)
         */
        private String convertToNewAddr(String address, String oldDomain, String newDomain, boolean addrCanBeDomainOnly) {
            String addr = address.trim();
            String[] parts = EmailUtil.getLocalPartAndDomain(addr);
            if (parts == null && !addrCanBeDomainOnly) {
                warn("convertToNewAddr", "encountered invalid address", "addr=[%s]", addr);
                return null;
            }
                
            String local = null;
            String domain = null;
            
            if (parts != null) {
               local = parts[0];
               domain = parts[1];
            } else {
               if (addr.charAt(0) == '@')
                   domain = addr.substring(1);
            }
            
            if (domain == null) {
                warn("convertToNewAddr", "encountered invalid address", "addr=[%s]", addr);
                return null;
            }
            
            if (domain.equals(oldDomain)) {
                if (local != null)
                    return local + "@" + newDomain;
                else
                    return "@" + newDomain;
            } else
                return addr;
        }
    
        /*
         * aliases in the old domain with target in other domains
         */
        private void handleForeignAlias(NamedEntry entry) {
            Alias alias = (Alias)entry;
            NamedEntry targetEntry = null;
            try {
                targetEntry = alias.searchTarget(false);
            } catch (ServiceException e) {
                warn(e, "handleForeignAlias", "target entry not found for alias" + "alias=[%s], target=[%s]", alias.getName(), targetEntry.getName());
                return;
            }
            
            // sanity check that the target is indeed in a different domain
            String targetName = targetEntry.getName();
            String[] targetParts = EmailUtil.getLocalPartAndDomain(targetName);
            if (targetParts == null) {
                warn("handleForeignAlias", "encountered invalid alias target address", "target=[%s]", targetName);
                return;
            }
            String targetDomain = targetParts[1];
            if (!targetDomain.equals(mOldDomainName)) {
                String aliasOldAddr = alias.getName();
                String[] aliasParts = EmailUtil.getLocalPartAndDomain(aliasOldAddr);
                if (aliasParts == null) {
                    warn("handleForeignAlias", "encountered invalid alias address", "alias=[%s]", aliasOldAddr);
                    return;
                }
                String aliasLocal = aliasParts[0];
                String aliasNewAddr = aliasLocal + "@" + mNewDomainName;
                if (targetEntry instanceof DistributionList) {
                    DistributionList dl = (DistributionList)targetEntry;
                    fixupForeignTarget(dl, aliasOldAddr, aliasNewAddr);
                } else if (targetEntry instanceof Account){
                    Account acct = (Account)targetEntry;
                    fixupForeignTarget(acct, aliasOldAddr, aliasNewAddr);
                } else {
                    warn("handleForeignAlias", "encountered invalid alias target type", "target=[%s]", targetName);
                    return;
                }
            }
        }
    
        private void fixupForeignTarget(DistributionList targetEntry, String aliasOldAddr,  String aliasNewAddr) {
            try {
                mProv.removeAlias(targetEntry, aliasOldAddr);
            } catch (ServiceException e) {
                warn("fixupTargetInOtherDomain", "cannot remove alias for dl" + "dl=[%s], aliasOldAddr=[%s], aliasNewAddr=[%s]", targetEntry.getName(), aliasOldAddr, aliasNewAddr);
            }
            
            // continue doing add even if remove failed 
            try {
                mProv.addAlias(targetEntry, aliasNewAddr);
            } catch (ServiceException e) {
                warn("fixupTargetInOtherDomain", "cannot add alias for dl" + "dl=[%s], aliasOldAddr=[%s], aliasNewAddr=[%s]", targetEntry.getName(), aliasOldAddr, aliasNewAddr);
            }
        }
        
        private void fixupForeignTarget(Account targetEntry, String aliasOldAddr,  String aliasNewAddr) {
            try {
                mProv.removeAlias(targetEntry, aliasOldAddr);
            } catch (ServiceException e) {
                warn("fixupTargetInOtherDomain", "cannot remove alias for account" + "acct=[%s], aliasOldAddr=[%s], aliasNewAddr=[%s]", targetEntry.getName(), aliasOldAddr, aliasNewAddr);
            }
            
            // we want to continue doing add even if remove failed 
            try {
                mProv.addAlias(targetEntry, aliasNewAddr);
            } catch (ServiceException e) {
                warn("fixupTargetInOtherDomain", "cannot add alias for account" + "acct=[%s], aliasOldAddr=[%s], aliasNewAddr=[%s]", targetEntry.getName(), aliasOldAddr, aliasNewAddr);
            }
        }
    
        /*
         * replace "old addrs of DL/accounts and their aliases that are members of DLs in other domains" to the new addrs
         */
        private void handleForeignDLMembers(NamedEntry entry) {
            Map<String, String> changedPairs = new HashMap<String, String>();
            
            String entryAddr = entry.getName();
            String[] oldNewPair = changedAddrPairs(entryAddr);
            if (oldNewPair != null)
                changedPairs.put(oldNewPair[0], oldNewPair[1]);
            
            String[] aliasesAddrs = entry.getMultiAttr(Provisioning.A_zimbraMailAlias, false);
            for (String aliasAddr : aliasesAddrs) {
                oldNewPair = changedAddrPairs(aliasAddr);
                if (oldNewPair != null)
                    changedPairs.put(oldNewPair[0], oldNewPair[1]);
            }
    
            mProv.renameAddressesInAllDistributionLists(changedPairs);
        }
        
        private String[] changedAddrPairs(String addr) {
            String[] parts = EmailUtil.getLocalPartAndDomain(addr);
            if (parts == null) {
                warn("changedAddrPairs", "encountered invalid address", "addr=[%s]", addr);
                return null;
            }
            
            String domain = parts[1];
            if (!domain.equals(mNewDomainName))
                return null;
            
            String localPart = parts[0];
            String[] oldNewAddrPairs = new String[2];
            oldNewAddrPairs[0] = localPart + "@" + mOldDomainName; 
            oldNewAddrPairs[1] = localPart + "@" + mNewDomainName; 
            
            return oldNewAddrPairs;
        }

    }
    
    private static void warn(Object o) {
        sRenameDomainLog.warn(o);
    }
    
    private static void warn(Object o, Throwable t) {
        sRenameDomainLog.warn(o, t);
    }
    
    private static void warn(String funcName, String desc, String format, Object ... objects) {
        warn(null, funcName,  desc,  format, objects);
    }

    private static void warn(Throwable t, String funcName, String desc, String format, Object ... objects) {
        if (sRenameDomainLog.isWarnEnabled())
            // mRenameDomainLog.warn(String.format(funcName + "(" + desc + "):" + format, objects), t);
            sRenameDomainLog.warn(String.format(funcName + "(" + desc + "):" + format, objects));
    }

    private static void debug(String format, Object ... objects) {
        if (sRenameDomainLog.isDebugEnabled())
            sRenameDomainLog.debug(String.format(format, objects));
    }

    private static void debug(String funcName, String desc, String format, Object ... objects) {
        debug(null, funcName,  desc,  format, objects);
    }

    private static void debug(Throwable t, String funcName, String desc, String format, Object ... objects) {
        if (sRenameDomainLog.isDebugEnabled())
            // mRenameDomainLog.warn(String.format(funcName + "(" + desc + "):" + format, objects), t);
            sRenameDomainLog.debug(String.format(funcName + "(" + desc + "):" + format, objects));
    }
    
}