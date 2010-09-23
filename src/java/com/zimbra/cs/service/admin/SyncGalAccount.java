/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DataSource.DataImport;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.cs.gal.GalImport;
import com.zimbra.soap.ZimbraSoapContext;

public class SyncGalAccount extends AdminDocumentHandler {
	
	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    
	    ZimbraSoapContext zsc = getZimbraSoapContext(context);
		Provisioning prov = Provisioning.getInstance();
		
		ZimbraLog.addToContext(ZimbraLog.C_ANAME, getAuthenticatedAccount(zsc).getName());
		
	    Iterator<Element> accounts = request.elementIterator(AdminConstants.E_ACCOUNT);
	    while (accounts.hasNext()) {
	    	Element account = accounts.next();
		    String accountId = account.getAttribute(AdminConstants.A_ID);
		    Account acct = prov.getAccountById(accountId);
		    if (acct == null)
	            throw AccountServiceException.NO_SUCH_ACCOUNT(accountId);
		    
		    ZimbraLog.addToContext(ZimbraLog.C_NAME, acct.getName());
		    
		    Iterator<Element> datasources = account.elementIterator(AdminConstants.E_DATASOURCE);
		    while (datasources.hasNext()) {
		    	Element dsElem = datasources.next();
		    	String by = dsElem.getAttribute(AdminConstants.A_BY);
		    	String name = dsElem.getText();
		    	DataSource ds = null;
		    	if (by.compareTo("id") == 0)
		    		ds = acct.getDataSourceById(name);
		    	else
			    	ds = acct.getDataSourceByName(name);
		    	if (ds == null)
		    		throw AccountServiceException.NO_SUCH_DATA_SOURCE(name);
		    	if (!ds.getType().equals(DataSource.Type.gal))
		    		continue;
		    	boolean fullSync = dsElem.getAttributeBool(AdminConstants.A_FULLSYNC, false);
		    	boolean reset = dsElem.getAttributeBool(AdminConstants.A_RESET, false);
		    	int fid = ds.getFolderId();
		    	
		    	DataImport di = DataSourceManager.getInstance().getDataImport(ds);
		    	if (di instanceof GalImport)
		    		((GalImport)di).importGal(fid, (reset ? reset : fullSync), reset);
		    }
	    }

	    Element response = zsc.createElement(AdminConstants.SYNC_GAL_ACCOUNT_RESPONSE);
		return response;
	}
	
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
    	// XXX todo
        notes.add(AdminRightCheckPoint.Notes.TODO);
    }
}
