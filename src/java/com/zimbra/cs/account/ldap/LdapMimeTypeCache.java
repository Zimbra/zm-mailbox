/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.cache.IMimeTypeCache;
import com.zimbra.cs.mime.MimeTypeInfo;

public class LdapMimeTypeCache implements IMimeTypeCache {

    private List<MimeTypeInfo> allMimeTypes;
    private Map<String, List<MimeTypeInfo>> mapByMimeType;
    private long refreshTTL = -1;
    private long lifetime;

    public LdapMimeTypeCache() {
    }

    @Override
    public synchronized void flushCache(Provisioning prov) throws ServiceException {
        refresh((LdapProv)prov);
    }

    @Override
    public synchronized List<MimeTypeInfo> getAllMimeTypes(Provisioning prov) throws ServiceException {
        refreshIfNecessary((LdapProv)prov);
        return allMimeTypes;
    }

    @Override
    public synchronized List<MimeTypeInfo> getMimeTypes(Provisioning prov, String mimeType)
            throws ServiceException {

        LdapProvisioning ldapProv = (LdapProvisioning) prov;

        refreshIfNecessary(ldapProv);
        List<MimeTypeInfo> mimeTypes = mapByMimeType.get(mimeType);
        if (mimeTypes == null) {
            mimeTypes = Collections.unmodifiableList(ldapProv.getMimeTypesByQuery(mimeType));
            mapByMimeType.put(mimeType, mimeTypes);
        }
        return mimeTypes;
    }

    private void refreshIfNecessary(LdapProv ldapProv) throws ServiceException {
        if (isStale()) {
            refresh(ldapProv);
        }
    }

    private boolean isStale() {
        if (allMimeTypes == null) {
            return true;
        }
        return refreshTTL != 0 && lifetime < System.currentTimeMillis();
    }

    private void refresh(LdapProv ldapProv) throws ServiceException {
        try {
            Server svr = Provisioning.getInstance().getLocalServer();
            refreshTTL = svr.getLdapCacheMimeTypeInfoMaxAge();
        } catch (ServiceException e) {
            refreshTTL = 15 * Constants.MILLIS_PER_MINUTE;
        }
        allMimeTypes = Collections.unmodifiableList(ldapProv.getAllMimeTypesByQuery());
        mapByMimeType = new HashMap<String, List<MimeTypeInfo>>();
        lifetime = System.currentTimeMillis() + refreshTTL;
    }
}
