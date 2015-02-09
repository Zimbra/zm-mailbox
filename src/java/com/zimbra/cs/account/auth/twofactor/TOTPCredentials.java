package com.zimbra.cs.account.auth.twofactor;

import java.util.HashSet;
import java.util.Set;

public class TOTPCredentials {
    private String secret;
    private Set<String> scratchCodes = new HashSet<String>();

    public TOTPCredentials(String secret, Set<String> scratchCodes) {
        this.secret = secret;
        if (scratchCodes != null && !scratchCodes.isEmpty()) {
            this.scratchCodes = scratchCodes;
        }
    }

    public TOTPCredentials(String secret) {
        this(secret, null);
    }

    public String getSecret() {
        return secret;
    }

    public Set<String> getScratchCodes() {
        return scratchCodes;
    }
}
