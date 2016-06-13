/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.mime.MimeTypeInfo;

public class LdapMimeTypeCache implements IMimeTypeCache {
	
	private List<MimeTypeInfo> mAllMimeTypes;
	private Map<String, List<MimeTypeInfo>> mMapByMimeType;
	private long mRefreshTTL;
	private long mLifetime;
	
	public LdapMimeTypeCache() {
		mRefreshTTL = LC.ldap_cache_mime_maxage.intValue() * Constants.MILLIS_PER_MINUTE;
	}
	
	@Override
	public synchronized void flushCache(Provisioning prov) throws ServiceException {
		refresh((LdapProv)prov);
	}
	
	@Override
	public synchronized List<MimeTypeInfo> getAllMimeTypes(Provisioning prov) throws ServiceException {
		refreshIfNecessary((LdapProv)prov);
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
	
	private void refreshIfNecessary(LdapProv ldapProv) throws ServiceException {
		if (isStale()) {
			refresh(ldapProv);
		}
	}
	
	private boolean isStale() {
		if (mAllMimeTypes == null)
			return true;
		
        return mRefreshTTL != 0 && mLifetime < System.currentTimeMillis();
    }
	
	private void refresh(LdapProv ldapProv) throws ServiceException {
		mAllMimeTypes = Collections.unmodifiableList(ldapProv.getAllMimeTypesByQuery());
		mMapByMimeType = new HashMap<String, List<MimeTypeInfo>>();
		mLifetime = System.currentTimeMillis() + mRefreshTTL;
	}
}
