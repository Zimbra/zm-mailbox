/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
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
