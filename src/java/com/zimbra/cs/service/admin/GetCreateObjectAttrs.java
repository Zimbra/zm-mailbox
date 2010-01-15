/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
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
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

public class GetCreateObjectAttrs extends RightDocumentHandler {
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        Element eTarget = request.getElement(AdminConstants.E_TARGET);
        String targetType = eTarget.getAttribute(AdminConstants.A_TYPE);
        
        DomainBy domainBy = null;
        String domain = null;
        Element eDomain = request.getOptionalElement(AdminConstants.E_DOMAIN);
        if (eDomain != null) {
            domainBy = DomainBy.fromString(eDomain.getAttribute(AdminConstants.A_BY));
            domain = eDomain.getText();
        }
        
        CosBy cosBy = null;
        String cos = null;
        Element eCos = request.getOptionalElement(AdminConstants.E_COS);
        if (eCos != null) {
            cosBy = CosBy.fromString(eCos.getAttribute(AdminConstants.A_BY));
            cos = eCos.getText();
        }
        
        GranteeBy granteeBy = GranteeBy.id;
        String grantee = zsc.getRequestedAccountId();
        
        if (!grantee.equals(zsc.getAuthtokenAccountId()))
            checkCheckRightRight(zsc, GranteeType.GT_USER, granteeBy, grantee);
        
        RightCommand.EffectiveRights er = RightCommand.getCreateObjectAttrs(Provisioning.getInstance(),
                                                                            targetType,
                                                                            domainBy, domain,
                                                                            cosBy, cos,
                                                                            granteeBy, grantee);

            

        Element resp = zsc.createElement(AdminConstants.GET_CREATE_OBJECT_ATTRS_RESPONSE);
        er.toXML_getCreateObjectAttrs(resp);
        return resp;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_checkRightUsr);
    }
}
