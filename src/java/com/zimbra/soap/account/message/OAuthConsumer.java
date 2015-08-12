package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAttribute;
import com.zimbra.common.soap.AccountConstants;

public class OAuthConsumer {
    @XmlAttribute(name=AccountConstants.A_ACCESS_TOKEN)
    private String accessToken;

    @XmlAttribute(name=AccountConstants.A_APPROVED_ON)
    private String approvedOn;

    @XmlAttribute(name=AccountConstants.A_CONSUMER_APP_NAME)
    private String applicationName;

    @XmlAttribute(name=AccountConstants.A_CONSUMER_DEVICE)
    private String device;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getApprovedOn() { return approvedOn; }
    public void setApprovedOn(String approvedOn) { this.approvedOn = approvedOn; }

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }
}
