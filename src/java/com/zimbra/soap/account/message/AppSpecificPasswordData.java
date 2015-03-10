package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AccountConstants;

public class AppSpecificPasswordData {

    @XmlAttribute(name=AccountConstants.A_APP_NAME)
    private String appName;

    @XmlAttribute(name=AccountConstants.A_DATE_CREATED)
    private Long dateCreated;

    @XmlAttribute(name=AccountConstants.A_DATE_LAST_USED)
    private Long dateLastUsed;

    public void setAppName(String appName) { this.appName = appName; }
    public String getAppName() { return appName; }

    public void setDateCreated(Long dateCreated) { this.dateCreated = dateCreated; }
    public Long getDateCreated() { return dateCreated; }

    public void setDateLastUsed(Long dateLastUsed) { this.dateLastUsed = dateLastUsed; }
    public Long getDateLastUsed() { return dateLastUsed; }
}
