/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.cs.datasource.ImportStatus;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;


public class GetImportStatus extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        Provisioning prov = Provisioning.getInstance();
        
        List<ImportStatus> statusList = DataSourceManager.getImportStatus(account);
        Element response = zsc.createElement(MailConstants.GET_IMPORT_STATUS_RESPONSE);

        for (ImportStatus status : statusList) {
            DataSource ds = prov.get(
                account, Key.DataSourceBy.id, status.getDataSourceId());
            Element eDataSource = response.addElement(ds.getType().name());
            eDataSource.addAttribute(MailConstants.A_ID, status.getDataSourceId());
            eDataSource.addAttribute(MailConstants.A_DS_IS_RUNNING, status.isRunning());
            
            if (status.hasRun() && !status.isRunning()) { // Has finished at least one run
                eDataSource.addAttribute(MailConstants.A_DS_SUCCESS, status.getSuccess());
                if (!status.getSuccess() && status.getError() != null) { // Failed, error available
                    eDataSource.addAttribute(MailConstants.A_DS_ERROR, status.getError());
                }
            }
        }
        return response;
    }
}
