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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Alias;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;


class RenameDomain {
    
static class RenameDomainVisitor implements NamedEntry.Visitor {
    
    private static final Log mRenameDomainLog = LogFactory.getLog("zimbra.provisioning.renamedomain");
    
    private DirContext mCtxt;
    private LdapProvisioning mProv;
    private String mOldDomainName;
    private String mNewDomainName;
    private Phase mPhase;
    
    public static enum Phase {
        PHASE_RENAME_ENTRIES,
        PHASE_FIX_FOREIGN_ALIASES,
        PHASE_FIX_FOREIGN_DL_MEMBERS
    }
    
    private static final String[] sDLAttrsNeedRename = {Provisioning.A_mail, 
                                                        Provisioning.A_zimbraMailAlias,
                                                        Provisioning.A_zimbraMailForwardingAddress,
                                                        Provisioning.A_zimbraMailDeliveryAddress, // ?
                                                        Provisioning.A_zimbraMailCanonicalAddress};
    
    private static final String[] sAcctAttrsNeedRename = {Provisioning.A_mail, 
                                                          Provisioning.A_zimbraMailAlias,
                                                          Provisioning.A_zimbraMailForwardingAddress,
                                                          Provisioning.A_zimbraMailDeliveryAddress, // ?
                                                          Provisioning.A_zimbraMailCanonicalAddress};
    
    
    private void warn(String funcName, String desc, String format, Object ... objects) {
        warn(null, funcName,  desc,  format, objects);
    }
    
    private void warn(Throwable t, String funcName, String desc, String format, Object ... objects) {
        if (mRenameDomainLog.isWarnEnabled())
            // mRenameDomainLog.warn(String.format(funcName + "(" + desc + "):" + format, objects), t);
            mRenameDomainLog.warn(String.format(funcName + "(" + desc + "):" + format, objects));
    }
    
    private void debug(String format, Object ... objects) {
        if (mRenameDomainLog.isDebugEnabled())
            mRenameDomainLog.debug(String.format(format, objects));
    }
    
    private void debug(String funcName, String desc, String format, Object ... objects) {
        debug(null, funcName,  desc,  format, objects);
    }
    
    private void debug(Throwable t, String funcName, String desc, String format, Object ... objects) {
        if (mRenameDomainLog.isDebugEnabled())
            // mRenameDomainLog.warn(String.format(funcName + "(" + desc + "):" + format, objects), t);
            mRenameDomainLog.debug(String.format(funcName + "(" + desc + "):" + format, objects));
    }
    
    RenameDomainVisitor(DirContext ctxt, LdapProvisioning prov, String oldDomainName, String newDomainName, Phase phase) {
        mCtxt = ctxt;
        mProv = prov;
        mOldDomainName = oldDomainName;
        mNewDomainName = newDomainName;
        mPhase = phase;
    }
    
    public void visit(NamedEntry entry) throws ServiceException {
        debug("(" + mPhase.name() + ") visiting " + entry.getName());
        
        if (mPhase == Phase.PHASE_RENAME_ENTRIES) {
            if (entry instanceof DistributionList)
                handleEntry(entry, true);  // PHASE_RENAME_ENTRIES
            else if (entry instanceof Account)
                handleEntry(entry, false); // PHASE_RENAME_ENTRIES
        } else if (mPhase == Phase.PHASE_FIX_FOREIGN_ALIASES) {
            if (entry instanceof Alias)
                handleForeignAlias(entry); // PHASE_FIX_FOREIGN_ALIASES
            else
                assert(false);  // by now there should only be foreign aliases in the old domain
        } else if (mPhase == Phase.PHASE_FIX_FOREIGN_DL_MEMBERS) {
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
                        LdapUtil.renameEntry(mCtxt, oldAliasDn, newAliasDn);
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
                LdapUtil.renameEntry(mCtxt, oldDn, newDn);
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
            mProv.modifyAttrsInternal(refreshedEntry, mCtxt, fixedAttrs);
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
                LdapUtil.createEntry(mCtxt, newDn, attributes, "renameDomain-createAccount");
            } catch (NameAlreadyBoundException e) {
                warn(e, "moveAccount", "createEntry", "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
            } catch (ServiceException e) {
                warn(e, "moveAccount", "createEntry", "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
            }
                    
            try {
                // move over all identities/sources/signatures etc. doesn't throw an exception, just logs
                LdapUtil.moveChildren(mCtxt, oldDn, newDn);
            } catch (ServiceException e) {
                warn(e, "moveAccount", "moveChildren", "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
            }
                
            try {
                LdapUtil.unbindEntry(mCtxt, oldDn);
            } catch (NamingException e) {
                warn(e, "moveAccount", "unbindEntry", "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
            }
        } else {
            // didn't need to move the account entry, still need to fixup the addr attrs
         
            try {
                mProv.modifyAttrsInternal(refreshedEntry, mCtxt, fixedAttrs);
            } catch (ServiceException e) {
                warn(e, "moveAccount", "modifyAttrsInternal", "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
            }
        }
    }
    
    private Map<String, Object> fixupAddrs(NamedEntry entry, String[] attrsNeedRename)  {

        // replace the addr attrs
        Map<String, Object> attrs = entry.getAttrs(false);
        for (String attr : attrsNeedRename) {
            String[] values = entry.getMultiAttr(attr, false);
            if (values.length > 0) {
                Set<String> newValues = new HashSet<String>();
                for (int i=0; i<values.length; i++) {
                    String newValue = convertToNewAddr(values[i], mOldDomainName, mNewDomainName);
                    if (newValue != null)
                        newValues.add(newValue);
                }
            
                // replace the attr with the new values
                attrs.put(attr, newValues.toArray(new String[newValues.size()]));
            }
        }
        
        return attrs;
    }

    /*
     * given an email address, and old domain name and a new domain name, returns:
     *   - if the domain of the address is the same as the old domain, returns localpart-of-addr@new-domain
     *   - otherwise returns the email addr as is.
     */
    private String convertToNewAddr(String addr, String oldDomain, String newDomain) {
        String[] parts = EmailUtil.getLocalPartAndDomain(addr);
        if (parts == null) {
            assert(false);
            warn("convertToNewAddr", "encountered invalid address", "addr=[%s]", addr);
            return null;
        }
            
        String local = parts[0];
        String domain = parts[1];
        if (domain.equals(oldDomain))
            return local + "@" + newDomain;
        else
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
}