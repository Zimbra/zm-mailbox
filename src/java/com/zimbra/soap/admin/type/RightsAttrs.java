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

import java.util.List;
import java.util.Collection;
import java.util.Collections;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;

import com.google.common.collect.Lists;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.Attr;

@XmlAccessorType(XmlAccessType.FIELD)
public class RightsAttrs {

    @XmlAttribute(name=AdminConstants.A_ALL, required=false)
    private Boolean all;

    //  Need to support 2 forms:
    //  1.    <a n="attributeName"/>
    //  2.    <attributeName/>
    //  TODO: Could we use @XmlAdapter to make it easier to use this class?
    @XmlAnyElement
    @XmlElementRefs({
        @XmlElementRef(name=AdminConstants.E_A, type=Attr.class)
    })
    private List <Object> objects = Lists.newArrayList();

    public RightsAttrs () {
    }

    public RightsAttrs (Collection <Object> objects) {
        this.setObjects(objects);
    }

    public RightsAttrs setObjects(Collection<Object> objects) {
        this.objects.clear();
        if (objects != null) {
            this.objects.addAll(objects);
        }
        return this;
    }

    public RightsAttrs addObject(Object object) {
        objects.add(object);
        return this;
    }

    public List <Object> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    public void setAll(Boolean all) { this.all = all; }

    public Boolean getAll() { return all; }
}
