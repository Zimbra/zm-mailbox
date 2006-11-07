/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.2
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimlets
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class UndeployZimlet extends AdminDocumentHandler {

	private static class UndeployThread implements Runnable {
		String name;
		String auth;
		public UndeployThread(String na, String au) {
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
	    String name = request.getAttribute(AdminService.A_NAME);
		String action = request.getAttribute(AdminService.A_ACTION, null);
		String auth = null;
		
		if (action == null)
			auth = lc.getRawAuthToken();
	    Element response = lc.createElement(AdminService.UNDEPLOY_ZIMLET_RESPONSE);
	    new Thread(new UndeployThread(name, auth)).start();
		return response;
	}
}
