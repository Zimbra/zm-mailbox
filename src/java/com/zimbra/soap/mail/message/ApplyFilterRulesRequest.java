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
import com.zimbra.soap.mail.type.IdsAttr;
import com.zimbra.soap.type.NamedElement;

/**
 * @zm-api-command-description Applies one or more filter rules to messages specified by a comma-separated ID list,
 * or returned by a search query.  One or the other can be specified, but not both.  Returns the list of ids of
 * existing messages that were affected.
 * <br />
 * Note that redirect actions are ignored when applying filter rules to existing messages.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_APPLY_FILTER_RULES_REQUEST)
public class ApplyFilterRulesRequest {

    /**
     * @zm-api-field-description Filter rules
     */
    @XmlElementWrapper(name=MailConstants.E_FILTER_RULES /* filterRules */, required=true)
    @XmlElement(name=MailConstants.E_FILTER_RULE /* filterRule */, required=false)
    private List<NamedElement> filterRules = Lists.newArrayList();

    /**
     * @zm-api-field-tag comma-sep-msg-ids
     * @zm-api-field-description Comma-separated list of message IDs
     */
    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private IdsAttr msgIds;

    /**
     * @zm-api-field-tag query-string
     * @zm-api-field-description Query string
     */
    @XmlElement(name=MailConstants.E_QUERY /* query */, required=false)
    private String query;

    public ApplyFilterRulesRequest() {
    }

    public void setFilterRules(Iterable <NamedElement> filterRules) {
        this.filterRules.clear();
        if (filterRules != null) {
            Iterables.addAll(this.filterRules,filterRules);
        }
    }

    public ApplyFilterRulesRequest addFilterRul(NamedElement filterRul) {
        this.filterRules.add(filterRul);
        return this;
    }

    public void setMsgIds(IdsAttr msgIds) { this.msgIds = msgIds; }
    public void setQuery(String query) { this.query = query; }
    public List<NamedElement> getFilterRules() {
        return Collections.unmodifiableList(filterRules);
    }
    public IdsAttr getMsgIds() { return msgIds; }
    public String getQuery() { return query; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("filterRules", filterRules)
            .add("msgIds", msgIds)
            .add("query", query)
            .toString();
    }
}
