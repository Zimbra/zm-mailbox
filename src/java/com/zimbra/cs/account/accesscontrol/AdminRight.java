/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.accesscontrol.generated.AdminRights;

public abstract class AdminRight extends Right {
    //
    // pseudo rights, should never actually be granted on any entry 
    //
    
    public static AttrRight PR_GET_ATTRS;
    public static AttrRight PR_SET_ATTRS;
    
    // pseudo rights to always allow/deny, used by admin soap handlers for 
    // API clean-ness when coding with legacy domain based AccessManager 
    // permission checking code.
    public static AdminRight PR_ALWAYS_ALLOW;
    public static AdminRight PR_SYSTEM_ADMIN_ONLY;
    
    // pseudo right for collecting effective admin preset right grants
    public static AdminRight PR_ADMIN_PRESET_RIGHT;
    
    static void init(RightManager rm) throws ServiceException {
        
        PR_GET_ATTRS = (AttrRight)newAdminSystemRight("PSEUDO_GET_ATTRS", RightType.getAttrs);
        PR_SET_ATTRS = (AttrRight)newAdminSystemRight("PSEUDO_SET_ATTRS", RightType.setAttrs);
        
        PR_ALWAYS_ALLOW = newAdminSystemRight("PSEUDO_ALWAYS_ALLOW", RightType.preset);
        PR_SYSTEM_ADMIN_ONLY = newAdminSystemRight("PSEUDO_SYSTEM_ADMIN_ONLY", RightType.preset);
        PR_ADMIN_PRESET_RIGHT = newAdminSystemRight("PSEUDO_ADMIN_PRESET_RIGHT", RightType.preset);
        
        if (LC.zimbra_rights_delegated_admin_supported.booleanValue())
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
