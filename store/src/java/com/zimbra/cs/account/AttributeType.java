/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.Map;

public enum AttributeType {
    TYPE_BOOLEAN("boolean"),
    TYPE_BINARY("binary"),
    TYPE_CERTIFICATE("certificate"),
    TYPE_DURATION("duration"),
    TYPE_GENTIME("gentime"),
    TYPE_EMAIL("email"),
    TYPE_EMAILP("emailp"),
    TYPE_INTL_EMAIL("intl_email"),
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
