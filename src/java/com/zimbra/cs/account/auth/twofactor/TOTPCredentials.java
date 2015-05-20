package com.zimbra.cs.account.auth.twofactor;

import java.util.ArrayList;
import java.util.List;

public class TOTPCredentials {
    private String secret;
    private List<String> scratchCodes = new ArrayList<String>();

    public TOTPCredentials(String secret, List<String> scratchCodes) {
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

    public List<String> getScratchCodes() {
        return scratchCodes;
    }
}
