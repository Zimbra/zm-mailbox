/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.type;

import java.util.EnumSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

/*
          {types}   = comma-separated list.  Legal values are:
                      appointment|chat|contact|conversation|document|
                      message|tag|task|wiki
                      (default is &quot;conversation&quot;)

 */
@XmlEnum
public enum ItemType {
    @XmlEnumValue("appointment") APPOINTMENT,
    @XmlEnumValue("chat") CHAT,
    @XmlEnumValue("contact") CONTACT,
    @XmlEnumValue("conversation") CONVERSATION,
    @XmlEnumValue("document") DOCUMENT,
    @XmlEnumValue("message") MESSAGE,
    @XmlEnumValue("tag") TAG,
    @XmlEnumValue("task") TASK,
    @XmlEnumValue("wiki") WIKI;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    @XmlTransient
    static final class CSVAdapter extends XmlAdapter<String, Set<ItemType>> {
        @Override
        public String marshal(Set<ItemType> set) throws Exception {
            return Joiner.on(',').skipNulls().join(set);
        }

        @Override
        public Set<ItemType> unmarshal(String csv) throws Exception {
            Set<ItemType> result = EnumSet.noneOf(ItemType.class);
            for (String token : Splitter.on(',').trimResults().omitEmptyStrings().split(csv)) {
                result.add(ItemType.valueOf(token.toUpperCase()));
            }
            return result;
        }
    }

}
