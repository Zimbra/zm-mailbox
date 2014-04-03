/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
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

import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;

// JsonPropertyOrder added to make sure JaxbToJsonTest.bug65572_BooleanAndXmlElements passes
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"tests", "actions","child"})
@JsonPropertyOrder({ "tests", "actions","child"})
public final class NestedRule {

    /**
     * @zm-api-field-description Filter tests
     */
    @XmlElement(name=MailConstants.E_FILTER_TESTS /* filterTests */, required=true)
    private FilterTests tests;

    /**
     * @zm-api-field-description Filter actions
     */
    @ZimbraJsonArrayForWrapper
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
    // in nested rule case, actions could be null.
    private List<FilterAction> actions;
    
    // For Nested Rule
    /**
     * @zm-api-field-description NestedRule child
     */
    @XmlElement(name=MailConstants.E_NESTED_RULE /* nestedRule */)
    private NestedRule child;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private NestedRule() {
        this(null);
    }
    
    public NestedRule(FilterTests tests) {
        this.tests = tests;
        this.actions = null;
    }

    public void setFilterTests(FilterTests value) {
        tests = value;
    }

    public void setFilterActions(Collection<FilterAction> list) {
        if (list != null) {
            if(actions == null){
                actions=Lists.newArrayList();
            }else{
                actions.clear();
            }
            actions.addAll(list);
        } else { // re-initialise actions
            actions = null;
        }
    }

    public NestedRule addFilterAction(FilterAction action) {
        if(actions == null){
            actions=Lists.newArrayList();
        }
        actions.add(action);
        return this;
    }

    public FilterTests getFilterTests() {
        return tests;
    }

    public List<FilterAction> getFilterActions() {
        // there must be no actions.size()==0 case. This is for just in case.
        if(actions == null || actions.size() == 0) {
            return null;
        }
        return Collections.unmodifiableList(actions);
    }

    public int getActionCount() {
        if(actions == null){
            return 0;
        }
        return actions.size();
    }
    
    // For Nested Rule
    public NestedRule getChild() {
        return child;
    }
    
    // For Nested Rule
    public void setChild(NestedRule nestedRule) {
        child = nestedRule;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("tests", tests)
            .add("actions", actions)
            .add("child", child)
            .toString();
    }
}
