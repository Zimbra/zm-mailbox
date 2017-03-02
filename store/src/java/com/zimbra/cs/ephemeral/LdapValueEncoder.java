package com.zimbra.cs.ephemeral;

import com.zimbra.cs.account.Provisioning;

/**
 * Value encoder that encodes auth and CSRF tokens in a backwards-compatible format.
 * @author iraykin
 *
 */
public class LdapValueEncoder extends DynamicExpirationValueEncoder {

    @Override
    public String encodeValue(EphemeralInput input, EphemeralLocation target) {
        String key = input.getEphemeralKey().getKey();
        if (key.equalsIgnoreCase(Provisioning.A_zimbraAuthTokens)) {
            return encodeAuthToken(input);
        } else if (key.equalsIgnoreCase(Provisioning.A_zimbraCsrfTokenData)) {
            return encodeCsrfToken(input);
        } else {
            return super.encodeValue(input, target);
        }
    }

    private String encodeAuthToken(EphemeralInput input) {
        //legacy format for auth tokens was tokenID|expiration|serverVersion
        String tokenId = input.getEphemeralKey().getDynamicComponent();
        String expiration = String.valueOf(input.getExpiration());
        String serverVersion = (String) input.getValue();
        return String.format("%s|%s|%s", tokenId, expiration, serverVersion);
    }

    private String encodeCsrfToken(EphemeralInput input) {
        //legacy format for CSRF tokens was crumb:data:expiration
        String crumb = input.getEphemeralKey().getDynamicComponent();
        String data = (String) input.getValue();
        String expiration = String.valueOf(input.getExpiration());
        return String.format("%s:%s:%s", data, crumb, expiration);
    }
}
