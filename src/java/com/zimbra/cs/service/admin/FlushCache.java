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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.cs.account.CacheExtension;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Provisioning.CacheEntryBy;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.httpclient.URLUtil;
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
        
        Server localServer = Provisioning.getInstance().getLocalServer();
        checkRight(zsc, context, localServer, Admin.R_flushCache);
        
        Element eCache = request.getElement(AdminConstants.E_CACHE);
        String typeStr = eCache.getAttribute(AdminConstants.A_TYPE);
        boolean allServers = eCache.getAttributeBool(AdminConstants.A_ALLSERVERS, false);
        
        String[] types = typeStr.split(",");
        
        for (String type : types) {
            CacheEntryType cacheType = null;
            
            try {
                cacheType = CacheEntryType.fromString(type);
                doFlush(context, cacheType, eCache);
            } catch (ServiceException e) {
                // see if it a registered extension
                CacheExtension ce = CacheExtension.getHandler(type);
                if (ce != null)
                    ce.flushCache();
                else
                    throw ServiceException.INVALID_REQUEST("invalid cache type "+type, null);
            }
        }

        if (allServers)
            flushCacheOnAllServers(zsc, request);
        
        Element response = zsc.createElement(AdminConstants.FLUSH_CACHE_RESPONSE);
        return response;
    }
   
    private void doFlush(Map<String, Object> context, CacheEntryType cacheType, Element eCache) throws ServiceException {
        
        switch (cacheType) {
        case skin:
            SkinUtil.flushSkinCache();
            FlushCache.sendFlushRequest(context, "/zimbra", "/js/skin.js");
            break;
        case locale:
            L10nUtil.flushLocaleCache();
            break;
        case license:
            flushLdapCache(CacheEntryType.config, eCache); // refresh global config for parsed license
            LdapProvisioning.refreshValidators();          // refresh other bits of cached license data
            break;
        case zimlet:
            FlushCache.sendFlushRequest(context, "/service", "/zimlet/res/all.js");
            // fall through to also flush ldap entries
        default:
            flushLdapCache(cacheType, eCache);
        }
    }
    
    private void flushLdapCache(CacheEntryType cacheType, Element eCache) throws ServiceException {
        List<Element> eEntries = eCache.listElements(AdminConstants.E_ENTRY);
        CacheEntry[] entries = null;
        if (eEntries.size() > 0) {
            entries = new CacheEntry[eEntries.size()];
            int i = 0;
            for (Element eEntry : eEntries) {
                entries[i++] = new CacheEntry(CacheEntryBy.valueOf(eEntry.getAttribute(AdminConstants.A_BY)),
                                              eEntry.getText());
            }
        }
        Provisioning.getInstance().flushCache(cacheType, entries);
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
    
    public static void flushChacheOnServer(Server server,ZimbraSoapContext zsc,String cacheType) throws ServiceException, IOException {
		String adminUrl = URLUtil.getAdminURL(server, AdminConstants.ADMIN_SERVICE_URI);
		SoapHttpTransport mTransport = new SoapHttpTransport(adminUrl);
		mTransport.setAuthToken(zsc.getRawAuthToken());
		XMLElement req = new XMLElement(AdminConstants.FLUSH_CACHE_REQUEST);
		req.addElement(AdminConstants.E_CACHE).addAttribute(AdminConstants.A_TYPE, cacheType);
		mTransport.invoke(req);		
    }
    
    private void flushCacheOnAllServers(ZimbraSoapContext zsc, Element origReq) throws ServiceException {
       
        Provisioning prov = Provisioning.getInstance();
        String localServerId = prov.getLocalServer().getId();
        
        for (Server server : prov.getAllServers(Provisioning.SERVICE_MAILBOX)) {
            
            if (localServerId.equals(server.getId()))
                continue;
            
            ZimbraLog.misc.debug("Flushing cache on server: " + server.getName());
            
            Element req = origReq.clone();
            Element eCache = req.getElement(AdminConstants.E_CACHE);
            eCache.addAttribute(AdminConstants.A_ALLSERVERS, false);
            
            String adminUrl = URLUtil.getAdminURL(server, AdminConstants.ADMIN_SERVICE_URI);
            SoapHttpTransport mTransport = new SoapHttpTransport(adminUrl);
            mTransport.setAuthToken(zsc.getRawAuthToken());
            
            try {
                mTransport.invoke(req);     
            } catch (ServiceException e) {
                // log and continue
                ZimbraLog.misc.warn("Encountered exception while FlushCache on server: " + server.getName() + 
                        ", skip and continue with the next server", e);
            } catch (IOException e) {
                // log and continue
                ZimbraLog.misc.warn("Encountered exception while FlushCache on server: " + server.getName() + 
                        ", skip and continue with the next server", e);
            }
        }
    }
    

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_flushCache);
    }
}