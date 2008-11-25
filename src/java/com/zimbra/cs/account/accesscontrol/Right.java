package com.zimbra.cs.account.accesscontrol;

import java.util.Set;
import com.zimbra.common.service.ServiceException;

public abstract class Right {
    
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
    private TargetType mTargetType;
    
    
    static void initKnownRights(RightManager rm) throws ServiceException {
        UserRight.initKnownUserRights(rm);
        AdminRight.initKnownAdminRights(rm);
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
    
    public boolean isUserRight() {
        return (this instanceof UserRight);
    }
    
    public boolean isPresetRight() {
        return mRightType==RightType.preset;
    }
    
    public boolean isAttrRight() {
        return mRightType==RightType.getAttrs || mRightType==RightType.setAttrs;
    }
    
    public boolean isComboRight() {
        return mRightType==RightType.combo;
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
    
    boolean applicableOnTargetType(TargetType targetType) {
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
    
    TargetType getTargetType() throws ServiceException {
        return mTargetType;
    }
    
    // for SOAP response only
    String getTargetTypeStr() {
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
}
