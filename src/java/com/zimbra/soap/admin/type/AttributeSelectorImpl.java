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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;
import com.google.common.base.Joiner;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
abstract public class AttributeSelectorImpl implements AttributeSelector {

    private static Joiner COMMA_JOINER = Joiner.on(",");
    private List<String> attrs = Lists.newArrayList();
    
    @XmlAttribute(name=AdminConstants.A_ATTRS) public String getAttrs() {
        return COMMA_JOINER.join(attrs);
    }
    
    public AttributeSelector setAttrs(String attrs) {
        this.attrs.clear();
        if (attrs != null) {
            addAttrs(attrs.split(","));
        }
        return this;
    }
    
    public AttributeSelector addAttrs(String attr) {
        attrs.add(attr);
        return this;
    }
    
    public AttributeSelector addAttrs(String ... attrNames) {
        for (String attrName : attrNames) {
            addAttrs(attrName);
        }
        return this;
    }

    public AttributeSelector addAttrs(Iterable<String> attrs) {
        if (attrs != null) {
            for (String attr : attrs) {
                addAttrs(attr);
            }
        }
        return this;
    }
}
