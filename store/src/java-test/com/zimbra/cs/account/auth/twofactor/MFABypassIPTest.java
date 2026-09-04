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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Functional tests for {@link MFABypassIP} over the in-memory MockProvisioning harness.
 *
 * Covers where the range list is read from (COS ahead of domain), which of the two context
 * addresses is evaluated, and the fail-closed behaviour on every bad input.
 */
public class MFABypassIPTest {

    private static final String DOMAIN = "bypass-test.zimbra.com";

    private Provisioning prov;
    private Domain domain;
    private int uniquifier;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        // A fresh mock per test so a COS created by one test cannot be picked up as the
        // fallback COS of another.
        prov = new MockProvisioning();
        Provisioning.setInstance(prov);
        domain = prov.createDomain(DOMAIN, new HashMap<String, Object>());
    }

    //  Helpers ------------------------------------------------------------

    /** An account with no COS, so the domain list is the only one in play. */
    private Account accountWithoutCos() throws Exception {
        return prov.createAccount("user" + (++uniquifier) + "@" + DOMAIN, "test123",
                new HashMap<String, Object>());
    }

    /** An account whose COS carries the given ranges. Pass no ranges for an empty COS. */
    private Account accountWithCosRanges(String... ranges) throws Exception {
        Map<String, Object> cosAttrs = new HashMap<String, Object>();
        if (ranges.length > 0) {
            cosAttrs.put(Provisioning.A_zimbraMFAbyPassIP, ranges);
        }
        Cos cos = prov.createCos("cos" + (++uniquifier), cosAttrs);

        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        acctAttrs.put(Provisioning.A_zimbraCOSId, cos.getId());
        return prov.createAccount("user" + uniquifier + "@" + DOMAIN, "test123", acctAttrs);
    }

    private void setDomainRanges(String... ranges) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMFAbyPassIP, ranges);
        prov.modifyAttrs(domain, attrs);
    }

    /** Context as the SOAP layer builds it behind a trusted proxy. */
    private Map<String, Object> ctx(String originatingIp, String peerIp) {
        Map<String, Object> authCtxt = new HashMap<String, Object>();
        authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, originatingIp);
        authCtxt.put(AuthContext.AC_REMOTE_IP, peerIp);
        return authCtxt;
    }

    /** Context as it looks with no proxy in front: peer address only. */
    private Map<String, Object> directCtx(String peerIp) {
        return ctx(null, peerIp);
    }

    //  Nothing configured -------------------------------------------------

    @Test
    public void noRangesAnywhereLeavesMfaInPlace() throws Exception {
        Account acct = accountWithoutCos();
        Assert.assertFalse(MFABypassIP.isBypassed(acct, directCtx("10.1.2.3")));
    }

    @Test
    public void emptyCosAndEmptyDomainLeaveMfaInPlace() throws Exception {
        Account acct = accountWithCosRanges();
        Assert.assertFalse(MFABypassIP.isBypassed(acct, directCtx("10.1.2.3")));
    }

    //  COS level ----------------------------------------------------------

    @Test
    public void cosRangeBypassesMatchingAddress() throws Exception {
        Account acct = accountWithCosRanges("10.0.0.0/8");
        Assert.assertTrue(MFABypassIP.isBypassed(acct, directCtx("10.1.2.3")));
    }

    @Test
    public void cosRangeDoesNotBypassOutsideAddress() throws Exception {
        Account acct = accountWithCosRanges("10.0.0.0/8");
        Assert.assertFalse(MFABypassIP.isBypassed(acct, directCtx("203.0.113.7")));
    }

    @Test
    public void anyOfSeveralCosRangesCanMatch() throws Exception {
        Account acct = accountWithCosRanges("10.0.0.0/8", "192.168.1.0/24", "2001:db8::/32");
        Assert.assertTrue(MFABypassIP.isBypassed(acct, directCtx("192.168.1.9")));
        Assert.assertTrue(MFABypassIP.isBypassed(acct, directCtx("2001:db8::abcd")));
        Assert.assertFalse(MFABypassIP.isBypassed(acct, directCtx("192.168.2.9")));
    }

    //  Domain level and precedence ----------------------------------------

    @Test
    public void domainRangeAppliesWhenAccountHasNoCos() throws Exception {
        Account acct = accountWithoutCos();
        setDomainRanges("172.16.0.0/12");
        Assert.assertTrue(MFABypassIP.isBypassed(acct, directCtx("172.16.4.5")));
        Assert.assertFalse(MFABypassIP.isBypassed(acct, directCtx("172.32.4.5")));
    }

    @Test
    public void domainRangeAppliesWhenCosListIsEmpty() throws Exception {
        Account acct = accountWithCosRanges();
        setDomainRanges("172.16.0.0/12");
        Assert.assertTrue(MFABypassIP.isBypassed(acct, directCtx("172.16.4.5")));
    }

    @Test
    public void cosTakesPrecedenceWhenItMatches() throws Exception {
        Account acct = accountWithCosRanges("10.0.0.0/8");
        setDomainRanges("192.168.0.0/16");
        Assert.assertTrue(MFABypassIP.isBypassed(acct, directCtx("10.1.2.3")));
    }

    @Test
    public void cosReplacesDomainRatherThanUnioningWithIt() throws Exception {
        // The decisive precedence test: the domain would bypass this address, but a COS
        // that configures any range at all decides on its own.
        Account acct = accountWithCosRanges("10.0.0.0/8");
        setDomainRanges("192.168.0.0/16");
        Assert.assertFalse(MFABypassIP.isBypassed(acct, directCtx("192.168.1.9")));
    }

    //  Which address is evaluated -----------------------------------------

    @Test
    public void originatingAddressWinsOverPeerAddress() throws Exception {
        Account acct = accountWithCosRanges("10.0.0.0/8");
        // Proxy peer is outside the range, real client is inside.
        Assert.assertTrue(MFABypassIP.isBypassed(acct, ctx("10.1.2.3", "203.0.113.1")));
    }

    @Test
    public void proxyAddressIsNotMistakenForTheClient() throws Exception {
        Account acct = accountWithCosRanges("10.0.0.0/8");
        // The proxy itself sits inside the whitelist but the real client does not: the
        // client must still be challenged.
        Assert.assertFalse(MFABypassIP.isBypassed(acct, ctx("203.0.113.1", "10.0.0.9")));
    }

    @Test
    public void peerAddressIsUsedWhenNoOriginatingAddress() throws Exception {
        Account acct = accountWithCosRanges("10.0.0.0/8");
        Assert.assertTrue(MFABypassIP.isBypassed(acct, directCtx("10.1.2.3")));
    }

    @Test
    public void blankOriginatingAddressFallsBackToPeerAddress() throws Exception {
        Account acct = accountWithCosRanges("10.0.0.0/8");
        Assert.assertTrue(MFABypassIP.isBypassed(acct, ctx("", "10.1.2.3")));
        Assert.assertTrue(MFABypassIP.isBypassed(acct, ctx("   ", "10.1.2.3")));
    }

    //  Fail-closed paths --------------------------------------------------

    @Test
    public void noAddressAtAllLeavesMfaInPlace() throws Exception {
        Account acct = accountWithCosRanges("10.0.0.0/8");
        Assert.assertFalse(MFABypassIP.isBypassed(acct, ctx(null, null)));
        Assert.assertFalse(MFABypassIP.isBypassed(acct, ctx("", "")));
        Assert.assertFalse(MFABypassIP.isBypassed(acct, new HashMap<String, Object>()));
    }

    @Test
    public void nullContextLeavesMfaInPlace() throws Exception {
        Account acct = accountWithCosRanges("10.0.0.0/8");
        Assert.assertFalse(MFABypassIP.isBypassed(acct, null));
    }

    @Test
    public void nullAccountLeavesMfaInPlace() throws Exception {
        Assert.assertFalse(MFABypassIP.isBypassed(null, directCtx("10.1.2.3")));
    }

    @Test
    public void unparseableAddressLeavesMfaInPlace() throws Exception {
        Account acct = accountWithCosRanges("10.0.0.0/8");
        Assert.assertFalse(MFABypassIP.isBypassed(acct, directCtx("not-an-ip")));
        Assert.assertFalse(MFABypassIP.isBypassed(acct, directCtx("10.0.0.256")));
        // Must never be resolved as a hostname.
        Assert.assertFalse(MFABypassIP.isBypassed(acct, directCtx("localhost")));
    }

    @Test
    public void oneBadRangeDoesNotStopTheOthersMatching() throws Exception {
        Account acct = accountWithCosRanges("not-a-cidr", "10.0.0.0/8");
        Assert.assertTrue(MFABypassIP.isBypassed(acct, directCtx("10.1.2.3")));
    }

    @Test
    public void oneBadRangeDoesNotBypassByItself() throws Exception {
        Account acct = accountWithCosRanges("10.0.0.0/999", "garbage", "");
        Assert.assertFalse(MFABypassIP.isBypassed(acct, directCtx("10.1.2.3")));
    }

    @Test
    public void badRangeOnCosStillSuppressesTheDomainList() throws Exception {
        // A COS that configures anything -- even something unusable -- owns the decision,
        // so a mistyped COS range must not silently fall through to a broader domain list.
        Account acct = accountWithCosRanges("garbage");
        setDomainRanges("10.0.0.0/8");
        Assert.assertFalse(MFABypassIP.isBypassed(acct, directCtx("10.1.2.3")));
    }

    //  Address forms ------------------------------------------------------

    @Test
    public void ipv6ClientMatchesIpv6Range() throws Exception {
        Account acct = accountWithCosRanges("2001:db8::/32");
        Assert.assertTrue(MFABypassIP.isBypassed(acct, directCtx("2001:db8::1")));
        Assert.assertFalse(MFABypassIP.isBypassed(acct, directCtx("2001:db9::1")));
    }

    @Test
    public void ipv4MappedClientMatchesIpv4Range() throws Exception {
        Account acct = accountWithCosRanges("10.0.0.0/8");
        Assert.assertTrue(MFABypassIP.isBypassed(acct, directCtx("::ffff:10.1.2.3")));
    }

    @Test
    public void bracketedIpv6LoopbackMatches() throws Exception {
        // RemoteIP reports IPv6 loopback in this bracketed form.
        Account acct = accountWithCosRanges("::1");
        Assert.assertTrue(MFABypassIP.isBypassed(acct, directCtx("[0:0:0:0:0:0:0:1]")));
    }

    @Test
    public void bareAddressActsAsSingleHost() throws Exception {
        Account acct = accountWithCosRanges("192.168.1.50");
        Assert.assertTrue(MFABypassIP.isBypassed(acct, directCtx("192.168.1.50")));
        Assert.assertFalse(MFABypassIP.isBypassed(acct, directCtx("192.168.1.51")));
    }

    @Test
    public void ipv4RangeDoesNotBypassIpv6Client() throws Exception {
        Account acct = accountWithCosRanges("0.0.0.0/0");
        Assert.assertTrue(MFABypassIP.isBypassed(acct, directCtx("203.0.113.1")));
        Assert.assertFalse(MFABypassIP.isBypassed(acct, directCtx("2001:db8::1")));
    }
}
