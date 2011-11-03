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

import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.SetUtil;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;

public class AttrRight extends AdminRight {

    private Set<TargetType> mTargetTypes = new HashSet<TargetType>();
    private Set<String> mAttrs;
    
    
    AttrRight(String name, RightType rightType) throws ServiceException {
        super(name, rightType);
        
        if (rightType != RightType.getAttrs && rightType != RightType.setAttrs)
            throw ServiceException.FAILURE("internal error", null);
    }
    
    @Override
    public boolean isAttrRight() {
        return true;
    }
    
    public String dump(StringBuilder sb) {
        super.dump(sb);
        
        sb.append("===== attrs right properties: =====\n");
        sb.append("target types: ");
        for (TargetType tt : mTargetTypes)
            sb.append(tt.name() + " ");
        sb.append("\n");
        
        if (mAttrs == null)
            sb.append("all attrs\n");
        else {
            sb.append("attrs:\n");
            for (String a : mAttrs) {
                sb.append("    " + a + "\n");
            }
        }
        
        return sb.toString();
    }
    
    @Override
    boolean overlaps(Right other) throws ServiceException {
        if (other.isPresetRight()) {
            return false;
        } else if (other.isAttrRight()) {
            return overlapAttrRight((AttrRight)other);
        } else if (other.isComboRight()) {
            ComboRight cr = (ComboRight)other;
            Set<AttrRight> otherAttrRights = cr.getAttrRights();
            for (AttrRight ar : otherAttrRights) {
                if (overlapAttrRight(ar)) {
                    return true;
                }
            }
            return false;
        } else
            throw ServiceException.FAILURE("internal error", null);
    }
        
    private boolean overlapAttrRight(AttrRight otherAttrRight) {
        if (this == otherAttrRight) {
            return true; 
        }
        if (SetUtil.intersect(getTargetTypes(), otherAttrRight.getTargetTypes()).isEmpty()) {
            return false;
        }
        if (allAttrs() || otherAttrRight.allAttrs()) {
            return true;
        }
        // neither is allAttrs
        return !SetUtil.intersect(getAttrs(), otherAttrRight.getAttrs()).isEmpty();
    }
    
    @Override
    boolean executableOnTargetType(TargetType targetType) {
        return mTargetTypes.contains(targetType);
    }
    
    @Override
    boolean grantableOnTargetType(TargetType targetType) {
        
        // return true if *any* of the applicable target types for the right 
        // can inherit from targetType
        for (TargetType tt : getTargetTypes()) {
            if (targetType.isInheritedBy(tt)) {
                return true;
            }
        }

        return false;
    }
    
    @Override
    Set<TargetType> getGrantableTargetTypes() {
        // return *union* of target types from which *any* of the target types
        // for the right can inherit from
        Set<TargetType> targetTypes = new HashSet<TargetType>();
        for (TargetType tt : getTargetTypes()) {
            SetUtil.union(targetTypes, tt.inheritFrom());
        }
        
        return targetTypes;
    }

    @Override
    void setTargetType(TargetType targetType) throws ServiceException {
        mTargetTypes.add(targetType);
    }
    
    @Override
    void verifyTargetType() throws ServiceException {
    }
    
    @Override
    public TargetType getTargetType() throws ServiceException {
        throw ServiceException.FAILURE("internal error", null);
    }
    
    public Set<TargetType> getTargetTypes() {
        return mTargetTypes;
    }
    
    // for SOAP response only
    @Override
    public String getTargetTypeStr() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (TargetType tt : mTargetTypes) {
            if (first)
                first = false;
            else    
                sb.append(",");
            
            sb.append(tt.getCode());
        }
        return sb.toString();
    }
    
    void addAttr(String attrName) throws ServiceException {
        if (getRightType() == RightType.setAttrs) {
            HardRules.checkForbiddenAttr(attrName);
        }
        
        if (mAttrs == null) {
            mAttrs = new HashSet<String>();
        }
        
        mAttrs.add(attrName);
    }

    public boolean allAttrs() {
        return (mAttrs == null);
    }

    /*
     * should be called after a call to allAttrs returns false
     */
    public Set<String> getAttrs() {
        return mAttrs;
    }
    
    Set<String> getAllAttrs() throws ServiceException {
        if (!allAttrs())
            throw ServiceException.FAILURE("internal error, can only be called if allAttrs is true", null);
        
        // this should not happen, since we've validated it in completeRight, just sanity check,
        // remove when things are stable 
        if (mTargetTypes.size() != 1)
            throw ServiceException.FAILURE("internal error", null);
        
        // get the sole target type, 
        TargetType tt = mTargetTypes.iterator().next();
        return AttributeManager.getInstance().getAllAttrsInClass(tt.getAttributeClass());
    }
    
    
    /**
     * returns if this right is suitable or the needed right
     * 
     * it is suitable if:
     *     - this right and the needed right type is the same get/set
     *     or
     *     - this right is set and the needed right is get
     * 
     * @param needed
     * @return
     */
    boolean suitableFor(RightType needed) {
        if (needed == mRightType ||
            needed == RightType.getAttrs && mRightType == RightType.setAttrs) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    void completeRight() throws ServiceException {
        super.completeRight();
        
        if (mTargetTypes.size() == 0) {
            throw ServiceException.PARSE_ERROR("missing target type", null);
        }
        
        // verify that if allAttrs, then there can be exactly one target type.
        if (allAttrs() == true && mTargetTypes.size() != 1) {
            throw ServiceException.PARSE_ERROR(
                    "there must be exactly one target type for getAttrs/setAttrs right " + 
                    "that cover all attributes", null);
        }
    }
    
    /**
     * validate the attribute name is present on all of the specified target types.
     * 
     * @param attrName
     * @param targetTypes
     */
    void validateAttr(String attrName) throws ServiceException {
        AttributeManager am = AttributeManager.getInstance();
        for (TargetType tt : mTargetTypes) {
            AttributeClass klass = tt.getAttributeClass();
            if (!am.getAllAttrsInClass(klass).contains(attrName))
                throw ServiceException.FAILURE("attribute " + attrName + " is not on " + tt.getCode(), null);
        }
    }

}
