/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.zimbra.cs.util.SkinUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class FlushCache extends AdminDocumentHandler {
    
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
        
        if (type.equals("skin"))
            SkinUtil.flushSkinCache();
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

}