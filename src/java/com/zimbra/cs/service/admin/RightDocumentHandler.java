/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class RightDocumentHandler extends AdminDocumentHandler {
    
    Entry getTargetEntry(Provisioning prov, Element eTarget, TargetType targetType) throws ServiceException {
        TargetBy targetBy = TargetBy.fromString(eTarget.getAttribute(AdminConstants.A_BY));
        String target = eTarget.getText();
         
        return TargetType.lookupTarget(prov, targetType, targetBy, target);
    }
    
    NamedEntry getGranteeEntry(Provisioning prov, Element eGrantee, GranteeType granteeType) throws ServiceException {
        if (!granteeType.allowedForAdminRights())
            throw ServiceException.INVALID_REQUEST("unsupported grantee type: " + granteeType.getCode(), null);
        
        GranteeBy granteeBy = GranteeBy.fromString(eGrantee.getAttribute(AdminConstants.A_BY));
        String grantee = eGrantee.getText();
        
        return GranteeType.lookupGrantee(prov, granteeType, granteeBy, grantee);
    }
    
    /**
     * check the checkRight right
     * 
     * check if the authed admin has the checkRight right on the user it is
     * checking right for.
     * 
     * @param zsc
     * @param granteeBy
     * @param grantee
     */
    protected void checkCheckRightRight(ZimbraSoapContext zsc, GranteeBy granteeBy, String grantee) throws ServiceException {
        NamedEntry granteeEntry = GranteeType.lookupGrantee(Provisioning.getInstance(), GranteeType.GT_USER, granteeBy, grantee);  
        Account granteeAcct = (Account)granteeEntry;
        
        // call checkRight instead of checkAccountRight because there is no 
        // backward compatibility issue for this SOAP.
        //
        // Note: granteeAcct is the target for the R_checkRight right here
        checkRight(zsc, granteeAcct, Admin.R_checkRight);
    }
    
    protected Pair<Boolean, Boolean> parseExpandAttrs(Element request) throws ServiceException {
        String expandAttrs = request.getAttribute(AdminConstants.A_EXPAND_ALL_ATTRS, null);
        boolean expandSetAttrs = false;
        boolean expandGetAttrs = false;
        if (expandAttrs != null) {
            String[] eas = expandAttrs.split(",");
            for (String e : eas) {
                String exp = e.trim();
                if (exp.equals("setAttrs"))
                    expandSetAttrs = true;
                else if (exp.equals("getAttrs"))
                    expandGetAttrs = true;
                else
                    throw ServiceException.INVALID_REQUEST("invalid " + AdminConstants.A_EXPAND_ALL_ATTRS + " value: " + exp, null);
            }
        }
        
        return new Pair<Boolean, Boolean>(expandSetAttrs, expandGetAttrs);
    }
}
