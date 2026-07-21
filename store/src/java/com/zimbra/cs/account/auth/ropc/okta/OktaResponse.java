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

package com.zimbra.cs.account.auth.ropc.okta;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class OktaResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private String expiresIn;

    @JsonProperty("mfa_token")
    private String mfaToken;

    @JsonProperty("oob_code")
    private String oobCode;

    @JsonProperty("error")
    private String error;

    @JsonProperty("interval")
    private String interval;

    @JsonProperty("error_description")
    private String errorDescription;

    private int httpStatusCode;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String v) {
        this.accessToken = v;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String v) {
        this.refreshToken = v;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String v) {
        this.tokenType = v;
    }

    public String getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(String v) {
        this.expiresIn = v;
    }

    public String getMfaToken() {
        return mfaToken;
    }

    public void setMfaToken(String v) {
        this.mfaToken = v;
    }

    public String getOobCode() {
        return oobCode;
    }

    public void setOobCode(String v) {
        this.oobCode = v;
    }

    public String getError() {
        return error;
    }

    public void setError(String v) {
        this.error = v;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String v) {
        this.errorDescription = v;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(int v) {
        this.httpStatusCode = v;
    }

    public boolean hasAccessToken() {
        return accessToken != null && !accessToken.isEmpty();
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }
}
