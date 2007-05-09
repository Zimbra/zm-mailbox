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
package com.zimbra.cs.service.admin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.service.wiki.WikiServiceException;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
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
	    String username = reqAttrs.get(AdminConstants.E_NAME);
	    String password = reqAttrs.get(AdminConstants.E_PASSWORD);

	    Element t = request.getOptionalElement(AdminConstants.E_TEMPLATE);
	    String template = null, dest = null;
	    if (t != null) {
	    	template = t.getText();
	    	dest = t.getAttribute(AdminConstants.A_DEST, "Template");
	    }
        Element response = lc.createElement(AdminConstants.INIT_NOTEBOOK_RESPONSE);
        
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(new String[] {"cmd", "InitNotebook" }, reqAttrs));         

        WikiUtil wiki = null;
        Element d = request.getOptionalElement(AdminConstants.E_DOMAIN);
        if (d != null || isDomainAdminOnly(lc)) {
        	String key = (d == null) ? DomainBy.name.name() : d.getAttribute(AdminConstants.A_BY);
        	String value = (d == null) ? getAuthTokenAccountDomain(lc).getName() : d.getText();

        	Domain domain = prov.get(DomainBy.fromString(key), value);

        	if (domain == null)
        		throw AccountServiceException.NO_SUCH_DOMAIN(value);

        	if (!canAccessDomain(lc, domain)) 
        		throw ServiceException.PERM_DENIED("can not access domain"); 

        	// initialize domain wiki
        	createWikiAccount(username, password, domain);
        	wiki = WikiUtil.getInstance();
        	wiki.initDomainWiki(domain, username);
        } else {
         	// initialize global wiki
        	createWikiAccount(username, password, prov.getConfig());
        	wiki = WikiUtil.getInstance();
        	wiki.initDefaultWiki(username);
        }
        if (template != null) {
        	try {
        		wiki.startImport(username, dest, new File(template));
        	} catch (Exception e) {
        		throw WikiServiceException.ERROR("error importing wiki templates", e);
        	}
        }
        return response;        
	}
	
	private void createWikiAccount(String username, String password, Entry entry) throws ServiceException {
		Provisioning prov = Provisioning.getInstance();

    	if (username == null)
    		username = entry.getAttr(Provisioning.A_zimbraNotebookAccount, null);

    	if (username != null && prov.get(AccountBy.name, username) == null)
    		prov.createAccount(username, password, null);
	}
}