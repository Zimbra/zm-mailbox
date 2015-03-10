package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_CREATE_APP_SPECIFIC_PASSWORD_REQUEST)
@XmlType(propOrder = {})
public class CreateAppSpecificPasswordRequest {

    public CreateAppSpecificPasswordRequest() {}

    public CreateAppSpecificPasswordRequest(String appName) {
        setAppName(appName);
    }

    @XmlAttribute(name=AccountConstants.A_APP_NAME)
    private String appName;

    public void setAppName(String name) {this.appName = name; }
    public String getAppName() {return appName; }
}
