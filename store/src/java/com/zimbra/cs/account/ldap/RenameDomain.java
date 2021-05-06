/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.account.ldap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Alias;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.cs.account.SearchDirectoryOptions.ObjectType;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.ldap.entry.LdapEntry;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.ldap.ILdapContext;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.soap.admin.type.CacheEntryType;


public class RenameDomain {

    public static abstract class RenameDomainLdapHelper {
        protected LdapProv mProv;
        protected ILdapContext mZlc;

        public RenameDomainLdapHelper(LdapProv prov, ILdapContext zlc) {
            mProv = prov;
            mZlc = zlc;
        }

        public abstract Account getAccountById(String id) throws ServiceException;
        public abstract DistributionList getDistributionListById(String id) throws ServiceException;
        public abstract DynamicGroup getDynamicGroupById(String id) throws ServiceException;

        public abstract void createEntry(String dn, Map<String, Object> attrs) throws ServiceException;
        public abstract void deleteEntry(String dn) throws ServiceException;
        public abstract void renameEntry(String oldDn, String newDn) throws ServiceException;

        public abstract void searchDirectory(SearchDirectoryOptions options, NamedEntry.Visitor visitor)
        throws ServiceException;

        public abstract void modifyLdapAttrs(Entry entry, Map<String, ? extends Object> attrs)
        throws ServiceException;

        public abstract void renameAddressesInAllDistributionLists(Map<String, String> changedPairs);
        public abstract void renameXMPPComponent(String zimbraId, String newName) throws ServiceException;
    }

    private static final Log sRenameDomainLog = LogFactory.getLog("zimbra.provisioning.renamedomain");

    private final LdapProv mProv;
    private final RenameDomainLdapHelper mLdapHelper;
    private final Domain mOldDomain;
    private final String mOldDomainId;   // save old domain id because we still need it after the old domain is deleted
    private final String mOldDomainName; // save old domain name because we still need it after the old domain is deleted
    private final String mNewDomainName;

    public RenameDomain(LdapProv prov, RenameDomainLdapHelper ldapHelper,
            Domain oldDomain, String newDomainName) {
        mProv = prov;
        mLdapHelper = ldapHelper;
        mOldDomain = oldDomain;
        mOldDomainId = mOldDomain.getId();
        mOldDomainName = mOldDomain.getName();
        mNewDomainName = newDomainName;
    }

    private RenameDomainVisitor getVisitor(RenamePhase phase) {
        return new RenameDomainVisitor(mProv, mLdapHelper, mOldDomainName, mNewDomainName, phase);
    }


