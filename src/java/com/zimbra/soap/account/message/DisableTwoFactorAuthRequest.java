package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_DISABLE_TWO_FACTOR_AUTH_REQUEST)
@XmlType(propOrder = {})
public class DisableTwoFactorAuthRequest {
}
