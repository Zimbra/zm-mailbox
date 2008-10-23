package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;

public class UserRight extends Right {
    
    // known rights
    public static UserRight RT_invite;
    public static UserRight RT_viewFreeBusy;
    public static UserRight RT_loginAs;

    static void initKnownUserRights(RightManager rm) throws ServiceException {
        RT_invite = rm.getUserRight("invite");
        RT_viewFreeBusy = rm.getUserRight("viewFreeBusy");
        RT_loginAs = rm.getUserRight("loginAs");
    }
    
    UserRight(String name) {
        super(name);
    }
    
}
