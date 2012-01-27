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

package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.zimbra.soap.base.KeyAndValue;

/*
   <pref name="{name}" modified="{modified-time}">{value}</pref>
 */
public class Pref implements KeyAndValue {

    /**
     * @zm-api-field-tag pref-name
     * @zm-api-field-description Preference name
     */
    @XmlAttribute(name="name", required=true)
    private String name;

    /**
     * @zm-api-field-tag pref-modified-time
     * @zm-api-field-description Preference modified time (may not be present)
     */
    @XmlAttribute(name="modified", required=false)
    private Long modifiedTimestamp;

    /**
     * @zm-api-field-tag pref-value
     * @zm-api-field-description Preference value
     */
    @XmlValue
    private String value;

    public Pref() {
    }

    public Pref(String name) {
        setName(name);
    }

    public Pref(String name, String value) {
        setName(name);
        setValue(value);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getModifiedTimestamp() { return modifiedTimestamp; }
    public void setModifiedTimestamp(Long timestamp) { this.modifiedTimestamp = timestamp; }

    @Override
    public String getValue() { return value; }
    @Override
    public void setValue(String value) { this.value = value; }

    public static Multimap<String, String> toMultimap(Iterable<Pref> prefs) {
        Multimap<String, String> map = ArrayListMultimap.create();
        for (Pref p : prefs) {
            map.put(p.getName(), p.getValue());
        }
        return map;
    }

    @Override
    public void setKey(String key) { setName(key); }
    @Override
    public String getKey() { return getName(); }
}
