package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class MigrationInfo {

    /**
     * @zm-api-field-tag sourceUser
     * @zm-api-field-description source System user account from where data will be migrated
     */
    @XmlAttribute(name = AdminConstants.A_SOURCE_USER, required = true)
    private String sourceUser;

    @XmlAttribute(name = AdminConstants.A_SOURCE_USER_PASSWORD, required = false)
    private String sourceUserPassword;

    /**
     * @zm-api-field-tag targetUser
     * @zm-api-field-description Target system user account to where data will be migrated
     */
    @XmlAttribute(name = AdminConstants.A_TARGET_USER, required = true)
    private String targetUser;

    /**
     * @zm-api-field-tag typesOfdata
     * @zm-api-field-description Which types of data is to be migrated.
     * e.g; imap(mail), caldav(calendar), contact, file(document), task
     */
    @XmlAttribute(name = AdminConstants.A_TYPES_OF_DATA, required = true)
    private String typesOfData;

    public MigrationInfo() {
        this ((String) null, (String) null, (String) null, (String) null);
    }

    public MigrationInfo(String sUser, String sPass, String tUser, String types) {
        this.sourceUser = sUser;
        this.sourceUserPassword = sPass;
        this.targetUser = tUser;
        this.typesOfData = types;
    }

    public String getSourceUser() {
        return sourceUser;
    }

    public void setSourceUser(String sourceUser) {
        this.sourceUser = sourceUser;
    }

    public String getSourceUserPassword() {
        return sourceUserPassword;
    }

    public void setSourceUserPassword(String sourceUserPassword) {
        this.sourceUserPassword = sourceUserPassword;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(String targetUser) {
        this.targetUser = targetUser;
    }

    public String getTypesOfData() {
        return typesOfData;
    }

    public void setTypesOfData(String typesOfData) {
        this.typesOfData = typesOfData;
    }
}