    public void execute() throws ServiceException {

        debug("Renaming domain %s(%s) to %s", mOldDomainName, mOldDomainId, mNewDomainName);

        RenameInfo renameInfo = beginRenameDomain();
        RenamePhase startingPhase = renameInfo.phase();
        RenamePhase phase = RenamePhase.FIX_FOREIGN_DL_MEMBERS;

        /*
         * 1. create the new domain
         */
        Domain newDomain = createNewDomain();
        debug("new domain: %s(%s)", newDomain.getName(), newDomain.getId());

        /*
         * 2. move all accounts, DLs, dynamic groups, and aliases
         */
        RenameDomainVisitor visitor;

        // first phase, move DLs, dynamic groups, and accounts and their aliases from
        // the old domain into the new domain
        phase = RenamePhase.RENAME_ENTRIES;
        if (phase.ordinal() >= startingPhase.ordinal()) {
            debug("Entering phase " + phase.toString());
            // don't need to setPhase for the first first, it was set or got from beginRenameDomain
            visitor = getVisitor(phase);

            SearchDirectoryOptions options = new SearchDirectoryOptions();
            options.setDomain(mOldDomain);
            options.setOnMaster(true);
            options.setFilterString(FilterId.RENAME_DOMAIN, null);
            options.setTypes(ObjectType.accounts, ObjectType.resources,
                    ObjectType.distributionlists, ObjectType.dynamicgroups);
            mLdapHelper.searchDirectory(options, visitor);
        }

        // second phase, go thru aliases that have not been moved yet, by now aliases
        // left in the domain should be aliases with target in other domains
        phase = RenamePhase.FIX_FOREIGN_ALIASES;
        if (phase.ordinal() >= startingPhase.ordinal()) {
            debug("Entering phase " + phase.toString());
            renameInfo.setPhase(phase);
            renameInfo.write(mProv, mOldDomain);
            visitor = getVisitor(phase);

            SearchDirectoryOptions options = new SearchDirectoryOptions();
            options.setDomain(mOldDomain);
            options.setOnMaster(true);
            options.setFilterString(FilterId.RENAME_DOMAIN, null);
            options.setTypes(ObjectType.aliases);
            mLdapHelper.searchDirectory(options, visitor);
        }

        // third phase, go thru DLs and accounts in the *new* domain,
        // rename the addresses in all DLs
        //     - the addresses to be renamed are: the DL/account's main address and
        //       all the aliases that were moved to the new domain
        //     - by now the DLs to modify should be those in other domains, because
        //       members of DLs in the old domain (now new domain) have been updated
        //       in the first pass.
        phase = RenamePhase.FIX_FOREIGN_DL_MEMBERS;
        if (phase.ordinal() >= startingPhase.ordinal()) {
            debug("Entering phase " + phase.toString());
            renameInfo.setPhase(phase);
            renameInfo.write(mProv, mOldDomain);
            visitor = getVisitor(phase);

            SearchDirectoryOptions options = new SearchDirectoryOptions();
            options.setDomain(newDomain);
            options.setOnMaster(true);
            options.setFilterString(FilterId.RENAME_DOMAIN, null);
            options.setTypes(ObjectType.accounts, ObjectType.resources, ObjectType.distributionlists);
            mLdapHelper.searchDirectory(options, visitor);
        }

        /*
         * 3. Delete the old domain
         */
        debug("Deleting old domain %s(%s)", mOldDomainName, mOldDomainId);
        // save zimbraDefaultDomainName because deleteDomain will erase it if it is the old domain
        String curDefaultDomain = mProv.getConfig().getAttr(Provisioning.A_zimbraDefaultDomainName);
        mProv.deleteDomainAfterRename(mOldDomainId);

        /*
         * 4. Modify system accounts that had been renamed
         */
        updateGlobalConfigSettings(curDefaultDomain);

        /*
         * 5. activate the new domain
         *    - restore zimbraId to the id of the old domain on the new domain
         *    - activate/enable the new domain
         */
        endRenameDomain(newDomain, mOldDomainId);

        /*
         * 6. fixup ZMPPComponments pointing to the renamed domain
         */
        fixupXMPPComponents();

        /*
         * 7. flush account cache on all servers
         */
        flushCacheOnAllServers(CacheEntryType.account);
    }

    public static enum RenamePhase {
        /*
         * Note: the following text is written in zimbraDomainRenameInfo -
         * change would require migration!!
         */
        RENAME_ENTRIES,
        FIX_FOREIGN_ALIASES,
        FIX_FOREIGN_DL_MEMBERS;

        public static RenamePhase fromString(String s) throws ServiceException {
            try {
                return RenamePhase.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.FAILURE("unknown phase: "+s, e);
            }
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
        private final String mSrcDomainName;
        private final String mDestDomainName;
        private RenamePhase mPhase;

        // convenient var so we don't need to check mSrcDomainName/mDestDomainName
        // when determining whether thsi is for src or desc
        private final boolean mIsSrc;

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
             *         SRC,{phase}:{destination-domain-name}
             *     On the destination(new) domain:
             *         DEST:{source-domain-name}
             */

            String renameInfo = domain.getAttr(Provisioning.A_zimbraDomainRenameInfo);
            if (StringUtil.isNullOrEmpty(renameInfo)) {
                debug("RenameInfo.load: domain=%s(%s), %s=not set",
                        domain.getName(), domain.getId(), Provisioning.A_zimbraDomainRenameInfo);
                return null;
            }
            debug("RenameInfo.load: domain=%s(%s), %s=%s",
                    domain.getName(), domain.getId(), Provisioning.A_zimbraDomainRenameInfo, renameInfo);

            int idx = renameInfo.indexOf(COLON);
            if (idx == -1) {
                throw ServiceException.FAILURE("invalid value in " +
                        Provisioning.A_zimbraDomainRenameInfo + ": " + renameInfo +
                        " missing " + COLON, null);
            }
            String statusPart = renameInfo.substring(0, idx);
            String domainName = renameInfo.substring(idx+1);
            if (StringUtil.isNullOrEmpty(domainName)) {
                throw ServiceException.FAILURE("invalid value in " +
                        Provisioning.A_zimbraDomainRenameInfo + ": " + renameInfo +
                        " missing domain name", null);
            }

            idx = statusPart.indexOf(COMMA);
            String srcOrDest = statusPart;
            RenamePhase phase = null;
            if (idx != -1) {
                srcOrDest = statusPart.substring(0, idx);
                phase = RenamePhase.fromString(statusPart.substring(idx+1));
            }

            if (srcOrDest.equals(SRC)) {
                if (!expectingSrc) {
                    throw ServiceException.FAILURE("invalid value in " +
                            Provisioning.A_zimbraDomainRenameInfo + ": " + renameInfo +
                            " missing " + DEST + " keyword", null);
                }

                if (phase == null) {
                    throw ServiceException.FAILURE("invalid value in " +
                            Provisioning.A_zimbraDomainRenameInfo + ": " + renameInfo +
                            " missing phase info for source domain" , null);
                }

                return new RenameInfo(null, domainName, phase);
            } else {
                if (expectingSrc) {
                    throw ServiceException.FAILURE("invalid value in " +
                            Provisioning.A_zimbraDomainRenameInfo + ": " + renameInfo +
                            " missing " + SRC + " keyword", null);
                }

                return new RenameInfo(domainName, null, phase);
            }
        }

