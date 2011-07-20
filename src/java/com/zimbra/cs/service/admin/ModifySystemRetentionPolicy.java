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

package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.mailbox.RetentionPolicyManager;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ModifySystemRetentionPolicyRequest;
import com.zimbra.soap.admin.message.ModifySystemRetentionPolicyResponse;
import com.zimbra.soap.mail.type.Policy;

public class ModifySystemRetentionPolicy extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        // check right
        CreateSystemRetentionPolicy.checkSetRight(zsc, context, this);
        
        ModifySystemRetentionPolicyRequest req = JaxbUtil.elementToJaxb(request);
        Policy p = req.getPolicy();
        if (p == null) {
            throw ServiceException.INVALID_REQUEST("policy not specified", null);
        }
        if (p.getId() == null) {
            throw ServiceException.INVALID_REQUEST("id not specified for policy", null);
        }
        
        RetentionPolicyManager mgr = RetentionPolicyManager.getInstance();
        String id = p.getId();
        Policy current = mgr.getPolicyById(id);
        if (current == null) {
            throw ServiceException.INVALID_REQUEST("Could not find system retention policy with id " + id, null);
        }
        String name = SystemUtil.coalesce(p.getName(), current.getName());
        String lifetime = SystemUtil.coalesce(p.getLifetime(), current.getLifetime());
        Policy latest = mgr.modifySystemPolicy(id, name, lifetime);
        ModifySystemRetentionPolicyResponse res = new ModifySystemRetentionPolicyResponse(latest);
        return JaxbUtil.jaxbToElement(res, zsc.getResponseProtocol().getFactory());
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add("Need set attr right on attribute " + CreateSystemRetentionPolicy.SYSTEM_RETENTION_POLICY_ATTR);
    }

}
