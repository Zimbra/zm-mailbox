/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Provisioning.CacheEntryBy;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.SkinUtil;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

public class FlushCache extends AdminDocumentHandler {

	public static final String FLUSH_CACHE = "flushCache";
    
    /**
     * must be careful and only allow deletes domain admin has access to
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        Element eCache = request.getElement(AdminConstants.E_CACHE);
        String type = eCache.getAttribute(AdminConstants.A_TYPE);
        
        if (isDomainAdminOnly(zsc)) {
            if (!CacheEntryType.account.name().equals(type))
                throw ServiceException.PERM_DENIED("cannot flush cache");
        }

        if (type.equals("zimlet")) {
            FlushCache.sendFlushRequest(context, "/service", "/zimlet/res/all.js");
        }
        if (type.equals("skin")) {
            SkinUtil.flushSkinCache();
            FlushCache.sendFlushRequest(context, "/zimbra", "/js/skin.js");
        }
        else if (type.equals("locale"))
            L10nUtil.flushLocaleCache();
        else {
            Provisioning prov = Provisioning.getInstance();
            List<Element> eEntries = eCache.listElements(AdminConstants.E_ENTRY);
            CacheEntry[] entries = null;
            if (eEntries.size() > 0) {
                entries = new CacheEntry[eEntries.size()];
                int i = 0;
                for (Element eEntry : eEntries) {
                    CacheEntry ce = new CacheEntry(CacheEntryBy.valueOf(eEntry.getAttribute(AdminConstants.A_BY)),
                                                  eEntry.getText());
                    
                    if (isDomainAdminOnly(zsc)) {
                        // must be flushing account 
                        AccountBy accountBy = (ce.mEntryBy==CacheEntryBy.id)? AccountBy.id : AccountBy.name;
                        Account account = prov.getFromCache(accountBy, ce.mEntryIdentity);
                        if (account != null && !canAccessEmail(zsc, account.getName()))
                            throw ServiceException.PERM_DENIED("cannot flush cache");
                    }
                    entries[i++] = ce;
                }
            }
            
            // domain admin cannot flush cache on all accounts
            if (isDomainAdminOnly(zsc) && entries == null)
                throw ServiceException.PERM_DENIED("cannot flush cache");
            
            prov.flushCache(type, entries);
        }

        Element response = zsc.createElement(AdminConstants.FLUSH_CACHE_RESPONSE);
        return response;
    }

	static void sendFlushRequest(Map<String,Object> context,
	                             String appContext, String resourceUri) throws ServiceException {
		ServletContext containerContext = (ServletContext)context.get(SoapServlet.SERVLET_CONTEXT);
		if (containerContext == null) {
			if (ZimbraLog.misc.isDebugEnabled()) {
				ZimbraLog.misc.debug("flushCache: no container context");
			}
			return;
		}
		ServletContext webappContext = containerContext.getContext(appContext);
		RequestDispatcher dispatcher = webappContext.getRequestDispatcher(resourceUri);
		if (dispatcher == null) {
			if (ZimbraLog.misc.isDebugEnabled()) {
				ZimbraLog.misc.debug("flushCache: no dispatcher for "+resourceUri);
			}
			return;
		}

		try {
			if (ZimbraLog.misc.isDebugEnabled()) {
				ZimbraLog.misc.debug("flushCache: sending flush request");
			}
			ServletRequest request = (ServletRequest)context.get(SoapServlet.SERVLET_REQUEST);
			request.setAttribute(FLUSH_CACHE, Boolean.TRUE);
			ServletResponse response = (ServletResponse)context.get(SoapServlet.SERVLET_RESPONSE);
			dispatcher.include(request, response);
		}
		catch (Throwable t) {
			// ignore error
			if (ZimbraLog.misc.isDebugEnabled()) {
				ZimbraLog.misc.debug("flushCache: "+t.getMessage());
			}
		}
	}

}