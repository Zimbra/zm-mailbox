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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.CONVERT_TO_MILLI;

public class IRopcUtil {

    public static Optional<Long> parseToMillis(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .flatMap(s -> {
                    try {
                        return Optional.of(Long.parseLong(s) * CONVERT_TO_MILLI);
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                })
                .filter(millis -> millis > 0);
    }

    public static Map<String, String> extractConfigsFromArgs(List<String> args) throws ServiceException {
        Map<String, String> configMap = new HashMap<>();

        try {
            if (args == null || args.isEmpty()) {
                return configMap;
            }

            for (String arg : args) {
                if (arg == null) {
                    continue;
                }
                String[] parts = arg.split("=", 2);

                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    if (key.isEmpty()) {
                        ZimbraLog.account.debug("Skipping configuration arguments with missing keys");
                        continue;
                    }
                    configMap.put(key, value);
                } else {
                    ZimbraLog.account.debug("Invalid Configuration arguments format. Expected 'key=value'");
                }
            }
            return configMap;
        } catch (Exception e) {
            ZimbraLog.account.error("Authentication failed while extracting args.", e);
            throw ServiceException.FAILURE("Authentication failed while extracting args .", null);
        }
    }
}
