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

public final class IRopcAuthRequest {

    private final String username;

    private final String password;

    private final String provider;

    private final String protocolContext;

    private final FactorType factorType;

    private final String deviceId;

    public enum FactorType {
        NONE, PUSH;

        public static FactorType fromConfig(String value) {
            if (value == null || value.trim().isEmpty()) {
                return NONE;
            }
            String normalized = value.trim().toUpperCase();
            try {
                return FactorType.valueOf(normalized);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unsupported factor type: " + value);
            }
        }
    }

    private IRopcAuthRequest(Builder builder) {
        this.username = builder.username;
        this.password = builder.password;
        this.provider = builder.provider;
        this.protocolContext = builder.protocolContext;
        this.factorType = builder.factorType;
        this.deviceId = builder.deviceId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getProvider() {
        return provider;
    }

    public String getProtocolContext() {
        return protocolContext;
    }

    public FactorType getFactorType() {
        return factorType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public static class Builder {

        private String username;

        private String password;

        private String provider;

        private String protocolContext;

        private FactorType factorType = FactorType.NONE;

        private String deviceId;

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder protocolContext(String protocolContext) {
            this.protocolContext = protocolContext;
            return this;
        }

        public Builder factorType(FactorType factorType) {
            this.factorType = factorType;
            return this;
        }

        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public IRopcAuthRequest build() {
            if (username == null || username.trim().isEmpty() || !username.contains("@")) {
                throw new IllegalArgumentException("username must be a valid email address");
            }
            if (password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("password must not be null or empty");
            }
            return new IRopcAuthRequest(this);
        }
    }
}
