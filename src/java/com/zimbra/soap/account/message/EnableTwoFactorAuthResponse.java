package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_ENABLE_TWO_FACTOR_AUTH_RESPONSE)
@XmlType(propOrder = {})
public class EnableTwoFactorAuthResponse {

    @XmlElement(name=AccountConstants.E_TWO_FACTOR_CREDENTIALS, type=TwoFactorCredentials.class)
    private TwoFactorCredentials credentials;

    public TwoFactorCredentials getCredentials() {return credentials; }
    public void setCredentials(TwoFactorCredentials credentials) {this.credentials = credentials; }
}
