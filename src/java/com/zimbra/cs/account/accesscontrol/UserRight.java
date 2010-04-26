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

    @Override
    boolean executableOnTargetType(TargetType targetType) {
        // special treatment for user right:
        // if a right is executable on account, it is executable on calendar resource
        if (mTargetType == TargetType.account)
            return (targetType == TargetType.account || targetType == TargetType.calresource);
        else
            return super.executableOnTargetType(targetType);    
    }
    
    
    /*
    String dump(StringBuilder sb) {
        // nothing in user right to dump
        return super.dump(sb);
    }
    */
    
    @Override
    boolean overlaps(Right other) throws ServiceException {
        return this==other; 
        // no need to check is other is a combo right, because
        // combo right can only contain admin rights.
    }
}
