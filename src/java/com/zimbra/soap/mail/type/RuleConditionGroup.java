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

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class RuleConditionGroup implements RuleComponent {

    @XmlAttribute(name=MailConstants.A_OPERATION, required=true)
    private final String operation;

    @XmlElements({
        @XmlElement(name=MailConstants.E_CONDITION_GROUP,
            type=RuleConditionGroup.class),
        @XmlElement(name=MailConstants.E_CONDITION,
            type=RuleCondition.class),
        @XmlElement(name=MailConstants.E_ACTION,
            type=RuleAction.class)
    })
    private List<RuleComponent> ruleComponents = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RuleConditionGroup() {
        this((String) null);
    }

    public RuleConditionGroup(String operation) {
        this.operation = operation;
    }

    public void setRuleComponents(Iterable <RuleComponent> ruleComponents) {
        this.ruleComponents.clear();
        if (ruleComponents != null) {
            Iterables.addAll(this.ruleComponents,ruleComponents);
        }
    }

    public RuleConditionGroup addRuleComponent(RuleComponent ruleComponent) {
        this.ruleComponents.add(ruleComponent);
        return this;
    }

    public String getOperation() { return operation; }
    public List<RuleComponent> getRuleComponents() {
        return Collections.unmodifiableList(ruleComponents);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("operation", operation)
            .add("ruleComponents", ruleComponents)
            .toString();
    }
}
