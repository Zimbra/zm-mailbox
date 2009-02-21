package com.zimbra.cs.account.accesscontrol;

import com.zimbra.cs.account.accesscontrol.generated.AdminRights;
import com.zimbra.cs.account.accesscontrol.generated.UserRights;

/**
 * 
 * bridging class so we don't have to include the "generated" classes 
 * at callsites, because that's kind of ugly.
 *
 */
public abstract class Rights {

    public static class Admin extends AdminRights {
    }
    
    public static class User extends UserRights {
    }
}
