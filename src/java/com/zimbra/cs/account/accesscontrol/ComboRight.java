package com.zimbra.cs.account.accesscontrol;

import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.service.ServiceException;

public class ComboRight extends AdminRight {
    private Set<Right> mRights = new HashSet<Right>();
    
    ComboRight(String name, RightType rightType) {
        super(name, rightType);
    }
    
    void addRight(Right right) {
        mRights.add(right);
    }
    
    @Override
    public boolean applicableOnTargetType(TargetType targetType) {
        return true;
    }

    @Override
    void setTargetType(TargetType targetType) throws ServiceException {
        throw ServiceException.FAILURE("target type is now allowed for combo right", null);
    }
    
    @Override
    void verifyTargetType() throws ServiceException {
    }
}
