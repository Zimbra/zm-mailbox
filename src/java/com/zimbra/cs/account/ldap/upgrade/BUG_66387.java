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
package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.account.Key.GranteeBy;
import com.zimbra.common.account.Key.TargetBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.account.SearchDirectoryOptions.MakeObjectOpt;
import com.zimbra.cs.account.SearchDirectoryOptions.SortOpt;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.generated.RightConsts;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;

public class BUG_66387 extends UpgradeOp {

    private int numInspected;
    private int numFixed;

    BUG_66387() throws ServiceException {
        AccessManager.getInstance();  // get the annoying log message out of the way
    }
    
    @Override
    Description getDescription() {
        return new Description(
                this, 
                new String[] {Provisioning.A_zimbraAllowFromAddress, Provisioning.A_zimbraPrefAllowAddressForDelegatedSender}, 
                new EntryType[] {EntryType.ACCOUNT, EntryType.DISTRIBUTIONLIST},
                "see notes",
                "see notes",
                "Any internal account or distribution list address listed in " + Provisioning.A_zimbraAllowFromAddress +
                " attribute is converted to a grant of sendAs (for account) or sendAsDistList (for DL) right from the named " +
                "account or DL.  The address is added to the " + Provisioning.A_zimbraPrefAllowAddressForDelegatedSender +
                " attribute of the granting account/DL.");
    }

    @Override
    void doUpgrade() throws ServiceException {
        searchAndFixAccounts();
        printer.println("Number of accounts using " + Provisioning.A_zimbraAllowFromAddress + ": " + numInspected);
        printer.println("Number of accounts migrated: " + numFixed);
        printer.println("Migration completed");
    }

    private int searchAndFixAccounts() throws ServiceException {
        String[] attrsToGet = new String[] { Provisioning.A_zimbraAllowFromAddress };
        SearchDirectoryOptions searchOpts = new SearchDirectoryOptions(attrsToGet);
        searchOpts.setTypes(SearchDirectoryOptions.ObjectType.accounts, SearchDirectoryOptions.ObjectType.resources);
        searchOpts.setSortOpt(SortOpt.SORT_ASCENDING);
        searchOpts.setSortAttr(Provisioning.A_zimbraMailDeliveryAddress);
        searchOpts.setMakeObjectOpt(MakeObjectOpt.NO_DEFAULTS);
        searchOpts.setFilterString(FilterId.LDAP_UPGRADE, "(" + Provisioning.A_zimbraAllowFromAddress + "=*)");

        List<NamedEntry> accounts = prov.searchDirectory(searchOpts);
        for (int i = 0; i < accounts.size(); ++i) {
            NamedEntry entry = accounts.get(i);
            if (entry instanceof Account) {
                Account acct = (Account) entry;
                fixAccount(acct.getName(), acct.getAllowFromAddress());
            }
        }
        return accounts.size();
    }

    private void fixAccount(String name, String[] allowFromAddresses) throws ServiceException {
        ++numInspected;
        Account account = prov.get(AccountBy.name, name);
        if (account == null) {
            // this shouldn't happen
            printer.println("Account " + name + " not found!  Skipping.");
            return;
        }

        boolean modified = false;
        printer.println("# Account: " + account.getName());
        String[] addrs = account.getAllowFromAddress();
        printer.println("  Current value = " + StringUtil.join(", ", addrs));
        Set<String> remainingAddrs = new HashSet<String>();
        for (String addr : addrs) {
            NamedEntry entry = lookupEntry(addr);
            if (entry instanceof Account) {
                if (!entry.getId().equalsIgnoreCase(account.getId())) {
                    doGrant(entry, account, addr);
                    modified = true;
                } else {
                    printer.println("    - removing redundant address " + addr);
                    modified = true;
                }
            } else if (entry instanceof Group) {
                doGrant(entry, account, addr);
                modified = true;
            } else {
                remainingAddrs.add(addr);
            }
        }

        if (modified) {
            Map<String, Object> attrsMap = new HashMap<String, Object>();
            if (!remainingAddrs.isEmpty()) {
                String[] remaining = remainingAddrs.toArray(new String[0]);
                attrsMap.put(Provisioning.A_zimbraAllowFromAddress, remaining);
                printer.println("  New value = " + StringUtil.join(", ", remaining));
            } else {
                attrsMap.put(Provisioning.A_zimbraAllowFromAddress, "");
                printer.println("  New value = <unset>");
            }
            prov.modifyAttrs(account, attrsMap, false, false);
            ++numFixed;
        } else {
            printer.println("  No change needed");
        }
        printer.println();
    }

    private NamedEntry lookupEntry(String address) throws ServiceException {
        NamedEntry entry = null;
        String domain = EmailUtil.getValidDomainPart(address);
        if (domain != null) {
            Provisioning prov = Provisioning.getInstance();
            Domain internalDomain = prov.getDomain(DomainBy.name, domain, true);
            if (internalDomain != null) {
                if (prov.isDistributionList(address)) {
                    entry = prov.getGroupBasic(DistributionListBy.name, address);
                } else {
                    entry = prov.get(AccountBy.name, address);
                }
            }
        }
        return entry;
    }

    private void doGrant(NamedEntry grantor, Account grantee, String address) throws ServiceException {
        String grantorTypeLabel = "account";
        String targetType = TargetType.account.getCode();
        String right = RightConsts.RT_sendAs;
        if (grantor instanceof Group) {
            grantorTypeLabel = "list";
            targetType = TargetType.group.getCode();
            right = RightConsts.RT_sendAsDistList;
        }
        prov.grantRight(targetType, TargetBy.name, grantor.getName(),
                GranteeType.GT_USER.getCode(), GranteeBy.name, grantee.getName(), null, right, null);
        printer.println("    - " + grantorTypeLabel + " " + grantor.getName() + " granting " + right + " right to " + grantee.getName());

        // Add address to grantor's zimbraPrefAllowAddressForDelegatedSender if it's a new value.
        String[] currAddrs = grantor.getMultiAttr(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender);
        Set<String> addrsLowercase = new HashSet<String>();
        for (String a : currAddrs) {
            addrsLowercase.add(a.toLowerCase());
        }
        boolean add;
        if (addrsLowercase.isEmpty()) {
            // If currently unset, no need to add the main address because that's the default value.
            add = !grantor.getName().equalsIgnoreCase(address);
        } else {
            // If currently set, only add a unique value.
            add = !addrsLowercase.contains(address.toLowerCase());
        }
        if (add) {
            Map<String,Object> attrs = new HashMap<String,Object>();
            StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPrefAllowAddressForDelegatedSender, address);
            prov.modifyAttrs(grantor, attrs);
            printer.println("    - address " + address + " added to " + Provisioning.A_zimbraPrefAllowAddressForDelegatedSender +
                    " attribute of " + grantorTypeLabel + " " + grantor.getName());
        }
    }
}
