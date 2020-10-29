package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_MIGRATE_USERS_DATA_RESPONSE)
public class MigrateUsersDataResponse {

    @XmlAttribute(name = "migId", required = false)
    private String id;

    public MigrateUsersDataResponse() {
        this(null);
    }

    public MigrateUsersDataResponse(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
