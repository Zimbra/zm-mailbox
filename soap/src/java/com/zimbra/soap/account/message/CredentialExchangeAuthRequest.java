package com.zimbra.soap.account.message;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.AccountSelector;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @zm-api-command-auth-required false
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Redeem the short-lived, single-use credential that the SAML login handed to the
 * client's loopback listener. The caller proves it started the login by presenting the verifier whose SHA-256
 * hash was sent as the challenge in the RelayState, and names the account it is signing in to. On success the
 * response is the same as <b>AuthResponse</b>. Every failure returns the same generic authentication failure.
 */
@XmlRootElement(name = AccountConstants.E_CREDENTIAL_EXCHANGE_AUTH_REQUEST)
@XmlType(propOrder = {})
public class CredentialExchangeAuthRequest {

    /**
     * @zm-api-field-tag credential
     * @zm-api-field-description Single-use exchange credential (<b>zcx1.</b> + 43 base64url characters)
     *     received by the loopback listener
     */
    @XmlElement(name = AccountConstants.E_CREDENTIAL, required = true)
    private String credential;

    /**
     * @zm-api-field-tag verifier
     * @zm-api-field-description Verifier (43 base64url characters) whose SHA-256 was sent as the challenge
     */
    @XmlElement(name = AccountConstants.E_VERIFIER, required = true)
    private String verifier;

    /**
     * @zm-api-field-description Account the credential was issued for
     */
    @XmlElement(name = AccountConstants.E_ACCOUNT, required = true)
    private AccountSelector account;

    /**
     * @zm-api-field-tag virtual-host
     * @zm-api-field-description Virtual host, used to complete an account name without a domain
     */
    @XmlElement(name = AccountConstants.E_VIRTUAL_HOST, required = false)
    private String virtualHost;

    public CredentialExchangeAuthRequest() {
    }

    public String getCredential() {
        return credential;
    }

    public CredentialExchangeAuthRequest setCredential(String credential) {
        this.credential = credential;
        return this;
    }

    public String getVerifier() {
        return verifier;
    }

    public CredentialExchangeAuthRequest setVerifier(String verifier) {
        this.verifier = verifier;
        return this;
    }

    public AccountSelector getAccount() {
        return account;
    }

    public CredentialExchangeAuthRequest setAccount(AccountSelector account) {
        this.account = account;
        return this;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public CredentialExchangeAuthRequest setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
        return this;
    }
}
