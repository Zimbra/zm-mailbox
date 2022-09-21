/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.util.yauth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;

/**
 * Implementation of Yahoo "Raw Auth" aka "Token Login v2"
 * See http://twiki.corp.yahoo.com/view/Membership/OpenTokenLogin
 */
public class RawAuth implements Auth {
    private final String appId;
    private String cookie;
    private String wssId;
    private long expiration;

    private static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(RawAuth.class);

    private static final boolean DEBUG = false;

    static {
        if (DEBUG) {
            Configurator.reconfigure();
            Configurator.setLevel(LOG.getName(), Level.DEBUG);
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
            System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
            System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");
            System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");
        }
    }


    private static final String GET_AUTH_TOKEN = "get_auth_token";
    private static final String GET_AUTH = "get_auth";

    // Auth request parameters
    private static final String LOGIN = "login";
    private static final String PASSWD = "passwd";
    private static final String APPID = "appid";
    private static final String TOKEN = "token";

    // Auth response fields
    private static final String AUTH_TOKEN = "AuthToken";
    private static final String COOKIE = "Cookie";
    private static final String WSSID = "WSSID";
    private static final String EXPIRATION = "Expiration";
    private static final String ERROR = "Error";
    private static final String ERROR_DESCRIPTION = "ErrorDescription";
    private static final String CAPTCHA_URL = "CaptchaUrl";
    private static final String CAPTCHA_DATA = "CaptchaData";

    // Maximum number of milliseconds between current time and expiration
    // time before cookie is considered no longer valid.
    private static final long EXPIRATION_LIMIT = 60 * 1000; // 1 minute

    public static String getToken(String appId, String user, String pass)
        throws AuthenticationException, IOException {
        debug("Sending getToken request: appId = %s, user = %s", appId, user);
        List<NameValuePair> nvp = new ArrayList<NameValuePair>();
        nvp.add(new BasicNameValuePair(APPID, appId));
        nvp.add(new BasicNameValuePair(LOGIN, user));
        nvp.add(new BasicNameValuePair(PASSWD, pass));
        Response res;
        try {
            res = doGet(GET_AUTH_TOKEN, nvp);
            String token = res.getRequiredField(AUTH_TOKEN);
            debug("Got getToken response: token = %s", token);
            return token;
        } catch (HttpException e) {
            throw new IOException("Unexpected error: ",e);
        }

    }

    public static RawAuth authenticate(String appId, String token)
        throws AuthenticationException, IOException {
        debug("Sending authenticate request: appId = %s, token = %s", appId, token);
        RawAuth auth = new RawAuth(appId);
        auth.authenticate(token);
        debug("Got authenticate response: %s", auth);
        return auth;
    }

    private RawAuth(String appId) {
        this.appId = appId;
    }

    @Override
    public String getAppId() {
        return appId;
    }

    @Override
    public String getCookie() {
        return cookie;
    }

    @Override
    public String getWSSID() {
        return wssId;
    }

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() + EXPIRATION_LIMIT > expiration;
    }

    private void authenticate(String token)
        throws AuthenticationException, IOException {
        List<NameValuePair> nvp = new ArrayList<NameValuePair>();
        nvp.add(new BasicNameValuePair(APPID, appId));
        nvp.add(new BasicNameValuePair(TOKEN, token));
        try {
            Response res = doGet(GET_AUTH, nvp);
            cookie = res.getRequiredField(COOKIE);
            wssId = res.getRequiredField(WSSID);
            String s = res.getRequiredField(EXPIRATION);
            try {
                expiration = System.currentTimeMillis() + Long.parseLong(s) * Constants.MILLIS_PER_SECOND;
            } catch (NumberFormatException e) {
                throw new IOException(
                    "Invalid integer value for field '" + EXPIRATION + "': " + s);
            }
        } catch (HttpException e) {
            throw new IOException("Unexpected error: ",e);
        }
    }

    private static Response doGet(String action, List<NameValuePair> paramsList)
        throws AuthenticationException, IOException, HttpException {
        String uri = LC.yauth_baseuri.value() + '/' + action;

        HttpGet httpget = new HttpGet(uri+"?"+ URLEncodedUtils.format(paramsList, "utf-8"));
        HttpResponse httpResp = HttpClientUtil.executeMethod(httpget);
        int rc = httpResp.getStatusLine().getStatusCode();

            Response res = new Response(httpResp);
            String error = res.getField(ERROR);
            // Request can sometimes fail even with a 200 status code, so always
            // check for "Error" attribute in response.
            if (rc == 200 && error == null) {
                return res;
            }
            if (rc == 999) {
                // Yahoo service temporarily unavailable (error code text not included)
                throw new AuthenticationException(
                    ErrorCode.TEMP_ERROR, "Unable to process request at this time");
            }
            ErrorCode code = error != null ?
                ErrorCode.get(error) : ErrorCode.GENERIC_ERROR;
            String description = res.getField(ERROR_DESCRIPTION);
            if (description == null) {
                description = code.getDescription();
            }
            AuthenticationException e = new AuthenticationException(code, description);
            e.setCaptchaUrl(res.getField(CAPTCHA_URL));
            e.setCaptchaData(res.getField(CAPTCHA_DATA));
            throw e;
    }

    private static class Response {
        final Map<String, String> attributes;

        Response(HttpResponse resp) throws IOException {
            debug("Response status: %s", resp.getStatusLine());
            attributes = new HashMap<String, String>();
            InputStream is = null;
            try {
                is = resp.getEntity().getContent();
	            BufferedReader br = new BufferedReader(
	                new InputStreamReader(is, resp.getEntity().getContentEncoding().getValue()));
	            String line;
	            while ((line = br.readLine()) != null) {
	                debug("Response line: %s", line);
	                int i = line.indexOf('=');
	                if (i != -1) {
	                    String name = line.substring(0, i);
	                    String value = line.substring(i + 1);
	                    attributes.put(name.toLowerCase(), value);
	                }
	            }
            } finally {
                ByteUtil.closeStream(is);
            }
        }

        String getRequiredField(String name) throws IOException {
            String value = getField(name);
            if (value == null) {
                throw new IOException("Response missing required '" + name + "' field");
            }
            return value;
        }

        String getField(String name) {
            String s = attributes.get(name.toLowerCase());
            if (s != null) {
                s = s.trim();
                if (s.length() > 0) {
                    return s;
                }
            }
            return null;
        }
    }

    @Override
    public String toString() {
        if (DEBUG) {
            return String.format("{appid=%s,cookie=%s,wssId=%s,expiration=%d}",
                                 appId, cookie, wssId, expiration);
        } else {
            return super.toString();
        }
    }

    private static void debug(String fmt, Object... args) {
        LOG.debug(String.format(fmt, args));
    }
}
