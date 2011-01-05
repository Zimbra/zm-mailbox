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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class AdminObjectInfo {

    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;
    @XmlAttribute(name=AdminConstants.A_ID, required=true)
    private final String id;
    @XmlElement(name=AdminConstants.E_A)
    private final List <Attr> attrList;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AdminObjectInfo() {
        this(null, null, null);
    }

    public AdminObjectInfo(String id, String name, Collection <Attr> attrs) {
        this.name = name;
        this.id = id;
        this.attrList = new ArrayList<Attr>();
        if (attrs != null) {
            this.attrList.addAll(attrs);
        }
    }

    public String getName() { return name; }
    public String getId() { return id; }
    public List<Attr> getAttrList() {
        return Collections.unmodifiableList(attrList);
    }
}
