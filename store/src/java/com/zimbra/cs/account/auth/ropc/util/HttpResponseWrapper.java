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

package com.zimbra.cs.account.auth.ropc.util;

import java.nio.charset.StandardCharsets;

/**
 * Immutable holder for an HTTP response after the connection has been released. The full body is
 * read into a {@code byte[]} before closing so callers never touch a live stream (zm-oauth-social
 * pattern).
 */
public final class HttpResponseWrapper {

    private final int statusCode;

    private final byte[] body;

    public HttpResponseWrapper(int statusCode, byte[] body) {
        this.statusCode = statusCode;
        this.body = (body == null) ? new byte[0] : body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public byte[] getBody() {
        return body;
    }

    public String getBodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }

    public boolean is2xx() {
        return statusCode >= 200 && statusCode < 300;
    }
}

