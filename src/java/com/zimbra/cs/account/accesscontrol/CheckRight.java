package com.zimbra.cs.account.accesscontrol;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

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
            
}
