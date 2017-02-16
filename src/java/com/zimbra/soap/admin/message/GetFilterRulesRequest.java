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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.ServerSelector;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get filter rules
 */
@XmlRootElement(name=AdminConstants.E_GET_FILTER_RULES_REQUEST)
public class GetFilterRulesRequest {
    /**
     * @zm-api-field-tag type
     * @zm-api-field-description Type can be either before or after
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=true)
    protected String type;

    /**
     * @zm-api-field-description Account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT /* account */, required=false)
    protected AccountSelector account;
    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_DOMAIN /* domain */, required=false)
    protected DomainSelector domain;
    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_COS /* cos */, required=false)
    protected CosSelector cos;
    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_SERVER /* server */, required=false)
    protected ServerSelector server;

    public GetFilterRulesRequest() {
        this.type = null;
        this.account = null;
        this.domain = null;
        this.cos = null;
        this.server = null;
    }

    public GetFilterRulesRequest(AccountSelector accountSelector, String type) {
        this.type = type;
        this.account = accountSelector;
        this.domain = null;
        this.cos = null;
        this.server = null;
    }

    public GetFilterRulesRequest(DomainSelector domainSelector, String type) {
        this.type = type;
        this.account = null;
        this.domain = domainSelector;
        this.cos = null;
        this.server = null;
    }

    public GetFilterRulesRequest(CosSelector cosSelector, String type) {
        this.type = type;
        this.account = null;
        this.domain = null;
        this.cos = cosSelector;
        this.server = null;
    }

    public GetFilterRulesRequest(ServerSelector serverSelector, String type) {
        this.type = type;
        this.account = null;
        this.domain = null;
        this.cos = null;
        this.server = serverSelector;
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
     * @return the domainSelector
     */
    public DomainSelector getDomain() {
        return domain;
    }

    /**
     * @param domainSelector the domainSelector to set
     */
    public void setDomain(DomainSelector domainSelector) {
        this.domain = domainSelector;
    }

    /**
     * @return the cosSelector
     */
    public CosSelector getCos() {
        return cos;
    }

    /**
     * @param cosSelector the cosSelector to set
     */
    public void setCos(CosSelector cosSelector) {
        this.cos = cosSelector;
    }

    /**
     * @return the serverSelector
     */
    public ServerSelector getServer() {
        return server;
    }

    /**
     * @param serverSelector the serverSelector to set
     */
    public void setServer(ServerSelector serverSelector) {
        this.server = serverSelector;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GetFilterRulesRequest ");
        sb.append(getToStringData());
        return sb.toString();
    }

    protected String getToStringData() {
        StringBuilder sb = new StringBuilder();
        sb.append("type : ").append(this.type);
        if (this.account != null) {
            sb.append(" for account ").append(this.account.getKey())
                .append(" by ").append(this.account.getBy());
        } else if (this.domain != null) {
            sb.append(" for domain ").append(this.domain.getKey())
            .append(" by ").append(this.domain.getBy());
        } else if (this.cos != null) {
            sb.append(" for cos ").append(this.cos.getKey())
            .append(" by ").append(this.cos.getBy());
        } else if (this.server != null) {
            sb.append(" for server ").append(this.server.getKey())
            .append(" by ").append(this.server.getBy());
        } else {
            sb.append("without any selector");
        }
        return sb.toString();
    }
}