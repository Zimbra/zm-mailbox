package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

/**
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Validate remote zimbra system connectivity and admin credentials
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_VALIDATE_REMOTE_ZIMBRA_CONNECTION_REQUEST)
public class ValidateRemoteZimbraConnectionRequest {

    /**
     * @zm-api-field-tag sourceHost
     * @zm-api-field-description Source zimbra system host address
     */
    @XmlAttribute(name = AdminConstants.A_SOURCE_HOST, required = true)
    private String sourceHost;

    /**
     * @zm-api-field-tag sourceAdminUserName
     * @zm-api-field-description Source zimbra admin user name to validate
     */
    @XmlAttribute(name = AdminConstants.A_SOURCE_ADMIN_USER, required = true)
    private String sourceAdminUserName;

    /**
     * @zm-api-field-tag sourceAdminPassword
     * @zm-api-field-description Source zimbra admin password to validate
     */
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
