package com.zimbra.cs.account.ldap.upgrade;

import com.zimbra.common.service.ServiceException;

public enum UpgradeTask {
    BUG_14531(GalLdapFilterDef.class),
    BUG_18277(MigrateDomainAdmins.class),
    BUG_22033(SetZimbraCreateTimestamp.class),
    BUG_27075(SetCosAndGlobalConfigDefault.class),
    BUG_29978(DomainPublicServiceProtocolAndPort.class),
    // BUG_31284(SetZimbraPrefFromDisplay.class),
    BUG_31694(MigrateZimbraMessageCacheSize.class),
    BUG_32557(DomainObjectClassAmavisAccount.class),
    BUG_33814(MigrateZimbraMtaAuthEnabled.class);
    
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
    
    LdapUpgrade getUpgrader() throws ServiceException {
        try {
            Object obj = mUpgradeClass.newInstance();
            if (obj instanceof LdapUpgrade)
                return (LdapUpgrade)obj;
        } catch (IllegalAccessException e) {
            throw ServiceException.FAILURE("IllegalAccessException", e);
        } catch (InstantiationException e) {
            throw ServiceException.FAILURE("InstantiationException", e);
        }
        throw ServiceException.FAILURE("unable to instantiate upgrade object", null);
    }

}

