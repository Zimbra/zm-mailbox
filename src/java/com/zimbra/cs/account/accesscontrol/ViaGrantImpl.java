package com.zimbra.cs.account.accesscontrol;

import com.zimbra.cs.account.Entry;
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
