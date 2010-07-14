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

import java.util.Set;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.accesscontrol.generated.RightConsts;

public abstract class Right extends RightConsts {
    
    public enum RightType {
        preset,
        getAttrs,
        setAttrs,
        combo;
        
        public static RightType fromString(String s) throws ServiceException {
            try {
                return RightType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR("unknown right type: " + s, e);
            }
        }
        
        public boolean isUserDefinable() {
            return this != preset;
        }
    }
    
    private final String mName;
    protected RightType mRightType;
    private String mDesc;  // a brief description
    private String mDoc;   // a more detailed description, use cases, examples
    private Boolean mDefault;
    protected TargetType mTargetType;
    private CheckRightFallback mFallback;
    
    
    static void init(RightManager rm) throws ServiceException {
        UserRight.init(rm);
        AdminRight.init(rm);
    }
    
    Right(String name, RightType rightType) {
        mRightType = rightType;
        mName = name;
    }
    
    String dump(StringBuilder sb) {
        if (sb == null)
            sb = new StringBuilder();
        
        sb.append("name         = " + mName + "\n");
        sb.append("type         = " + mRightType.name() + "\n");
        sb.append("desc         = " + mDesc + "\n");
        sb.append("doc          = " + mDoc + "\n");
        sb.append("default      = " + mDefault + "\n");
        sb.append("target Type  = " + mTargetType + "\n");

        return sb.toString();
    }
    
    /**
     * returns if this right overlaps the other right
     * @param other
     * @return
     */
    abstract boolean overlaps(Right other) throws ServiceException;
    
    public boolean isUserRight() {
        return false;
    }
    
    public boolean isPresetRight() {
        return false;
    }
    
    public boolean isAttrRight() {
        return false;
    }
    
    public boolean isComboRight() {
        return false;
    }
    
    public RightType getRightType() {
        return mRightType;
    }
    
    
    /**
     * - right name stored in zimbraACE.
     * - right name appear in XML
     * - right name displayed by CLI
     * 
     * @return 
     */
    public String getName() {
        return mName;
    }
    
    public String getDesc() {
        return mDesc;
    }
        
    void setDesc(String desc) {
        mDesc = desc;
    }
    
    public String getDoc() {
        return mDoc;
    }
        
    void setDoc(String doc) {
        mDoc = doc;
    }
    
    public Boolean getDefault() {
        return mDefault;
    }
    
    void setDefault(Boolean defaultValue) {
        mDefault = defaultValue;
    }
    
    public CheckRightFallback getFallback() {
        return mFallback;
    }
    
    void setFallback(CheckRightFallback fallback) {
        mFallback = fallback;
    }
    
    boolean executableOnTargetType(TargetType targetType) {
        return (mTargetType == targetType);
    }
    
    boolean grantableOnTargetType(TargetType targetType) {
        return targetType.isInheritedBy(mTargetType);
    }
    
    /**
     * returns if the subDomain modifier can be specified for the right
     * @return
     */
    boolean allowSubDomainModifier() {
        return executableOnTargetType(TargetType.domain);
    }
    
    /*
     * overriden only in InlineAttrRight
     */
    boolean isTheSameRight(Right other) {
        return this == other;
    }
    
    /*
     * for reporting granting error
     */
    final String reportGrantableTargetTypes() {
        Set<TargetType> targetTypes = getGrantableTargetTypes();
        StringBuilder sb = new StringBuilder();
        for (TargetType tt : targetTypes)
            sb.append(tt.getCode() + " ");
        return sb.toString();
    }
    
    protected Set<TargetType> getGrantableTargetTypes() {
        return mTargetType.inheritFrom();
    }

    void setTargetType(TargetType targetType) throws ServiceException {
        if (mTargetType != null)
            throw ServiceException.PARSE_ERROR("target type already set", null);
        
        mTargetType = targetType;
    }
    
    void verifyTargetType() throws ServiceException {
        if (mTargetType == null)
            throw ServiceException.PARSE_ERROR("missing target type", null);
    }
    
    public TargetType getTargetType() throws ServiceException {
        return mTargetType;
    }
    
    // for SOAP response only
    public String getTargetTypeStr() {
        return mTargetType.getCode();
    }
    
    /*
     * - verify that all things are well with this object, catch loose ends
     *   that were not catched during paring
     * 
     * - populate internal aux data structures  
     * 
     * - after this method is called for an right object, no change should be done 
     *   to the object.
     */
    void completeRight() throws ServiceException {
        if (getDesc() == null)
            throw ServiceException.PARSE_ERROR("missing description", null);
        verifyTargetType();
    }

    public static void main(String[] args) throws ServiceException {
        // init rights
        RightManager.getInstance();
        
        Right r1 = Rights.Admin.R_domainAdminAccountRights;
        Right r2 = Rights.Admin.R_domainAdminRights;
        boolean overlaps = r1.overlaps(r2);
        System.out.println(r1.getName() +  " " + r2.getName() + " => " + overlaps);
        
        r1 = Rights.Admin.R_modifyCos;
        overlaps = r1.overlaps(r2);
        System.out.println(r1.getName() +  " " + r2.getName() + " => " + overlaps);
    }
}
