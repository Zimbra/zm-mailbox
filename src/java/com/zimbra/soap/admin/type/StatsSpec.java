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
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlMixed;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class StatsSpec {

    @XmlAttribute(name=AdminConstants.A_LIMIT, required=false)
    private final String limit;

    @XmlAttribute(name=AdminConstants.A_NAME, required=false)
    private final String name;

    // Used for Jaxb marshalling
    // Mixed content can contain instances of Element class "values"
    // Text data is represented as java.util.String for text.
    //
    // Note: StatsValueWrapper needs an @XmlRootElement annotation in order
    // to avoid getting a schemagen error:
    @XmlElementRefs({
        @XmlElementRef(name=AdminConstants.E_VALUES,
            type=StatsValueWrapper.class)
    })
    @XmlMixed 
    private List <Object> content;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private StatsSpec() {
        this(null, null);
    }

    public StatsSpec(String limit, String name) {
        this.limit = limit;
        this.name = name;
    }

    public StatsSpec setStatValues(StatsValueWrapper statValues) {
        if (content == null)
            content = Lists.newArrayList();
        for (Object obj : content) {
            if (obj instanceof StatsValueWrapper) {
                content.remove(obj);
            }
        }
        this.content.add(statValues);
        return this;
    }

    public void setValue(String value) {
        if (content == null)
            content = Lists.newArrayList();
        for (Object obj : content) {
            if (obj instanceof String) {
                content.remove(obj);
            }
        }
        this.content.add(value);
    }

    public String getLimit() { return limit; }
    public String getName() { return name; }

    public StatsValueWrapper getStatValues() {
        for (Object obj : content) {
            if (obj instanceof StatsValueWrapper)
                return (StatsValueWrapper) obj;
        }
        return null;
    }

    public String getValue() {
        if (content == null)
            return null;
        StringBuilder sb = null;
        for (Object obj : content) {
            if (obj instanceof String) {
                if (sb == null) 
                    sb = new StringBuilder();
                sb.append((String) obj);
            }
        }
        if (sb == null) 
            return null;
        else
            return sb.toString();
    }
}
