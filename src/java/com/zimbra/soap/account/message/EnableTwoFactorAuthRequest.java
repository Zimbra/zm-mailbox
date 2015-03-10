package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_ENABLE_TWO_FACTOR_AUTH_REQUEST)
@XmlType(propOrder = {})
public class EnableTwoFactorAuthRequest {

    public EnableTwoFactorAuthRequest() {}

    /**
     * @zm-api-field-description The name of the account for which to enable two-factor auth
     */
    @XmlElement(name=AccountConstants.A_NAME, required=true)
    private String acctName;

    /**
     * @zm-api-field-description Password to use in conjunction with an account
     */
    @XmlElement(name=AccountConstants.A_PASSWORD, required=true)
    private String password;

    public String getPassword() { return password; }
    public EnableTwoFactorAuthRequest setPassword(String password) { this.password = password; return this; }

    public String getName() { return acctName; }
    public EnableTwoFactorAuthRequest setName(String acctName) { this.acctName = acctName; return this; }
}