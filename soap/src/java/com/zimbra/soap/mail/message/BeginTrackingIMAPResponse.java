package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_BEGIN_TRACKING_IMAP_RESPONSE)
public class BeginTrackingIMAPResponse {
    public BeginTrackingIMAPResponse() {};
}
