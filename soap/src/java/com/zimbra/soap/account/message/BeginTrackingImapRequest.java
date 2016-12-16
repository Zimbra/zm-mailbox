package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_BEGIN_TRACKING_IMAP_REQUEST)
public class BeginTrackingImapRequest {
    public BeginTrackingImapRequest() {};
}
