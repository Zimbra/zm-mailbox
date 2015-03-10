package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_REVOKE_APP_SPECIFIC_PASSWORD_RESPONSE)
@XmlType(propOrder = {})
public class RevokeAppSpecificPasswordResponse {

    public RevokeAppSpecificPasswordResponse() {}
}
