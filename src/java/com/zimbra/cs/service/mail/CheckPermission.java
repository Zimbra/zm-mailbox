/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.UserRight;
import com.zimbra.soap.ZimbraSoapContext;

public class CheckPermission extends MailDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        Element eTarget = request.getElement(MailConstants.E_TARGET);
        String targetType = eTarget.getAttribute(MailConstants.A_TARGET_TYPE);
        
        TargetType tt = TargetType.fromCode(targetType);
        String targetBy = eTarget.getAttribute(MailConstants.A_TARGET_BY);
        String targetValue = eTarget.getText();
        
        NamedEntry entry = null;

        Element response = zsc.createElement(MailConstants.CHECK_PERMISSION_RESPONSE);
        
        if (TargetType.account == tt) {
            AccountBy acctBy = AccountBy.fromString(targetBy);
            entry = prov.get(acctBy, targetValue, zsc.getAuthToken());
            
            if (entry == null && acctBy == AccountBy.id) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(targetValue);
            }
            
            // otherwise, the target could be an external user, let it fall through
            // to return the default permission.
            
        } else if (TargetType.calresource == tt) {
            Key.CalendarResourceBy crBy = Key.CalendarResourceBy.fromString(targetBy);
            entry = prov.get(crBy, targetValue);
            
            if (entry == null && crBy == Key.CalendarResourceBy.id) {
                throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(targetValue);
            }
            
        } else if (TargetType.dl == tt) {
            Key.DistributionListBy dlBy = Key.DistributionListBy.fromString(targetBy);
            entry = prov.getGroupBasic(dlBy, targetValue);
            
            if (entry == null && dlBy == Key.DistributionListBy.id) {
                throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(targetValue);
            }
            
        } else {
            throw ServiceException.INVALID_REQUEST("invalid target type: " + targetType, null);
        }
        
        List<UserRight> rights = new ArrayList<UserRight>();
        for (Element eRight : request.listElements(MailConstants.E_RIGHT)) {
            UserRight r = RightManager.getInstance().getUserRight(eRight.getText());
            rights.add(r); 
        }
        
        boolean finalResult = true;
        AccessManager am = AccessManager.getInstance();
        for (UserRight right : rights) {
            boolean allow = am.canDo(zsc.getAuthToken(), entry, right, false);
            response.addElement(MailConstants.E_RIGHT).addAttribute(MailConstants.A_ALLOW, allow).setText(right.getName());
            finalResult = finalResult & allow;
        }
            
        return returnResponse(response, finalResult);
    }
    
    private Element returnResponse(Element response, boolean allow) {
        response.addAttribute(MailConstants.A_ALLOW, allow);
        return response;
    }
}
