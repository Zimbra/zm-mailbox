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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"filterTests", "filterActions"})
public class FilterRule {

    @XmlAttribute(name=MailConstants.A_NAME, required=false)
    private String name;

    @XmlAttribute(name=MailConstants.A_ACTIVE, required=true)
    private final boolean active;

    @XmlElement(name=MailConstants.E_FILTER_TESTS, required=true)
    private final FilterTests filterTests;

    @XmlElementWrapper(name=MailConstants.E_FILTER_ACTIONS, required=false)
    @XmlElements({
        @XmlElement(name=MailConstants.E_ACTION_KEEP,
            type=FilterActionKeep.class),
        @XmlElement(name=MailConstants.E_ACTION_DISCARD,
            type=FilterActionDiscard.class),
        @XmlElement(name=MailConstants.E_ACTION_FILE_INTO,
            type=FilterActionFileInto.class),
        @XmlElement(name=MailConstants.E_ACTION_FLAG,
            type=FilterActionFlag.class),
        @XmlElement(name=MailConstants.E_ACTION_TAG,
            type=FilterActionTag.class),
        @XmlElement(name=MailConstants.E_ACTION_REDIRECT,
            type=FilterActionRedirect.class),
        @XmlElement(name=MailConstants.E_ACTION_REPLY,
            type=FilterActionReply.class),
        @XmlElement(name=MailConstants.E_ACTION_NOTIFY,
            type=FilterActionNotify.class),
        @XmlElement(name=MailConstants.E_ACTION_STOP,
            type=FilterActionStop.class)
    })
    private List<FilterAction> filterActions = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private FilterRule() {
        this(false, (FilterTests) null);
    }

    public FilterRule(boolean active, FilterTests filterTests) {
        this.active = active;
        this.filterTests = filterTests;
    }

    public void setName(String name) { this.name = name; }
    public void setFilterActions(Iterable <FilterAction> filterActions) {
        this.filterActions.clear();
        if (filterActions != null) {
            Iterables.addAll(this.filterActions,filterActions);
        }
    }

    public FilterRule addFilterAction(FilterAction filterAction) {
        this.filterActions.add(filterAction);
        return this;
    }

    public String getName() { return name; }
    public boolean getActive() { return active; }
    public FilterTests getFilterTests() { return filterTests; }
    public List<FilterAction> getFilterActions() {
        return Collections.unmodifiableList(filterActions);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", name)
            .add("active", active)
            .add("filterTests", filterTests)
            .add("filterActions", filterActions)
            .toString();
    }
}
