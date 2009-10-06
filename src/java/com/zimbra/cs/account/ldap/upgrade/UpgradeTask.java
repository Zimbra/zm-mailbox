package com.zimbra.cs.account.ldap.upgrade;

import com.zimbra.common.service.ServiceException;

public enum UpgradeTask {
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
    BUG_41000(ZimbraGalLdapFilterDef_zimbraAutoComplete_zimbraSearch.class);
    
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

