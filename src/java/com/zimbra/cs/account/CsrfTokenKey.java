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




/**
 * @author zimbra
 *
 */
public class CsrfTokenKey {

    public static final int KEY_SIZE_BYTES = 32;
    private byte[] csrfTokenKey;
    private long keyVersion;
    private long keyCreatedAt;
    private static HashMap<String, CsrfTokenKey> tokenCache = new HashMap<String, CsrfTokenKey>();
    private static CsrfTokenKey latestCsrfKey;

    public byte[] getKey() {
        return csrfTokenKey;
    }

    public long getVersion() {
        return keyVersion;
    }

    public long getCreated() {
        return keyCreatedAt;
    }

    void setKey(byte[] key) {
        csrfTokenKey = key;
    }

    /**
     * @param version
     * @param key
     * @throws ServiceException
     */
    CsrfTokenKey(long version, byte[] key) throws ServiceException {
        keyVersion = version;
        keyCreatedAt = System.currentTimeMillis();
        if (key != null) {
            csrfTokenKey = key;
        } else {
            SecureRandom random = new SecureRandom();
            csrfTokenKey = new byte[KEY_SIZE_BYTES];
            random.nextBytes(csrfTokenKey);
        }
    }

    private CsrfTokenKey(String k) throws ServiceException {
        String parts[] = k.split(":");
        if (parts.length != 3)
            throw ServiceException.INVALID_REQUEST("invalid auth token key",
                null);
        String ver = parts[0];
        String created = parts[1];
        String data = parts[2];

        try {
            keyVersion = Long.parseLong(ver);
        } catch (NumberFormatException e) {
            throw ServiceException.INVALID_REQUEST(
                "invalid auth token key version", e);
        }

        try {
            keyCreatedAt = Long.parseLong(created);
        } catch (NumberFormatException e) {
            throw ServiceException.INVALID_REQUEST(
                "invalid auth token key created data", e);
        }

        try {
            csrfTokenKey = Hex.decodeHex(data.toCharArray());
        } catch (DecoderException e) {
            throw ServiceException.INVALID_REQUEST(
                "invalid auth token key data", e);
        }
    }

    private static synchronized void refresh(boolean reload)
        throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        // force reload
        if (reload)
            prov.reload(config);

        String key = config.getAttr(Provisioning.A_zimbraCsrfTokenKey);

        if (key == null) {
            prov.reload(config);
            key = config.getAttr(Provisioning.A_zimbraCsrfTokenKey);
        }

        // bootstrap. automatically create new random key
        if (key == null) {
            CsrfTokenKey csrfKey = new CsrfTokenKey(0, null);
            HashMap<String, String> attrs = new HashMap<String, String>();
            attrs.put(Provisioning.A_zimbraCsrfTokenKey, csrfKey.getEncoded());
            Provisioning.getInstance().modifyAttrs(config, attrs);
            key = config.getAttr(Provisioning.A_zimbraCsrfTokenKey);
        }

        CsrfTokenKey csrfKey = tokenCache.get(key);
        if (csrfKey == null) {
            csrfKey = new CsrfTokenKey(key);
            tokenCache.put(key, csrfKey);
            tokenCache.put(Long.toString(csrfKey.keyVersion), csrfKey);
            if (latestCsrfKey == null || latestCsrfKey.keyVersion < csrfKey.keyVersion)
                latestCsrfKey = csrfKey;
        }
    }

    public static synchronized CsrfTokenKey getCurrentKey()
        throws ServiceException {
        if (latestCsrfKey == null) {
            refresh(false);
        }
        return latestCsrfKey;
    }

    public String getEncoded() {
        return keyVersion + ":" + keyCreatedAt + ":"
            + new String(Hex.encodeHex(csrfTokenKey));
    }

    /**
     * given a particular version, return the
     *
     * @param version
     * @return
     * @throws ServiceException
     */
    public static CsrfTokenKey getVersion(String version)
        throws ServiceException {
        CsrfTokenKey key = tokenCache.get(version);
        // if not found, refresh our map. The config object will get reloaded if
        // it is older
        // then the TTL
        if (key == null)
            refresh(false);

        key = tokenCache.get(version);

        // still null, force config reload from LDAP
        if (key == null)
            refresh(true);
        key = tokenCache.get(version);

        // return it, even if null
        return key;
    }

}
