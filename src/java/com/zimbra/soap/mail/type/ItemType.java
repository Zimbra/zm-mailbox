/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.soap.mail.type;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;

/*
          {types}   = comma-separated list.  Legal values are:
                      appointment|chat|contact|conversation|document|
                      message|tag|task|wiki
                      (default is &quot;conversation&quot;)

 */
@XmlEnum
public enum ItemType {
    @XmlEnumValue("appointment") APPOINTMENT ("appointment"),
    @XmlEnumValue("chat") CHAT ("chat"),
    @XmlEnumValue("contact") CONTACT ("contact"),
    @XmlEnumValue("conversation") CONVERSATION ("conversation"),
    @XmlEnumValue("document") DOCUMENT ("document"),
    @XmlEnumValue("message") MESSAGE ("message"),
    @XmlEnumValue("tag") TAG ("tag"),
    @XmlEnumValue("task") TASK ("task"),
    @XmlEnumValue("wiki") WIKI ("wiki");
    
    private static Map<String, ItemType> nameToValue = new HashMap<String, ItemType>();
    
    static {
        for (ItemType type : values()) {
            nameToValue.put(type.toString(), type);
        }
    }
    private String name;
    
    private ItemType(String name) {
        this.name = name;
    }
    
    public static ItemType fromString(String s)
    throws ServiceException {
        ItemType type = nameToValue.get(s);
        if (type == null) {
            throw ServiceException.INVALID_REQUEST("Invalid type '" + s + "'.  Valid types are " +
                StringUtil.join(",", values()), null);
        }
        return type;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
