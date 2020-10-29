package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.NONE)
public class MigrationInfo {

    @XmlAttribute
    private String sourceUser;
    @XmlAttribute
    private String sourceUserPassword;
    @XmlAttribute
    private String targetUser;
    @XmlAttribute
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
