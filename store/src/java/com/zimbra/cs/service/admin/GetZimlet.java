/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class GetZimlet extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Set<String> reqAttrs = getReqAttrs(request, AttributeClass.zimletEntry);
        
        Element z = request.getElement(AdminConstants.E_ZIMLET);
        String n = z.getAttribute(AdminConstants.A_NAME);

        Zimlet zimlet = prov.getZimlet(n);

        if (zimlet == null)
            throw AccountServiceException.NO_SUCH_ZIMLET(n);
        
        AdminAccessControl aac = checkRight(zsc, context, zimlet, AdminRight.PR_ALWAYS_ALLOW);

        Element response = zsc.createElement(AdminConstants.GET_ZIMLET_RESPONSE);
        encodeZimlet(response, zimlet, reqAttrs, aac.getAttrRightChecker(zimlet));
        
        return response;
    }

    static void encodeZimlet(Element response, Zimlet zimlet) throws ServiceException {
        encodeZimlet(response, zimlet, null, null);
    }

    static void encodeZimlet(Element response, Zimlet zimlet, Set<String> reqAttrs, 
            AttrRightChecker attrRightChecker) throws ServiceException {
        Element zim = response.addElement(AdminConstants.E_ZIMLET);
        zim.addAttribute(AdminConstants.A_NAME, zimlet.getName());
        zim.addAttribute(AdminConstants.A_ID, zimlet.getId());
        String keyword = zimlet.getAttr(Provisioning.A_zimbraZimletKeyword);
        if (keyword != null)
            zim.addAttribute(AdminConstants.A_HAS_KEYWORD, keyword);
        Map<String,Object> attrs = zimlet.getUnicodeAttrs();
        ToXML.encodeAttrs(zim, attrs, reqAttrs, attrRightChecker);
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getZimlet);
        notes.add(String.format(AdminRightCheckPoint.Notes.GET_ENTRY, Admin.R_getZimlet.getName()));
    }
}
