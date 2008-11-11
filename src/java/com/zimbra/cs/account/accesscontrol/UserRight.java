package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;

public class UserRight extends Right {
    
    // known rights
    public static UserRight RT_invite;
    public static UserRight RT_loginAs;
    public static UserRight RT_sendAs;
    public static UserRight RT_viewFreeBusy;
    
    static void initKnownUserRights(RightManager rm) throws ServiceException {
        RT_invite = rm.getUserRight("invite");
        RT_loginAs = rm.getUserRight("loginAs");
        RT_sendAs = rm.getUserRight("sendAs");
        RT_viewFreeBusy = rm.getUserRight("viewFreeBusy");
    }
    
    UserRight(String name) {
        super(name, RightType.preset);
    }
    
}
