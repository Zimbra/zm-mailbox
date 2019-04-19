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

public class DivElementPolicy implements ElementPolicy {

    @Override
    public String apply(String elementName, List<String> attrs) {
        boolean showHideQuotedText = false;
        // check if the class attribute is listed
        final int classIndex = attrs.indexOf("class");
        if (classIndex >= 0) {
            // remove the class attribute and its value
            final String value = attrs.remove(classIndex + 1);
            attrs.remove(classIndex);
            // gmail and yahoo use a specific div class name to indicate
            // quoted text
            showHideQuotedText = "gmail_quote".equals(value) || "yahoo_quoted".equals(value);
        }
        // check if the id attribute is listed
        final int idIndex = attrs.indexOf("id");
        if (idIndex >= 0) {
            // remove the id attribute and its value
            final String value = attrs.remove(idIndex + 1);
            attrs.remove(idIndex);
            // AOL uses a specific id value to indicate quoted text
            showHideQuotedText = value.startsWith("AOLMsgPart");
        }
        // insert a class attribute with a value of "elided-text" to
        // hide/show quoted text
        if (showHideQuotedText) {
            attrs.add("class");
            attrs.add("elided-text");
        }
        return "div";
    }

}
