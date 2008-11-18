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
    
    /*
    enum EntryType {
        account,
        cos;
        
        public static EntryType fromString(String s) throws ServiceException {
            try {
                return EntryType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR("unknown entry type: " + s, e);
            }
        }
    }
    */
    
    static void initKnownAdminRights(RightManager rm) throws ServiceException {
        
        R_PSEUDO_GET_ATTRS = new AttrRight("PSEUDO_GET_ATTRS", RightType.getAttrs);
        R_PSEUDO_SET_ATTRS = new AttrRight("PSEUDO_SET_ATTRS", RightType.setAttrs);
        
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
    
    static AdminRight newAdminRight(String name, RightType rightType) {
        if (rightType == RightType.getAttrs || rightType == RightType.setAttrs)
            return new AttrRight(name, rightType);
        else if (rightType == RightType.combo)
            return new ComboRight(name, rightType);
        else
            return new AdminRight(name, rightType);
    }
    
    protected AdminRight(String name, RightType rightType) {
        super(name, rightType);
    }

    /*
    String dump(StringBuilder sb) {
        // nothing in user right to dump
        return super.dump(sb);
    }
    */
}
