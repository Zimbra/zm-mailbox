package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_CREATE_APP_SPECIFIC_PASSWORD_RESPONSE)
@XmlType(propOrder = {})
public class CreateAppSpecificPasswordResponse {

    public CreateAppSpecificPasswordResponse() {}

    @XmlAttribute(name=AccountConstants.A_PASSWORD)
    private String password;

    public void setPassword(String password) {this.password = password; }
    public String getPassword() {return password; }
}
