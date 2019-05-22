package com.zimbra.cs.html.owasp.policies;

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

import java.util.List;

import org.owasp.html.ElementPolicy;

import com.zimbra.cs.html.owasp.OwaspHtmlSanitizer;

public class BaseElementPolicy implements ElementPolicy {

    @Override
    public String apply(String elementName, List<String> attrs) {

        final int hrefIndex = attrs.indexOf("href");
        if (hrefIndex != -1) {
            String hrefValue = attrs.get(hrefIndex + 1);
            OwaspHtmlSanitizer.zThreadLocal.set(hrefValue);
        }
        return "base";
    }

}
