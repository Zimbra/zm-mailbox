/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account;

import java.util.Map;
import java.util.Random;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;

public class AuthTokenProperties implements Cloneable {

    public static final String C_TYPE_ZIMBRA_USER = "zimbra";
    public static final String C_TYPE_EXTERNAL_USER = "external";
    public static final String C_ID  = "id";
    // original admin id
    public static final String C_AID  = "aid";
    public static final String C_EXP = "exp";
    public static final String C_ADMIN = "admin";
    public static final String C_DOMAIN = "domain";
    public static final String C_DLGADMIN = "dlgadmin";
    public static final String C_TYPE = "type";
    public static final String C_EXTERNAL_USER_EMAIL = "email";
    public static final String C_DIGEST = "digest";
    public static final String C_VALIDITY_VALUE  = "vv";
    public static final String C_AUTH_MECH = "am";
    public static final String C_USAGE = "u";
    //cookie ID for keeping track of account's cookies
    public static final String C_TOKEN_ID = "tid";
    //mailbox server version where this account resides
    public static final String C_SERVER_VERSION = "version";
    public static final String C_CSRF = "csrf";
    public static final String C_KEY_VERSION = "kv";
 
    private String accountId;
    private String adminAccountId;
    private int validityValue = -1;
    private long expires;
    private String encoded;
    private boolean isAdmin;
    private boolean isDomainAdmin;
    private boolean isDelegatedAdmin;
    private String type;
    private String externalUserEmail;
    private String digest;
    private String accessKey; // just a dummy placeholder for now until accesskey auth is implemented in ZimbraAuthToken
    private String proxyAuthToken;
    private AuthMech authMech;
    private Integer tokenID = -1;
    private String server_version;   // version of the mailbox server where this account resides
    private boolean csrfTokenEnabled;
    private Usage usage; // what this token will be used for

    public AuthTokenProperties(Account acct, boolean isAdmin, Account adminAcct, long expires, AuthMech authMech, Usage usage) {
        if (acct != null) {
            accountId = acct.getId();
            validityValue = acct.getAuthTokenValidityValue();
            this.isAdmin = isAdmin && "TRUE".equals(acct.getAttr(Provisioning.A_zimbraIsAdminAccount));
            isDomainAdmin = isAdmin && "TRUE".equals(acct.getAttr(Provisioning.A_zimbraIsDomainAdminAccount));
            isDelegatedAdmin = isAdmin && "TRUE".equals(acct.getAttr(Provisioning.A_zimbraIsDelegatedAdminAccount));
            if (acct instanceof GuestAccount) {
                type = C_TYPE_EXTERNAL_USER;
                GuestAccount g = (GuestAccount) acct;
                digest = g.getDigest();
                accessKey = g.getAccessKey();
                externalUserEmail = g.getName();
            } else {
                type = C_TYPE_ZIMBRA_USER;
            }
            server_version = getServerVersion(acct);
        }
        adminAccountId = adminAcct != null ? adminAcct.getId() : null;
        this.expires = expires;
        this.authMech = authMech;
        this.usage = usage;
        encoded = null;
        tokenID = new Random().nextInt(Integer.MAX_VALUE-1) + 1;
    }

    //Zimbra Mobile Gateway (zmgApp) has been removed from the product as part of ZBUG-3249, however this Java signature is needed to compile zm-mailbox
    public AuthTokenProperties(String acctId, boolean zmgApp, String externalEmail, String pass, String digest, long expires) {
        accountId = acctId;
        this.expires = expires;
        // commented out as part of ZBUG-3249: externalUserEmail = externalEmail == null && !zmgApp ? "public" : externalEmail;
        this.digest = digest != null ? digest : AuthToken.generateDigest(externalEmail, pass);
        this.type = C_TYPE_EXTERNAL_USER;
        tokenID = new Random().nextInt(Integer.MAX_VALUE-1) + 1;
        try {
            Account acct = Provisioning.getInstance().getAccountById(accountId);
            if (acct != null) {
                server_version = getServerVersion(acct);
            }
        } catch (ServiceException e) {
            ZimbraLog.account.error("Unable to get the user account: %s", accountId, e);
        }
    }

