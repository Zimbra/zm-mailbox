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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.RuleInfo;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_GET_RULES_RESPONSE)
public class GetRulesResponse {

    @XmlElementWrapper(name="rules", required=false)
    @XmlElement(name=MailConstants.E_RULE, required=false)
    private List<RuleInfo> rules = Lists.newArrayList();

    public GetRulesResponse() {
    }

    public void setRules(Iterable <RuleInfo> rules) {
        this.rules.clear();
        if (rules != null) {
            Iterables.addAll(this.rules,rules);
        }
    }

    public GetRulesResponse addRul(RuleInfo rul) {
        this.rules.add(rul);
        return this;
    }

    public List<RuleInfo> getRules() {
        return Collections.unmodifiableList(rules);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("rules", rules)
            .toString();
    }
}
