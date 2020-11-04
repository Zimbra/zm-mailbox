package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_VALIDATE_REMOTE_ZIMBRA_CONNECTION_REQUEST)
public class ValidateRemoteZimbraConnectionRequest {

    @XmlAttribute(name = AdminConstants.A_SOURCE_HOST, required = true)
    private String sourceHost;
    @XmlAttribute(name = AdminConstants.A_SOURCE_ADMIN_USER, required = true)
    private String sourceAdminUserName;
    @XmlAttribute(name = AdminConstants.A_SOURCE_ADMIN_PASSWORD, required = true)
    private String sourceAdminPassword;

    public ValidateRemoteZimbraConnectionRequest() {
        this(null, null, null);
    }

    public ValidateRemoteZimbraConnectionRequest(String host, String username, String password) {
        this.sourceHost = host;
        this.sourceAdminUserName = username;
        this.sourceAdminPassword = password;
    }

    public String getSourceHost() {
        return sourceHost;
    }

    public void setSourceHost(String sourceHost) {
        this.sourceHost = sourceHost;
    }

    public String getSourceAdminUserName() {
        return sourceAdminUserName;
    }

    public void setSourceAdminUserName(String sourceAdminUserName) {
        this.sourceAdminUserName = sourceAdminUserName;
    }

    public String getSourceAdminPassword() {
        return sourceAdminPassword;
    }

    public void setSourceAdminPassword(String sourceAdminPassword) {
        this.sourceAdminPassword = sourceAdminPassword;
    }
}
