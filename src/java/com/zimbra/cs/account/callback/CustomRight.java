package com.zimbra.cs.account.callback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.AttrRight;
import com.zimbra.cs.account.accesscontrol.ComboRight;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.TargetType;

public class CustomRight  extends AttributeCallback {

    private static final String KEY = CustomRight.class.getName();
    public void preModify(Map context, String attrName, Object value,
            Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {
        
        /*
         * This callback is for all custom right attrs, and it handles all in one shot.  
         * If we've been called just return.
         */ 
        Object done = context.get(KEY);
        if (done == null)
            context.put(KEY, KEY);
        else
            return;
        
        String rightType = null;
        String desc = null;
            
        if (entry == null || isCreate) {
            rightType = (String)attrsToModify.get(Provisioning.A_zimbraRightType);
            desc = (String)attrsToModify.get(Provisioning.A_description);
        } else {
            rightType = entry.getAttr(Provisioning.A_zimbraRightType);
            desc = entry.getAttr(Provisioning.A_description);
        }
        
        if (StringUtil.isNullOrEmpty(rightType))
            throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraRightType + " is required", null);
        
        if (StringUtil.isNullOrEmpty(desc))
            throw ServiceException.INVALID_REQUEST(Provisioning.A_description + " is required", null);
        
        Right.RightType rt = Right.RightType.fromString(rightType);
        if (!rt.isUserDefinable())
            throw ServiceException.INVALID_REQUEST("invalid right type for user defined right", null);
        
        MultiValueMod targetTypesMod = multiValueMod(attrsToModify, Provisioning.A_zimbraRightTargetType);
        MultiValueMod rightAttrsMod = multiValueMod(attrsToModify, Provisioning.A_zimbraRightAttrs);
        MultiValueMod rightRightsMod = multiValueMod(attrsToModify, Provisioning.A_zimbraRightRights);
        
        Set<String> targetTypes = newValuesToBe(rightAttrsMod, entry, Provisioning.A_zimbraRightTargetType);
        Set<String> rightAttrs = newValuesToBe(rightAttrsMod, entry, Provisioning.A_zimbraRightAttrs);
        Set<String> rightRights = newValuesToBe(rightRightsMod, entry, Provisioning.A_zimbraRightRights);
        
        if (rt == Right.RightType.combo) {
            if (targetTypes != null && !targetTypes.isEmpty())
                throw ServiceException.INVALID_REQUEST("target type is not allowed for combo right", null);
            
            // must not contain any attrs
            if (rightAttrs != null && !rightAttrs.isEmpty())
                throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraRightAttrs + " is not allowed for combo right", null);
            
            // must contian some rights
            if (rightRights == null || rightRights.isEmpty())
                throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraRightRights + " cannot be empty for combo right", null);
            
            // validate all rights exist
            RightManager rm = RightManager.getInstance();
            for (String r : rightRights) {
                if (!rm.rightExists(r))
                    throw ServiceException.INVALID_REQUEST("unknow right " + r, null);
            }
                
        } else if (rt == Right.RightType.getAttrs || rt == Right.RightType.setAttrs) {
            // must contain some attrs.  
            // NOTE: custom right is not allowed for "all attrs".  There are system rights that allow all attrs on each target type.
            if (rightAttrs == null || rightAttrs.isEmpty())
                throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraRightAttrs + " cannot be empty for attr right", null);
            
            // must not contain any rights
            if (rightRights != null && !rightRights.isEmpty())
                throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraRightRights + " is not allowed for attr right", null);
            
            // must contain only valid attrs
            List<TargetType> targetTypesList = null;
            if (targetTypes != null) {
                targetTypesList = new ArrayList<TargetType>();
                for (String tt : targetTypes)
                    targetTypesList.add(TargetType.fromString(tt));
            }
                
            // validate all attributes exist on one of the target types
            for (String a : rightAttrs)
                AttrRight.validateAttr(a, targetTypesList);
        }
        
    }
    
    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
    }
}
