package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;

public class UserRight extends Right {
    
    // known rights
    public static Right RT_invite;
    public static Right RT_viewFreeBusy;
    public static Right RT_loginAs;

    static void initKnownUserRights(RightManager rm) throws ServiceException {
        RT_invite = rm.getRight("invite");
        RT_viewFreeBusy = rm.getRight("viewFreeBusy");
        RT_loginAs = rm.getRight("loginAs");
    }
    
    UserRight(String name) {
        super(name);
    }
    
}
