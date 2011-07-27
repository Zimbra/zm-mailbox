/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Entry;

/**
 * Util class to bridge DistributionList and DynamicGroup
 * 
 * On SOAP and zmprov, "dl" is the target type to be specified/returned 
 * everywhere a TargetType is expected.
 * 
 * Internally, the a target is identified as a DynamicGroup, the TargetTtype 
 * in changed to "group" internally.  All dl rights are applicable on dynamic groups. 
 *
 * A dump ground for all special tweaks for this design.
 */
public class GroupUtil {

    public static boolean isGroup(TargetType targetType) {
        return (targetType == TargetType.dl || targetType == TargetType.group);
    }
    
    static void checkSpecifiedTargetType(TargetType targetType) throws ServiceException{
        if (targetType == TargetType.group) {
            throw ServiceException.INVALID_REQUEST("invalid target type: " + targetType.getCode(), null);
        }
    }
    /**
     * For all ACL commands, every where dl is specified as a target type, we recognize  
     * it as valid for both DistributionList and DynamicGroup.
     * 
     * The target type "group" cannot be specified.
     * 
     * @param entry
     * @param targetTypeSpecified target type specified by user
     * @return
     */
    static TargetType fixupTargetType(Entry entry, TargetType targetTypeSpecified) 
    throws ServiceException {
        checkSpecifiedTargetType(targetTypeSpecified);
        
        if (entry instanceof DynamicGroup) {
            return TargetType.group;
        } else {
            return targetTypeSpecified;
        }
    }
    
    
    /**
     * 
     * @param targetType
     * @return
     */
    public static TargetType disguiseDynamicGroupAsDL(TargetType targetType) {
        if (targetType == TargetType.group) {
            return TargetType.dl;
        } else {
            return targetType;
        }
    }
}
