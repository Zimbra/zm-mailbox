/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.datasource;

import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.client.CalDavClient;

public class CalDavDataImport extends MailItemImport {

	public CalDavDataImport(DataSource ds) {
		super(ds);
	}
	
	public void importData(List<Integer> folderIds, boolean fullSync)
			throws ServiceException {
	}

	public String test() throws ServiceException {
		try {
	        DataSource ds = getDataSource();
			CalDavClient client = new CalDavClient(getTargetUrl());
			client.setCredential(ds.getUsername(), ds.getDecryptedPassword());
			client.login(getPrincipalUrl());
		} catch (Exception e) {
			return e.getMessage();
		}
		return null;
	}

	private String getPrincipalUrl() {
        DataSource ds = getDataSource();
		String attrs[] = ds.getMultiAttr(Provisioning.A_zimbraDataSourceAttribute);
		for (String a : attrs) {
			if (a.startsWith("p:"))
				return a.substring(2);
		}
		return null;
	}
	
	private String getTargetUrl() {
        DataSource ds = getDataSource();
        DataSource.ConnectionType ctype = ds.getConnectionType();
        StringBuilder url = new StringBuilder();
        
        switch (ctype) {
        case ssl:
        	url.append("https://");
        	break;
        case cleartext:
        default:
        	url.append("http://");
        	break;
        		
        }
        url.append(ds.getHost()).append(":").append(ds.getPort());
        return url.toString();
	}
}
