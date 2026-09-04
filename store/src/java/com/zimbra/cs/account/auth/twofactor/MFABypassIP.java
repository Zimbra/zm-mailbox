/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.auth.twofactor;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CidrMatcher;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthContext;

/**
 * Evaluates the {@code zimbraMFAbyPassIP} whitelist for an authenticating account.
 *
 * When the account's source address falls inside one of the configured CIDR ranges, the
 * two-factor auth challenge is skipped. The password is still validated by the normal auth
 * path; only the second factor is bypassed.
 *
 * Ranges are read from the COS first and, only if the COS has none, from the domain. Every
 * failure mode is fail-closed: if the source address cannot be determined, if a configured
 * range is unparseable, or if provisioning lookup fails, the challenge is issued as usual.
 */
public final class MFABypassIP {

    private MFABypassIP() {
    }

    /**
     * @param acct the authenticating account
     * @param authCtxt the auth context populated by the calling auth path; supplies the
     *                 originating and peer IP addresses
     * @return true if the MFA challenge should be skipped for this login
     */
    public static boolean isBypassed(Account acct, Map<String, Object> authCtxt) {
        if (acct == null) {
            return false;
        }
        try {
            Ranges ranges = resolveRanges(acct);
            if (ranges == null) {
                // Nothing configured anywhere: behave exactly as before the feature existed.
                return false;
            }

            String originatingIp = asString(authCtxt, AuthContext.AC_ORIGINATING_CLIENT_IP);
            String peerIp = asString(authCtxt, AuthContext.AC_REMOTE_IP);
            // A present-but-blank originating header must not suppress the fallback to the
            // peer address, or a client on a whitelisted network would be challenged.
            String clientIp = originatingIp != null ? originatingIp : peerIp;

            if (clientIp == null) {
                ZimbraLog.security.warn(
                        "MFA IP bypass: no source IP available for account %s, issuing MFA challenge", acct.getName());
                return false;
            }

            String matched = firstMatch(ranges, clientIp);

            // Logged whenever the feature is configured, so a deployment can be checked
            // against what the server actually sees behind its proxy.
            ZimbraLog.security.info(
                    "MFA IP bypass: account=%s originatingIP=%s peerIP=%s evaluated=%s %s=%s bypass=%s%s",
                    acct.getName(), originatingIp, peerIp, clientIp, ranges.level, ranges.entryName,
                    matched != null, matched == null ? "" : " matchedRange=" + matched);

            return matched != null;
        } catch (ServiceException e) {
            ZimbraLog.security.warn(
                    "MFA IP bypass: evaluation failed for account %s, issuing MFA challenge", acct.getName(), e);
            return false;
        }
    }

    private static String firstMatch(Ranges ranges, String clientIp) {
        for (String spec : ranges.cidrs) {
            if (StringUtil.isNullOrEmpty(spec)) {
                continue;
            }
            CidrMatcher matcher;
            try {
                matcher = CidrMatcher.parse(spec);
            } catch (IllegalArgumentException e) {
                // One bad value must not decide the outcome for the whole list either way.
                ZimbraLog.security.warn("MFA IP bypass: ignoring unparseable range '%s' on %s %s: %s",
                        spec, ranges.level, ranges.entryName, e.getMessage());
                continue;
            }
            if (matcher.matches(clientIp)) {
                return spec;
            }
        }
        return null;
    }

    /**
     * COS takes precedence over domain: a COS that configures any range decides the policy
     * for its accounts on its own, rather than being widened by the domain's list.
     */
    private static Ranges resolveRanges(Account acct) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();

        Cos cos = prov.getCOS(acct);
        if (cos != null) {
            String[] cosRanges = cos.getMultiAttr(Provisioning.A_zimbraMFAbyPassIP);
            if (cosRanges != null && cosRanges.length > 0) {
                return new Ranges(cosRanges, "cos", cos.getName());
            }
        }

        Domain domain = prov.getDomain(acct);
        if (domain != null) {
            String[] domainRanges = domain.getMultiAttr(Provisioning.A_zimbraMFAbyPassIP);
            if (domainRanges != null && domainRanges.length > 0) {
                return new Ranges(domainRanges, "domain", domain.getName());
            }
        }

        return null;
    }

    /**
     * @return the context value as a trimmed string, or null if absent or blank
     */
    private static String asString(Map<String, Object> authCtxt, String key) {
        if (authCtxt == null) {
            return null;
        }
        Object value = authCtxt.get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private static class Ranges {
        private final String[] cidrs;
        private final String level;
        private final String entryName;

        Ranges(String[] cidrs, String level, String entryName) {
            this.cidrs = cidrs;
            this.level = level;
            this.entryName = entryName;
        }
    }
}
