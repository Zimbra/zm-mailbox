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

package com.zimbra.cs.service;

import com.zimbra.common.localconfig.LC;
import java.net.URI;
import java.util.*;

public class WebClientLogoffUrlRegistry {

    // Static set of registered logoff paths (e.g., "/service/extension/samllogout")
    private static final Set<String> registeredLogoffPaths =
            Collections.synchronizedSet(new HashSet<>());

    /**
     * Extensions call this at init time to register their logout path.
     * @param path of extension's logout
     */
    public static void register(String path) {
        registeredLogoffPaths.add(path);
    }

    /**
     * Builds the combined logoff URL list:
     * 1. Starts with LC.zimbra_web_client_logoff_urls (space-delimited)
     * 2. Conditionally appends zimbraWebClientLogoutUrl if its path
     *    matches any entry in registeredLogoffPaths
     *
     * @param zimbraWebClientLogoutUrl the domain's configured logout URL
     * @return space-delimited string of all logoff URLs
     */
    public static String build(String zimbraWebClientLogoutUrl) {
        // Start with the localconfig value
        String lcUrls = LC.zimbra_web_client_logoff_urls.value();
        StringBuilder result = new StringBuilder();
        if (lcUrls != null && !lcUrls.trim().isEmpty()) {
            result.append(lcUrls.trim());
        }

        // If zimbraWebClientLogoutUrl's path matches a registered path, include it
        if (zimbraWebClientLogoutUrl != null && !zimbraWebClientLogoutUrl.trim().isEmpty()) {
            try {
                String path = new URI(zimbraWebClientLogoutUrl.trim()).getPath();
                if (registeredLogoffPaths.contains(path)) {
                    if (result.length() > 0) {
                        result.append(" ");
                    }
                    result.append(zimbraWebClientLogoutUrl.trim());
                }
            } catch (Exception e) {
                // Malformed URL — don't include
            }
        }

        return result.toString();
    }

    /**
     * Clears all registered paths. For unit testing only.
     */
    public static void clearForTest() {
        registeredLogoffPaths.clear();
    }
}
