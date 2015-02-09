package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;

@XmlRootElement(name=AccountConstants.E_TWO_FACTOR_AUTH_RESPONSE)
@XmlType(propOrder = {})
public class TwoFactorAuthResponse {

    @XmlElement(name=AccountConstants.E_TWO_FACTOR_CREDENTIALS, type=TwoFactorCredentials.class)
    private TwoFactorCredentials credentials;

    @XmlElement(name=MailConstants.E_INFO, required=false)
    private String infoString;

    public TwoFactorCredentials getCredentials() {return credentials; }
    public void setCredentials(TwoFactorCredentials credentials) {this.credentials = credentials; }

    public String getInfo() {return infoString; }
    public TwoFactorAuthResponse setInfo(String info) {this.infoString = info; return this; }
}
