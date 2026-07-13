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

public final class IRopcAuthResult {

    public enum Status {
        SUCCESS,
        MFA_CHALLENGE,
        INVALID_CREDENTIALS,
        POLICY_DENIED,
        ERROR
    }

    private final Status status;

    private final MFAChallenge challenge;

    private final String refreshToken;

    private final String accessToken;

    private final long accessTokenExpiresAtMillis;

    private final String errorCode;

    private final String errorDescription;

    private IRopcAuthResult(Status status, MFAChallenge challenge, String refreshToken,
                            String accessToken, long accessTokenExpiresAtMillis, String errorCode,
                            String errorDescription) {
        this.status = status;
        this.challenge = challenge;
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.accessTokenExpiresAtMillis = accessTokenExpiresAtMillis;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public Status getStatus() {
        return status;
    }

    public MFAChallenge getChallenge() {
        return challenge;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getAccessTokenExpiresAtMillis() {
        return accessTokenExpiresAtMillis;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public static IRopcAuthResult success(String refreshToken, String accessToken, long expiresAtMillis) {
        return new IRopcAuthResult(Status.SUCCESS, null, refreshToken, accessToken, expiresAtMillis, null, null);
    }

    public static IRopcAuthResult challenge(MFAChallenge c) {
        return new IRopcAuthResult(Status.MFA_CHALLENGE, c, null, null, 0L, null, null);
    }

    public static IRopcAuthResult invalidCredentials(String code, String desc) {
        return new IRopcAuthResult(Status.INVALID_CREDENTIALS, null, null, null, 0L, code, desc);
    }

    public static IRopcAuthResult policyDenied(String desc) {
        return new IRopcAuthResult(Status.POLICY_DENIED, null, null, null, 0L, "policy_denied", desc);
    }

    public static IRopcAuthResult error(String code, String desc) {
        return new IRopcAuthResult(Status.ERROR, null, null, null, 0L, code, desc);
    }
}
