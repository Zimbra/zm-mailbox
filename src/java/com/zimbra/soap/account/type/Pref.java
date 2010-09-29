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

/*
   <pref name="{name}" modified="{modified-time}">{value}</pref>
 */
public class Pref {
    
    @XmlAttribute private String name;
    @XmlAttribute(name="modified") private Long modifiedTimestamp;
    @XmlValue private String value;

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
    public Pref setName(String name) { this.name = name; return this; }
    
    public Long getModifiedTimestamp() { return modifiedTimestamp; }
    public Pref setModifiedTimestamp(Long timestamp) { this.modifiedTimestamp = timestamp; return this; }
    
    public String getValue() { return value; }
    public Pref setValue(String value) { this.value = value; return this; }
    
    public static Multimap<String, String> toMultimap(Iterable<Pref> prefs) {
        Multimap<String, String> map = ArrayListMultimap.create();
        for (Pref p : prefs) {
            map.put(p.getName(), p.getValue());
        }
        return map;
    }
}
