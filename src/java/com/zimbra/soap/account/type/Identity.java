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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/*
     <identity name={identity-name} id="...">
       <a name="{name}">{value}</a>
       ...
       <a name="{name}">{value}</a>
     </identity>*

 */
@XmlType(propOrder = {})
public class Identity {
    @XmlAttribute private String name;
    @XmlAttribute private String id;
    @XmlElement(name="a") private List<Attr> attrs = new ArrayList<Attr>();

    public Identity() {
    }
    
    public Identity(Identity i) {
        name = i.getName();
        id = i.getId();
        attrs.addAll(Lists.transform(i.getAttrs(), Attr.COPY));
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public List<Attr> getAttrs() {
        return Collections.unmodifiableList(attrs);
    }
    
    public void setAttrs(Iterable<Attr> attrs) {
        Iterables.addAll(this.attrs, attrs);
    }
    
    public Multimap<String, String> getAttrsMultimap() {
        return Attr.toMultimap(attrs);
    }
}
