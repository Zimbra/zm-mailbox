/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.cache.IMimeTypeCache;
import com.zimbra.cs.mime.MimeTypeInfo;

class LdapMimeTypeCache implements IMimeTypeCache {
	
	private List<MimeTypeInfo> mAllMimeTypes;
	private Map<String, List<MimeTypeInfo>> mMapByMimeType;
	private long mRefreshTTL;
	private long mLifetime;
	
	LdapMimeTypeCache() {
		mRefreshTTL = LC.ldap_cache_mime_maxage.intValue() * Constants.MILLIS_PER_MINUTE;
	}
	
	@Override
	public synchronized void flushCache(Provisioning prov) throws ServiceException {
		refresh((LdapProvisioning)prov);
	}
	
	@Override
	public synchronized List<MimeTypeInfo> getAllMimeTypes(Provisioning prov) 
	throws ServiceException {
		refreshIfNecessary((LdapProvisioning)prov);
		return mAllMimeTypes;
	}
	
	@Override
	public synchronized List<MimeTypeInfo> getMimeTypes(Provisioning prov, String mimeType)
	throws ServiceException {
	    
	    LdapProvisioning ldapProv = (LdapProvisioning) prov;
	    
		refreshIfNecessary(ldapProv);
		List<MimeTypeInfo> mimeTypes = mMapByMimeType.get(mimeType);
		if (mimeTypes == null) {
			mimeTypes = Collections.unmodifiableList(ldapProv.getMimeTypesByQuery(mimeType));
			mMapByMimeType.put(mimeType, mimeTypes);
		}
		return mimeTypes;
	}
	
	private void refreshIfNecessary(LdapProvisioning prov) throws ServiceException {
		if (isStale())
			refresh(prov);
	}
	
	private boolean isStale() {
		if (mAllMimeTypes == null)
			return true;
		
        return mRefreshTTL != 0 && mLifetime < System.currentTimeMillis();
    }
	
	private void refresh(LdapProvisioning prov) throws ServiceException {
		mAllMimeTypes = Collections.unmodifiableList(prov.getAllMimeTypesByQuery());
		mMapByMimeType = new HashMap<String, List<MimeTypeInfo>>();
		mLifetime = System.currentTimeMillis() + mRefreshTTL;
	}
}
