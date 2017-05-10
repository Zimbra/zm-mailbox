package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlRootElement(name=MailConstants.E_BEGIN_TRACKING_IMAP_REQUEST)
public class BeginTrackingIMAPRequest {
    public BeginTrackingIMAPRequest() {};
}
