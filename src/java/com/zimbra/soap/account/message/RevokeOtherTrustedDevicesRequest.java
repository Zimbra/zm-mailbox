package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_REVOKE_OTHER_TRUSTED_DEVICES_REQUEST)
public class RevokeOtherTrustedDevicesRequest {

    public RevokeOtherTrustedDevicesRequest() {}
}
