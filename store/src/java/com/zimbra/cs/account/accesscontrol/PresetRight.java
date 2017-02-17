/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
    
    //
    // TODO: disguise group target as dl for R_checkRightGrp right
    //
    
    @Override
    Set<TargetType> getGrantableTargetTypes() {
        Set<TargetType> targetTypes = new HashSet<TargetType>();
        for (TargetType targetType : mTargetType.inheritFrom()) {
            targetTypes.add(targetType);
        }
        return targetTypes;
    }
    
    @Override
    boolean grantableOnTargetType(TargetType targetType) {
        return targetType.isInheritedBy(mTargetType);
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
