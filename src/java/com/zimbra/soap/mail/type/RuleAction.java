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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlMixed;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class RuleAction implements RuleComponent {

    @XmlAttribute(name=MailConstants.A_NAME, required=true)
    private final String name;

    // Used for Jaxb marshalling
    // Mixed content can contain instances of Element class "arg" (E_FILTER_ARG)
    // Text data is represented as java.util.String for text.
    //
    // Note: FilterArg needs an @XmlRootElement annotation in order
    // to avoid schemagen error:
    //  error: Invalid @XmlElementRef : 
    //      Type "com.zimbra.soap.mail.type.FilterArg"
    //      or any of its subclasses are not known to this context.
    @XmlElementRefs({
        @XmlElementRef(name=MailConstants.E_FILTER_ARG, type=FilterArg.class)
    })
    @XmlMixed
    private List <Object> content;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RuleAction() {
        this((String) null);
    }

    public RuleAction(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    protected void setContent(Iterable <Object> content) {
        this.content.clear();
        if (content != null) {
            Iterables.addAll(this.content, content);
        }
    }

    protected List<Object> getContent() {
        return Collections.unmodifiableList(content);
    }

    public RuleAction setFilterArgs(List<FilterArg> filterArgs) {
        for (Object obj : content) {
            if (obj instanceof FilterArg)
                this.content.remove(obj);
        }
        Iterables.addAll(this.content, filterArgs);
        return this;
    }

    public RuleAction addFilterArg(FilterArg filterArg) {
        this.content.add(filterArg);
        return this;
    }

    public List<FilterArg> getFilterArgs() {
        List<FilterArg> fArgs = Lists.newArrayList();
        for (Object obj : content) {
            if (obj instanceof FilterArg)
                fArgs.add((FilterArg) obj);
        }
        return Collections.unmodifiableList(fArgs);
    }

    public RuleAction setValue(String value) {
        for (Object obj : content) {
            if (obj instanceof String)
                this.content.remove(obj);
        }
        this.content.add(value);
        return this;
    }

    public void addValue(String value) {
        this.content.add(value);
    }

    public String getValue() {
        StringBuilder sb = new StringBuilder();
        for (Object obj : content) {
            if (obj instanceof String)
                sb.append((String) obj);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", name)
            .add("content", content)
            .toString();
    }
}
