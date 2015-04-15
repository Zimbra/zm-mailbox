package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_GET_TRUSTED_DEVICES_REQUEST)
public class GetTrustedDevicesRequest {
    public GetTrustedDevicesRequest() {}
}
