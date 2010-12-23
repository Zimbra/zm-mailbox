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

package com.zimbra.soap.admin.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Collections;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

@XmlType(propOrder = {})
@XmlRootElement
abstract public class AdminAttrsImpl implements AdminAttrs {

    @XmlElement(name=AdminConstants.E_A)
    private List<Attr> a = new ArrayList<Attr>();

    public AdminAttrsImpl setA(Collection<Attr> attrs) {
        this.a.clear();
        if (attrs != null) {
            this.a.addAll(attrs);
        }
        return this;
    }

    public AdminAttrsImpl addA(Attr attr) {
        a.add(attr);
        return this;
    }

    public List<Attr> getA() {
        return Collections.unmodifiableList(a);
    }
}
