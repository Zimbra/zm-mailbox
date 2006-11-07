/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;


public class ModifyDataSource extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account account = getRequestedAccount(zsc);
        
        Element ePop3 = request.getElement(MailService.E_DS_POP3);
        String id = ePop3.getAttribute(MailService.A_ID);

        Map<String, Object> dsAttrs = new HashMap<String, Object>();
        String value = ePop3.getAttribute(MailService.A_NAME, null);
        if (value != null)
            dsAttrs.put(Provisioning.A_zimbraDataSourceName, value);
        value = ePop3.getAttribute(MailService.A_DS_IS_ENABLED, null); 
        if (value != null)
            dsAttrs.put(Provisioning.A_zimbraDataSourceEnabled,
                LdapUtil.getBooleanString(ePop3.getAttributeBool(MailService.A_DS_IS_ENABLED)));
        value = ePop3.getAttribute(MailService.A_FOLDER, null);
        if (value != null)
        	dsAttrs.put(Provisioning.A_zimbraDataSourceFolderId, value);
        value = ePop3.getAttribute(MailService.A_DS_HOST, null);
        if (value != null)
        	dsAttrs.put(Provisioning.A_zimbraDataSourceHost, value);
        value = ePop3.getAttribute(MailService.A_DS_PORT, null);
        if (value != null)
        	dsAttrs.put(Provisioning.A_zimbraDataSourcePort, value);
        value = ePop3.getAttribute(MailService.A_DS_CONNECTION_TYPE, null);
        if (value != null)
            dsAttrs.put(Provisioning.A_zimbraDataSourceConnectionType, value);
        value = ePop3.getAttribute(MailService.A_DS_USERNAME, null);
        if (value != null)
        	dsAttrs.put(Provisioning.A_zimbraDataSourceUsername, value);
        value = ePop3.getAttribute(MailService.A_DS_PASSWORD, null);
        if (value != null)
        	dsAttrs.put(Provisioning.A_zimbraDataSourcePassword, value);
        prov.modifyDataSource(account, id, dsAttrs);
        
        Element response = zsc.createElement(MailService.MODIFY_DATA_SOURCE_RESPONSE);

        return response;
    }
}
