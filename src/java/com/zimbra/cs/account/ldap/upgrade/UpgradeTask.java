/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;

public enum UpgradeTask {
    BUG_10287(zimbraPrefCalendarReminderSendEmail.class),
    BUG_14531(ZimbraGalLdapFilterDef_zimbraSync.class),
    BUG_18277(AdminRights.class),
    BUG_22033(ZimbraCreateTimestamp.class),
    BUG_27075(CosAndGlobalConfigDefault.class),   // e.g. -b 27075 5.0.12
    BUG_29978(DomainPublicServiceProtocolAndPort.class),
    // BUG_31284(ZimbraPrefFromDisplay.class),
    BUG_31694(ZimbraMessageCacheSize.class),
    BUG_32557(DomainObjectClassAmavisAccount.class),
    BUG_32719(ZimbraHsmPolicy.class),
    BUG_33814(ZimbraMtaAuthEnabled.class),
    BUG_41000(ZimbraGalLdapFilterDef_zimbraAutoComplete_zimbraSearch.class),
    BUG_42877(ZimbraGalLdapAttrMap.class),
    BUG_42896(ZimbraMailQuota_constraint.class),
    BUG_43147(GalSyncAccountContactLimit.class),
    BUG_46297(ZimbraContactHiddenAttributes.class),
    BUG_46883(ZimbraContactRankingTableSize.class),
    BUG_46961(ZimbraGalLdapAttrMap_fullName.class),
    BUG_42828(ZimbraGalLdapAttrMap_ZimbraContactHiddenAttributes_externalCalendarResource.class),
    BUG_43779(ZimbraGalLdapFilterDef_zimbraGroup.class),
    BUG_50258(ZimbraMtaSaslAuthEnable.class),
    BUG_50465(DisableBriefcase.class);
    
    
    private Class mUpgradeClass;
    
    UpgradeTask(Class klass) {
        mUpgradeClass = klass;
    }

    static UpgradeTask fromString(String bugNumber) throws ServiceException {
        String bug = "BUG_" + bugNumber;
        
        try {
            return UpgradeTask.valueOf(bug);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    String getBugNumber() {
        String bug = this.name();
        return bug.substring(4);
    }
    
    LdapUpgrade getUpgrader() throws ServiceException {
        try {
            Object obj = mUpgradeClass.newInstance();
            if (obj instanceof LdapUpgrade) {
                LdapUpgrade ldapUpgrade = (LdapUpgrade)obj;
                ldapUpgrade.setBug(getBugNumber());
                return ldapUpgrade;
            }
        } catch (IllegalAccessException e) {
            throw ServiceException.FAILURE("IllegalAccessException", e);
        } catch (InstantiationException e) {
            throw ServiceException.FAILURE("InstantiationException", e);
        }
        throw ServiceException.FAILURE("unable to instantiate upgrade object", null);
    }

    public static void main(String[] args) throws ServiceException {
        // sanity test
        for (UpgradeTask upgradeTask : UpgradeTask.values()) {
            LdapUpgrade upgrade = upgradeTask.getUpgrader();
            
            System.out.println("====================================");
            System.out.println("Testing " + upgrade.getBug() + " ");
            
            upgrade.setVerbose(true);
            upgrade.doUpgrade();
        }
    }
    
}

