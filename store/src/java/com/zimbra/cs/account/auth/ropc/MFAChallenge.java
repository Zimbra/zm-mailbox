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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MFAChallenge {

    public static final String REFRESH_TOKEN = "refresh_token";

    public static final String ACCESS_TOKEN = "access_token";

    public static final String ACCESS_EXPIRES_AT = "access_expires_at";

    private final String providerName;

    private final String dedupeKey;

    private final MFAFactorType factorType;

    private final ConcurrentHashMap<String, String> state;

    public MFAChallenge(String providerName, String dedupeKey, MFAFactorType factorType,
                        Map<String, String> initialState) {
        this.providerName = providerName;
        this.dedupeKey = dedupeKey;
        this.factorType = factorType;
        this.state = new ConcurrentHashMap<String, String>();
        if (initialState != null) {
            this.state.putAll(initialState);
        }
    }

    public String getProviderName() {
        return providerName;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public MFAFactorType getFactorType() {
        return factorType;
    }

    public String get(String key) {
        return state.get(key);
    }

    public void put(String key, String value) {
        if (key != null && value != null) {
            state.put(key, value);
        }
    }

    public Map<String, String> getState() {
        return Collections.unmodifiableMap(state);
    }
}

