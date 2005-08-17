/*
 * Created on Jan 10, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import com.zimbra.cs.service.ServiceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author schemers
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AuthTokenKey {
    
    public static final int KEY_SIZE_BYTES = 32;
    private byte[] mKey;
    private long mVersion;
    private long mCreated;
    private static HashMap mCache = new HashMap();
    private static AuthTokenKey sLatestKey;

    private static Log mLog = LogFactory.getLog(AuthTokenKey.class); 
    
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
        try {
            mVersion = version;
            mCreated = System.currentTimeMillis();
            if (key != null) { 
                mKey = key;
            } else {
                SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
                mKey = new byte[KEY_SIZE_BYTES];
                random.nextBytes(mKey);
            }
        } catch (NoSuchAlgorithmException e) {
            throw ServiceException.FAILURE("unable to initialize SecureRandom", e);
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
    static AuthTokenKey getVersion(String version) throws ServiceException {
        AuthTokenKey key = (AuthTokenKey) mCache.get(version);
        // if not found, refresh our map. The config object will get reloaded if it is older
        // then the TTL
        if (key == null) 
            refresh(false);
        
        key = (AuthTokenKey) mCache.get(version);

        // still null, force config reload from LDAP
        if (key == null) 
            refresh(true);
        key = (AuthTokenKey) mCache.get(version);
        
        // return it, even if null
        return key;
    }

    private static synchronized void refresh(boolean reload) throws ServiceException {
        Config config = Provisioning.getInstance().getConfig();
        // force reload
        if (reload)
            config.reload();
        
        String[] keys = config.getMultiAttr(Provisioning.A_liquidAuthTokenKey);

        if (keys.length == 0) {
            config.reload();
            keys = config.getMultiAttr(Provisioning.A_liquidAuthTokenKey);
        }

        // bootstrap. automatically create new random key
        if (keys.length == 0) {
            AuthTokenKey key = new AuthTokenKey(0, null);
            HashMap attrs = new HashMap();
            attrs.put(Provisioning.A_liquidAuthTokenKey, key.getEncoded());
            config.modifyAttrs(attrs);
            keys = config.getMultiAttr(Provisioning.A_liquidAuthTokenKey);
        }

        for (int i=0; i < keys.length; i++) {
            AuthTokenKey key = (AuthTokenKey) mCache.get(keys[i]);
            if (key == null) {
                key = new AuthTokenKey(keys[i]);
                mCache.put(keys[i], key);
                mCache.put(Long.toString(key.mVersion), key);
                if (sLatestKey == null || sLatestKey.mVersion < key.mVersion)
                    sLatestKey = key;
            }
        }        
    }

    static synchronized AuthTokenKey getCurrentKey() throws ServiceException {
        if (sLatestKey == null) {
            refresh(false);
        }
        return sLatestKey;
    }
}
