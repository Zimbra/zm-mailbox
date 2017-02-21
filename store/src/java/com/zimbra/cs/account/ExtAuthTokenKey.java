/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account;

import java.security.SecureRandom;
import java.util.HashMap;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;


/**
 * @author zimbra
 *
 */
public class ExtAuthTokenKey {

    public static final int KEY_SIZE_BYTES = 32;
    private byte[] key;
    private long version;
    private long created;
    private static HashMap<String, ExtAuthTokenKey> keyCache = new HashMap<String, ExtAuthTokenKey>();
    private static ExtAuthTokenKey latestExtAuthKey;

    public byte[] getKey() {
        return key;
    }

    public long getVersion() {
        return version;
    }

    public long getCreated() {
        return created;
    }

    void setKey(byte[] key) {
        this.key = key;
    }

    ExtAuthTokenKey(long version, byte[] key) throws ServiceException {
        this.version = version;
        created = System.currentTimeMillis();
        if (key != null) {
            this.key = key;
        } else {
            SecureRandom random = new SecureRandom();
            this.key = new byte[KEY_SIZE_BYTES];
            random.nextBytes(this.key);
        }
    }

    private ExtAuthTokenKey(String k) throws ServiceException {
        String parts[] = k.split(":");
        if (parts.length != 3)
            throw ServiceException.INVALID_REQUEST("invalid auth token key", null);
        String ver = parts[0];
        String created = parts[1];
        String data = parts[2];

        try {
            version = Long.parseLong(ver);
        } catch (NumberFormatException e) {
            throw ServiceException.INVALID_REQUEST("invalid auth token key version", e);
        }

        try {
            this.created = Long.parseLong(created);
        } catch (NumberFormatException e) {
            throw ServiceException.INVALID_REQUEST("invalid auth token key created data", e);
        }

        try {
            key = Hex.decodeHex(data.toCharArray());
        } catch (DecoderException e) {
            throw ServiceException.INVALID_REQUEST("invalid auth token key data", e);
        }
    }

    public String getEncoded() {
        return version+":"+created+":"+new String(Hex.encodeHex(key));
    }

    /**
     * given a particular version, return the
     * @param version
     * @return
     * @throws ServiceException
     */
    public static ExtAuthTokenKey getVersion(String version) throws ServiceException {
        ExtAuthTokenKey key = keyCache.get(version);
        // if not found, refresh our map. The config object will get reloaded if it is older
        // then the TTL
        if (key == null)
            refresh(false);

        key = keyCache.get(version);

        // still null, force config reload from LDAP
        if (key == null)
            refresh(true);
        key = keyCache.get(version);

        // return it, even if null
        return key;
    }

    private static synchronized void refresh(boolean reload) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        // force reload
        if (reload)
            prov.reload(config);

        String key = config.getAttr(Provisioning.A_zimbraExternalAccountProvisioningKey);

        if (StringUtil.isNullOrEmpty(key)) {
            prov.reload(config);
            key = config.getAttr(Provisioning.A_zimbraExternalAccountProvisioningKey);
        }

        // bootstrap. automatically create new random key
        if (StringUtil.isNullOrEmpty(key)) {
            ExtAuthTokenKey extAuthkey = new ExtAuthTokenKey(0, null);
            HashMap<String, String> attrs = new HashMap<String, String>();
            attrs.put(Provisioning.A_zimbraExternalAccountProvisioningKey, extAuthkey.getEncoded());
            Provisioning.getInstance().modifyAttrs(config, attrs);
            key = config.getAttr(Provisioning.A_zimbraExternalAccountProvisioningKey);
        }

        ExtAuthTokenKey extAuthKey = keyCache.get(key);
        if (extAuthKey == null) {
            extAuthKey = new ExtAuthTokenKey(key);
            keyCache.put(key, extAuthKey);
            keyCache.put(Long.toString(extAuthKey.version), extAuthKey);
            if (latestExtAuthKey == null || latestExtAuthKey.version < latestExtAuthKey.version)
                latestExtAuthKey = extAuthKey;
        }
    }

    public static synchronized ExtAuthTokenKey getCurrentKey() throws ServiceException {
        if (latestExtAuthKey == null) {
            refresh(false);
        }
        return latestExtAuthKey;
    }

}
