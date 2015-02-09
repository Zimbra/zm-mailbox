package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_TWO_FACTOR_AUTH_REQUEST)
@XmlType(propOrder = {})
public class TwoFactorAuthRequest {

    public TwoFactorAuthRequest() {}

    @XmlElement(name=AccountConstants.E_ACTION, required=false)
    private String action;

    public String getAction() { return action; }
    public TwoFactorAuthRequest setAction(String action) { this.action = action; return this; }
}