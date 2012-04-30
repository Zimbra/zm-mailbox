/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.soap.jaxb;

import java.util.Map;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.google.common.collect.Maps;

/** Test enum for JAXB testing.  Values differ from representation in Xml/JSON. Has 1 empty string representation */
@XmlEnum
public enum ViewEnum {
    @XmlEnumValue("") UNKNOWN (""),
    @XmlEnumValue("search folder") SEARCH_FOLDER ("search folder"),
    @XmlEnumValue("tag") TAG ("tag"),
    @XmlEnumValue("conversation") CONVERSATION ("conversation"),
    @XmlEnumValue("message") MESSAGE ("message"),
    @XmlEnumValue("contact") CONTACT ("contact"),
    @XmlEnumValue("document") DOCUMENT ("document"),
    @XmlEnumValue("appointment") APPOINTMENT ("appointment"),
    @XmlEnumValue("virtual conversation") VIRTUAL_CONVERSATION ("virtual conversation"),
    @XmlEnumValue("remote folder") REMOTE_FOLDER ("remote folder"),
    @XmlEnumValue("wiki") WIKI ("wiki"),
    @XmlEnumValue("task") TASK ("task"),
    @XmlEnumValue("chat") CHAT ("chat");

    private static Map<String, ViewEnum> nameToView = Maps.newHashMap();

    static {
        for (ViewEnum v : ViewEnum.values()) {
            nameToView.put(v.toString(), v);
        }
    }

    private String name;

    private ViewEnum(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static ViewEnum fromString(String name) {
        if (name == null) {
            name = "";
        }
        return nameToView.get(name);
    }
}