        public void write(LdapProv prov, Domain domain) throws ServiceException {
            HashMap<String, Object> attrs = new HashMap<String, Object>();

            String renameInfoStr;

            if (mIsSrc)
                renameInfoStr = encodeSrc();
            else
                renameInfoStr = encodeDest();
            attrs.put(Provisioning.A_zimbraDomainRenameInfo, renameInfoStr);

            debug("RenameInfo.write: domain=%s(%s), %s=%s",
                    domain.getName(), domain.getId(), Provisioning.A_zimbraDomainRenameInfo, renameInfoStr);
            prov.modifyAttrs(domain, attrs);
        }
    }

    private RenameInfo beginRenameDomain() throws ServiceException {

        // see if the domain is currently shutdown and/or being renamed
        boolean domainIsShutdown = mOldDomain.isShutdown();
        RenameInfo renameInfo = RenameInfo.load(mOldDomain, true);

        if (domainIsShutdown && renameInfo == null) {
            throw ServiceException.INVALID_REQUEST("domain " + mOldDomainName +
                    " is shutdown without rename domain info", null);
        }

        if (renameInfo != null && !renameInfo.destDomainName().equals(mNewDomainName)) {
            throw ServiceException.INVALID_REQUEST(
                    "domain " + mOldDomainName + " was being renamed to " +
                    renameInfo.destDomainName() +
                    " it cannot be renamed to " + mNewDomainName +
                    " until the previous rename is finished", null);
        }

        // okay, this is either a new rename or a restart of a previous rename that did not finish

        // mark domain shutdown and rejecting mails
        // mProv.modifyDomainStatus(mOldDomain, Provisioning.DOMAIN_STATUS_SHUTDOWN);
        debug("Locking old domain %s(%s)", mOldDomainName, mOldDomainId);
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_SHUTDOWN);
        attrs.put(Provisioning.A_zimbraMailStatus, Provisioning.MAIL_STATUS_DISABLED);
        mProv.modifyAttrs(mOldDomain, attrs, false, false);  // skip callback

        RenamePhase phase = RenamePhase.RENAME_ENTRIES;

        if (renameInfo == null) {
            // new rename
            renameInfo = new RenameInfo(null, mNewDomainName, phase);
            renameInfo.write(mProv, mOldDomain);
        } else {
            // restart of a previous rename that did not finish
            phase = renameInfo.phase();
        }

        flushCacheOnAllServers(CacheEntryType.domain);

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

        // use a new zimbraId so getDomainById of the old domain will not return this half baked domain
        domainAttrs.remove(Provisioning.A_zimbraId);
        domainAttrs.remove(Provisioning.A_zimbraDomainName);
        domainAttrs.remove(Provisioning.A_zimbraMailStatus);

        // will the new domain get a new create timestamp? or should it inherit the
        // old domain's timestamp?  use new timestamp seems more right.
        domainAttrs.remove(Provisioning.A_zimbraCreateTimestamp);

        // Remove DKIM parameter, user/admin need to recreate DKIM entries as per new domain name.
        domainAttrs.remove("DKIMDomain");
        domainAttrs.remove("DKIMIdentity");
        domainAttrs.remove("DKIMKey");
        domainAttrs.remove("DKIMSelector");
        domainAttrs.remove("DKIMPublicKey");

        // domain level system accounts should be updated to use the new domain name
        String curNotebookAcctName = (String)domainAttrs.get(Provisioning.A_zimbraNotebookAccount);
        String newNotebookAcctName = getNewAddress(curNotebookAcctName);
        if (curNotebookAcctName != null && newNotebookAcctName != null) {
            domainAttrs.remove(Provisioning.A_zimbraNotebookAccount);
            domainAttrs.put(Provisioning.A_zimbraNotebookAccount, newNotebookAcctName);
        }

