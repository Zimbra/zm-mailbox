/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.datasource.DataSourceListner;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.db.DbImapFolder;
import com.zimbra.cs.db.DbPop3Message;
import com.zimbra.cs.event.EventStore;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.type.DataSourceType;


public class DeleteDataSource extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account account = getRequestedAccount(zsc);

        if (!canModifyOptions(zsc, account))
            throw ServiceException.PERM_DENIED("can not modify options");

        Mailbox mbox = getRequestedMailbox(zsc);

        for (Element eDsrc : request.listElements()) {
            DataSource dsrc = null;
            String name, id = eDsrc.getAttribute(MailConstants.A_ID, null);

            if (id != null)
                dsrc = prov.get(account, Key.DataSourceBy.id, id);
            else if ((name = eDsrc.getAttribute(MailConstants.A_NAME, null)) != null)
                dsrc = prov.get(account, Key.DataSourceBy.name, name);
            else
                throw ServiceException.INVALID_REQUEST("must specify either 'id' or 'name'", null);

            // note that we're not checking the element name against the actual data source's type
            if (dsrc == null)
                continue;
            String dataSourceId = dsrc.getId();
            DataSourceType dstype = dsrc.getType();

            prov.deleteDataSource(account, dataSourceId);
            DbDataSource.deletePurgedDataForDataSource(mbox, dataSourceId);
            if (dstype == DataSourceType.pop3)
                DbPop3Message.deleteUids(mbox, dataSourceId);
            else if (dstype == DataSourceType.imap) {
                DbImapFolder.deleteImapData(mbox, dataSourceId);
                DataSourceListner.deleteDataSource(account, dsrc);
            }
            DbDataSource.deleteAllMappings(dsrc);
            DataSourceManager.cancelSchedule(account, dataSourceId);
            deleteDataSourceEvents(account.getId(), dataSourceId);
        }
        Element response = zsc.createElement(MailConstants.DELETE_DATA_SOURCE_RESPONSE);
        return response;
    }

    private void deleteDataSourceEvents(String accountId, String dataSourceId) {
        EventStore eventStore = null;
        try {
            eventStore = EventStore.getFactory().getEventStore(accountId);
        } catch (ServiceException e) {
            ZimbraLog.datasource.debug("event store not configured - no need to delete datasource-related events");
        }
        if (eventStore != null) {
            try {
                eventStore.deleteEvents(dataSourceId);
            } catch (ServiceException e) {
                ZimbraLog.datasource.error("error deleting datasource events for %s", dataSourceId, e);
            }
        }

    }
}