    public AuthTokenProperties(Map<?, ?> map) throws AuthTokenException {
        accountId = (String) map.get(C_ID);
        adminAccountId = (String) map.get(C_AID);
        expires = Long.parseLong((String) map.get(C_EXP));
        String ia = (String) map.get(C_ADMIN);
        isAdmin = "1".equals(ia);
        String da = (String) map.get(C_DOMAIN);
        isDomainAdmin = "1".equals(da);
        String dlga = (String) map.get(C_DLGADMIN);
        isDelegatedAdmin = "1".equals(dlga);
        type = (String)map.get(C_TYPE);

        try {
            String authMechStr = (String)map.get(C_AUTH_MECH);
            authMech = AuthMech.fromString(authMechStr);
            String usageCode = (String)map.get(C_USAGE);
            if (usageCode != null) {
                usage = Usage.fromCode(usageCode);
            } else {
                usage = Usage.AUTH;
            }
        } catch (ServiceException e) {
            throw new AuthTokenException("service exception", e);
        }
        externalUserEmail = (String)map.get(C_EXTERNAL_USER_EMAIL);
        digest = (String)map.get(C_DIGEST);
        String vv = (String)map.get(C_VALIDITY_VALUE);
        if (vv != null) {
            try {
                validityValue = Integer.parseInt(vv);
            } catch (NumberFormatException e) {
                ZimbraLog.account.debug("invalid validity value : %s", vv, e);
                validityValue = -1;
            }
        } else {
            validityValue = -1;
        }

        String tid = (String)map.get(C_TOKEN_ID);
        if(tid !=null) {
            try {
                tokenID = Integer.parseInt(tid);
            } catch (NumberFormatException e) {
                ZimbraLog.account.debug("invalid token id : %s", tid, e);
                tokenID = -1;
            }
        } else {
            tokenID = -1;
        }
        server_version = (String)map.get(C_SERVER_VERSION);

        String csrf = (String) map.get(C_CSRF);
        if (csrf != null) {
            csrfTokenEnabled = "1".equals(map.get(C_CSRF));
        }
    }

    private String getServerVersion(Account acct) {
        String server_version = null;
        try {
            Server server = acct.getServer();
            if (server != null) {
                server_version = server.getServerVersion();
            } else {
                server_version = Provisioning.getInstance().getLocalServer().getServerVersion();
            }
        } catch (ServiceException e) {
            ZimbraLog.account.error("Unable to fetch server version for the user account", e);
        }
        return server_version;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getAdminAccountId() {
        return adminAccountId;
    }

    public int getValidityValue() {
        return validityValue;
    }

    public long getExpires() {
        return expires;
    }

    public String getEncoded() {
        return encoded;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isDomainAdmin() {
        return isDomainAdmin;
    }

    public boolean isDelegatedAdmin() {
        return isDelegatedAdmin;
    }

    public String getType() {
        return type;
    }

    public String getExternalUserEmail() {
        return externalUserEmail;
    }

    public String getDigest() {
        return digest;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getProxyAuthToken() {
        return proxyAuthToken;
    }

    public AuthMech getAuthMech() {
        return authMech;
    }

    public Integer getTokenID() {
        return tokenID;
    }

    public String getServerVersion() {
        return server_version;
    }

    public boolean isCsrfTokenEnabled() {
        return csrfTokenEnabled;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setEncoded(String encoded) {
        this.encoded = encoded;
    }

    public void setProxyAuthToken(String proxyAuthToken) {
        this.proxyAuthToken = proxyAuthToken;
    }

    public void setCsrfTokenEnabled(boolean csrfTokenEnabled) {
        this.csrfTokenEnabled = csrfTokenEnabled;
    }

    public void setTokenID(Integer tokenID) {
        this.tokenID = tokenID;
    }

    @Override
    public AuthTokenProperties clone() throws CloneNotSupportedException {
        return (AuthTokenProperties) super.clone();
    }
}
