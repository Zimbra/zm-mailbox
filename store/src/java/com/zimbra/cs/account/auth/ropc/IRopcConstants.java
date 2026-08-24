/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.account.auth.ropc;

/**
 * Constants used across the IdP ROPC authentication flow.
 */
public final class IRopcConstants {
    public static final String PROVIDER = "provider";

    public static final String GRANT_PASSWORD = "password";

    public static final String GRANT_REFRESH = "refresh_token";

    public static final String GRANT_OOB = "urn:okta:params:oauth:grant-type:oob";

    public static final String FACTOR = "factor";

    public static final String REFRESH = "REFRESH";

    public static final String PROVIDER_NAME_OKTA = "okta";

    public static final String FACTOR_NONE = "NONE";

    public static final String FACTOR_PUSH = "PUSH";

    public static final String REQUEST_PARAM_GRANT_TYPE = "grant_type";

    public static final String REQUEST_PARAM_RFRESH_TOKEN = "refresh_token";

    public static final String REQUEST_PARAM_USERNAME = "username";

    public static final String REQUEST_PARAM_PASSWORD = "password";

    public static final String REQUEST_PARAM_MFA_TOKEN = "mfa_token";

    public static final String REQUEST_PARAM_CHANNEL_HINT = "channel_hint";

    public static final String REQUEST_PARAM_CHALLENGE_TYPES_SUPPORTED = "challenge_types_supported";

    public static final String CHALLENGE_TYPES_SUPPORTED_FOR_PUSH_REQ = "http://auth0.com/oauth/grant-type/mfa-oob";

    public static final String GRANT_TYPE_FOR_POLL = "http://auth0.com/oauth/grant-type/mfa-oob";

    public static final String REQUEST_PARAM_OOB_CODE = "oob_code";

    public static final String REQUEST_PARAM_SCOPE = "scope";

    public static final String TOKEN_ENDPOINT = "token_endpoint";

    public static final String REQUEST_PARAM_CLIENT_ID = "client_id";

    public static final String REQUEST_PARAM_CLIENT_SECRET = "client_secret";

    public static final String REQUEST_PARAM_CLIENT_SECRET_AUTH_TYPE = "client_secret_auth_type";

    public static final String REQUEST_PARAM_CONNECTION_TIMEOUT = "http_connect_ms";

    public static final String REQUEST_PARAM_SOCKET_TIMEOUT = "http_socket_ms";

    public static final int CONNECTION_TIMEOUT_DEFAULT = 5000;

    public static final int SOCKET_TIMEOUT_DEFAULT = 1000;

    public static final String ACCEPT = "Accept";

    public static final String APPLICATION_JSON = "application/json";

    public static final String CLIENT_SECRET_AUTH_TYPE_BASIC = "BASIC";

    public static final String AUTHORIZATION = "Authorization";

    public static final String BASIC_HEADER = "Basic ";

    public static final String AUTHORIZATION_PENDING = "authorization_pending";

    public static final String SLOW_DOWN = "slow_down";

    public static final String EXPIRED_TOKEN = "expired_token";

    public static final String INVALID_GRANT = "invalid_grant";

    public static final String INVALID_REQUEST = "invalid_request";

    public static final String AUTHORIZATION_ERROR_MSG = "another authorization server";

    public static final String ACCESS_DENIED = "access_denied";

    public static final String OKTA_REQUEST_TYPE_TOKEN = "token";

    public static final String ERROR_DESCRIPTION_SIGN_ON_POLICY = "sign on policy";

    public static final String OKTA_REQUEST_TYPE_CHALLENGE = "challenge";

    public static final String TOKEN_ENPOINT_CORE = "/token";

    public static final String CHALLENGE_ENPOINT_CORE = "/challenge";

    public static final String HTTP_APPEND = "http_";

    public static final String MFA_REQUIRED = "mfa_required";

    public static final String PUSH = "push";

    public static final String INTERVAL = "interval";

    public static final String POLLING_TIMEOUT = "polling_timeout";

    public static final long POLLING_TIMEOUT_DEFAULT = 30 * 1000L;

    public static final String POLLING_INTERVAL = "polling_interval";

    public static final long POLLING_INTERVAL_DEFAULT = 5 * 1000L;

    public static final String SCOPE_DEFAULT = "openid offline_access";

    public static final long ACCESS_EXPIRY_DEFAULT = 3600;

    public static final int MAX_LOG_LENGTH = 500;

    public static final long CONVERT_TO_MILLI = 1000L;

    public static final long TTL_MS = 3600000L;

    public static final String IP = "ip";

    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    public static final String HEADER_AUTH0_FORWARDED_FOR = "auth0-forwarded-for";

    public static final int HARD_EXPIRY_DEFAULT = 60;

    public static final String EAS = "eas";

    public static final String OUTLOOK = "outlook";

    public static final String FULL_AUTH = "full_auth";

    public static final String HEADER_USER_AGENT = "User-Agent";

    public static final String HEADER_X_DEVICE_FINGERPRINT = "X-Device-Fingerprint";

    public static final String USER_AGENT = "UserAgent";

    public static final String DEVICE_ID = "deviceID";

    public static final long CACHE_GRACE_PERIOD_MIN_DURATION = 30;

    public static final long CACHE_EXPIRY_MIN_DURATION = 30;

    public static final String AUTH_REQUEST_TYPE = "OPTIONS";




}
