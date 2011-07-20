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
import com.zimbra.soap.admin.message.GetSystemRetentionPolicyResponse;
import com.zimbra.soap.mail.type.RetentionPolicy;

public class GetSystemRetentionPolicy extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        // check right
        checkGetRight(zsc, context);
        
        RetentionPolicy rp = RetentionPolicyManager.getInstance().getSystemRetentionPolicy();
        GetSystemRetentionPolicyResponse res = new GetSystemRetentionPolicyResponse(rp);
        return JaxbUtil.jaxbToElement(res, zsc.getResponseProtocol().getFactory());
    }
    
    private void checkGetRight(ZimbraSoapContext zsc, Map<String, Object> context) 
    throws ServiceException {
        Config config = Provisioning.getInstance().getConfig();
        AdminAccessControl.GetAttrsRight gar = new AdminAccessControl.GetAttrsRight();
        gar.addAttr(CreateSystemRetentionPolicy.SYSTEM_RETENTION_POLICY_ATTR);
        checkRight(zsc, context, config, gar);
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add("Need get attr right on attribute " + 
                CreateSystemRetentionPolicy.SYSTEM_RETENTION_POLICY_ATTR);
    }

}
