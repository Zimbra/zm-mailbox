/*
 * ***** BEGIN LICENSE BLOCK *****
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
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
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
        
        //
        // Note, to defend against harvest attack, if the target is not found, return "not allowed"
        // instead of NO_SUCH_XXX.
        //
        Element response = zsc.createElement(MailConstants.CHECK_PERMISSION_RESPONSE);
        
        if (TargetType.account == tt) {
            entry = prov.get(AccountBy.fromString(targetBy), targetValue, zsc.getAuthToken());
        } else if (TargetType.calresource == tt) {
            entry = prov.get(CalendarResourceBy.fromString(targetBy), targetValue);
        } else if (TargetType.dl == tt) {
            entry = prov.get(DistributionListBy.fromString(targetBy), targetValue);
        } else
            throw ServiceException.INVALID_REQUEST("invalid target type: " + targetType, null);
        
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
