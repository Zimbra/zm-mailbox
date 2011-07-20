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
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.mailbox.RetentionPolicyManager;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.CreateSystemRetentionPolicyRequest;
import com.zimbra.soap.admin.message.CreateSystemRetentionPolicyResponse;
import com.zimbra.soap.mail.type.Policy;

public class CreateSystemRetentionPolicy extends AdminDocumentHandler {

    static final String SYSTEM_RETENTION_POLICY_ATTR = Provisioning.A_zimbraMailPurgeSystemPolicy;
    
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        // check right
        checkSetRight(zsc, context, this);
        
        CreateSystemRetentionPolicyRequest req = JaxbUtil.elementToJaxb(request);
        
        Policy keep = req.getKeepPolicy();
        Policy purge = req.getPurgePolicy();
        if (keep == null && purge == null) {
            throw ServiceException.INVALID_REQUEST("No keep or purge policy specified.", null); 
        }
        if (keep != null && purge != null) {
            throw ServiceException.INVALID_REQUEST("Cannot specify both keep and purge policy.", null);
        }
        
        Policy newPolicy;
        if (keep != null) {
            newPolicy = RetentionPolicyManager.getInstance().createSystemKeepPolicy(keep.getName(), keep.getLifetime());
        } else {
            newPolicy = RetentionPolicyManager.getInstance().createSystemPurgePolicy(purge.getName(), purge.getLifetime());
        }
        CreateSystemRetentionPolicyResponse res = new CreateSystemRetentionPolicyResponse(newPolicy);
        return JaxbUtil.jaxbToElement(res, zsc.getResponseProtocol().getFactory());
    }
    
    static void checkSetRight(ZimbraSoapContext zsc, Map<String, Object> context,
            AdminDocumentHandler handler) 
    throws ServiceException {
        Config config = Provisioning.getInstance().getConfig();
        AdminAccessControl.SetAttrsRight sar = new AdminAccessControl.SetAttrsRight();
        sar.addAttr(CreateSystemRetentionPolicy.SYSTEM_RETENTION_POLICY_ATTR);
        handler.checkRight(zsc, context, config, sar);
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add("Need set attr right on attribute " + SYSTEM_RETENTION_POLICY_ATTR);
    }
}
