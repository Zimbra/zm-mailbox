package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.accesscontrol.generated.UserRights;

public class UserRight extends Right {
    
    static void init(RightManager rm) throws ServiceException {
        UserRights.init(rm);
    }
    
    UserRight(String name) {
        super(name, RightType.preset);
    }
    
    @Override
    public boolean isUserRight() {
        return true;
    }
    
    @Override
    public boolean isPresetRight() {
        return true;
    }
    
    /*
    String dump(StringBuilder sb) {
        // nothing in user right to dump
        return super.dump(sb);
    }
    */
}
