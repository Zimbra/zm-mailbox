/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class ContactMetaData {

    @XmlAttribute(name=MailConstants.A_SECTION, required=false)
    private String section;

    @XmlElement(name=AdminConstants.E_A, required=false)
    private List<Attr> attrs = Lists.newArrayList();

    public ContactMetaData() {
    }

    public void setSection(String section) { this.section = section; }
    public void setAttrs(Iterable <Attr> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs,attrs);
        }
    }

    public ContactMetaData addAttr(Attr attr) {
        this.attrs.add(attr);
        return this;
    }

    public String getSection() { return section; }
    public List<Attr> getAttrs() {
        return Collections.unmodifiableList(attrs);
    }
}
