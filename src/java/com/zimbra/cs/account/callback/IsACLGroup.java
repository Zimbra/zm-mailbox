/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class IsACLGroup extends AttributeCallback {

    private static final String KEY = "IsACLGroupCallback";
    @Override
    public void preModify(Map context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry, boolean isCreate)
    throws ServiceException {
        
        if (context.get(KEY) != null) {
            return;
        }
        
        context.put(KEY, KEY);
        
        Boolean isACLGroup = null;
        String memberURL = null;
        
        SingleValueMod isACLGroupMod = singleValueMod(attrsToModify, Provisioning.A_zimbraIsACLGroup);
        SingleValueMod memberURLMod = singleValueMod(attrsToModify, Provisioning.A_memberURL); 
        
        if (isACLGroupMod.setting()) {
            isACLGroup = Boolean.valueOf(isACLGroupMod.value());
        }
        
        if (memberURLMod.setting()) {
            memberURL = memberURLMod.value();
        }
        
        // cannot unset either attr, this is enforced in the schema
        
        // get current value of isACLGroup, if it not being modified
        if (entry != null) {
            if (isACLGroup == null) {
                isACLGroup = entry.getBooleanAttr(Provisioning.A_zimbraIsACLGroup, true);
            }
        }
        
        // if we still don't have a value by now, we are creating the entry,
        // and zimbraIsACLGroupis not provided in the request.
        // Set it to true so our validation later will work consistenly.
        if (isACLGroup == null) {
            isACLGroup = Boolean.TRUE;
        }
        
        if (isACLGroup) {
            // setting memberURL is not allowed
            if (memberURL != null) {
                throw ServiceException.INVALID_REQUEST("cannot set " + Provisioning.A_memberURL + 
                        " when " +  Provisioning.A_zimbraIsACLGroup + " is TRUE", null);
            } else {
                /*
                 * memberURL is not being modified.
                 * 
                 * if we are not creating, set memberURL to the default URL.
                 * otherwise (we are creating), don't need to do anything since default memberURL
                 * will be set in LdapProvisioning.createDynamicGroup 
                 */
                if (entry != null) {
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
