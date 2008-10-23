package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;

public abstract class Right {
    
    private final String mName;
    private String mDesc;  // a brief description
    private String mDoc;   // a more detailed description, use cases, examples
    private Boolean mDefault;
    
    static void initKnownRights(RightManager rm) throws ServiceException {
        UserRight.initKnownUserRights(rm);
        AdminRight.initKnownAdminRights(rm);
    }

    Right(String name) {
        mName = name;
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
    
    public String getDoc() {
        return mDoc;
    }
    
    public Boolean getDefault() {
        return mDefault;
    }
    
    void setDesc(String desc) {
        mDesc = desc;
    }
    
    void setDoc(String doc) {
        mDoc = doc;
    }

    void setDefault(Boolean defaultValue) {
        mDefault = defaultValue;
    }

}
