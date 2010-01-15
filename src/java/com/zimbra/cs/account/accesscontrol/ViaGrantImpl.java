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

import com.zimbra.cs.account.AccessManager.ViaGrant;

public class ViaGrantImpl extends ViaGrant {
    String mTargetType;
    String mTargetName;
    String mGranteeType;
    String mGranteeName;
    String mRight;
    boolean mIsNegativeGrant;
    
    public ViaGrantImpl(TargetType targetType,
                        String targetName,
                        GranteeType granteeType,
                        String granteeName,
                        Right right,
                        boolean isNegativeGrant) {
        mTargetType = targetType.getCode();
        mTargetName = targetName;
        mGranteeType = granteeType.getCode();
        mGranteeName = granteeName;
        mRight = right.getName();
        mIsNegativeGrant = isNegativeGrant;
    }
    
    public ViaGrantImpl(String targetType,
                        String target,
                        String granteeType,
                        String granteeName,
                        String right,
                        boolean isNegativeGrant) {
        mTargetType = targetType;
        mTargetName = target;
        mGranteeType = granteeType;
        mGranteeName = granteeName;
        mRight = right;
        mIsNegativeGrant = isNegativeGrant;
    }
    
    public String getTargetType() { 
        return mTargetType;
    } 
    
    public String getTargetName() {
        return mTargetName;
    }
    
    public String getGranteeType() {
        return mGranteeType;
    }
    
    public String getGranteeName() {
        return mGranteeName;
    }
    
    public String getRight() {
        return mRight;
    }
    
    public boolean isNegativeGrant() {
        return mIsNegativeGrant;
    }
}
