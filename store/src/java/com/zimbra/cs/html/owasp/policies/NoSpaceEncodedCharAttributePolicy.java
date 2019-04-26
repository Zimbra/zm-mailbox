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
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.owasp.html.AttributePolicy;

import com.zimbra.common.localconfig.DebugConfig;

public class NoSpaceEncodedCharAttributePolicy implements AttributePolicy {

    private static final Pattern AV_JS_ENTITY = Pattern.compile(DebugConfig.defangAvJsEntity);
    private static final Pattern AV_SCRIPT_TAG = Pattern.compile(DebugConfig.defangAvScriptTag,
        Pattern.CASE_INSENSITIVE);

    @Override
    public String apply(String elementName, String attributeName, String value) {
        value = removeAnySpacesAndEncodedChars(value);
        value = AV_JS_ENTITY.matcher(value).replaceAll("JS-ENTITY-BLOCKED");
        value = AV_SCRIPT_TAG.matcher(value).replaceAll("SCRIPT-TAG-BLOCKED");
        return value;
    }

    private static String removeAnySpacesAndEncodedChars(String result) {
        String sanitizedStr = result;
        StringBuilder sb = new StringBuilder();
        int index = result.indexOf(":");
        if (index > -1) {
            String jsString = result.substring(0, index);
            char[] chars = jsString.toCharArray();
            for (int i = 0; i < chars.length; ++i) {
                if (!Character.isSpace(chars[i])) {
                    sb.append(chars[i]);
                }
            }
        }
        String temp = sb.toString();
        temp = StringEscapeUtils.unescapeHtml(temp);
        if (index != -1 && (temp.toLowerCase().contains("javascript")
            || temp.toLowerCase().contains("vbscript"))) {
            sanitizedStr = temp + result.substring(index);
        }
        return sanitizedStr;
    }

}
