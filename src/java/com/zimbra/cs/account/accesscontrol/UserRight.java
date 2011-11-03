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

import java.util.EnumSet;
import java.util.Set;

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
    
    // special treatment for user right:
    // - disguise calendar resource as account
    // - disguise dynamic group as distribution list
    //
    // For users rights, accounts and calresoruces are treated equally; and 
    // distribution list and dynamic group are treated equally.
    //
    // e.g.
    // if a right is executable on account, it is executable on calendar resource.
    // if a right is grantable on a target from which account rights can be inherited,
    // the right is grantable on the target from which calendar resource rights can be 
    // inherited
    private TargetType disguiseTargetType(TargetType targetType) {
        if (targetType == TargetType.calresource) {
            return TargetType.account;
        } else if (targetType == TargetType.group) {
            return TargetType.dl;
        } else {
            return targetType;
        }
    }
    

    @Override
    boolean executableOnTargetType(TargetType targetType) {
        targetType = disguiseTargetType(targetType);
        return super.executableOnTargetType(targetType); 
    }
    
    @Override
    boolean grantableOnTargetType(TargetType targetType) {
        targetType = disguiseTargetType(targetType);
        
        /*
         * see if there is restriction on grant target type for the right
         */
        if (mGrantTargetType != null) {
            return mGrantTargetType == targetType;
        }
        return targetType.isInheritedBy(mTargetType);
    }
    

    @Override
    Set<TargetType> getGrantableTargetTypes() {
        if (mGrantTargetType != null) {
            return EnumSet.of(mGrantTargetType);
        }
        return mTargetType.inheritFrom();
    }
    
    @Override
    boolean allowSubDomainModifier() {
        return false;
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
