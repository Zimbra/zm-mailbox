package com.zimbra.cs.account.accesscontrol;

import java.util.Set;
import com.zimbra.common.service.ServiceException;

public abstract class Right {
    
    enum RightType {
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
    }
    
    protected RightType mRightType;
    private final String mName;
    private String mDesc;  // a brief description
    private String mDoc;   // a more detailed description, use cases, examples
    private Boolean mDefault;
    private TargetType mTargetType;
    
    
    static void initKnownRights(RightManager rm) throws ServiceException {
        UserRight.initKnownUserRights(rm);
        AdminRight.initKnownAdminRights(rm);
    }
    
    
    Right(String name, RightType rightType) {
        mRightType = rightType;
        mName = name;
    }
    
    public boolean isUserRight() {
        return (this instanceof UserRight);
    }
    
    RightType getRightType() {
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
    
    public boolean applicableOnTargetType(TargetType targetType) {
        return (mTargetType == targetType);
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
    
    void verify() throws ServiceException {
        if (getDesc() == null)
            throw ServiceException.PARSE_ERROR("missing description", null);
        verifyTargetType();
    }
}
