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

public class AElementPolicy implements ElementPolicy {

    /**
     * make sure all <a> tags have a target="_blank" attribute set.
     */
    @Override
    public String apply(String elementName, List<String> attrs) {
        final int hrefIndex = attrs.indexOf("href");
        if (hrefIndex == -1) {
            //links that don't have a href don't need target="_blank"
            return "a";
        }
        String hrefValue = attrs.get(hrefIndex + 1);
        //LOCAL links don't need target="_blank"
        if (hrefValue.indexOf('#') == 0)
        {
            return "a";
        }
        final int targetIndex = attrs.indexOf("target");
        if (targetIndex != -1) {
            attrs.remove(targetIndex);
            attrs.remove(targetIndex);// value
        }
        attrs.add("target");
        attrs.add("_blank");
        return "a";

    }

}