        // the new domain is created shutdown and rejecting mails
        domainAttrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_SHUTDOWN);
        domainAttrs.put(Provisioning.A_zimbraMailStatus, Provisioning.MAIL_STATUS_DISABLED);

        Domain newDomain = null;
        try {
            debug("Creating new domain %s", mNewDomainName);
            newDomain = mProv.createDomain(mNewDomainName, domainAttrs);
        } catch (AccountServiceException e) {
            if (e.getCode().equals(AccountServiceException.DOMAIN_EXISTS)) {
                newDomain = mProv.get(Key.DomainBy.name, mNewDomainName);
                if (newDomain == null)  { // this should not happen
                    throw ServiceException.FAILURE("failed to load existing domain " + mNewDomainName, null);
                }

                // the new domain already exists, make sure it is the one that was being renamed to
                RenameInfo renameInfo = RenameInfo.load(newDomain, false);

                if (renameInfo == null) {
                    // no rename info, indicating that the new domain already exists, and was NOT created
                    // by a previous rename domain.  reactivate the old domain and throw an exception
                    endRenameDomain(mOldDomain, null);
                    throw ServiceException.INVALID_REQUEST("domain " + mNewDomainName + " already exists", null);
                }

                if (!renameInfo.srcDomainName().equals(mOldDomainName)) {
                    throw ServiceException.INVALID_REQUEST(
                            "domain " + mNewDomainName + " was being renamed from " +
                            renameInfo.srcDomainName() +
                            " it cannot be renamed from " + mOldDomainName +
                            " until the previous rename is finished" , null);
                }
                return newDomain;  // all is well
            } else
                throw e;

        }

        RenameInfo renameInfo = new RenameInfo(mOldDomainName, null, null);
        renameInfo.write(mProv, newDomain);
        return newDomain;
    }

    /*
     * activate the domain.
     * if domainId is not null, set domain's zimbraId to the id.
     */
    private void endRenameDomain(Domain domain, String domainId) throws ServiceException {

        debug("endRenameDomain domain=%s(%s), domainId=%s",
                domain.getName(), domain.getId(), domainId==null?"null":domainId);

        HashMap<String, Object> attrs = new HashMap<String, Object>();
        if (domainId != null)
            attrs.put(Provisioning.A_zimbraId, domainId);
        attrs.put(Provisioning.A_zimbraDomainRenameInfo, "");
        attrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_ACTIVE);
        attrs.put(Provisioning.A_zimbraMailStatus, Provisioning.MAIL_STATUS_ENABLED);
        mLdapHelper.modifyLdapAttrs(domain, attrs);  // skip callback

        flushCacheOnAllServers(CacheEntryType.domain);
    }

    static class RenameDomainVisitor implements NamedEntry.Visitor {
        private final LdapProv mProv;
        private final RenameDomainLdapHelper mLdapHelper;
        private final String mOldDomainName;
        private final String mNewDomainName;
        private final RenamePhase mPhase;

        private static final Set<String> sAddrContainsDomainOnly;

        static {
            sAddrContainsDomainOnly = new HashSet<String>();

            sAddrContainsDomainOnly.add(Provisioning.A_zimbraMailCatchAllAddress);
            sAddrContainsDomainOnly.add(Provisioning.A_zimbraMailCatchAllCanonicalAddress);
            sAddrContainsDomainOnly.add(Provisioning.A_zimbraMailCatchAllForwardingAddress);
        }

        private static final String[] sDLAttrsNeedRename = {
                Provisioning.A_mail,
                Provisioning.A_zimbraMailAlias,
                Provisioning.A_zimbraMailForwardingAddress,
                Provisioning.A_zimbraMailDeliveryAddress, // ?
                Provisioning.A_zimbraMailCanonicalAddress,
                Provisioning.A_zimbraMailCatchAllAddress,
                Provisioning.A_zimbraMailCatchAllCanonicalAddress,
                Provisioning.A_zimbraMailCatchAllForwardingAddress,
                Provisioning.A_zimbraPrefAllowAddressForDelegatedSender};

        private static final String[] sAcctAttrsNeedRename = {
                Provisioning.A_mail,
                Provisioning.A_zimbraMailAlias,
                Provisioning.A_zimbraMailForwardingAddress,
                Provisioning.A_zimbraMailDeliveryAddress, // ?
                Provisioning.A_zimbraMailCanonicalAddress,
                Provisioning.A_zimbraMailCatchAllAddress,
                Provisioning.A_zimbraMailCatchAllCanonicalAddress,
                Provisioning.A_zimbraMailCatchAllForwardingAddress,
                Provisioning.A_zimbraPrefAllowAddressForDelegatedSender};


        private static boolean addrContainsDomainOnly(String addr) {
            return (sAddrContainsDomainOnly.contains(addr));
        }

        private RenameDomainVisitor(LdapProv prov, RenameDomainLdapHelper ldapHelper,
                String oldDomainName, String newDomainName, RenamePhase phase) {
            mProv = prov;
            mLdapHelper= ldapHelper;
            mOldDomainName = oldDomainName;
            mNewDomainName = newDomainName;
            mPhase = phase;
        }

        @Override
        public void visit(NamedEntry entry) throws ServiceException {
            debug("(" + mPhase.toString() + ") visiting " + entry.getName());

            if (mPhase == RenamePhase.RENAME_ENTRIES) {
                handleEntry(entry);
            } else if (mPhase == RenamePhase.FIX_FOREIGN_ALIASES) {
                if (entry instanceof Alias)
                    handleForeignAlias(entry); // PHASE_FIX_FOREIGN_ALIASES
                else
                    assert(false);  // by now there should only be foreign aliases in the old domain
            } else if (mPhase == RenamePhase.FIX_FOREIGN_DL_MEMBERS) {
                handleForeignDLMembers(entry);
            }
        }

        private void handleEntry(NamedEntry entry) throws ServiceException {
            LdapEntry ldapEntry = (LdapEntry)entry;
            String[] parts = EmailUtil.getLocalPartAndDomain(entry.getName());

            Entry.EntryType entryType = entry.getEntryType();

            String newDn = null;

            try {
                if (Entry.EntryType.ACCOUNT == entryType || Entry.EntryType.CALRESOURCE == entryType) {
                    newDn = mProv.getDIT().accountDNRename(ldapEntry.getDN(), parts[0], mNewDomainName);
                } else if (Entry.EntryType.DISTRIBUTIONLIST == entryType) {
                    newDn = mProv.getDIT().distributionListDNRename(ldapEntry.getDN(), parts[0], mNewDomainName);
                } else if (Entry.EntryType.DYNAMICGROUP == entryType) {
                    newDn = mProv.getDIT().dynamicGroupDNRename(ldapEntry.getDN(), parts[0], mNewDomainName);
                } else {
                    warn((Throwable) null, "handleEntry", "encountered invalid entry type", "entry=[%s]", entry.getName());
                    return;
                }
            } catch (ServiceException e) {
                warn(e, "handleEntry", "cannot get new DN, entry not handled", "entry=[%s]", entry.getName());
                return;
            }

            // Step 1. move the all aliases of the entry that are in the old domain to the new domain
            String[] aliases = null;

            if (Entry.EntryType.ACCOUNT == entryType) {
                aliases = ((Account)entry).getAliases();
            } else if (Entry.EntryType.CALRESOURCE == entryType) {
                aliases = ((CalendarResource)entry).getAliases();
            } else if (Entry.EntryType.DISTRIBUTIONLIST == entryType) {
                aliases = ((DistributionList)entry).getAliases();
            } else if (Entry.EntryType.DYNAMICGROUP == entryType) {
                aliases = ((DynamicGroup)entry).getAliases();
            } else {
                warn((Throwable) null, "handleEntry",
                        "encountered invalid entry type", "entry=[%s]", entry.getName());
                return;
            }
            handleAliases(entry, aliases, newDn);

            // Step 2. move the entry to the new domain and fixup all the addr attrs that
            //         contain the old domain
            String oldDn = ((LdapEntry)entry).getDN();
            moveEntry(entry, oldDn, newDn);
        }

        /*
         *
         */
        private void handleAliases(NamedEntry targetEntry, String[] aliases, String newTargetDn) {
            LdapEntry ldapEntry = (LdapEntry)targetEntry;
            String oldDn = ldapEntry.getDN();

            // move aliases in the old domain if there are any
            for (int i=0; i<aliases.length; i++) {

                // for dl and dynamic group, the main dl addr is also in the zimbraMailAlias.
                // To be consistent with account, we don't move that when we move aliases;
                // and will move it when we move the entry itself
                if (aliases[i].equals(targetEntry.getName()))
                    continue;

                String[] parts = EmailUtil.getLocalPartAndDomain(aliases[i]);
                if (parts == null) {
                    assert(false);
                    warn("moveEntry", "encountered invalid alias address",
                            "alias=[%s], entry=[%s]", aliases[i], targetEntry.getName());
                    continue;
                }
                String aliasLocal = parts[0];
                String aliasDomain = parts[1];
                if (aliasDomain.equals(mOldDomainName)) {
                    // move the alias
                    // ug, aliasDN and aliasDNRename also throw ServiceExeption -
                    // declared vars outside the try block so we can log it in the catch blocks
                    String oldAliasDn = "";
                    String newAliasDn = "";
                    try {
                        oldAliasDn = mProv.getDIT().aliasDN(oldDn, mOldDomainName, aliasLocal, mOldDomainName);
                        newAliasDn = mProv.getDIT().aliasDNRename(newTargetDn, mNewDomainName, aliasLocal+"@"+mNewDomainName);
                        if (!oldAliasDn.equals(newAliasDn)) {
                            mLdapHelper.renameEntry(oldAliasDn, newAliasDn);
                        }
                    } catch (ServiceException e) {
                        // log the error and continue
                        warn(e, "moveEntry", "alias not moved",
                                "alias=[%s], entry=[%s], oldAliasDn=[%s], newAliasDn=[%s]",
                                aliases[i], targetEntry.getName(), oldAliasDn, newAliasDn);
                    }
                }
            }
        }

        private void moveEntry(NamedEntry entry, String oldDn, String newDn) {
            Entry.EntryType entryType = entry.getEntryType();
            String entryId = entry.getId();

            NamedEntry refreshedEntry = null;

            if (!oldDn.equals(newDn)) {
                // move the entry
                try {
                    mLdapHelper.renameEntry(oldDn, newDn);
                } catch (ServiceException e) {
                    warn(e, "moveEntry", "renameEntry failed",
                            "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
                }

                // refresh for the new DN
                // (the entry will be read from the master, since we forced using LDAP
                // master for the rename domain process - bug 56768)
                // do not catch here, if we can't refresh - we can't modify,
                // just let it throw and proceed to the next entry
                try {
                    if (Entry.EntryType.ACCOUNT == entryType || Entry.EntryType.CALRESOURCE == entryType) {
                        refreshedEntry = mLdapHelper.getAccountById(entryId);
                    } else if (Entry.EntryType.DISTRIBUTIONLIST == entryType) {
                        refreshedEntry = mLdapHelper.getDistributionListById(entryId);
                    } else if (Entry.EntryType.DYNAMICGROUP == entryType) {
                        refreshedEntry = mLdapHelper.getDynamicGroupById(entryId);
                    }
                } catch (ServiceException e) {
                    warn(e, "moveEntry", "failed to get entry by id after move, entry not modified",
                            "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
                    // if we can't refresh - we can't modify, just return and proceed to the next entry
                    return;
                }
            }

            if (refreshedEntry == null) {
                warn((Throwable) null, "moveEntry", "entry not found after rename, entry not modified",
                        "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
                return;
            }

            // modify the entry in the new domain
            Map<String, Object> fixedAttrs = fixupAddrs(entry, sDLAttrsNeedRename);

            // modify the entry
            try {
                mLdapHelper.modifyLdapAttrs(refreshedEntry, fixedAttrs);
            } catch (ServiceException e) {
                warn(e, "moveEntry", "modifyAttrsInternal",
                        "entry=[%s], oldDn=[%s], newDn=[%s]", entry.getName(), oldDn, newDn);
            }
        }

        private Map<String, Object> fixupAddrs(NamedEntry entry, String[] attrsNeedRename)  {

            // replace the addr attrs
            Map<String, Object> attrs = Maps.newHashMap(entry.getAttrs(false));

            // remove the pseudo attr on dynamic group
            if (entry instanceof DynamicGroup) {
                attrs.remove(Provisioning.A_member);
            }

            for (String attr : attrsNeedRename) {
                boolean addrCanBeDomainOnly = addrContainsDomainOnly(attr);

                String[] values = entry.getMultiAttr(attr, false);
                if (values.length > 0) {
                    Set<String> newValues = new HashSet<String>();
                    for (int i=0; i<values.length; i++) {
                        String newValue = convertToNewAddr(values[i],
                                mOldDomainName, mNewDomainName, addrCanBeDomainOnly);
                        if (newValue != null) {
                            newValues.add(newValue);
                        }
                    }

                    // replace the attr with the new values
                    // if there is any address format error and the address cannot be converted,
                    // whatever the current value will be carried to the new entry.
                    if (newValues.size() > 0) {
                        attrs.put(attr, newValues.toArray(new String[newValues.size()]));
                    }
                }
            }

            return attrs;
        }

        /*
         * given an email address, and old domain name and a new domain name, returns:
         *   - if the domain of the address is the same as the old domain,
         *     returns localpart-of-addr@new-domain
         *   - otherwise returns the email addr as is.
         *
         *   Note, for some addresses they can be in the @domain format (no local part)
         */
        private String convertToNewAddr(String address, String oldDomain,
                String newDomain, boolean addrCanBeDomainOnly) {
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
               if (addr.charAt(0) == '@') {
                   domain = addr.substring(1);
               }
            }

            if (domain == null) {
                warn("convertToNewAddr", "encountered invalid address", "addr=[%s]", addr);
                return null;
            }

            if (domain.equals(oldDomain)) {
                if (local != null) {
                    return local + "@" + newDomain;
                } else {
                    return "@" + newDomain;
                }
            } else {
                return addr;
            }
        }

        /*
         * aliases in the old domain with target in other domains
         */
        private void handleForeignAlias(NamedEntry entry) {
            Alias alias = (Alias)entry;
            NamedEntry targetEntry = null;
            try {
                targetEntry = mProv.searchAliasTarget(alias, false);
            } catch (ServiceException e) {
                warn(e, "handleForeignAlias", "target entry not found for alias" + "alias=[%s], target=[%s]",
                        alias.getName(), alias.getAttr(Provisioning.A_zimbraAliasTargetId));
                return;
            }

            // orphan alias
            if (targetEntry == null) {
                warn("handleForeignAlias", "encountered orphan alias", "alias=[%s]", alias.getName());
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
                } else if (targetEntry instanceof DynamicGroup) {
                    DynamicGroup dynGroup = (DynamicGroup)targetEntry;
                    fixupForeignTarget(dynGroup, aliasOldAddr, aliasNewAddr);
                } else {
                    warn("handleForeignAlias", "encountered invalid alias target type", "target=[%s]", targetName);
                    return;
                }
            }
        }

        private void fixupForeignTarget(DistributionList targetEntry,
                String aliasOldAddr,  String aliasNewAddr) {
            try {
                mProv.removeAlias(targetEntry, aliasOldAddr);
            } catch (ServiceException e) {
                warn("fixupTargetInOtherDomain", "cannot remove alias for dl" +
                        "dl=[%s], aliasOldAddr=[%s], aliasNewAddr=[%s]",
                        targetEntry.getName(), aliasOldAddr, aliasNewAddr);
            }

            // continue doing add even if remove failed
            try {
                mProv.addAlias(targetEntry, aliasNewAddr);
            } catch (ServiceException e) {
                warn("fixupTargetInOtherDomain", "cannot add alias for dl" +
                        "dl=[%s], aliasOldAddr=[%s], aliasNewAddr=[%s]",
                        targetEntry.getName(), aliasOldAddr, aliasNewAddr);
            }
        }

        private void fixupForeignTarget(DynamicGroup targetEntry,
                String aliasOldAddr,  String aliasNewAddr) {
            try {
                mProv.removeGroupAlias(targetEntry, aliasOldAddr);
            } catch (ServiceException e) {
                warn("fixupTargetInOtherDomain", "cannot remove alias for dynamic group" +
                        "group=[%s], aliasOldAddr=[%s], aliasNewAddr=[%s]",
                        targetEntry.getName(), aliasOldAddr, aliasNewAddr);
            }

            // continue doing add even if remove failed
            try {
                mProv.addGroupAlias(targetEntry, aliasNewAddr);
            } catch (ServiceException e) {
                warn("fixupTargetInOtherDomain", "cannot add alias for dynamic group" +
                        "group=[%s], aliasOldAddr=[%s], aliasNewAddr=[%s]",
                        targetEntry.getName(), aliasOldAddr, aliasNewAddr);
            }
        }

        private void fixupForeignTarget(Account targetEntry,
                String aliasOldAddr, String aliasNewAddr) {
            try {
                mProv.removeAlias(targetEntry, aliasOldAddr);
            } catch (ServiceException e) {
                warn("fixupTargetInOtherDomain", "cannot remove alias for account" +
                        "acct=[%s], aliasOldAddr=[%s], aliasNewAddr=[%s]",
                        targetEntry.getName(), aliasOldAddr, aliasNewAddr);
            }

            // we want to continue doing add even if remove failed
            try {
                mProv.addAlias(targetEntry, aliasNewAddr);
            } catch (ServiceException e) {
                warn("fixupTargetInOtherDomain", "cannot add alias for account" +
                        "acct=[%s], aliasOldAddr=[%s], aliasNewAddr=[%s]",
                        targetEntry.getName(), aliasOldAddr, aliasNewAddr);
            }
        }

        /*
         * replace "old addrs of DL/accounts and their aliases that are members
         * of DLs in other domains" to the new addrs
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

            mLdapHelper.renameAddressesInAllDistributionLists(changedPairs);
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

    /*
     * Given an address, return the new address to be in the new domain
     * if domain of the old address is the same as the old domain name.
     *
     * Return null if addr:
     *     - is null
     *     - cannot be parsed
     *     - does not contain the old domain name
     */
    private String getNewAddress(String addr) {

        if (addr != null) {
            String[] parts = EmailUtil.getLocalPartAndDomain(addr);
            if (parts == null) {
                warn("getNewAccountName", "encountered invalid address", "addr=[%s]", addr);
                return null;
            }

            String localPart = parts[0];
            String domain = parts[1];
            if (!domain.equals(mOldDomainName))
                return null;

            return localPart + "@" + mNewDomainName;
        }
        return null;
    }

    /*
     * Given a domain name, return the new domain name if the given domain
     * name is the old domain name.
     *
     * Return null if domainName:
     *     - is null
     *     - does not contain the old domain name
     */
    private String getNewDomain(String domainName) {
        if (domainName != null) {
            if (!domainName.equals(mOldDomainName))
                return null;

            return mNewDomainName;
        }
        return null;
    }

    private void updateSystemAccount(Entry entry, String attrName, Map<String, Object> attrMap) {
        String curAddr = entry.getAttr(attrName);
        String newAddr = getNewAddress(curAddr);
        if (curAddr != null && newAddr != null) {
            attrMap.put(attrName, newAddr);
        }
    }

    // TODO: should modify FlushCache to take more than one entry types, so that we
    // can also flsuh the global config cache in the same request when we flush accounts.
    private void updateGlobalConfigSettings(String curDefaultDomainName) {

        try {
            Config config = mProv.getConfig();

            HashMap<String, Object> attrMap = new HashMap<String, Object>();
            updateSystemAccount(config, Provisioning.A_zimbraNotebookAccount, attrMap);
            updateSystemAccount(config, Provisioning.A_zimbraSpamIsSpamAccount, attrMap);
            updateSystemAccount(config, Provisioning.A_zimbraSpamIsNotSpamAccount, attrMap);
            updateSystemAccount(config, Provisioning.A_zimbraAmavisQuarantineAccount, attrMap);

            String newDomainName = getNewDomain(curDefaultDomainName);
            if (curDefaultDomainName != null && newDomainName != null)
                attrMap.put(Provisioning.A_zimbraDefaultDomainName, newDomainName);

            mProv.modifyAttrs(config, attrMap);
            flushCacheOnAllServers(CacheEntryType.config);

        } catch (ServiceException e) {
            // just log it an continue
            warn("failed to update system accounts on global config", e);
        }
    }

    private void fixupXMPPComponents() throws ServiceException{

        int domainLen = mOldDomainName.length();

        for (XMPPComponent xmpp : mProv.getAllXMPPComponents()) {
            if (mOldDomainId.equals(xmpp.getDomainId())) {
                String curName = xmpp.getName();
                if (curName.endsWith(mOldDomainName)) {
                    String newName = curName.substring(0, curName.length() - domainLen) + mNewDomainName;
                    debug("Renaming XMPP component " + curName + " to " + newName);
                    mLdapHelper.renameXMPPComponent(xmpp.getId(), newName);
                }
            }
        }
    }

    private void flushCacheOnAllServers(CacheEntryType type) throws ServiceException {
        SoapProvisioning soapProv = new SoapProvisioning();
        String adminUrl = null;

        for (Server server : mProv.getAllMailClientServers()) {

            try {
                adminUrl = URLUtil.getAdminURL(server, AdminConstants.ADMIN_SERVICE_URI, true);
            } catch (ServiceException e) {
                warn(e, "flushCacheOnAllServers", "", "type=[%s]", type);
                continue;
            }

            soapProv.soapSetURI(adminUrl);

            try {
                soapProv.soapZimbraAdminAuthenticate();
                soapProv.flushCache(type, null);

            } catch (ServiceException e) {
                warn(e, "flushCacheOnAllServers", "", "type=[%s] server=[%s]", type, server.getName());
            }
        }
    }

    private static void warn(Object o, Throwable t) {
        sRenameDomainLog.warn(o, t);
    }

    private static void warn(String funcName, String desc, String format, Object ... objects) {
        warn(null, funcName,  desc,  format, objects);
    }

    private static void warn(Throwable t, String funcName, String desc, String format, Object ... objects) {
        if (sRenameDomainLog.isWarnEnabled())
            sRenameDomainLog.warn(String.format(funcName + "(" + desc + "):" + format, objects), t);
    }

    private static void debug(String format, Object ... objects) {
        if (sRenameDomainLog.isDebugEnabled())
            sRenameDomainLog.debug(String.format(format, objects));
    }

    private static void info(String format, Object ... objects) {
        sRenameDomainLog.info(String.format(format, objects));
    }

}