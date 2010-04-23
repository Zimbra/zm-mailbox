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

import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.service.ServiceException;

public class PresetRight extends AdminRight {

    PresetRight(String name) {
        super(name, RightType.preset);
    }
    
    @Override
    public boolean isPresetRight() {
        return true;
    }
    
    
    // don't allow an account right to be granted on calendar resource and vice versa
    // it is only allowed for user rights
    // TODO: revisit, may be able to do with just removing account from setInheritedByTargetTypes for calresource
    //       and remove this very ugly check.   
    //       warning, need to examine all call sites of inherited by/from
    private boolean mutualExcludeAccountAndCalResource(TargetType targetType) {
        return ((mTargetType == TargetType.account && targetType == TargetType.calresource) ||
                (mTargetType == TargetType.calresource && targetType == TargetType.account));
    }
    
    @Override
    boolean grantableOnTargetType(TargetType targetType) {
        
        if (mutualExcludeAccountAndCalResource(targetType))
            return false;
        
        return targetType.isInheritedBy(mTargetType);
    }
    
    @Override
    protected Set<TargetType> getGrantableTargetTypes() {
        Set<TargetType> targetTypes = new HashSet<TargetType>();
        for (TargetType targetType : mTargetType.inheritFrom()) {
            if (!mutualExcludeAccountAndCalResource(targetType))
                targetTypes.add(targetType);
        }
        return targetTypes;
    }
    
    @Override
    boolean overlaps(Right other) throws ServiceException {
        if (other.isPresetRight())
            return this==other;
        else if (other.isAttrRight())
            return false;
        else if (other.isComboRight())
            return ((ComboRight)other).containsPresetRight(this);
        else
            throw ServiceException.FAILURE("internal error", null);
    }
}
