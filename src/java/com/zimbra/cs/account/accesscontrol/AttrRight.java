package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;

public class AttrRight extends AdminRight {

    private List<TargetType> mTargetTypes = new ArrayList<TargetType>();
    private Set<String> mAttrs;
    
    
    AttrRight(String name, RightType rightType) {
        super(name, rightType);
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
    boolean applicableOnTargetType(TargetType targetType) {
        return mTargetTypes.contains(targetType);
    }

    @Override
    void setTargetType(TargetType targetType) throws ServiceException {
        mTargetTypes.add(targetType);
    }
    
    @Override
    void verifyTargetType() throws ServiceException {
    }
    
    @Override
    TargetType getTargetType() throws ServiceException {
        throw ServiceException.FAILURE("internal error", null);
    }
    
    public List<TargetType> getTargetTypes() {
        return mTargetTypes;
    }
    
    // for SOAP response only
    @Override
    String getTargetTypeStr() {
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
    
    void addAttr(String attrName) {
        if (mAttrs == null)
            mAttrs = new HashSet<String>();
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
        TargetType tt = mTargetTypes.get(0);
        return AttributeManager.getInstance().getAllAttrsInClass(tt.getAttributeClass());
    }
    
    // setAttrs imply getAttrs on the same set of attrs
    boolean applicableToRightType(RightType needed) {
        if (needed == mRightType ||
            needed == RightType.getAttrs && mRightType == RightType.setAttrs)
            return true;
        else
            return false;
    }

    @Override
    void completeRight() throws ServiceException {
        super.completeRight();
        
        if (mTargetTypes.size() == 0)
            throw ServiceException.PARSE_ERROR("missing target type", null);

        // verify that if allAttrs, then there can be exactly one target type.
        if (allAttrs() == true && mTargetTypes.size() != 1)
            throw ServiceException.PARSE_ERROR("there must be exactly one target type for getAttrs/setAttrs right that cover all attributes", null);
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
