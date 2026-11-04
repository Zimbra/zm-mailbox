/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc. All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description List active OAuth2 sessions for one account, all accounts in a domain,
 * or all accounts in a COS. Exactly one of account, domain or cos must be specified.
 * Revoked and expired sessions, and web, EAS and ZCO sessions, are not returned.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_GET_OAUTH2_ACTIVE_SESSIONS_REQUEST)
public class GetOAuth2ActiveSessionsRequest {

    /**
     * @zm-api-field-tag account
     * @zm-api-field-description List sessions of this account
     */
    @XmlElement(name = AdminConstants.E_ACCOUNT /* account */, required = false)
    private AccountSelector account;

    /**
     * @zm-api-field-tag domain
     * @zm-api-field-description List sessions of all accounts in this domain
     */
    @XmlElement(name = AdminConstants.E_DOMAIN /* domain */, required = false)
    private DomainSelector domain;

    /**
     * @zm-api-field-tag cos
     * @zm-api-field-description List sessions of all accounts in this COS
     */
    @XmlElement(name = AdminConstants.E_COS /* cos */, required = false)
    private CosSelector cos;

    /**
     * no-argument constructor wanted by JAXB
     */
    public GetOAuth2ActiveSessionsRequest() {
    }

    public static GetOAuth2ActiveSessionsRequest forAccount(AccountSelector account) {
        GetOAuth2ActiveSessionsRequest req = new GetOAuth2ActiveSessionsRequest();
        req.setAccount(account);
        return req;
    }

    public static GetOAuth2ActiveSessionsRequest forDomain(DomainSelector domain) {
        GetOAuth2ActiveSessionsRequest req = new GetOAuth2ActiveSessionsRequest();
        req.setDomain(domain);
        return req;
    }

    public static GetOAuth2ActiveSessionsRequest forCos(CosSelector cos) {
        GetOAuth2ActiveSessionsRequest req = new GetOAuth2ActiveSessionsRequest();
        req.setCos(cos);
        return req;
    }

    /**
     * Ensures exactly one listing scope (account, domain or cos) is set.
     */
    public void validate() throws ServiceException {
        int scopes = (account != null ? 1 : 0) + (domain != null ? 1 : 0) + (cos != null ? 1 : 0);
        if (scopes != 1) {
            throw ServiceException.INVALID_REQUEST(
                    "exactly one of account, domain or cos must be specified", null);
        }
    }

    public AccountSelector getAccount() { return account; }
    public void setAccount(AccountSelector account) { this.account = account; }

    public DomainSelector getDomain() { return domain; }
    public void setDomain(DomainSelector domain) { this.domain = domain; }

    public CosSelector getCos() { return cos; }
    public void setCos(CosSelector cos) { this.cos = cos; }

    @Override
    public String toString() {
        return String.format("GetOAuth2ActiveSessionRequest [account=%s, domain=%s, cos=%s]", account, domain, cos);
    }
}
