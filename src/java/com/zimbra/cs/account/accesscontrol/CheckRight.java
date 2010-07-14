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

import com.zimbra.cs.account.Account;
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
     
    
    // master control to enable/disable group targets
    // TODO: - check all callsites of Right.grantableOnTargetType and 
    //         RightChecker.rightApplicableOnTargetType
    //         see if they can be optimized
    //
    public static boolean allowGroupTarget(Right rightNeeded) {
        // group target is only supported for admin rights
        boolean allowed = !rightNeeded.isUserRight();
        
        // group targets can be turned off by a localconfig key
        if (DebugConfig.disableGroupTargetForAdminRight) {
            allowed = false;
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
