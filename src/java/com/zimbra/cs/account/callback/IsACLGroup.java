package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class IsACLGroup extends AttributeCallback {

    @Override
    public void preModify(Map context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry, boolean isCreate)
            throws ServiceException {
        
        /*
         * Set memberURL to the ACL capable URL is setting zimbraIsACL Group to TRUE 
         */
        SingleValueMod isACLgroup = singleValueMod(attrsToModify, Provisioning.A_zimbraIsACLGroup);
        if (isACLgroup.setting()) {
            boolean isACLGroup = ProvisioningConstants.TRUE.equals(isACLgroup.value());
            
            if (isACLGroup) {
                if (attrsToModify.get(Provisioning.A_memberURL) != null) {
                    throw ServiceException.INVALID_REQUEST("cannot set " + Provisioning.A_memberURL + 
                            " while setting " +  Provisioning.A_zimbraIsACLGroup + " to TRUE", null);
                }
                
                if (!isCreate && entry instanceof DynamicGroup) {
                    attrsToModify.put(Provisioning.A_memberURL, ((DynamicGroup) entry).getDefaultMemberURL()); 
                }
            }
        }
        
    }

    @Override
    public void postModify(Map context, String attrName, Entry entry,
            boolean isCreate) {
        // TODO Auto-generated method stub
        
    }
}
