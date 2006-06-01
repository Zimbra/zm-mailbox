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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.wiki.WikiServiceException;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.cs.wiki.WikiUtil;

public class InitNotebook extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    
        ZimbraSoapContext lc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();
	    
	    Map<String,String> reqAttrs = new HashMap<String,String>();
	    for (Element el : request.listElements()) {
	    	reqAttrs.put(el.getName(), el.getText());
	    }
	    String username = reqAttrs.get(AdminService.E_NAME);
	    String password = reqAttrs.get(AdminService.E_PASSWORD);

	    Element t = request.getOptionalElement(AdminService.E_TEMPLATE);
	    String template = null, dest = null;
	    if (t != null) {
	    	template = t.getText();
	    	dest = t.getAttribute(AdminService.A_DEST, "Template");
	    }
        Element response = lc.createElement(AdminService.INIT_NOTEBOOK_RESPONSE);
        
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(new String[] {"cmd", "InitNotebook" }, reqAttrs));         

        WikiUtil wiki = null;
        Element d = request.getOptionalElement(AdminService.E_DOMAIN);
        if (d != null || isDomainAdminOnly(lc)) {
        	String key = (d == null) ? AdminService.BY_NAME : d.getAttribute(AdminService.A_BY);
        	String value = (d == null) ? getAuthTokenAccountDomain(lc).getName() : d.getText();

        	Domain domain = null;

        	if (key.equals(AdminService.BY_NAME)) {
        		domain = prov.getDomainByName(value);
        	} else if (key.equals(AdminService.BY_ID)) {
        		domain = prov.getDomainById(value);
        	} else {
        		throw ServiceException.INVALID_REQUEST("unknown value for by: "+key, null);
        	}

        	if (domain == null)
        		throw AccountServiceException.NO_SUCH_DOMAIN(value);

        	if (!canAccessDomain(lc, domain)) 
        		throw ServiceException.PERM_DENIED("can not access domain"); 

        	String defaultUsername = domain.getAttr(Provisioning.A_zimbraNotebookDomainAccount, null);

        	if (username == null && defaultUsername == null)
        		throw ServiceException.INVALID_REQUEST("username is empty", null);

        	if (username == null)
        		username = defaultUsername;

        	if (!username.equals(defaultUsername)) {
        		Map<String,String> attrMap = new HashMap<String,String>();
        		attrMap.put(Provisioning.A_zimbraNotebookDomainAccount, username);
        		domain.modifyAttrs(attrMap);
        	}

        	// initialize domain wiki
        	wiki = new WikiUtil(username, password);
        	wiki.initDomainWiki(domain);
        	
        } else {
        	
        	Config globalConfig = prov.getConfig();
        	String defaultUsername = globalConfig.getAttr(Provisioning.A_zimbraNotebookDefaultAccount);

        	if (username == null && defaultUsername == null)
        		throw ServiceException.INVALID_REQUEST("username is empty", null);

        	if (username == null)
        		username = defaultUsername;
        	
        	if (!username.equals(defaultUsername)) {
        		Map<String,String> attrMap = new HashMap<String,String>();
        		attrMap.put(Provisioning.A_zimbraNotebookDefaultAccount, username);
        		globalConfig.modifyAttrs(attrMap);
        	}
        	
        	// initialize global wiki
        	wiki = new WikiUtil(username, password);
        	wiki.initDefaultWiki();
        	
        }
        if (template != null) {
        	try {
        		wiki.startImport(dest, new File(template));
        	} catch (Exception e) {
        		throw WikiServiceException.ERROR("error importing wiki templates", e);
        	}
        }
        return response;        
	}
}