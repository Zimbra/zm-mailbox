package com.zimbra.cs.account.accesscontrol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;

public class AttrRight extends AdminRight {

    class Attr {
        private String mAttrName;
        private boolean mLimit;
        
        Attr(String name, boolean limit) {
            mAttrName = name;
        }
    }  
    
    private Set<TargetType> mTargetTypes;
    private Map<String, Attr> mAttrs;
    
    
    AttrRight(String name, RightType rightType) {
        super(name, rightType);
    }
    
    void initAttrs() throws ServiceException {
        if (mAttrs != null)
            throw ServiceException.PARSE_ERROR("attrs already set", null);
        
        mAttrs = new HashMap<String, Attr>();
    }
    
    @Override
    public boolean applicableOnTargetType(TargetType targetType) {
        return (mTargetTypes == null || mTargetTypes.contains(targetType));
    }

    @Override
    void setTargetType(TargetType targetType) throws ServiceException {
        if (mTargetTypes == null)
            mTargetTypes = new HashSet<TargetType>();
        mTargetTypes.add(targetType);
    }
    
    @Override
    void verifyTargetType() throws ServiceException {
    }
    
    void addAttr(String attrName, boolean limit) {
        mAttrs.put(attrName, new Attr(attrName, limit));
    }
    
    boolean allAttrs() {
        return (mAttrs == null);
    }

    Set<String> getAttrs() {
        return mAttrs.keySet();
    }
    
    Boolean getAttrLimit(String attrName) {
        Attr attr = mAttrs.get(attrName);
        if (attr == null)
            return null;
        else
            return attr.mLimit;
    }
}
