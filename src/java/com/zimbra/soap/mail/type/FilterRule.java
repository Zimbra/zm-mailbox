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
import com.google.common.collect.Lists;

import java.util.Collection;
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
import com.zimbra.soap.type.ZmBoolean;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

// JsonPropertyOrder added to make sure JaxbToJsonTest.bug65572_BooleanAndXmlElements passes
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"tests", "actions"})
@JsonPropertyOrder({ "name", "active", "tests", "actions" })
public final class FilterRule {

    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=true)
    private final String name;

    @XmlAttribute(name=MailConstants.A_ACTIVE /* active */, required=true)
    private final ZmBoolean active;

    @XmlElement(name=MailConstants.E_FILTER_TESTS /* filterTests */, required=true)
    private FilterTests tests;

    @XmlElementWrapper(name=MailConstants.E_FILTER_ACTIONS /* filterActions */, required=false)
    @XmlElements({
        @XmlElement(name=MailConstants.E_ACTION_KEEP /* actionKeep */, type=FilterAction.KeepAction.class),
        @XmlElement(name=MailConstants.E_ACTION_DISCARD /* actionDiscard */, type=FilterAction.DiscardAction.class),
        @XmlElement(name=MailConstants.E_ACTION_FILE_INTO /* actionFileInto */, type=FilterAction.FileIntoAction.class),
        @XmlElement(name=MailConstants.E_ACTION_FLAG /* actionFlag */, type=FilterAction.FlagAction.class),
        @XmlElement(name=MailConstants.E_ACTION_TAG /* actionTag */, type=FilterAction.TagAction.class),
        @XmlElement(name=MailConstants.E_ACTION_REDIRECT /* actionRedirect */, type=FilterAction.RedirectAction.class),
        @XmlElement(name=MailConstants.E_ACTION_REPLY /* actionReply */, type=FilterAction.ReplyAction.class),
        @XmlElement(name=MailConstants.E_ACTION_NOTIFY /* actionNotify */, type=FilterAction.NotifyAction.class),
        @XmlElement(name=MailConstants.E_ACTION_STOP /* actionStop */, type=FilterAction.StopAction.class)
    })
    private final List<FilterAction> actions = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private FilterRule() {
        this(null, false);
    }

    public FilterRule(String name, boolean active) {
        this.name = name;
        this.active = ZmBoolean.fromBool(active);
    }

    public FilterRule(String name, FilterTests tests, boolean active) {
        this.name = name;
        this.tests = tests;
        this.active = ZmBoolean.fromBool(active);
    }

    public static FilterRule createForNameFilterTestsAndActiveSetting(String name, FilterTests tests, boolean active) {
        return new FilterRule(name, tests, active);
    }

    public void setFilterTests(FilterTests value) {
        tests = value;
    }

    public void setFilterActions(Collection<FilterAction> list) {
        actions.clear();
        if (list != null) {
            actions.addAll(list);
        }
    }

    public FilterRule addFilterAction(FilterAction action) {
        actions.add(action);
        return this;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return ZmBoolean.toBool(active);
    }

    public FilterTests getFilterTests() {
        return tests;
    }

    public List<FilterAction> getFilterActions() {
        return Collections.unmodifiableList(actions);
    }

    public int getActionCount() {
        return actions.size();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", name)
            .add("active", active)
            .add("tests", tests)
            .add("actions", actions)
            .toString();
    }
}
