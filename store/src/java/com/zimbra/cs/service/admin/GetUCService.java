/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.UCService;
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author pshao
 */
public class GetUCService extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Set<String> reqAttrs = getReqAttrs(request, AttributeClass.ucService);
        
        Element eUCService = request.getElement(AdminConstants.E_UC_SERVICE);
        String by = eUCService.getAttribute(AdminConstants.A_BY);
        String name = eUCService.getText();

        if (Strings.isNullOrEmpty(name)) {
            throw ServiceException.INVALID_REQUEST("must specify a value for a uc service", null);
        }
        
        UCService ucService = prov.get(Key.UCServiceBy.fromString(by), name);
        
        if (ucService == null) {
            throw AccountServiceException.NO_SUCH_UC_SERVICE(name);
        }
        
        AdminAccessControl aac = checkRight(zsc, context, ucService, AdminRight.PR_ALWAYS_ALLOW);
        
        // reload the uc service 
        prov.reload(ucService);
        
        Element response = zsc.createElement(AdminConstants.GET_UC_SERVICE_RESPONSE);
        encodeUCService(response, ucService, reqAttrs, aac.getAttrRightChecker(ucService));

        return response;
    }

    public static void encodeUCService(Element e, UCService s, Set<String> reqAttrs,
            AttrRightChecker attrRightChecker) throws ServiceException {
        Element eUCService = e.addElement(AdminConstants.E_UC_SERVICE);
        eUCService.addAttribute(AdminConstants.A_NAME, s.getName());
        eUCService.addAttribute(AdminConstants.A_ID, s.getId());
        Map<String, Object> attrs = s.getUnicodeAttrs();
        
        ToXML.encodeAttrs(eUCService, attrs, reqAttrs, attrRightChecker);
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getUCService);
        notes.add(String.format(AdminRightCheckPoint.Notes.GET_ENTRY, Admin.R_getUCService.getName()));
    }
}
