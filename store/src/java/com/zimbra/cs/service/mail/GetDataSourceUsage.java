/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.DataSourceUsage;
import com.zimbra.soap.mail.message.GetDataSourceUsageResponse;

public class GetDataSourceUsage extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        GetDataSourceUsageResponse resp = new GetDataSourceUsageResponse();
        resp.setDataSourceQuota(account.getDataSourceQuota());
        resp.setDataSourceTotalQuota(account.getDataSourceTotalQuota());
        for (DataSource ds: account.getAllDataSources()) {
            DataSourceUsage dsu = new DataSourceUsage();
            dsu.setId(ds.getId());
            dsu.setUsage(ds.getUsage());
            resp.addDataSourceUsage(dsu);
        }
        return zsc.jaxbToElement(resp);
    }
}
