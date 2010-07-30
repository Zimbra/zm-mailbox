/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.localconfig.DebugConfig;

public abstract class CheckRight {
    // input to the class
    protected Entry mTarget;
    protected Right mRightNeeded;
    protected boolean mCanDelegateNeeded;
    
    protected Provisioning mProv; 
    protected TargetType mTargetType;
    
    
    protected CheckRight(Entry target, Right rightNeeded, boolean canDelegateNeeded) {
        
        mProv = Provisioning.getInstance();

        mTarget = target;
        mRightNeeded = rightNeeded;
        mCanDelegateNeeded = canDelegateNeeded;
    }
     
    // bug 46840
    // master control to enable/disable group targets
    // TODO: - check all callsites of Right.grantableOnTargetType and 
    //         RightChecker.rightApplicableOnTargetType
    //         see if they can be optimized
    //
    public static boolean allowGroupTarget(Right rightNeeded) throws ServiceException {
        // group target is only supported for admin rights
        boolean allowed = !rightNeeded.isUserRight();
        
        if (rightNeeded.isUserRight()) {
            // for perf reason, for user rights, groups target is supported 
            // only if target type of the right is not account.
            // i.e. account right cannot be granted on groups
            
            if (rightNeeded.getTargetType() == TargetType.account)
                allowed = false;
            else
                allowed = true;
            
        } else {
            // group targets can be turned off for admin rights by a localconfig key
            allowed = !DebugConfig.disableGroupTargetForAdminRight;
        }
        return allowed;
    }

    
    /*
     * check if rightNeeded is applicable on target type 
     */
    static boolean rightApplicableOnTargetType(TargetType targetType, 
            Right rightNeeded, boolean canDelegateNeeded) {
        if (canDelegateNeeded) {
            // check if the right is grantable on the target
            if (!rightNeeded.grantableOnTargetType(targetType))
                return false;
        } else {
            // check if the right is executable on the target
            if (!rightNeeded.executableOnTargetType(targetType))
                return false;
        }
        return true;
    }
}
