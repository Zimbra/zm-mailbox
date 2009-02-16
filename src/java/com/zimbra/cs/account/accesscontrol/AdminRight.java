package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;

public abstract class AdminRight extends Right {
    // pseudo rights, should never actually be granted on any entry 
    public static AdminRight R_PSEUDO_GET_ATTRS;
    public static AdminRight R_PSEUDO_SET_ATTRS;
    
    // known rights
    public static AdminRight R_addAccountAlias;
    public static AdminRight R_addDistributionListAlias;
    public static AdminRight R_adminLoginAs;
    public static AdminRight R_getAccount;
    public static AdminRight R_getAccountInfo;
    public static AdminRight R_getAccountMembership;
    public static AdminRight R_getGlobalConfig;
    public static AdminRight R_getDomain;
    public static AdminRight R_getMailboxInfo;
    public static AdminRight R_createAccount;
    public static AdminRight R_createAlias;
    public static AdminRight R_renameAccount;
    public static AdminRight R_viewGAL;

    
    static void initKnownAdminRights(RightManager rm) throws ServiceException {
        
        R_PSEUDO_GET_ATTRS = newAdminSystemRight("PSEUDO_GET_ATTRS", RightType.getAttrs);
        R_PSEUDO_SET_ATTRS = newAdminSystemRight("PSEUDO_SET_ATTRS", RightType.setAttrs);
        
        R_addAccountAlias      = rm.getAdminRight(RT_addAccountAlias);
        R_addDistributionListAlias = rm.getAdminRight(RT_addDistributionListAlias);
        R_adminLoginAs         = rm.getAdminRight(RT_adminLoginAs);
        R_getAccount           = rm.getAdminRight(RT_getAccount);
        R_getAccountInfo       = rm.getAdminRight(RT_getAccountInfo);
        R_getAccountMembership = rm.getAdminRight(RT_getAccountMembership);
        R_getGlobalConfig      = rm.getAdminRight(RT_getGlobalConfig);
        R_getDomain            = rm.getAdminRight(RT_getDomain);
        R_getMailboxInfo       = rm.getAdminRight(RT_getMailboxInfo);
        R_createAccount        = rm.getAdminRight(RT_createAccount);
        R_createAlias          = rm.getAdminRight(RT_createAlias);
        R_renameAccount        = rm.getAdminRight(RT_renameAccount);
        R_viewGAL              = rm.getAdminRight(RT_viewGAL);

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
