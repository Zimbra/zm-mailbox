package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;

public class UserRight extends Right {
    
    // known rights
    public static UserRight R_invite;
    public static UserRight R_loginAs;
    public static UserRight R_sendAs;
    public static UserRight R_viewFreeBusy;
    
    static void initKnownUserRights(RightManager rm) throws ServiceException {
        R_invite = rm.getUserRight("invite");
        R_loginAs = rm.getUserRight("loginAs");
        R_sendAs = rm.getUserRight("sendAs");
        R_viewFreeBusy = rm.getUserRight("viewFreeBusy");
    }
    
    UserRight(String name) {
        super(name, RightType.preset);
    }
    
    /*
    String dump(StringBuilder sb) {
        // nothing in user right to dump
        return super.dump(sb);
    }
    */
}
