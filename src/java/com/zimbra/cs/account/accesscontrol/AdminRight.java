package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;

public class AdminRight extends Right {
    
    static void initKnownAdminRights(RightManager rm) throws ServiceException {
    }
    
    AdminRight(String code) {
        super(code);
    }
    
}
