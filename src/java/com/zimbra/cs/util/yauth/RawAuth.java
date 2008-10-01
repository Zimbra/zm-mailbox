/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util.yauth;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

/**
 * Implementation of Yahoo "Raw Auth" aka "Token Login v2"
 * See http://twiki.corp.yahoo.com/view/Membership/OpenTokenLogin
 */
public class RawAuth implements Auth {
    private final String appId;
    private String cookie;
    private String wssId;
    private long expiration;

    private static final Logger LOG = Logger.getLogger(RawAuth.class);

    private static final boolean DEBUG = false;

    private static final String BASE_URI = "https://login.yahoo.com/WSLogin/V1";
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
        throws IOException {
        debug("Sending getToken request: appId = %s, user = %s", appId, user);
        Response res = doGet(GET_AUTH_TOKEN,
            new NameValuePair(APPID, appId),
            new NameValuePair(LOGIN, user),
            new NameValuePair(PASSWD, pass));
        String token = res.getRequiredField(AUTH_TOKEN);
        debug("Got getToken response: token = %s", token);
        return token;
    }

    public static RawAuth authenticate(String appId, String token)
        throws IOException {
        debug("Sending authenticate request: appId = %s, token = %s", appId, token);
        RawAuth auth = new RawAuth(appId);
        auth.authenticate(token);
        debug("Got authenticate response: %s", auth);
        return auth;
    }

    private RawAuth(String appId) {
        this.appId = appId;
    }
    
    public String getAppId() {
        return appId;
    }

    public String getCookie() {
        return cookie;
    }
    
    public String getWSSID() {
        return wssId;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() + EXPIRATION_LIMIT > expiration;
    }
    
    private void authenticate(String token)
        throws IOException {
        Response res = doGet(GET_AUTH, new NameValuePair(APPID, appId),
                                       new NameValuePair(TOKEN, token));
        cookie = res.getRequiredField(COOKIE);
        wssId = res.getRequiredField(WSSID);
        String s = res.getRequiredField(EXPIRATION);
        try {
            expiration = System.currentTimeMillis() + Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new IOException(
                "Invalid integer value for field '" + EXPIRATION + "': " + s);
        }
    }

    private static Response doGet(String action, NameValuePair... params)
        throws IOException {
        String uri = BASE_URI + '/' + action;
        GetMethod method = new GetMethod(uri);
        method.setQueryString(params);
        int rc = new HttpClient().executeMethod(method);
        Response res = new Response(method);
        String error = res.getField(ERROR);
        // Request can sometimes fail even with a 200 status code, so always
        // check for "Error" attribute in response.
        if (rc == 200 && error == null) {
            return res;
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
        return res;
    }

    private static class Response {
        final Map<String, String> attributes;

        Response(GetMethod method) throws IOException {
            attributes = new HashMap<String, String>();
            InputStream is = method.getResponseBodyAsStream();
            BufferedReader br = new BufferedReader(
                new InputStreamReader(is, method.getResponseCharSet()));
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
            return s != null && s.length() > 0 ? s.trim() : null;
        }
    }

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
