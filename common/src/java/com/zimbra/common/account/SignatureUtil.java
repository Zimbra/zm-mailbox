/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.account;

import com.google.common.collect.ImmutableBiMap;

public class SignatureUtil {

    public static final ImmutableBiMap<String, String> ATTR_TYPE_MAP = ImmutableBiMap.of(
        ZAttrProvisioning.A_zimbraPrefMailSignature, "text/plain",
        ZAttrProvisioning.A_zimbraPrefMailSignatureHTML, "text/html");
        
    public static String mimeTypeToAttrName(String mimeType) {
        return ATTR_TYPE_MAP.inverse().get(mimeType);
    }

    public static String attrNameToMimeType(String attrName) {
        return ATTR_TYPE_MAP.get(attrName);
    }
}
