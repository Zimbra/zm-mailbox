/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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
