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

import com.google.common.collect.Lists;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class StatsSpec {

    @XmlAttribute(name=AdminConstants.A_LIMIT /* limit */, required=false)
    private final String limit;

    @XmlAttribute(name=AdminConstants.A_NAME /* name */, required=false)
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
        this((String) null, (String) null);
    }

    public StatsSpec(String limit, String name) {
        this.limit = limit;
        this.name = name;
    }

    /**
     * Needed by JAXB.
     */
    private void setContent(List <Object> content) {
        this.content = content;
    }

    /**
     * Needed by JAXB.
     */
    private List <Object> getContent() {
        return content;
    }

    /**
     * Non-JAXB method
     */
    public StatsSpec setStatValues(StatsValueWrapper statValues) {
        if (getContent() == null)
            setContent(Lists.newArrayList());
        for (Object obj : getContent()) {
            if (obj instanceof StatsValueWrapper) {
                getContent().remove(obj);
            }
        }
        this.getContent().add(statValues);
        return this;
    }

    /**
     * Non-JAXB method
     */
    public void setValue(String value) {
        if (getContent() == null)
            setContent(Lists.newArrayList());
        for (Object obj : getContent()) {
            if (obj instanceof String) {
                getContent().remove(obj);
            }
        }
        this.getContent().add(value);
    }

    public String getLimit() { return limit; }
    public String getName() { return name; }

    /**
     * Non-JAXB method
     */
    public StatsValueWrapper getStatValues() {
        for (Object obj : getContent()) {
            if (obj instanceof StatsValueWrapper)
                return (StatsValueWrapper) obj;
        }
        return null;
    }

    /**
     * Non-JAXB method
     */
    public String getValue() {
        if (getContent() == null)
            return null;
        StringBuilder sb = null;
        for (Object obj : getContent()) {
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
