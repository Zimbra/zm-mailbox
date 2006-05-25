/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
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
    
    AttributeType(String name) {
        TM.sTypeMap.put(name, this);
    }
    
    public static AttributeType getType(String name) {
        return TM.sTypeMap.get(name);
    }
}
