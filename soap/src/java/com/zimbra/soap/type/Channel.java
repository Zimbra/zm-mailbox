/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server Copyright
 * (C) 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>. *****
 * END LICENSE BLOCK *****
 */

package com.zimbra.soap.type;

import java.util.Map;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

@XmlEnum
public enum Channel {
    @XmlEnumValue("email") EMAIL("email");

    private static Map<String, Channel> nameToChannel = Maps.newHashMap();
    static {
        for (Channel v : Channel.values()) {
            nameToChannel.put(v.toString(), v);
        }
    }

    private String name;

    private Channel(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Channel fromString(String name) {
        return nameToChannel.get(Strings.nullToEmpty(name));
    }
}