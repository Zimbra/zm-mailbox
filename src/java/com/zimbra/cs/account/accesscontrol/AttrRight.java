package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeInfo;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.accesscontrol.AdminRight.DefinedBy;
import com.zimbra.cs.account.accesscontrol.Right.RightType;

public class AttrRight extends AdminRight {

    static class Attr {
        private String mAttrName;
        private boolean mLimit;
        
        Attr(String attrName, boolean limit) {
            mAttrName = attrName;
            mLimit = limit;
        }
        
        static String attrNameFromLdapAttrValue(String attr) {
            String[] parts = attr.split(":");
            return parts[0];
        }
        
        static Attr fromLdapAttrValue(String attr) {
            String[] parts = attr.split(":");
            boolean limit = false;
            if (parts.length == 2 && parts[1].equals("l"))
                limit = true;
            return new Attr(parts[0], limit);
        }
        
        String toLdapStringValue() {
            if (mLimit)
                return mAttrName + ":l";
            else
                return mAttrName;
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
    
    private List<TargetType> mTargetTypes;
    private Set<Attr> mAttrs;
    
    
    AttrRight(String name, RightType rightType, DefinedBy definedBy) {
        super(name, rightType, definedBy);
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
    
    @Override
    boolean applicableOnTargetType(TargetType targetType) {
        return (mTargetTypes == null || mTargetTypes.contains(targetType));
    }

    @Override
    void setTargetType(TargetType targetType) throws ServiceException {
        if (mTargetTypes == null)
            mTargetTypes = new ArrayList<TargetType>();
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
        if (mTargetTypes != null) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (TargetType tt : mTargetTypes) {
                if (!first)
                    sb.append(",");
                sb.append(tt.getCode());
            }
            return sb.toString();
        }
        return null;
    }
    
    void addAttr(String attrName, boolean limit) {
        if (mAttrs == null)
            mAttrs = new HashSet<Attr>();
        mAttrs.add(new Attr(attrName, limit));
    }
    
    void addAttr(Attr attr) {
        if (mAttrs == null)
            mAttrs = new HashSet<Attr>();
        mAttrs.add(attr);
    }
    
    
    boolean allAttrs() {
        return (mAttrs == null);
    }

    Set<Attr> getAttrs() {
        return mAttrs;
    }
    
    Set<String> getAllAttrs() throws ServiceException {
        if (!allAttrs())
            throw ServiceException.FAILURE("internal error, can only be called if allAttrs is true", null);
        
        // this should not happen, since we've validated it in postParse, just sanity check,
        // remove when things are stable 
        if (mTargetTypes == null || mTargetTypes.size() != 1)
            throw ServiceException.FAILURE("internal error", null);
        
        // get the sole target type, 
        TargetType tt = mTargetTypes.get(0);
        return AttributeManager.getInstance().getAttrsInClass(tt.getAttributeClass());
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

        // verify that if allAttrs, then there can be exactly one target type.
        if (allAttrs() == true && (mTargetTypes == null || mTargetTypes.size() != 1))
            throw ServiceException.PARSE_ERROR("there must be exactly one target type for getAttrs/setAttrs right that cover all attributes", null);
    }
    
    /**
     * validate the attribute name is present on one of the specified target types.
     * if targetTypes is null, the attribute must be an attribute known to the system.
     * 
     * @param attrName
     * @param targetTypes
     */
    public static void validateAttr(String attrName, List<TargetType> targetTypes) throws ServiceException {
        // when called from CustomRight LDAP attribute callback, the attrName may contain a ":l" 
        // at the end, ignore that.
        attrName = Attr.attrNameFromLdapAttrValue(attrName);
        
        AttributeManager am = AttributeManager.getInstance();
        if (targetTypes == null) {
            if (am.getAttributeInfo(attrName) != null)
                return; // good
        } else {
            for (TargetType tt : targetTypes) {
                AttributeClass klass = tt.getAttributeClass();
                if (am.getAttrsInClass(klass).contains(attrName))
                    return; // good
            }
        }
        throw ServiceException.FAILURE("unknown attribute: " + attrName, null);
    }

}
