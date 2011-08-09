/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.ldap.Check;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.CheckHostnameResolveRequest;
import com.zimbra.soap.admin.message.CheckHostnameResolveResponse;

/**
 * @author schemers
 */
public class CheckHostnameResolve extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        CheckHostnameResolveRequest req = JaxbUtil.elementToJaxb(request);
        String host = req.getHostname().toLowerCase();

        Provisioning.Result r = Check.checkHostnameResolve(host);

        return zsc.jaxbToElement(
            CheckHostnameResolveResponse.fromCodeMessage(
                    r.getCode(), r.getMessage()));
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.ALLOW_ALL_ADMINS);
    }
}