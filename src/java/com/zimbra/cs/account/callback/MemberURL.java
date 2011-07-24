package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class MemberURL extends AttributeCallback {

    @Override
    public void preModify(Map context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry, boolean isCreate)
            throws ServiceException {

        /*
         * verify memberURL cannot be modified if the group is a an ACL group
         */

        boolean isACLGroup = true;
        if (entry == null || isCreate) {
            SingleValueMod isACLgroup = singleValueMod(attrsToModify, Provisioning.A_zimbraIsACLGroup);
            if (isACLgroup.setting()) {
                isACLGroup = ProvisioningConstants.TRUE.equals(isACLgroup.value());
            }
            // else, unsetting, the default for zimbraIsACLGroup is TRUE in createDynamicGroup 
        } else {
            // modifying
            SingleValueMod isACLgroup = singleValueMod(attrsToModify, Provisioning.A_zimbraIsACLGroup);
            if (isACLgroup.setting()) {
                isACLGroup = ProvisioningConstants.TRUE.equals(isACLgroup.value());
            } else {
                // unsetting
                isACLGroup = false;
            }
        }
        
        if (isACLGroup) {
            throw ServiceException.INVALID_REQUEST("cannot modify " + Provisioning.A_memberURL +
                    "  when " + Provisioning.A_zimbraIsACLGroup + " is TRUE", null);
        }
        
    }


    @Override
    public void postModify(Map context, String attrName, Entry entry,
            boolean isCreate) {
        // TODO Auto-generated method stub
        
    }
}
