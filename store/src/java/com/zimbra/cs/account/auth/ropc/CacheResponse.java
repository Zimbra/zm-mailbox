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
 * holds the result of a credential cache lookup.
 * contains flags for cache hit, rejection skip, and the auth type associated with the cached entry.
 */
public class CacheResponse {

    private boolean cacheHit;

    private String authType;

    private boolean rejectionSkip;

    public CacheResponse(boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    public CacheResponse(boolean cacheHit, String authType) {
        this.authType = authType;
        this.cacheHit = cacheHit;
    }

    public CacheResponse(boolean cacheHit, boolean rejectionSkip) {
        this.cacheHit = cacheHit;
        this.rejectionSkip = rejectionSkip;
    }

    public boolean isCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public boolean getRejectionSkip() {
        return rejectionSkip;
    }

    public void setRejectionSkip(boolean rejectionSkip) {
        this.rejectionSkip = rejectionSkip;
    }

}
