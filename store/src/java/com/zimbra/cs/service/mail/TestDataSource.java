/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016, 2019 Synacor, Inc.
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

import com.zimbra.common.account.Key.DataSourceBy;
import com.zimbra.common.account.ZAttrProvisioning.DataSourceAuthMechanism;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.HashMap;
import java.util.Map;


public class TestDataSource extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account account = getRequestedAccount(zsc);
        
        if (!canModifyOptions(zsc, account))
            throw ServiceException.PERM_DENIED("can not modify options");

        // Parse request
        Element eDataSource = CreateDataSource.getDataSourceElement(request);
        DataSourceType type = DataSourceType.fromString(eDataSource.getName());

        String error = testDataSource(prov, account, eDataSource, type);

        Element response = zsc.createElement(MailConstants.TEST_DATA_SOURCE_RESPONSE);
        eDataSource = response.addElement(type.toString());
        if (error == null) {
            eDataSource.addAttribute(MailConstants.A_DS_SUCCESS, true);
        } else {
            eDataSource.addAttribute(MailConstants.A_DS_SUCCESS, false);
            eDataSource.addAttribute(MailConstants.A_DS_ERROR, error);
        }
        
        return response;
    }

    static String testDataSource(Provisioning prov, Account account, Element eDataSource, DataSourceType type)
            throws ServiceException {
        Map<String, Object> testAttrs = new HashMap<String, Object>();
        String testId = "TestId";

        String id = eDataSource.getAttribute(MailConstants.A_ID, null);
        String password = null;
        if (id != null) {
            // Testing existing data source
            DataSource dsOrig = prov.get(account, DataSourceBy.id, id);
            Map<String, Object> origAttrs = dsOrig.getAttrs();
            for (String key : origAttrs.keySet()) {
                if (key.equals(Provisioning.A_zimbraDataSourcePassword)) {
                    password = dsOrig.getDecryptedPassword();
                } else {
                    testAttrs.put(key, dsOrig.getAttr(key));
                }
            }
        }

        // Get values from SOAP request.  If testing an existing data source,
        // values in the request override the persisted values.
        String value = eDataSource.getAttribute(MailConstants.A_DS_HOST, null);
        if (value != null) {
            testAttrs.put(Provisioning.A_zimbraDataSourceHost, value);
        }
        value = eDataSource.getAttribute(MailConstants.A_DS_PORT, null);
        if (value != null) {
            testAttrs.put(Provisioning.A_zimbraDataSourcePort, value);
        }
        value = eDataSource.getAttribute(MailConstants.A_DS_CONNECTION_TYPE, null);
        if (value != null) {
            testAttrs.put(Provisioning.A_zimbraDataSourceConnectionType, value);
        }
        value = eDataSource.getAttribute(MailConstants.A_DS_USERNAME, null);
        if (value != null) {
            testAttrs.put(Provisioning.A_zimbraDataSourceUsername, value);
        }
        value = eDataSource.getAttribute(MailConstants.A_DS_PASSWORD, null);
        if (value != null) {
            password = value;
        }
        value = eDataSource.getAttribute(MailConstants.A_DS_LEAVE_ON_SERVER, null);
        if (value != null) {
            testAttrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer, value.toUpperCase());
        }
        value = eDataSource.getAttribute(MailConstants.A_FOLDER, null);
        if (value != null) {
            testAttrs.put(Provisioning.A_zimbraDataSourceFolderId, value);
        }
        value = eDataSource.getAttribute(MailConstants.A_DS_OAUTH_TOKEN, null);
        if (value != null) {
            testAttrs.put(Provisioning.A_zimbraDataSourceOAuthToken, DataSource.encryptData(testId, value));
            testAttrs.put(Provisioning.A_zimbraDataSourceAuthMechanism, DataSourceAuthMechanism.XOAUTH2.name());
        }
        if (password != null) {
            // Password has to be encrypted explicitly since this is a temporary object.
            // The current implementation of LdapDataSource doesn't perform encryption until
            // the DataSource is saved.
            testAttrs.put(Provisioning.A_zimbraDataSourcePassword, DataSource.encryptData(testId, password));
        }

        value = eDataSource.getAttribute(MailConstants.A_DS_SMTP_PASSWORD, null);
        if (value != null) {
            // Again password has to be encrypted explicitly.
            testAttrs.put(Provisioning.A_zimbraDataSourceSmtpAuthPassword, DataSource.encryptData(testId, value));
        }

        // import class
        value = eDataSource.getAttribute(MailConstants.A_DS_IMPORT_CLASS, DataSourceManager.getDefaultImportClass(type));
        if (value != null) {
        	testAttrs.put(Provisioning.A_zimbraDataSourceImportClassName, value);
        }

        // SMTP attributes
        CreateDataSource.processSmtpAttrs(testAttrs, eDataSource, true, testId);

        // Common optional attributes
        ModifyDataSource.processCommonOptionalAttrs(account, testAttrs, eDataSource);

        // Perform test and assemble response

        DataSource ds = new DataSource(account, type, "Test", testId, testAttrs, null);

        String error = null;
        try {
        	DataSourceManager.test(ds);
        } catch (ServiceException x) {
        	Throwable t = SystemUtil.getInnermostException(x);
        	error = t.getMessage();
        	if (error == null) {
        		error = "datasource test failed";
            }
        }
        return error;
    }
    
    public static void testDataSourceConnection(Provisioning prov, Element eDataSource, DataSourceType type,
        Account account) throws ServiceException {
        String error = testDataSource(prov, account, eDataSource, type);
        if (error != null) {
            throw ServiceException.INVALID_REQUEST("DataSource test failed with error: " + error,
                null);
        }
    }
}
