/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.Folder;
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
        DataSourceType type = DataSourceType.fromString(eDataSource.getName());
        Map<String, Object> dsAttrs = new HashMap<String, Object>();

        // Common attributes
        validateFolderId(account, mbox, eDataSource, type);
        String name = eDataSource.getAttribute(MailConstants.A_NAME);
        dsAttrs.put(Provisioning.A_zimbraDataSourceFolderId, eDataSource.getAttribute(MailConstants.A_FOLDER));
        dsAttrs.put(Provisioning.A_zimbraDataSourceEnabled,
            LdapUtil.getLdapBooleanString(eDataSource.getAttributeBool(MailConstants.A_DS_IS_ENABLED)));
        dsAttrs.put(Provisioning.A_zimbraDataSourceImportOnly,
                LdapUtil.getLdapBooleanString(eDataSource.getAttributeBool(MailConstants.A_DS_IS_IMPORTONLY,false)));
        dsAttrs.put(Provisioning.A_zimbraDataSourceHost, eDataSource.getAttribute(MailConstants.A_DS_HOST));
        dsAttrs.put(Provisioning.A_zimbraDataSourcePort, eDataSource.getAttribute(MailConstants.A_DS_PORT));
        dsAttrs.put(Provisioning.A_zimbraDataSourceConnectionType, eDataSource.getAttribute(MailConstants.A_DS_CONNECTION_TYPE));
        dsAttrs.put(Provisioning.A_zimbraDataSourceUsername, eDataSource.getAttribute(MailConstants.A_DS_USERNAME));
        dsAttrs.put(Provisioning.A_zimbraDataSourcePassword, eDataSource.getAttribute(MailConstants.A_DS_PASSWORD));
        
        String defaultSignature = eDataSource.getAttribute(MailConstants.A_DS_DEFAULT_SIGNATURE, null);
        if (defaultSignature != null) {
            dsAttrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, defaultSignature);
        }
        
        String forwardReplySignature = eDataSource.getAttribute(MailConstants.A_DS_FORWARD_REPLY_SIGNATURE, null);
        if (forwardReplySignature != null) {
            dsAttrs.put(Provisioning.A_zimbraPrefForwardReplySignatureId, forwardReplySignature);
        }
        
        // type
        dsAttrs.put(Provisioning.A_zimbraDataSourceType, type.toString());
        
        // import class
        String importClass = eDataSource.getAttribute(MailConstants.A_DS_IMPORT_CLASS, DataSourceManager.getDefaultImportClass(type));
        if (importClass != null) {
        	dsAttrs.put(Provisioning.A_zimbraDataSourceImportClassName, importClass);
        }
        
        // Common optional attributes
        ModifyDataSource.processCommonOptionalAttrs(dsAttrs, eDataSource);
        
        // POP3-specific attributes
        if (type == DataSourceType.pop3) {
            dsAttrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer,
                LdapUtil.getLdapBooleanString(eDataSource.getAttributeBool(MailConstants.A_DS_LEAVE_ON_SERVER, true)));
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
     * Confirms that the folder attribute specifies a valid folder id and is not
     * within the subtree of another datasource
     */
    static void validateFolderId(Account account, Mailbox mbox, Element eDataSource, DataSourceType dsType)
    throws ServiceException {
        int folderId = (int) eDataSource.getAttributeLong(MailConstants.A_FOLDER);
        String id = eDataSource.getAttribute(MailConstants.A_ID, null);

        try {
            mbox.getFolderById(null, folderId);
        } catch (NoSuchItemException e) {
            throw ServiceException.INVALID_REQUEST("Invalid folder id: " + folderId, null);
        }
        for (DataSource ds : account.getAllDataSources()) {
            if (id != null && ds.getId().equals(id))
                continue;
            try {
                for (Folder fldr : mbox.getFolderById(null, ds.getFolderId()).getSubfolderHierarchy()) {
                    if (fldr.getId() == folderId)
                        if ( (DataSourceType.pop3.equals(dsType)) && (DataSourceType.pop3.equals(ds.getType())) ) {
                            // Allows unified inbox to work for more than one Pop3 datasource
                        } else {
                            throw ServiceException.INVALID_REQUEST("Folder location conflict: " + fldr.getPath(), null);
                        }
                }
            } catch (NoSuchItemException e) {
            }
        }
    }
}
