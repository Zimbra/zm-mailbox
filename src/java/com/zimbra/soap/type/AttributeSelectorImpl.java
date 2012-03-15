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

package com.zimbra.soap.type;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.collect.Lists;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
abstract public class AttributeSelectorImpl implements AttributeSelector {

    private static Joiner COMMA_JOINER = Joiner.on(",");
    private List<String> attrs = Lists.newArrayList();

    public AttributeSelectorImpl() {
    }

    public AttributeSelectorImpl(String attrs) {
        setAttrs(attrs);
    }

    public AttributeSelectorImpl(String ... attrNames) {
        addAttrs(attrNames);
    }

    public AttributeSelectorImpl(Iterable<String> attrs) {
        addAttrs(attrs);
    }

    @Override
    public AttributeSelector setAttrs(String attrs) {
        this.attrs.clear();
        if (attrs != null) {
            addAttrs(attrs.split(","));
        }
        return this;
    }

    @Override
    public AttributeSelector addAttrs(String attr) {
        if (attr != null)
            attrs.add(attr);
        return this;
    }

    @Override
    public AttributeSelector addAttrs(String ... attrNames) {
        for (String attrName : attrNames) {
            addAttrs(attrName);
        }
        return this;
    }

    @Override
    public AttributeSelector addAttrs(Iterable<String> attrs) {
        if (attrs != null) {
            for (String attr : attrs) {
                addAttrs(attr);
            }
        }
        return this;
    }

    /**
     * @zm-api-field-tag request-attrs
     * @zm-api-field-description Comma separated list of attributes
     */
    @Override
    @XmlAttribute(name=AdminConstants.A_ATTRS, required=false)
    public String getAttrs() {
        if (attrs.size() == 0)
            return null;
        return COMMA_JOINER.join(attrs);
    }

    public Objects.ToStringHelper addToStringInfo(
            Objects.ToStringHelper helper) {
    return helper
        .add("attrs", getAttrs());
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
