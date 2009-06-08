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
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.Provisioning.XMPPComponentBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * <GetXMPPComponentRequest>
 *    <xmppcomponent by="by">identifier</xmppcomponent>
 * </GetXMPPComponentRequest>   
 */
public class GetXMPPComponent extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        Set<String> reqAttrs = getReqAttrs(request, AttributeClass.xmppComponent);
        Element id = request.getElement(AdminConstants.E_XMPP_COMPONENT);
        String byStr = id.getAttribute(AdminConstants.A_BY);
        String name = id.getText();
        
        if (name == null || name.equals(""))
            throw ServiceException.INVALID_REQUEST("must specify a value for a xmppcomponent", null);
        
        XMPPComponentBy by = Provisioning.XMPPComponentBy.valueOf(byStr);
        
        XMPPComponent comp = prov.get(by, name);
        if (comp == null)
            throw AccountServiceException.NO_SUCH_XMPP_COMPONENT(name);
        
        AdminAccessControl aac = checkRight(zsc, context, comp, AdminRight.PR_ALWAYS_ALLOW);
        
        Element response = zsc.createElement(AdminConstants.GET_XMPPCOMPONENT_RESPONSE);
        encodeXMPPComponent(response, comp, reqAttrs, aac.getAttrRightChecker(comp));
        return response;
    }
    
    public static Element encodeXMPPComponent(Element parent, XMPPComponent comp) {
        return encodeXMPPComponent(parent, comp, null, null);
    }
    
    public static Element encodeXMPPComponent(Element parent, XMPPComponent comp,
            Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        Element e = parent.addElement(AccountConstants.E_XMPP_COMPONENT);
        e.addAttribute(AccountConstants.A_NAME, comp.getName());
        e.addAttribute(AccountConstants.A_ID, comp.getId());
        
        try { // for testing only
            e.addAttribute("x-domainName", comp.getDomain().getName());
        } catch (ServiceException ex) {}

        try { // for testing only
            e.addAttribute("x-serverName", comp.getServer().getName());
        } catch (ServiceException ex) {}
        
        ToXML.encodeAttrs(e, comp.getUnicodeAttrs(), reqAttrs, attrRightChecker);
        return e;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getXMPPComponent);
        notes.add(String.format(AdminRightCheckPoint.Notes.GET_ENTRY, Admin.R_getXMPPComponent.getName()));
    }

}
