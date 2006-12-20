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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.db.DbPop3Message;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;


public class DeleteDataSource extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account account = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);

        for (Element eDsrc : request.listElements()) {
            DataSource dsrc = null;
            String name = eDsrc.getAttribute(MailService.A_NAME, null);
            if (name != null)
                dsrc = prov.get(account, DataSourceBy.name, name);
            else
                dsrc = prov.get(account, DataSourceBy.id, eDsrc.getAttribute(MailService.A_ID));

            // note that we're not checking the element name against the actual data source's type
            if (dsrc == null)
                continue;
            String dataSourceId = dsrc.getId();
            DataSource.Type dstype = dsrc.getType();

            prov.deleteDataSource(account, dataSourceId);
            if (dstype == DataSource.Type.pop3)
                DbPop3Message.deleteUids(mbox, dataSourceId);
        }
        
        Element response = zsc.createElement(MailService.DELETE_DATA_SOURCE_RESPONSE);
        return response;
    }
}
