/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.ServerSelector;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;
import com.zimbra.soap.mail.type.FilterRule;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Modify Filter rules
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_MODIFY_FILTER_RULES_REQUEST)
public class ModifyFilterRulesRequest {
    /**
     * @zm-api-field-tag type
     * @zm-api-field-description Type can be either before or after
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=true)
    protected String type;
    /**
     * @zm-api-field-description Account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT)
    protected AccountSelector account;
    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_DOMAIN)
    protected DomainSelector domain;
    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_COS)
    protected CosSelector cos;
    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_SERVER)
    protected ServerSelector server;

    /**
     * @zm-api-field-description Filter rules
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=AdminConstants.E_FILTER_RULES /* filterRules */, required=true)
    @XmlElement(name=AdminConstants.E_FILTER_RULE /* filterRule */, required=false)
    protected List<FilterRule> filterRules = Lists.newArrayList();

    public ModifyFilterRulesRequest() {
        this.type = null;
        this.account = null;
        this.domain = null;
        this.cos = null;
        this.server = null;
        this.filterRules = null;
    }

    public ModifyFilterRulesRequest(AccountSelector account, List<FilterRule> filterRules, String type) {
        this.type = type;
        this.account = account;
        this.domain = null;
        this.cos = null;
        this.server = null;
        this.filterRules = filterRules;
    }

    public ModifyFilterRulesRequest(DomainSelector domain, List<FilterRule> filterRules, String type) {
        this.type = type;
        this.account = null;
        this.domain = domain;
        this.cos = null;
        this.server = null;
        this.filterRules = filterRules;
    }

    public ModifyFilterRulesRequest(CosSelector cos, List<FilterRule> filterRules, String type) {
        this.type = type;
        this.account = null;
        this.domain = null;
        this.cos = cos;
        this.server = null;
        this.filterRules = filterRules;
    }

    public ModifyFilterRulesRequest(ServerSelector server, List<FilterRule> filterRules, String type) {
        this.type = type;
        this.account = null;
        this.domain = null;
        this.cos = null;
        this.server = server;
        this.filterRules = filterRules;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
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

    /**
     * @param domainSelector the domainSelector to set
     */
    public void setDomain(DomainSelector domain) {
        this.domain = domain;
    }

    /**
     * @return the domainSelector
     */
    public DomainSelector getDomain() {
        return domain;
    }

    /**
     * @param cosSelector the cosSelector to set
     */
    public void setCos(CosSelector cos) {
        this.cos = cos;
    }

    /**
     * @return the cosSelector
     */
    public CosSelector getCos() {
        return cos;
    }

    /**
     * @param serverSelector the serverSelector to set
     */
    public void setServer(ServerSelector server) {
        this.server = server;
    }

    /**
     * @return the serverSelector
     */
    public ServerSelector getServer() {
        return server;
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper.add("type", this.type);
        if (this.account != null) {
            helper.add("account", this.account.getKey());
            helper.add("by", this.account.getBy());
        } else if (this.domain != null) {
            helper.add("domain", this.domain.getKey());
            helper.add("by", this.domain.getBy());
        } else if (this.cos != null) {
            helper.add("cos", this.cos.getKey());
            helper.add("by", this.cos.getBy());
        } else if (this.server != null) {
            helper.add("server", this.server.getKey());
            helper.add("by", this.server.getBy());
        } else {
            helper.add("selector", null);
        }
        helper.add("filterRules", this.filterRules);
        return helper;
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
