/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.html;

import java.util.regex.Pattern;

import org.apache.xerces.xni.XMLString;
import org.cyberneko.html.filters.Purifier;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import com.zimbra.common.localconfig.DebugConfig;


/**
 * @author zimbra
 *
 */
public class HtmlPurifier extends Purifier {

    private static final Pattern VALID_IMG_TAG = Pattern.compile(
        DebugConfig.defangOwaspValidImgTag, Pattern.CASE_INSENSITIVE);
    private static final PolicyFactory sanitizer = Sanitizers.FORMATTING.and(Sanitizers.IMAGES);
    private static final Pattern IMG_SKIP_OWASPSANITIZE = Pattern.compile(
        DebugConfig.defangImgSkipOwaspSanitize, Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_ONLOAD_METHOD = Pattern.compile(
        DebugConfig.defangOnloadMethod, Pattern.CASE_INSENSITIVE);

    /* (non-Javadoc)
     * @see org.cyberneko.html.filters.Purifier#purifyText(org.apache.xerces.xni.XMLString)
     */
    @Override
    protected XMLString purifyText(XMLString text) {
        String temp = text.toString();


        if (IMG_SKIP_OWASPSANITIZE.matcher(temp).find()) {
            return text;
        }


        if (VALID_IMG_TAG.matcher(temp).find()) {
            temp = sanitizer.sanitize(temp);
        }


        if (VALID_ONLOAD_METHOD.matcher(temp).find()) {
            temp = sanitizer.sanitize(temp);
        }

        XMLString n = new XMLString();
        n.setValues(temp.toCharArray(), 0, temp.length());

        return super.purifyText(n);
    }

}
