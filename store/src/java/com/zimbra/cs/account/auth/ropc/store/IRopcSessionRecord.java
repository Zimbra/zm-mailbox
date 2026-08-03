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

package com.zimbra.cs.account.auth.ropc.store;

import com.zimbra.common.localconfig.LC;
import java.util.concurrent.TimeUnit;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.HARD_EXPIRY_DEFAULT;

/**
 * In-memory representation of a stored IdP token record.
 */
public final class IRopcSessionRecord {

    private Long id;

    private String username;

    private String deviceId;

    private String userAgent;

    private String ip;

    private String provider;

    private String refreshToken;

    private String idToken;

    private String passwordHash;

    private String protocol;

    private Long createdAt;

    private Long lastUpdatedAt;

    private IRopcSessionRecord(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.deviceId = builder.deviceId;
        this.userAgent = builder.userAgent;
        this.ip = builder.ip;
        this.provider = builder.provider;
        this.refreshToken = builder.refreshToken;
        this.idToken = builder.idToken;
        this.passwordHash = builder.passwordHash;
        this.protocol = builder.protocol;
        this.createdAt = builder.createdAt;
        this.lastUpdatedAt = builder.lastUpdatedAt;

    }

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        this.id = v;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long v) {
        this.createdAt = v;
    }

    public Long getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Long v) {
        this.lastUpdatedAt = v;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String v) {
        this.provider = v;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String v) {
        this.refreshToken = v;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String v) {
        this.idToken = v;
    }

    public String getPasswordHash() {
        return passwordHash;

    }

    public void setPasswordHash(String v) {
        this.passwordHash = v;
    }

    public boolean isHardSessionExpired() {
        long maxAgeMillis = TimeUnit.DAYS.toMillis(LC.mfa_idp_hard_reauth_in_days.intValue());
        if (maxAgeMillis <= 0) {
            maxAgeMillis = TimeUnit.DAYS.toMillis(HARD_EXPIRY_DEFAULT);
        }
        return System.currentTimeMillis() >= (this.createdAt + maxAgeMillis);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public static class Builder {
        private Long id;

        private String username;

        private String deviceId;

        private String userAgent;

        private String ip;

        private String provider;

        private String refreshToken;

        private String idToken;

        private String passwordHash;

        private String protocol;

        private Long createdAt;

        private Long lastUpdatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder ip(String ip) {
            this.ip = ip;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder idToken(String idToken) {
            this.idToken = idToken;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder createdAt(Long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastUpdatedAt(Long lastUpdatedAt) {
            this.lastUpdatedAt = lastUpdatedAt;
            return this;
        }

        public IRopcSessionRecord build() {
            if (this.createdAt == null) {
                this.createdAt = System.currentTimeMillis();
            }
            if (this.lastUpdatedAt == null) {
                this.lastUpdatedAt = System.currentTimeMillis();
            }

            return new IRopcSessionRecord(this);
        }
    }
}
