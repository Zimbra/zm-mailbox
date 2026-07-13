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

import com.zimbra.cs.account.auth.AuthContext;
import java.util.Map;

public final class IRopcAuthRequest {

    private final String username;

    private final String password;

    private final String refreshToken;

    private final String deviceId;

    private final AuthContext.Protocol protocol;

    private final String clientIp;

    private final String userAgent;

    private final Map<String, String> config;

    private IRopcAuthRequest(Builder b) {
        this.username = b.username;
        this.password = b.password;
        this.refreshToken = b.refreshToken;
        this.deviceId = b.deviceId;
        this.protocol = b.protocol;
        this.clientIp = b.clientIp;
        this.userAgent = b.userAgent;
        this.config = b.config;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public AuthContext.Protocol getProtocol() {
        return protocol;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public boolean isRefresh() {
        return refreshToken != null && !refreshToken.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String username;

        private String password;

        private String refreshToken;

        private String deviceId;

        private AuthContext.Protocol protocol;

        private String clientIp;

        private String userAgent;

        private Map<String, String> config;

        public Builder username(String v) {
            this.username = v; return this;
        }

        public Builder password(String v) {
            this.password = v; return this;
        }

        public Builder refreshToken(String v) {
            this.refreshToken = v; return this;
        }

        public Builder deviceId(String v) {
            this.deviceId = v; return this;
        }

        public Builder protocol(AuthContext.Protocol v) {
            this.protocol = v; return this;
        }

        public Builder clientIp(String v) {
            this.clientIp = v; return this;
        }

        public Builder userAgent(String v) {
            this.userAgent = v; return this;
        }

        public Builder config(Map<String, String> v) {
            this.config = v; return this;
        }

        public IRopcAuthRequest build() {
            return new IRopcAuthRequest(this); }
    }
}
