/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.FilterRule;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Modify Outgoing Filter rules
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_MODIFY_OUTGOING_FILTER_RULES_REQUEST)
public final class ModifyOutgoingFilterRulesRequest {

    /**
     * @zm-api-field-description Outgoing filter rules
     */
    @XmlElementWrapper(name=MailConstants.E_FILTER_RULES, required=true)
    @XmlElement(name=MailConstants.E_FILTER_RULE, required=false)
    private final List<FilterRule> rules = Lists.newArrayList();

    public void setFilterRules(Collection<FilterRule> list) {
        rules.clear();
        if (list != null) {
            rules.addAll(list);
        }
    }

    public void addFilterRule(FilterRule rule) {
        rules.add(rule);
    }

    public void addFilterRule(Collection<FilterRule> list) {
        rules.addAll(list);
    }

    public List<FilterRule> getFilterRules() {
        return Collections.unmodifiableList(rules);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("rules", rules).toString();
    }
}
