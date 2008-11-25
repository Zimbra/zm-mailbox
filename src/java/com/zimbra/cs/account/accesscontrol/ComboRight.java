package com.zimbra.cs.account.accesscontrol;

import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.accesscontrol.AdminRight.DefinedBy;
import com.zimbra.cs.account.accesscontrol.AttrRight.Attr;

public class ComboRight extends AdminRight {
    // directly contained rights
    private Set<Right> mRights = new HashSet<Right>();
    
    // all preset rights contained in this combo right
    private Set<Right> mPresetRights = new HashSet<Right>();
    
    // all attr rights contained in this combo right
    private Set<AttrRight> mAttrRights = new HashSet<AttrRight>();
    
    ComboRight(String name, RightType rightType, DefinedBy definedBy) {
        super(name, rightType, definedBy);
    }
    
    String dump(StringBuilder sb) {
        super.dump(sb);
        
        sb.append("===== combo right properties: =====\n");
        
        sb.append("rights:\n");
        for (Right r : mRights) {
            sb.append("    ");
            sb.append(r.getName());
            sb.append("\n");
        }
        
        return sb.toString();
    }
    void addRight(Right right) {
        mRights.add(right);
    }
    
    @Override
    boolean applicableOnTargetType(TargetType targetType) {
        return true;
    }

    @Override
    void setTargetType(TargetType targetType) throws ServiceException {
        throw ServiceException.FAILURE("target type is now allowed for combo right", null);
    }
    
    @Override
    void verifyTargetType() throws ServiceException {
    }
    
    @Override
    TargetType getTargetType() throws ServiceException {
        throw ServiceException.FAILURE("internal error", null);
    }
    
    @Override
    String getTargetTypeStr() {
        return null;
    }
    
    @Override
    void completeRight() throws ServiceException {
        super.completeRight();
        
        expand(this, mPresetRights, mAttrRights);
    }
    
    private Set<Right> getDirectlyContainedRights() {
        return mRights;
    }
    
    private static void expand(ComboRight right, Set<Right> presetRights, Set<AttrRight> attrRights) throws ServiceException {
        for (Right r : right.getDirectlyContainedRights()) {
            if (r.isPresetRight())
                presetRights.add(r);
            else if (r.isAttrRight())
                attrRights.add((AttrRight)r);
            else if (r.isComboRight())
                expand((ComboRight)r, presetRights, attrRights);
            else
                throw ServiceException.FAILURE("internal error", null);
        }
    }
    
    boolean containsPresetRight(Right right) {
        return mPresetRights.contains(right);
    }
    
    // get all (direct or indirect) preset rights contained by this combo right 
    Set<Right> getPresetRights() {
        return mPresetRights;
    }
    
    // get all (direct or indirect) attr rights contained by this combo right 
    Set<AttrRight> getAttrRights() {
        return mAttrRights;
    }
    
    // get directed contained rights
    Set<Right> getRights() {
        return mRights;
    }

}
