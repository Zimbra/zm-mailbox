/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.oauth;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.account.oauth.utils.OAuthServiceProvider;

/**
 */
public class OAuthAccessorSerializer implements MemcachedSerializer<OAuthAccessor> {

    private static final Log LOG = ZimbraLog.oauth;

    @Override
    public Object serialize(OAuthAccessor value) {
        String consumer_key = (String) value.consumer.getProperty("key");
        String approved_on = (String) value.consumer.getProperty("approved_on");
        String device = (String) value.consumer.getProperty("device");
        String token_secret = value.tokenSecret;
        String callback = (String) value.getProperty(OAuth.OAUTH_CALLBACK);
        String user = (String) value.getProperty("user");
        String authorized;
        if (value.getProperty("authorized") != null) {
            authorized = value.getProperty("authorized").toString();
        } else {
            authorized = null;
        }
        String zauthtoken = (String) value.getProperty("ZM_AUTH_TOKEN");
        String verifier = (String) value.getProperty(OAuth.OAUTH_VERIFIER);

        String result = "consumer_key:" + consumer_key + ",token_secret:" + token_secret + //
                ",callback:" + callback + ",user:" + user + ",authorized:" + authorized + //
                ",zauthtoken:" + zauthtoken + ",verifier:" + verifier +",approved_on:" + approved_on +",device="+device;
        //return value.encode().toString();

        LOG.debug("put value: " + result + " into memcache.");

        return result;
    }

    @Override
    public OAuthAccessor deserialize(Object obj) throws ServiceException {

        String value = (String) obj;
        LOG.debug("get value: " + value);
        String consumer_key = value.substring(0, value.indexOf(",token_secret")).substring(13);
        String token_secret = value.substring(value.indexOf(",token_secret"), value.indexOf(",callback")).substring(14);
        String callback = value.substring(value.indexOf(",callback"), value.indexOf(",user")).substring(10);
        String user = value.substring(value.indexOf(",user"), value.indexOf(",authorized")).substring(6);
        String authorized = value.substring(value.indexOf(",authorized"), value.indexOf(",zauthtoken")).substring(12);
        String zauthtoken = value.substring(value.indexOf(",zauthtoken"), value.indexOf(",verifier")).substring(12);
        String verifier = value.substring(value.indexOf(",verifier"), value.indexOf(",approved_on")).substring(10);
        String approved_on = value.substring(value.indexOf(",approved_on"), value.indexOf(",device")).substring(13);
        String device = value.substring(value.indexOf(",device")).substring(8);

        LOG.debug("[consumer_key:%s, callback:%s, user:%s, authorized:%s, zauthtoken:%s, verifier:%s, approved_on:%s, device:%s]",
                consumer_key, callback, user, authorized, zauthtoken, verifier, approved_on, device);

        try {
            OAuthConsumer consumer = OAuthServiceProvider.getConsumer(consumer_key);
            OAuthAccessor accessor = new OAuthAccessor(consumer);
            accessor.tokenSecret = token_secret;
            accessor.setProperty(OAuth.OAUTH_CALLBACK, callback);

            if (!user.equals("null")) {
                accessor.setProperty("user", user);
            }

            if (authorized.equalsIgnoreCase(Boolean.FALSE.toString())) {
                accessor.setProperty("authorized", Boolean.FALSE);
            } else if (authorized.equalsIgnoreCase(Boolean.TRUE.toString())) {
                accessor.setProperty("authorized", Boolean.TRUE);
            }

            if (!zauthtoken.equals("null")) {
                accessor.setProperty("ZM_AUTH_TOKEN", zauthtoken);
                AuthToken zimbraAuthToken = ZimbraAuthToken.getAuthToken(zauthtoken);
                final Account account = zimbraAuthToken.getAccount();
                OAuthServiceProvider.setAccountPropertiesForAccessor(account, accessor);
            }

            if (!verifier.equals("null")) {
                accessor.setProperty(OAuth.OAUTH_VERIFIER, verifier);
            }

            if (null != approved_on) {
                accessor.consumer.setProperty("approved_on", approved_on);
            }

            if (null != device) {
                accessor.consumer.setProperty("device", device);
            }
            return accessor;
        } catch (Exception e) {
            //need more hack here for hadnling IOException properly
            throw ServiceException.FAILURE("IOException", e);
        }
    }
}
