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
package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.DataSource;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.common.soap.*;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.ZimbraSoapContext;

public class ModifyDataSource extends AdminDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_ID };
    protected String[] getProxiedAccountPath()  { return TARGET_ACCOUNT_PATH; }

    /**
     * must be careful and only allow modifies to accounts/attrs domain admin has access to
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        String id = request.getAttribute(AdminConstants.E_ID);

        Account account = prov.get(AccountBy.id, id, zsc.getAuthToken());
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(id);

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");
        
        Element dsEl = request.getElement(AccountConstants.E_DATA_SOURCE);
        Map<String, Object> attrs = AdminService.getAttrs(dsEl);
        
        String dsId = dsEl.getAttribute(AccountConstants.A_ID);
        DataSource ds = prov.get(account, DataSourceBy.id, dsId);
        if (ds == null)
            throw ServiceException.INVALID_REQUEST("Cannot find data source with id=" + dsId, null);
        
        if (isDomainAdminOnly(zsc)) {
            AttributeClass klass = getAttributeClassFromType(ds.getType());
            for (String attrName : attrs.keySet()) {
                if (attrName.charAt(0) == '+' || attrName.charAt(0) == '-')
                    attrName = attrName.substring(1);

                if (!AttributeManager.getInstance().isDomainAdminModifiable(attrName, klass))
                    throw ServiceException.PERM_DENIED("can not modify attr: "+attrName);
            }
        }
        
        ZimbraLog.addDataSourceNameToContext(ds.getName());
        
        prov.modifyDataSource(account, dsId, attrs);
        
        Element response = zsc.createElement(AdminConstants.MODIFY_DATA_SOURCE_RESPONSE);
        return response;
    }
    
    static AttributeClass getAttributeClassFromType(DataSource.Type type) {
        if (type == DataSource.Type.pop3)
            return AttributeClass.pop3DataSource;
        else if (type == DataSource.Type.imap)
            return AttributeClass.imapDataSource;
        else
            return AttributeClass.dataSource;
    }
}
