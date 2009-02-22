/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.accesscontrol.generated.AdminRights;

public abstract class AdminRight extends Right {
    //
    // pseudo rights, should never actually be granted on any entry 
    //
    
    public static AdminRight R_PSEUDO_GET_ATTRS;
    public static AdminRight R_PSEUDO_SET_ATTRS;
    
    // pseudo to always allow/deny, used by admin soap handlers for 
    // API clean-ness when coding with legacy domain based AccessManager 
    // permission checking code.
    public static AdminRight R_PSEUDO_ALWAYS_ALLOW;
    public static AdminRight R_PSEUDO_ALWAYS_DENY;
    
    static void init(RightManager rm) throws ServiceException {
        
        R_PSEUDO_GET_ATTRS = newAdminSystemRight("PSEUDO_GET_ATTRS", RightType.getAttrs);
        R_PSEUDO_SET_ATTRS = newAdminSystemRight("PSEUDO_SET_ATTRS", RightType.setAttrs);
        
        R_PSEUDO_ALWAYS_ALLOW = newAdminSystemRight("PSEUDO_ALWAYS_ALLOW", RightType.preset);
        R_PSEUDO_ALWAYS_DENY = newAdminSystemRight("PSEUDO_ALWAYS_DENY", RightType.preset);
        
        AdminRights.init(rm);
    }
    
    protected AdminRight(String name, RightType rightType) {
        super(name, rightType);
    }
    
    static AdminRight newAdminSystemRight(String name, RightType rightType) {
        return newAdminRight(name, rightType);
    }
    
    private static AdminRight newAdminRight(String name, RightType rightType) {
        if (rightType == RightType.getAttrs || rightType == RightType.setAttrs)
            return new AttrRight(name, rightType);
        else if (rightType == RightType.combo)
            return new ComboRight(name);
        else
            return new PresetRight(name);
    }
    
    /*
    String dump(StringBuilder sb) {
        // nothing in user right to dump
        return super.dump(sb);
    }
    */
}
