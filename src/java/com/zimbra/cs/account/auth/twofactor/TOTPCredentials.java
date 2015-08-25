package com.zimbra.cs.account.auth.twofactor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.zimbra.cs.ldap.LdapDateUtil;

public class TOTPCredentials {
    private String secret;
    private List<String> scratchCodes = new ArrayList<String>();
    private String timestamp;

    public TOTPCredentials(String secret, List<String> scratchCodes) {
        this.secret = secret;
        if (scratchCodes != null && !scratchCodes.isEmpty()) {
            this.scratchCodes = scratchCodes;
        }
        this.timestamp = LdapDateUtil.toGeneralizedTime(new Date());
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

    public String getTimestamp() {
        return timestamp;
    }
}
