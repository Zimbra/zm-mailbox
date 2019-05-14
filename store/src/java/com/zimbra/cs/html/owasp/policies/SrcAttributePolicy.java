package com.zimbra.cs.html.owasp.policies;
import java.net.URI;
import java.net.URISyntaxException;

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
import org.owasp.html.AttributePolicy;

import com.zimbra.cs.html.owasp.OwaspHtmlSanitizer;

public class SrcAttributePolicy implements AttributePolicy {

    @Override
    public String apply(String elementName, String attributeName, String srcValue) {
        String base = OwaspHtmlSanitizer.zThreadLocal.get();

        if (base != null && srcValue != null) {
            URI baseHrefURI = null;
            try {
                baseHrefURI = new URI(base);
            } catch (URISyntaxException e) {
                if (!base.endsWith("/"))
                    base += "/";
            }
            if (srcValue.indexOf(":") == -1) {
                if (!srcValue.startsWith("/")) {
                    srcValue = "/" + srcValue;
                }

                if (baseHrefURI != null) {
                    try {
                        srcValue = baseHrefURI.resolve(srcValue).toString();
                        return srcValue;
                    } catch (IllegalArgumentException e) {
                        // ignore and do string-logic
                    }
                }
                srcValue = base + srcValue;
            }
        }
        return srcValue;
    }

}
