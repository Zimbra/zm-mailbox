package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;

public abstract class AdminRight extends Right {
    // pseudo rights, should never actually be granted on any entry 
    public static AdminRight R_PSEUDO_GET_ATTRS;
    public static AdminRight R_PSEUDO_SET_ATTRS;
    
    // known rights
    public static AdminRight R_addAccountAlias;
    public static AdminRight R_addCalendarResourceAlias;
    public static AdminRight R_addDistributionListAlias;
    public static AdminRight R_addDistributionListMember;
    public static AdminRight R_adminLoginAs;
    public static AdminRight R_checkDomainMXRecord;
    public static AdminRight R_countAccount;
    public static AdminRight R_createAccount;
    public static AdminRight R_createCalendarResource;
    public static AdminRight R_createCos;
    public static AdminRight R_createAlias;
    public static AdminRight R_createDistributionList;
    public static AdminRight R_createServer;
    public static AdminRight R_createSubDomain;
    public static AdminRight R_createTopDomain;
    public static AdminRight R_deleteAccount;
    public static AdminRight R_deleteAlias;
    public static AdminRight R_getAccount;
    public static AdminRight R_deleteCalendarResource;
    public static AdminRight R_deleteDistributionList;
    public static AdminRight R_getAccountInfo;
    public static AdminRight R_getAccountMembership;
    public static AdminRight R_getGlobalConfig;
    public static AdminRight R_getDomain;
    public static AdminRight R_getMailboxInfo;
    public static AdminRight R_renameAccount;
    public static AdminRight R_renameCalendarResource;
    public static AdminRight R_renameDistributionList;
    public static AdminRight R_removeAccountAlias;
    public static AdminRight R_removeCalendarResourceAlias;
    public static AdminRight R_removeDistributionListAlias;
    public static AdminRight R_removeDistributionListMember;
    public static AdminRight R_setAccountPassword;
    public static AdminRight R_setCalendarResourcePassword;
    public static AdminRight R_accessGAL;

    
    static void initKnownAdminRights(RightManager rm) throws ServiceException {
        
        R_PSEUDO_GET_ATTRS = newAdminSystemRight("PSEUDO_GET_ATTRS", RightType.getAttrs);
        R_PSEUDO_SET_ATTRS = newAdminSystemRight("PSEUDO_SET_ATTRS", RightType.setAttrs);
        
        R_addAccountAlias             = rm.getAdminRight(RT_addAccountAlias);
        R_addCalendarResourceAlias    = rm.getAdminRight(RT_addCalendarResourceAlias);
        R_addDistributionListAlias    = rm.getAdminRight(RT_addDistributionListAlias);
        R_addDistributionListMember   = rm.getAdminRight(RT_addDistributionListMember);
        R_adminLoginAs                = rm.getAdminRight(RT_adminLoginAs);
        R_checkDomainMXRecord         = rm.getAdminRight(RT_checkDomainMXRecord);
        R_countAccount                = rm.getAdminRight(RT_countAccount);
        R_createAccount               = rm.getAdminRight(RT_createAccount);
        R_createAlias                 = rm.getAdminRight(RT_createAlias);
        R_createCos                   = rm.getAdminRight(RT_createCos);
        R_createCalendarResource      = rm.getAdminRight(RT_createCalendarResource);
        R_createDistributionList      = rm.getAdminRight(RT_createDistributionList);
        R_createServer                = rm.getAdminRight(RT_createServer);
        R_createSubDomain             = rm.getAdminRight(RT_createSubDomain);
        R_createTopDomain             = rm.getAdminRight(RT_createTopDomain);
        R_deleteAccount               = rm.getAdminRight(RT_deleteAccount);
        R_deleteAlias                 = rm.getAdminRight(RT_deleteAlias);
        R_deleteCalendarResource      = rm.getAdminRight(RT_deleteCalendarResource);
        R_deleteDistributionList      = rm.getAdminRight(RT_deleteDistributionList);
        R_getAccount                  = rm.getAdminRight(RT_getAccount);
        R_getAccountInfo              = rm.getAdminRight(RT_getAccountInfo);
        R_getAccountMembership        = rm.getAdminRight(RT_getAccountMembership);
        R_getGlobalConfig             = rm.getAdminRight(RT_getGlobalConfig);
        R_getDomain                   = rm.getAdminRight(RT_getDomain);
        R_getMailboxInfo              = rm.getAdminRight(RT_getMailboxInfo);
        R_renameAccount               = rm.getAdminRight(RT_renameAccount);
        R_renameCalendarResource      = rm.getAdminRight(RT_renameCalendarResource);
        R_renameDistributionList      = rm.getAdminRight(RT_renameDistributionList);
        R_removeAccountAlias          = rm.getAdminRight(RT_removeAccountAlias);
        R_removeCalendarResourceAlias  = rm.getAdminRight(RT_removeCalendarResourceAlias);
        R_removeDistributionListAlias  = rm.getAdminRight(RT_removeDistributionListAlias);
        R_removeDistributionListMember = rm.getAdminRight(RT_removeDistributionListMember);
        R_setAccountPassword          = rm.getAdminRight(RT_setAccountPassword);
        R_setCalendarResourcePassword = rm.getAdminRight(RT_setCalendarResourcePassword);
        R_accessGAL                     = rm.getAdminRight(RT_accessGAL);

    }
    
    protected AdminRight(String name, RightType rightType) {
        super(name, rightType);
    }
    
    static AdminRight newAdminSystemRight(String name, RightType rightType) {
        return newAdminRight(name, rightType);
    }
    
    private static AdminRight newAdminRight(String name, RightType rightType) {
        if (rightType == RightType.getAttrs || rightType == RightType.setAttrs)
            return new AttrRight(name, rightType);
        else if (rightType == RightType.combo)
            return new ComboRight(name);
        else
            return new PresetRight(name);
    }
    
    /*
    String dump(StringBuilder sb) {
        // nothing in user right to dump
        return super.dump(sb);
    }
    */
}
