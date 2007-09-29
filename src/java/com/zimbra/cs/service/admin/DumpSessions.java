/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.io.StringWriter;
import java.util.Map;

import com.zimbra.cs.session.SessionCache;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class DumpSessions extends AdminDocumentHandler {

	@Override
	public Element handle(Element request, Map<String, Object> context) {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
		Element response = lc.createElement(AdminService.DUMP_SESSIONS_RESPONSE);
		
		StringWriter sw = new StringWriter();
		SessionCache.dumpState(sw);
		response.setText(sw.toString());
		
		return response;
	}

}
