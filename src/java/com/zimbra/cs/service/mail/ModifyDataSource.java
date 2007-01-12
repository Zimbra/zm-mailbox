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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.db.DbPop3Message;
import com.zimbra.cs.mailbox.Mailbox;
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
        boolean wipeOutOldData = false;
        
        Element ePop3 = request.getElement(MailConstants.E_DS_POP3);
        String id = ePop3.getAttribute(MailConstants.A_ID);
        DataSource ds = prov.get(account, DataSourceBy.id, id);

        Map<String, Object> dsAttrs = new HashMap<String, Object>();
        String value = ePop3.getAttribute(MailConstants.A_NAME, null);
        if (value != null)
            dsAttrs.put(Provisioning.A_zimbraDataSourceName, value);
        value = ePop3.getAttribute(MailConstants.A_DS_IS_ENABLED, null);
        if (value != null)
            dsAttrs.put(Provisioning.A_zimbraDataSourceEnabled,
                LdapUtil.getBooleanString(ePop3.getAttributeBool(MailConstants.A_DS_IS_ENABLED)));
        value = ePop3.getAttribute(MailConstants.A_FOLDER, null);
        if (value != null)
        	dsAttrs.put(Provisioning.A_zimbraDataSourceFolderId, value);
        
        value = ePop3.getAttribute(MailConstants.A_DS_HOST, null);
        if (value != null && !value.equals(ds.getHost())) {
            dsAttrs.put(Provisioning.A_zimbraDataSourceHost, value);
            wipeOutOldData = true;
        }
        
        value = ePop3.getAttribute(MailConstants.A_DS_PORT, null);
        if (value != null)
        	dsAttrs.put(Provisioning.A_zimbraDataSourcePort, value);
        value = ePop3.getAttribute(MailConstants.A_DS_CONNECTION_TYPE, null);
        if (value != null)
            dsAttrs.put(Provisioning.A_zimbraDataSourceConnectionType, value);
        
        value = ePop3.getAttribute(MailConstants.A_DS_USERNAME, null);
        if (value != null && !value.equals(ds.getUsername())) {
        	dsAttrs.put(Provisioning.A_zimbraDataSourceUsername, value);
        	wipeOutOldData = true;
        }
    
        value = ePop3.getAttribute(MailConstants.A_DS_PASSWORD, null);
        if (value != null)
        	dsAttrs.put(Provisioning.A_zimbraDataSourcePassword, value);

        value = ePop3.getAttribute(MailConstants.A_DS_LEAVE_ON_SERVER, null);
        if (value != null) {
            boolean newValue = ePop3.getAttributeBool(MailConstants.A_DS_LEAVE_ON_SERVER);
            if (newValue != ds.leaveOnServer()) {
                dsAttrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer,
                    LdapUtil.getBooleanString(newValue));
                wipeOutOldData = true;
            }
        }
        
        prov.modifyDataSource(account, id, dsAttrs);
        
        if (wipeOutOldData) {
            // Host, username or leaveOnServer changed.  Wipe out
            // UID's for the data source.
            Mailbox mbox = getRequestedMailbox(zsc);
            DbPop3Message.deleteUids(mbox, ds.getId());
        }

        Element response = zsc.createElement(MailConstants.MODIFY_DATA_SOURCE_RESPONSE);

        return response;
    }
}
