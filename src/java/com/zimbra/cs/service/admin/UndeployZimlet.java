/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

import java.util.Map;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class UndeployZimlet extends AdminDocumentHandler {

	private static class UndeployThread implements Runnable {
		String name;
		ZAuthToken auth;
		public UndeployThread(String na, ZAuthToken au) {
			name = na;
			auth = au;
		}
		public void run() {
			try {
				ZimletUtil.uninstallZimlet(name, auth);
			} catch (Exception e) {
				ZimbraLog.zimlet.info("undeploy", e);
			}
		}
	}
	
	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
	    String name = request.getAttribute(AdminConstants.A_NAME);
		String action = request.getAttribute(AdminConstants.A_ACTION, null);
		ZAuthToken auth = null;
		
		if (action == null)
			auth = lc.getRawAuthToken();
	    Element response = lc.createElement(AdminConstants.UNDEPLOY_ZIMLET_RESPONSE);
	    new Thread(new UndeployThread(name, auth)).start();
		return response;
	}
}
