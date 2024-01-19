/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2019 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.common.account.ZAttrProvisioning.DataSourceAuthMechanism;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.datasource.DataSourceListner;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.soap.ZimbraSoapContext;


public class CreateDataSource extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element eDataSource = getDataSourceElement(request);
        DataSourceType type = DataSourceType.fromString(eDataSource.getName());

        Account account = getRequestedAccount(zsc);
        
        if (eDataSource.getAttributeBool(MailConstants.A_DS_TEST, false)) {
            TestDataSource.testDataSourceConnection(prov, eDataSource, type, account);
        }
        if (!canModifyOptions(zsc, account))
            throw ServiceException.PERM_DENIED("can not modify options");
        
        Mailbox mbox = getRequestedMailbox(zsc);
        
        // Create the data source

        String folderIdStr = eDataSource.getAttribute(MailConstants.A_FOLDER);
        int folderId = Integer.valueOf(folderIdStr);
        String name = eDataSource.getAttribute(MailConstants.A_NAME);
        boolean returnFolderId = false;
        if (folderId == -1) {
            folderId = mbox.createFolder(null, "/" + getUniqueDSFolderName(mbox, name), new Folder.FolderOptions()).getId();
            folderIdStr = String.valueOf(folderId);
            returnFolderId = true;
        } else {
            validateFolderId(account, mbox, eDataSource, type);
        }

        Map<String, Object> dsAttrs = new HashMap<>();

        // Common attributes
        dsAttrs.put(Provisioning.A_zimbraDataSourceFolderId, folderIdStr);
        dsAttrs.put(Provisioning.A_zimbraDataSourceEnabled,
            LdapUtil.getLdapBooleanString(eDataSource.getAttributeBool(MailConstants.A_DS_IS_ENABLED)));
        dsAttrs.put(Provisioning.A_zimbraDataSourceImportOnly,
                LdapUtil.getLdapBooleanString(eDataSource.getAttributeBool(MailConstants.A_DS_IS_IMPORTONLY, false)));
        dsAttrs.put(Provisioning.A_zimbraDataSourceHost, eDataSource.getAttribute(MailConstants.A_DS_HOST, null));
        dsAttrs.put(Provisioning.A_zimbraDataSourcePort, eDataSource.getAttribute(MailConstants.A_DS_PORT, null));
        dsAttrs.put(Provisioning.A_zimbraDataSourceConnectionType, eDataSource.getAttribute(MailConstants.A_DS_CONNECTION_TYPE, prov.getConfig().getDataSourceConnectionTypeAsString()));
        dsAttrs.put(Provisioning.A_zimbraDataSourceUsername, eDataSource.getAttribute(MailConstants.A_DS_USERNAME, null));
        String value = eDataSource.getAttribute(MailConstants.A_DS_PASSWORD, null);
        if (value != null) {
            dsAttrs.put(Provisioning.A_zimbraDataSourcePassword, value);
        }

        // type
        dsAttrs.put(Provisioning.A_zimbraDataSourceType, type.toString());
        
        // import class
        String importClass = eDataSource.getAttribute(MailConstants.A_DS_IMPORT_CLASS, DataSourceManager.getDefaultImportClass(type));
        if (importClass != null) {
        	dsAttrs.put(Provisioning.A_zimbraDataSourceImportClassName, importClass);
        }

        // SMTP attributes
        processSmtpAttrs(dsAttrs, eDataSource, false, null);

        // Common optional attributes
        ModifyDataSource.processCommonOptionalAttrs(account, dsAttrs, eDataSource);

        // POP3-specific attributes
        if (type == DataSourceType.pop3) {
            dsAttrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer,
                LdapUtil.getLdapBooleanString(eDataSource.getAttributeBool(MailConstants.A_DS_LEAVE_ON_SERVER, true)));
        }
        
        value = eDataSource.getAttribute(MailConstants.A_DS_OAUTH_TOKEN, null);
        if (value != null) {
            dsAttrs.put(Provisioning.A_zimbraDataSourceOAuthToken, value);
            dsAttrs.put(Provisioning.A_zimbraDataSourceAuthMechanism, DataSourceAuthMechanism.XOAUTH2.name());
        }

        value = eDataSource.getAttribute(MailConstants.A_DS_CLIENT_ID, null);
        if (value != null) {
            dsAttrs.put(Provisioning.A_zimbraDataSourceOAuthClientId, value);
        }

        value = eDataSource.getAttribute(MailConstants.A_DS_CLIENT_SECRET, null);
        if (value != null) {
            dsAttrs.put(Provisioning.A_zimbraDataSourceOAuthClientSecret, value);
        }

        value = eDataSource.getAttribute(MailConstants.A_DS_REFRESH_TOKEN, null);
        if (value != null) {
            dsAttrs.put(Provisioning.A_zimbraDataSourceOAuthRefreshToken, value);
        }

        value = eDataSource.getAttribute(MailConstants.A_DS_REFRESH_TOKEN_URL, null);
        if (value != null) {
            dsAttrs.put(Provisioning.A_zimbraDataSourceOAuthRefreshTokenUrl, value);
        }

        DataSource ds;
        try {
            ds = prov.createDataSource(account, type, name, dsAttrs);
            if (type == DataSourceType.imap) {
                DataSourceListner.createDataSource(account, ds);
            }
        } catch (ServiceException e) {
            if (returnFolderId) {
                // we should delete the auto-created folder
                mbox.delete(null, folderId, MailItem.Type.FOLDER);
            }
            throw e;
        }
        ZimbraLog.addDataSourceNameToContext(ds.getName());
        
        // Assemble response
        Element response = zsc.createElement(MailConstants.CREATE_DATA_SOURCE_RESPONSE);
        eDataSource = response.addElement(type.toString());
        eDataSource.addAttribute(MailConstants.A_ID, ds.getId());
        if (returnFolderId) {
            eDataSource.addAttribute(MailConstants.A_FOLDER, folderId);
        }

        return response;
    }

    private static String getUniqueDSFolderName(Mailbox mbox, String dsName) throws ServiceException {
        String name = dsName;
        while (true) {
            try {
                mbox.getFolderByName(null, Mailbox.ID_FOLDER_USER_ROOT, name);
                name = dsName + "_" + new Random().nextInt(1000);
            } catch (NoSuchItemException e) {
                break;
            }
        }
        return name;
    }

    /**
     * Gets the data source element from the given request.
     */
    public static Element getDataSourceElement(Element request)
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
    public static void validateFolderId(Account account, Mailbox mbox, Element eDataSource, DataSourceType dsType)
    throws ServiceException {
        int folderId = eDataSource.getAttributeInt(MailConstants.A_FOLDER);
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
            } catch (NoSuchItemException ignored) {
            }
        }
    }

    public static void processSmtpAttrs(Map<String, Object> dsAttrs, Element eDataSource, boolean encryptPasswordHere,
            String dsId)
            throws ServiceException {
        boolean smtpEnabled = false;
        String value = eDataSource.getAttribute(MailConstants.A_DS_SMTP_ENABLED, null);
        if (value != null) {
            smtpEnabled = eDataSource.getAttributeBool(MailConstants.A_DS_SMTP_ENABLED);
            dsAttrs.put(Provisioning.A_zimbraDataSourceSmtpEnabled, LdapUtil.getLdapBooleanString(smtpEnabled));
        }
        if (smtpEnabled) {
            dsAttrs.put(Provisioning.A_zimbraDataSourceSmtpHost,
                    eDataSource.getAttribute(MailConstants.A_DS_SMTP_HOST));
            dsAttrs.put(Provisioning.A_zimbraDataSourceSmtpPort,
                    eDataSource.getAttribute(MailConstants.A_DS_SMTP_PORT));
            dsAttrs.put(Provisioning.A_zimbraDataSourceSmtpConnectionType,
                    eDataSource.getAttribute(MailConstants.A_DS_SMTP_CONNECTION_TYPE));
            dsAttrs.put(Provisioning.A_zimbraDataSourceSmtpAuthRequired,
                    LdapUtil.getLdapBooleanString(eDataSource.getAttributeBool(MailConstants.A_DS_SMTP_AUTH_REQUIRED)));
            dsAttrs.put(Provisioning.A_zimbraDataSourceEmailAddress,
                    eDataSource.getAttribute(MailConstants.A_DS_EMAIL_ADDRESS));
        }
        value = eDataSource.getAttribute(MailConstants.A_DS_SMTP_USERNAME, null);
        if (value != null) {
            dsAttrs.put(Provisioning.A_zimbraDataSourceSmtpAuthUsername, value);
        }
        value = eDataSource.getAttribute(MailConstants.A_DS_SMTP_PASSWORD, null);
        if (value != null) {
            dsAttrs.put(Provisioning.A_zimbraDataSourceSmtpAuthPassword,
                    encryptPasswordHere ? DataSource.encryptData(dsId, value) : value);
        }
    }
}
