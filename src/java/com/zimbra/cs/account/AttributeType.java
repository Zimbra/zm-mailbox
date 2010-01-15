/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.Map;

public enum AttributeType {
    TYPE_BOOLEAN("boolean"),
    TYPE_DURATION("duration"),
    TYPE_GENTIME("gentime"),
    TYPE_EMAIL("email"),
    TYPE_EMAILP("emailp"),
    TYPE_CS_EMAILP("cs_emailp"),
    TYPE_ENUM("enum"),
    TYPE_ID("id"),
    TYPE_INTEGER("integer"),
    TYPE_PORT("port"),
    TYPE_PHONE("phone"),
    TYPE_STRING("string"),
    TYPE_ASTRING("astring"),
    TYPE_OSTRING("ostring"),
    TYPE_CSTRING("cstring"),
    TYPE_REGEX("regex"),
    TYPE_LONG("long");

    private static class TM {
        static Map<String, AttributeType> sTypeMap = new HashMap<String, AttributeType>();
    }
    
    private String mName; 
    AttributeType(String name) {
        mName = name;
        TM.sTypeMap.put(name, this);
    }
    
    public static AttributeType getType(String name) {
        return TM.sTypeMap.get(name);
    }
    
    String getName() {
        return mName;
    }
}
