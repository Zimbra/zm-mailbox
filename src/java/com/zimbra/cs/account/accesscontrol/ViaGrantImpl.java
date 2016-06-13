/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
