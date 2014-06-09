package com.zimbra.soap.admin.message;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;

/**
 * @author gsolovyev
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Deregister authtokens that have been deregistered on the sending server
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_REFRESH_REGISTERED_AUTHTOKENS_REQUEST)
public class RefreshRegisteredAuthTokensRequest {
    public RefreshRegisteredAuthTokensRequest() {}

    /**
     * @zm-api-field-description Tokens
     */
    @XmlElement(name=AdminConstants.E_TOKEN /* token */, required=true)
    private List<String> tokens = Lists.newArrayList();

    public List<String> getTokens() {
        return tokens;
    }

    public void addToken(String token) {
        tokens.add(token);
    }
}
