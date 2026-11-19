package com.zimbra.soap.account.message;

import com.zimbra.common.soap.AccountConstants;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Response to {@code CredentialExchangeAuthRequest}. It has the same content as {@code AuthResponse}.
 */
@XmlRootElement(name = AccountConstants.E_CREDENTIAL_EXCHANGE_AUTH_RESPONSE)
@XmlType(propOrder = {})
public class CredentialExchangeAuthResponse extends AuthResponse {
}
