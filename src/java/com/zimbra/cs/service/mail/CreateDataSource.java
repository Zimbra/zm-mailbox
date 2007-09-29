/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;


public class CreateDataSource extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account account = getRequestedAccount(zsc);
        
        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");
        
        // Create the data source.  Currently only POP3 is supported.
        Element ePop3 = request.getElement(MailService.E_DS_POP3);
        Map<String, Object> dsAttrs = new HashMap<String, Object>();
        
        String name = ePop3.getAttribute(MailService.A_NAME);
        dsAttrs.put(Provisioning.A_zimbraDataSourceFolderId, ePop3.getAttribute(MailService.A_FOLDER));
        dsAttrs.put(Provisioning.A_zimbraDataSourceEnabled,
            LdapUtil.getBooleanString(ePop3.getAttributeBool(MailService.A_DS_IS_ENABLED)));
        dsAttrs.put(Provisioning.A_zimbraDataSourceHost, ePop3.getAttribute(MailService.A_DS_HOST));
        dsAttrs.put(Provisioning.A_zimbraDataSourcePort, ePop3.getAttribute(MailService.A_DS_PORT));
        dsAttrs.put(Provisioning.A_zimbraDataSourceConnectionType, ePop3.getAttribute(MailService.A_DS_CONNECTION_TYPE));
        dsAttrs.put(Provisioning.A_zimbraDataSourceUsername, ePop3.getAttribute(MailService.A_DS_USERNAME));
        dsAttrs.put(Provisioning.A_zimbraDataSourcePassword, ePop3.getAttribute(MailService.A_DS_PASSWORD));
        dsAttrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer,
            LdapUtil.getBooleanString(ePop3.getAttributeBool(MailService.A_DS_LEAVE_ON_SERVER, true)));
        
        DataSource ds = prov.createDataSource(account, DataSource.Type.pop3, name, dsAttrs);
        
        // Assemble response
        Element response = zsc.createElement(MailService.CREATE_DATA_SOURCE_RESPONSE);
        ePop3 = response.addElement(MailService.E_DS_POP3);
        ePop3.addAttribute(MailService.A_ID, ds.getId());
        
        return response;
    }
}
