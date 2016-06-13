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

package com.zimbra.cs.account.oauth.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.server.OAuthServlet;

import org.apache.commons.codec.digest.DigestUtils;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.account.oauth.OAuthTokenCache;

/**
 * Utility methods for providers that store consumers, tokens and secrets in
 * local cache (HashSet). Consumer key is used as the name, and its credentials are
 * stored in HashSet.
 *
 * OAuth 1.0A specification is followed.
 * @author Yutaka Obuchi
 */

public class OAuthServiceProvider {

    public static final OAuthRevAValidator VALIDATOR = new OAuthRevAValidator();

    public static synchronized OAuthConsumer getConsumer(OAuthMessage requestMessage)
            throws IOException, OAuthProblemException {
        return getConsumer(requestMessage.getConsumerKey());
    }

    public static synchronized OAuthConsumer getConsumer(String consumer_key)
            throws IOException, OAuthProblemException {
        String[] registeredConsumers;
        try {
            // TODO - need to lookup the domain first
            registeredConsumers = Provisioning.getInstance().getConfig().getMultiAttr(Provisioning.A_zimbraOAuthConsumerCredentials);
        } catch (ServiceException e) {
            throw new OAuthProblemException("token_rejected");
        }

        OAuthConsumer oAuthConsumer = null;
        for (String consumer : registeredConsumers) {
            String s[] = consumer.split(":");
            if (s.length >= 2 && s[0].equals(consumer_key)) {
                oAuthConsumer = new OAuthConsumer(null, consumer_key, s[1], null);
                oAuthConsumer.setProperty("key", consumer_key);
                oAuthConsumer.setProperty("app_name", s.length > 2 ? s[2] : "");
                break;
            }
        }

        if (oAuthConsumer == null) {
            throw new OAuthProblemException("token_rejected");
        }

        return oAuthConsumer;
    }

    /**
     * Get the access token and token secret for the given oauth_token.
     */
    public static synchronized OAuthAccessor getAccessor(OAuthMessage requestMessage)
            throws IOException, OAuthProblemException,ServiceException {

        // try to load from memcache if not throw exception
        String consumer_token = requestMessage.getToken();
        OAuthAccessor accessor = null;

        accessor = OAuthTokenCache.get(consumer_token, OAuthTokenCache.REQUEST_TOKEN_TYPE);
        if (accessor == null){
            accessor = OAuthTokenCache.get(consumer_token, OAuthTokenCache.ACCESS_TOKEN_TYPE);
        }

        if(accessor == null){
            OAuthProblemException problem = new OAuthProblemException("token_expired");
            throw problem;
        }
        return accessor;
    }

    public static synchronized void setAccountPropertiesForAccessor(Account account, OAuthAccessor accessor) throws UnsupportedEncodingException {
        accessor.setProperty("ZM_ACC_DISPLAYNAME", account.getAttr(Provisioning.A_displayName) == null ? "" : URLEncoder.encode(account.getAttr(Provisioning.A_displayName), "UTF-8"));
        accessor.setProperty("ZM_ACC_CN", account.getName()==null ? "" : URLEncoder.encode(account.getName(), "UTF-8"));
        accessor.setProperty("ZM_ACC_GIVENNAME", account.getAttr(Provisioning.A_givenName) == null ? "" : URLEncoder.encode(account.getAttr(Provisioning.A_givenName), "UTF-8"));
        accessor.setProperty("ZM_ACC_SN", account.getAttr(Provisioning.A_sn) == null ? "" : URLEncoder.encode(account.getAttr(Provisioning.A_sn), "UTF-8"));
        accessor.setProperty("ZM_ACC_EMAIL", account.getMail() == null ? "" :  URLEncoder.encode(account.getMail(), "UTF-8"));
    }

    /**
     * Mark OAuth consumer as authorized and update accessor properties.
     */
    public static synchronized void markAsAuthorized(OAuthAccessor accessor,
            String userId, String zauthtoken) throws OAuthException {
        accessor.setProperty("user", userId);
        accessor.setProperty("authorized", Boolean.TRUE);
        accessor.setProperty("ZM_AUTH_TOKEN", zauthtoken);
        AuthToken zimbraAuthToken;
        try {
            zimbraAuthToken = ZimbraAuthToken.getAuthToken(zauthtoken);
            final Account account = zimbraAuthToken.getAccount();
            setAccountPropertiesForAccessor(account, accessor);
        } catch (AuthTokenException | UnsupportedEncodingException | ServiceException e) {
            throw new OAuthException(e);
        }
        accessor.consumer.setProperty("approved_on", Long.toString(System.currentTimeMillis()));
    }

    /**
     * Generate request token and secret for a consumer.
     *
     * @throws OAuthException
     */
    public static synchronized void generateRequestToken(
            OAuthAccessor accessor)
                    throws OAuthException,ServiceException {

        String consumer_key = (String) accessor.consumer.getProperty("key");

        String token_data = consumer_key + System.nanoTime();
        String token = DigestUtils.sha256Hex(token_data);

        String secret_data = consumer_key + System.nanoTime() + token;
        String secret = DigestUtils.sha256Hex(secret_data);

        accessor.requestToken = token;
        accessor.tokenSecret = secret;
        accessor.accessToken = null;

        // add to Memcache
        OAuthTokenCache.put(accessor,OAuthTokenCache.REQUEST_TOKEN_TYPE);
    }

    /**
     * Generate a access token for OAuthConsumer.
     *
     * @throws OAuthException
     */
    public static synchronized void generateAccessToken(OAuthAccessor accessor)
            throws OAuthException,ServiceException {

        String consumer_key = (String) accessor.consumer.getProperty("key");

        String token_data = consumer_key + System.nanoTime();
        String token = DigestUtils.sha256Hex(token_data);

        accessor.accessToken = token;
    }

    public static void handleException(Exception e, HttpServletRequest request,
            HttpServletResponse response, boolean sendBody)
                    throws IOException, ServletException {
        String realm = (request.isSecure()) ? "https://" : "http://";
        realm += request.getLocalName();
        OAuthServlet.handleException(response, e, realm, sendBody);
    }

    /**
     * Generate a verifier.
     *
     * @throws OAuthException
     */
    public static synchronized void generateVerifier(
            OAuthAccessor accessor)
                    throws OAuthException,ServiceException {

        String consumer_key = (String) accessor.consumer.getProperty("key");

        String verifier_data = consumer_key + System.nanoTime() + accessor.requestToken;
        String verifier = DigestUtils.sha256Hex(verifier_data);

        ZimbraLog.oauth.debug("generated verifier:" + verifier);
        accessor.setProperty(OAuth.OAUTH_VERIFIER, verifier);

        // add to memcache
        OAuthTokenCache.put(accessor, OAuthTokenCache.REQUEST_TOKEN_TYPE);
    }
}

