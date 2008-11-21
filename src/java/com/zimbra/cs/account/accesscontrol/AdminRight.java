package com.zimbra.cs.account.accesscontrol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;

public class AdminRight extends Right {
    // pseudo rights, should never actually be granted on any entry 
    public static AdminRight R_PSEUDO_GET_ATTRS;
    public static AdminRight R_PSEUDO_SET_ATTRS;
    
    // known rights
    public static AdminRight R_createAccount;
    public static AdminRight R_renameAccount;
    public static AdminRight R_renameCalendarResource;
    public static AdminRight R_renameDistributionList;
    public static AdminRight R_renameCos;
    public static AdminRight R_renameServer;
    public static AdminRight R_deleteZimlet;
    public static AdminRight R_testGlobalConfigRemoveMe;
    public static AdminRight R_testGlobalGrantRemoveMe;
    
    public static AdminRight R_getAccount;
    public static AdminRight R_viewDummy;
    public static AdminRight R_modifyAccount;
    public static AdminRight R_configureQuota;
    public static AdminRight R_configureQuotaWithinLimit;
    
    public static AdminRight R_domainAdmin;

    static enum DefinedBy {
        system,
        custom;
    }
    
    static void initKnownAdminRights(RightManager rm) throws ServiceException {
        
        R_PSEUDO_GET_ATTRS = newAdminSystemRight("PSEUDO_GET_ATTRS", RightType.getAttrs);
        R_PSEUDO_SET_ATTRS = newAdminSystemRight("PSEUDO_SET_ATTRS", RightType.setAttrs);
        
        R_createAccount = rm.getAdminRight("createAccount");
        R_renameAccount = rm.getAdminRight("renameAccount");
        R_renameCalendarResource = rm.getAdminRight("renameCalendarResource");
        R_renameDistributionList = rm.getAdminRight("renameDistributionList");
        R_renameCos = rm.getAdminRight("renameCos");
        R_renameServer = rm.getAdminRight("renameServer");
        R_deleteZimlet = rm.getAdminRight("deleteZimlet");
        R_testGlobalConfigRemoveMe = rm.getAdminRight("testGlobalConfigRemoveMe");
        R_testGlobalGrantRemoveMe = rm.getAdminRight("testGlobalGrantRemoveMe");
        
        R_getAccount = rm.getAdminRight("getAccount");
        R_viewDummy = rm.getAdminRight("viewDummy");
        R_modifyAccount = rm.getAdminRight("modifyAccount");
        R_configureQuota = rm.getAdminRight("configureQuota");
        R_configureQuotaWithinLimit = rm.getAdminRight("configureQuotaWithinLimit");
        
        R_domainAdmin = rm.getAdminRight("domainAdmin");

    }
    

    
    private DefinedBy mDefinedBy;
    private String  mRightId;  // for custom right
    
    protected AdminRight(String name, RightType rightType, DefinedBy definedBy) {
        super(name, rightType);
        mDefinedBy = definedBy;
    }
    
    private void setId(String rightId) {
        mRightId = rightId;
    }
    
    static AdminRight newAdminSystemRight(String name, RightType rightType) {
        return newAdminRight(name, rightType, DefinedBy.system);
    }
        
    static AdminRight newAdminCustomRight(String name, RightType rightType, String rightZimbraId) {
        AdminRight right = newAdminRight(name, rightType, DefinedBy.custom);
        right.setId(rightZimbraId);
        return right;
    }
    
    private static AdminRight newAdminRight(String name, RightType rightType, DefinedBy definedBy) {
        if (rightType == RightType.getAttrs || rightType == RightType.setAttrs)
            return new AttrRight(name, rightType, definedBy);
        else if (rightType == RightType.combo)
            return new ComboRight(name, rightType, definedBy);
        else
            return new AdminRight(name, rightType, definedBy);
    }
    


    /*
    String dump(StringBuilder sb) {
        // nothing in user right to dump
        return super.dump(sb);
    }
    */
}
