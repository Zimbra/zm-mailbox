/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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
package com.zimbra.cs.html.owasp.policies;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.owasp.html.AttributePolicy;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

/** ZBUG-4108
 */
public class AltAttributePolicy implements AttributePolicy {
    private static final Pattern ON_HANDLER_RE = Pattern.compile("(?i)\\bon[a-z]+\\s*[=&/]");

    private boolean stripperEnabled = LC.zimbra_owasp_strip_alt_tags_with_handlers.booleanValue();

    @Override
    public String apply(String elementName, String attributeName, String value) {
        ZimbraLog.filter.info(String.format("defang <%s %s=«%s» />", elementName, attributeName, value));
        if (value == null) {
            return null;
        }

        if (!stripperEnabled) {
            return value;
        }

        final Matcher matcher = ON_HANDLER_RE.matcher(value);
        if (matcher.find()) {
            ZimbraLog.filter.debug(String.format("defang <%s %s/> attr «%s»", elementName, attributeName, value));
            return null;
        }

        return value;
    }
}
