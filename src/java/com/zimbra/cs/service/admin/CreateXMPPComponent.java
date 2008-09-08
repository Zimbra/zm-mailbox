/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.service.account.ToXML;
import com.zimbra.soap.ZimbraSoapContext;


/**
 * 
 */
public class CreateXMPPComponent extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        // <CreateXMPPComponentRequest>
        //    <xmppComponent name="name">
        //       <domain [by="id, name, virtualHostname, krb5Realm"]>domainId</domain>
        //       <server[by="id, name, serviceHostname"]>serviceId</domain>
        //       <a n="zimbraXMPPComponentCategory">category (see XEP-0030)</a>
        //       <a n="zimbraXMPPComponentName">long component name</a>
        //       [<a n="zimbraXMPPComponentType">type from XEP-0030</a>]
        //    </xmppComponent>
        //
        
        Element cEl = request.getElement(AccountConstants.E_XMPP_COMPONENT);
        Map<String, Object> attrs = AdminService.getAttrs(cEl);
        
        Element domainElt = cEl.getElement(AdminConstants.E_DOMAIN);
        String byStr = domainElt.getAttribute(AdminConstants.A_BY, "id");
        DomainBy domainby = DomainBy.valueOf(byStr);
        Domain domain = Provisioning.getInstance().get(domainby,domainElt.getText());
        
        Element serverElt = cEl.getElement(AdminConstants.E_SERVER);
        String serverByStr = serverElt.getAttribute(AdminConstants.A_BY);
        Server server = prov.get(ServerBy.fromString(serverByStr), serverElt.getText());
        
        String name = cEl.getAttribute(AccountConstants.A_NAME);
        
        if (!name.endsWith(domain.getName())) {
            throw ServiceException.INVALID_REQUEST("Specified component name must be full name, and must be a subdomain of the specified parent", null);
        }
        
        XMPPComponent comp = prov.createXMPPComponent(name, domain, server, attrs);
        
        Element response = zsc.createElement(AdminConstants.CREATE_XMPPCOMPONENT_RESPONSE);
        ToXML.encodeXMPPComponent(response, comp);
        return response;
    }
}
