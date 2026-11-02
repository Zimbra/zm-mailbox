package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.AccountSelector;

@XmlRootElement(name=AccountConstants.E_EXCHANGE_AUTH_REQUEST)
@XmlType(propOrder = {})
public class ExchangeAuthRequest {

    @XmlElement(name=AccountConstants.E_CREDENTIAL, required=true)
    private String credential;

    @XmlElement(name=AccountConstants.E_VERIFIER, required=true)
    private String verifier;

    @XmlElement(name=AccountConstants.E_ACCOUNT, required=true)
    private AccountSelector account;

    @XmlElement(name=AccountConstants.E_VIRTUAL_HOST, required=false)
    private String virtualHost;

    public ExchangeAuthRequest() {
    }

    public String getCredential() { return credential; }
    public ExchangeAuthRequest setCredential(String credential) { this.credential = credential; return this; }

    public String getVerifier() { return verifier; }
    public ExchangeAuthRequest setVerifier(String verifier) { this.verifier = verifier; return this; }

    public AccountSelector getAccount() { return account; }
    public ExchangeAuthRequest setAccount(AccountSelector account) { this.account = account; return this; }

    public String getVirtualHost() { return virtualHost; }
    public ExchangeAuthRequest setVirtualHost(String virtualHost) { this.virtualHost = virtualHost; return this; }
}