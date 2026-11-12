package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-auth-required false
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Redeem the short-lived, single-use credential that the SAML login handed to the
 * Zimbra Connector for Outlook (ZCO) loopback listener. The caller proves it started the login by presenting
 * the verifier whose SHA-256 hash was sent as the challenge in the RelayState, and names the account it is
 * signing in to. On success the response is the same as <b>AuthResponse</b>. Every failure returns the same
 * generic authentication failure.
 */
@XmlRootElement(name = AccountConstants.E_EXCHANGE_AUTH_REQUEST)
@XmlType(propOrder = {})
public class ExchangeAuthRequest {

    @XmlElement(name = AccountConstants.E_CREDENTIAL /* credential */, required = true)
    private String credential;

    @XmlElement(name = AccountConstants.E_VERIFIER /* verifier */, required = true)
    private String verifier;

    @XmlElement(name = AccountConstants.E_ACCOUNT /* account */, required = true)
    private AccountSelector account;

    @XmlElement(name = AccountConstants.E_VIRTUAL_HOST /* virtualHost */, required = false)
    private String virtualHost;

    public ExchangeAuthRequest() {
    }

    public String getCredential() {
        return credential;
    }

    public ExchangeAuthRequest setCredential(String credential) {
        this.credential = credential; return this;
    }

    public String getVerifier() { return verifier; }

    public ExchangeAuthRequest setVerifier(String verifier) {
        this.verifier = verifier; return this;
    }

    public AccountSelector getAccount() { return account; }

    public ExchangeAuthRequest setAccount(AccountSelector account) { this.account = account; return this; }

    public String getVirtualHost() { return virtualHost; }

    public ExchangeAuthRequest setVirtualHost(String virtualHost) { this.virtualHost = virtualHost; return this; }
}
