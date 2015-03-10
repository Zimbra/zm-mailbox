package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_GET_APP_SPECIFIC_PASSWORDS_REQUEST)
@XmlType(propOrder = {})
public class GetAppSpecificPasswordsRequest {

    public GetAppSpecificPasswordsRequest() {}
}
