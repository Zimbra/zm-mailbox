/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.DataSourceBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.soap.ZimbraSoapContext;


public class ImportData extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account account = getRequestedAccount(zsc);

        for (Element elem : request.listElements()) {
            DataSource ds;

            String name, id = elem.getAttribute(MailConstants.A_ID, null);
            if (id != null) {
                ds = prov.get(account, Key.DataSourceBy.id, id);
                if (ds == null) {
                    throw ServiceException.INVALID_REQUEST("Could not find Data Source with id " + id, null);
                }
            } else if ((name = elem.getAttribute(MailConstants.A_NAME, null)) != null) {
                ds = prov.get(account, Key.DataSourceBy.name, name);
                if (ds == null) {
                    throw ServiceException.INVALID_REQUEST("Could not find Data Source with name " + name, null);
                }
            } else {
                throw ServiceException.INVALID_REQUEST("must specify either 'id' or 'name'", null);
            }

            ZimbraLog.addDataSourceNameToContext(ds.getName());
            DataSourceManager.asyncImportData(ds);
        }

        Element response = zsc.createElement(MailConstants.IMPORT_DATA_RESPONSE);
        return response;
    }

}
