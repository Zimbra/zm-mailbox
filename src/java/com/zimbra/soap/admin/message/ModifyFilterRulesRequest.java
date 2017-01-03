/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

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

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.mail.type.FilterRule;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Modify Filter rules
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_MODIFY_FILTER_RULES_REQUEST)
public final class ModifyFilterRulesRequest {
    /**
     * @zm-api-field-description Account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT)
    private AccountSelector account;

    /**
     * @zm-api-field-description Filter rules
     */
    @XmlElementWrapper(name=AdminConstants.E_FILTER_RULES /* filterRules */, required=true)
    @XmlElement(name=AdminConstants.E_FILTER_RULE /* filterRule */, required=false)
    private List<FilterRule> filterRules = Lists.newArrayList();

    public ModifyFilterRulesRequest() {
    }

    public ModifyFilterRulesRequest(AccountSelector account, List<FilterRule> filterRules) {
        this.account = account;
        this.filterRules = filterRules;
    }

    /**
     * @return the accountSelector
     */
    public AccountSelector getAccount() {
        return account;
    }

    /**
     * @param accountSelector the accountSelector to set
     */
    public void setAccount(AccountSelector accountSelector) {
        this.account = accountSelector;
    }

    public void setFilterRules(Iterable <FilterRule> filterRules) {
        this.filterRules.clear();
        if (filterRules != null) {
            Iterables.addAll(this.filterRules,filterRules);
        }
    }

    public void addFilterRule(FilterRule filterRule) {
        this.filterRules.add(filterRule);
    }

    /**
     * Add additional filter rules
     */
    public void addFilterRules(Iterable <FilterRule> filterRules) {
        if (filterRules != null) {
            Iterables.addAll(this.filterRules, filterRules);
        }
    }

    public List<FilterRule> getFilterRules() {
        return filterRules;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("filterRules", filterRules);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
