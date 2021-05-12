/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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

import java.util.List;

import org.owasp.html.ElementPolicy;

import com.zimbra.common.util.ZimbraLog;

public class BackgroundPolicy implements ElementPolicy {

    @Override
    public String apply(String elementName, List<String> attrs) {
        final int bgIndex = attrs.indexOf("background");
        if (bgIndex == -1) {
            return elementName;
        }
        if (bgIndex%2 != 0) {
            ZimbraLog.mailbox.debug("Keyword 'background' found as attribute value instead of attribute name, so ignoring.");
            return elementName;
        }

        String bgValue = attrs.get(bgIndex + 1);
        attrs.remove(bgIndex);
        attrs.remove(bgIndex);// value
        attrs.add("dfbackground");
        attrs.add(bgValue);
        return elementName;
    }
}