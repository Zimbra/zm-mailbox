package com.zimbra.cs.account.accesscontrol;

import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.service.ServiceException;

public class AttrRight extends AdminRight {

    class Attr {
        private String mAttrName;
        private boolean mLimit;
        
        Attr(String attrName, boolean limit) {
            mAttrName = attrName;
            mLimit = limit;
        }
        
        String getAttrName() {
            return mAttrName;
        }
        
        boolean getLimit() {
            return mLimit;
        }
        
        String dump(StringBuilder sb) {
            sb.append(mAttrName);
            if (mLimit)
                sb.append(" (limit)");
            return sb.toString();
        }
    }  
    
    private Set<TargetType> mTargetTypes;
    private Set<Attr> mAttrs;
    
    
    AttrRight(String name, RightType rightType) {
        super(name, rightType);
    }
    
    String dump(StringBuilder sb) {
        super.dump(sb);
        
        sb.append("===== attrs right properties: =====\n");
        if (mTargetTypes == null)
            sb.append("all target types\n");
        else {
            sb.append("target types: ");
            for (TargetType tt : mTargetTypes)
                sb.append(tt.name() + " ");
            sb.append("\n");
        }
        
        if (mAttrs == null)
            sb.append("all attrs\n");
        else {
            sb.append("attrs:\n");
            for (Attr a : mAttrs) {
                sb.append("    ");
                a.dump(sb);
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }

    
    void initAttrs() throws ServiceException {
        if (mAttrs != null)
            throw ServiceException.PARSE_ERROR("attrs already set", null);
        
        mAttrs = new HashSet<Attr>();
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
        mAttrs.add(new Attr(attrName, limit));
    }
    
    boolean allAttrs() {
        return (mAttrs == null);
    }

    Set<Attr> getAttrs() {
        return mAttrs;
    }

}
