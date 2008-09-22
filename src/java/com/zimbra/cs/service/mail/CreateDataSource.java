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
package com.zimbra.cs.service.mail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.soap.ZimbraSoapContext;


public class CreateDataSource extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account account = getRequestedAccount(zsc);

        if (!canModifyOptions(zsc, account))
            throw ServiceException.PERM_DENIED("can not modify options");
        
        Mailbox mbox = getRequestedMailbox(zsc);
        
        // Create the data source
        Element eDataSource = getDataSourceElement(request);
        DataSource.Type type = DataSource.Type.fromString(eDataSource.getName());
        Map<String, Object> dsAttrs = new HashMap<String, Object>();

        // Common attributes
        validateFolderId(mbox, eDataSource);
        String name = eDataSource.getAttribute(MailConstants.A_NAME);
        dsAttrs.put(Provisioning.A_zimbraDataSourceFolderId, eDataSource.getAttribute(MailConstants.A_FOLDER));
        dsAttrs.put(Provisioning.A_zimbraDataSourceEnabled,
            LdapUtil.getBooleanString(eDataSource.getAttributeBool(MailConstants.A_DS_IS_ENABLED)));
        dsAttrs.put(Provisioning.A_zimbraDataSourceHost, eDataSource.getAttribute(MailConstants.A_DS_HOST));
        dsAttrs.put(Provisioning.A_zimbraDataSourcePort, eDataSource.getAttribute(MailConstants.A_DS_PORT));
        dsAttrs.put(Provisioning.A_zimbraDataSourceConnectionType, eDataSource.getAttribute(MailConstants.A_DS_CONNECTION_TYPE));
        dsAttrs.put(Provisioning.A_zimbraDataSourceUsername, eDataSource.getAttribute(MailConstants.A_DS_USERNAME));
        dsAttrs.put(Provisioning.A_zimbraDataSourcePassword, eDataSource.getAttribute(MailConstants.A_DS_PASSWORD));
        
        // type
        dsAttrs.put(Provisioning.A_zimbraDataSourceType, type.toString());
        
        // import class
        String importClass = eDataSource.getAttribute(MailConstants.A_DS_IMPORT_CLASS, DataSource.getDefaultImportClass(type));
        if (importClass != null) {
        	dsAttrs.put(Provisioning.A_zimbraDataSourceImportClassName, importClass);
        }
        
        // Common optional attributes
        ModifyDataSource.processCommonOptionalAttrs(dsAttrs, eDataSource);
        
        // POP3-specific attributes
        if (type == DataSource.Type.pop3) {
            dsAttrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer,
                LdapUtil.getBooleanString(eDataSource.getAttributeBool(MailConstants.A_DS_LEAVE_ON_SERVER, true)));
        }
        
        DataSource ds = prov.createDataSource(account, type, name, dsAttrs);
        ZimbraLog.addDataSourceNameToContext(ds.getName());
        
        // Assemble response
        Element response = zsc.createElement(MailConstants.CREATE_DATA_SOURCE_RESPONSE);
        eDataSource = response.addElement(type.toString());
        eDataSource.addAttribute(MailConstants.A_ID, ds.getId());
        
        return response;
    }
    
    /**
     * Gets the data source element from the given request.
     */
    static Element getDataSourceElement(Element request)
    throws ServiceException {
        List<Element> subElements = request.listElements();
        if (subElements.size() != 1) {
            String msg = "Only 1 data source allowed per request.  Found " + subElements.size();
            throw ServiceException.INVALID_REQUEST(msg, null);
        }
        
        return subElements.get(0);
    }

    /**
     * Confirms that the folder attribute specifies a valid folder id.
     */
    static void validateFolderId(Mailbox mbox, Element eDataSource)
    throws ServiceException {
        int folderId = (int) eDataSource.getAttributeLong(MailConstants.A_FOLDER);
        try {
            mbox.getFolderById(null, folderId);
        } catch (NoSuchItemException e) {
            throw ServiceException.INVALID_REQUEST("Invalid folder id: " + folderId, null);
        }
    }

}
