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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zimbra.common.service.ServiceException;
import java.io.IOException;

/**
 * JSON helpers around a single shared, thread-safe {@link ObjectMapper}. Model classes stay
 * logic-free POJOs; all read/write goes through here.
 */
public final class JsonUtilities {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonUtilities() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static <T> T read(byte[] json, Class<T> type) throws ServiceException {
        if (json == null || json.length == 0) {
            throw ServiceException.PARSE_ERROR("empty JSON body", null);
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw ServiceException.PARSE_ERROR("failed to parse JSON as " + type.getSimpleName(), e);
        }
    }

    public static String write(Object value) throws ServiceException {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (IOException e) {
            throw ServiceException.FAILURE("failed to serialize JSON", e);
        }
    }
}

