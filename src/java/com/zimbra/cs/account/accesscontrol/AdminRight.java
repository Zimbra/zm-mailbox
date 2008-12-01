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
    }
    
    private String  mRightId;  // for custom right
    
    protected AdminRight(String name, RightType rightType) {
        super(name, rightType);
    }
    
    private void setId(String rightId) {
        mRightId = rightId;
    }
    
    static AdminRight newAdminSystemRight(String name, RightType rightType) {
        return newAdminRight(name, rightType);
    }

    
    private static AdminRight newAdminRight(String name, RightType rightType) {
        if (rightType == RightType.getAttrs || rightType == RightType.setAttrs)
            return new AttrRight(name, rightType);
        else if (rightType == RightType.combo)
            return new ComboRight(name, rightType);
        else
            return new AdminRight(name, rightType);
    }
    


    /*
    String dump(StringBuilder sb) {
        // nothing in user right to dump
        return super.dump(sb);
    }
    */
}
