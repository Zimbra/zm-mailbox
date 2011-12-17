/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
