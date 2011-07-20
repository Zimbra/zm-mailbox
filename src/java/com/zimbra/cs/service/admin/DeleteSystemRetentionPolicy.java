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
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.mailbox.RetentionPolicyManager;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.DeleteSystemRetentionPolicyRequest;
import com.zimbra.soap.admin.message.DeleteSystemRetentionPolicyResponse;
import com.zimbra.soap.mail.type.Policy;

public class DeleteSystemRetentionPolicy extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        // check right
        CreateSystemRetentionPolicy.checkSetRight(zsc, context, this);
        
        DeleteSystemRetentionPolicyRequest req = JaxbUtil.elementToJaxb(request);
        Policy policy = req.getPolicy();
        if (policy == null) {
            throw ServiceException.INVALID_REQUEST("policy not specified", null);
        }
        String id = policy.getId();
        if (id == null) {
            throw ServiceException.INVALID_REQUEST("Policy id not specified", null);
        }
        boolean success = RetentionPolicyManager.getInstance().deleteSystemPolicy(id);
        if (!success) {
            throw ServiceException.INVALID_REQUEST("Could not find policy with id " + id, null);
        }
        DeleteSystemRetentionPolicyResponse res = new DeleteSystemRetentionPolicyResponse();
        return JaxbUtil.jaxbToElement(res, zsc.getResponseProtocol().getFactory()); 
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add("Need set attr right on attribute " + CreateSystemRetentionPolicy.SYSTEM_RETENTION_POLICY_ATTR);
    }

}
