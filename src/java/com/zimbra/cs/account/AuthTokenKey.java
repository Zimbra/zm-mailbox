/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on Jan 10, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import java.security.SecureRandom;
import java.util.HashMap;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import com.zimbra.common.service.ServiceException;

/**
 * @author schemers
 */
public class AuthTokenKey {
    
    public static final int KEY_SIZE_BYTES = 32;
    private byte[] mKey;
    private long mVersion;
    private long mCreated;
    private static HashMap<String, AuthTokenKey> mCache = new HashMap<String, AuthTokenKey>();
    private static AuthTokenKey sLatestKey;
    
    public byte[] getKey() {
        return mKey;
    }
    
    public long getVersion() {
        return mVersion;
    }
    
    public long getCreated() {
        return mCreated;
    }

    void setKey(byte[] key) {
        mKey = key;
    }

    AuthTokenKey(long version, byte[] key) throws ServiceException {
        mVersion = version;
        mCreated = System.currentTimeMillis();
        if (key != null) { 
            mKey = key;
        } else {
            SecureRandom random = new SecureRandom();
            mKey = new byte[KEY_SIZE_BYTES];
            random.nextBytes(mKey);
        }
    }

    private AuthTokenKey(String k) throws ServiceException {
        String parts[] = k.split(":");
        if (parts.length != 3)
            throw ServiceException.INVALID_REQUEST("invalid auth token key", null);
        String ver = parts[0];
        String created = parts[1];
        String data = parts[2];
        
        try {
            mVersion = Long.parseLong(ver);
        } catch (NumberFormatException e) {
            throw ServiceException.INVALID_REQUEST("invalid auth token key version", e);
        }
        
        try {
            mCreated = Long.parseLong(created);
        } catch (NumberFormatException e) {
            throw ServiceException.INVALID_REQUEST("invalid auth token key created data", e);
        }

        try {
            mKey = Hex.decodeHex(data.toCharArray());
        } catch (DecoderException e) {
            throw ServiceException.INVALID_REQUEST("invalid auth token key data", e);
        }
    }

    public String getEncoded() {
        return mVersion+":"+mCreated+":"+new String(Hex.encodeHex(mKey));
    }

    /**
     * given a particular version, return the 
     * @param version
     * @return
     * @throws ServiceException
     */
    public static AuthTokenKey getVersion(String version) throws ServiceException {
        AuthTokenKey key = mCache.get(version);
        // if not found, refresh our map. The config object will get reloaded if it is older
        // then the TTL
        if (key == null) 
            refresh(false);
        
        key = mCache.get(version);

        // still null, force config reload from LDAP
        if (key == null) 
            refresh(true);
        key = mCache.get(version);
        
        // return it, even if null
        return key;
    }

    private static synchronized void refresh(boolean reload) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        // force reload
        if (reload)
            prov.reload(config);
        
        String[] keys = config.getMultiAttr(Provisioning.A_zimbraAuthTokenKey);

        if (keys.length == 0) {
            prov.reload(config);
            keys = config.getMultiAttr(Provisioning.A_zimbraAuthTokenKey);
        }

        // bootstrap. automatically create new random key
        if (keys.length == 0) {
            AuthTokenKey key = new AuthTokenKey(0, null);
            HashMap<String, String> attrs = new HashMap<String, String>();
            attrs.put(Provisioning.A_zimbraAuthTokenKey, key.getEncoded());
            Provisioning.getInstance().modifyAttrs(config, attrs);
            keys = config.getMultiAttr(Provisioning.A_zimbraAuthTokenKey);
        }

        for (int i=0; i < keys.length; i++) {
            AuthTokenKey key = mCache.get(keys[i]);
            if (key == null) {
                key = new AuthTokenKey(keys[i]);
                mCache.put(keys[i], key);
                mCache.put(Long.toString(key.mVersion), key);
                if (sLatestKey == null || sLatestKey.mVersion < key.mVersion)
                    sLatestKey = key;
            }
        }        
    }

    public static synchronized AuthTokenKey getCurrentKey() throws ServiceException {
        if (sLatestKey == null) {
            refresh(false);
        }
        return sLatestKey;
    }
}
