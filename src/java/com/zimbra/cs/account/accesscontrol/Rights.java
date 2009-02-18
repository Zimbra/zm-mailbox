package com.zimbra.cs.account.accesscontrol;

import com.zimbra.cs.account.accesscontrol.generated.ZAdminRight;
import com.zimbra.cs.account.accesscontrol.generated.ZUserRight;

/**
 * 
 * bridging class so we don't have to include the "generated" classes 
 * at callsites, because that's kind of ugly.
 *
 */
public abstract class Rights {

    public static class Admin extends ZAdminRight {
    }
    
    public static class User extends ZUserRight {
    }
}
