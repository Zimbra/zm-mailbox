/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link DomainAccessManager}, exercised through real
 * {@link Account} and {@link Domain} domain objects from the in-memory
 * MockProvisioning harness. Focuses on the credential-based access checks
 * (the {@code (Account, Account, boolean)} overloads), the admin-adequacy
 * predicate, and the static quota-limit logic, asserting the full decision
 * matrix (admin, domain-admin, parent, same/different domain).
 */
public class DomainAccessManagerTest {

    private DomainAccessManager mgr;

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        mgr = new DomainAccessManager();
        prov.createDomain("example.com", new HashMap<String, Object>());
        prov.createDomain("other.com", new HashMap<String, Object>());
    }

    private Account account(String name, Map<String, Object> attrs) throws Exception {
        return prov.createAccount(name, "secret", attrs);
    }

    /* Create an account with an explicit, stable zimbraId so an AuthToken can reload it. */
    private Account accountWithId(String name, String id, Map<String, Object> attrs) throws Exception {
        attrs.put(Provisioning.A_zimbraId, id);
        return prov.createAccount(name, "secret", attrs);
    }

    private AuthToken token(Account acct, boolean isAdmin) {
        return new ZimbraAuthToken(acct, isAdmin, null);
    }

    /* Create a domain whose status is "suspended" so checkDomainStatus() rejects it. */
    private void suspendedDomain(String name) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_SUSPENDED);
        prov.createDomain(name, attrs);
    }

    /*
     * Attaches an in-memory log4j2 appender to the "zimbra.account" logger and
     * returns the live list its captured messages are appended to.
     */
    private static List<String> captureAccountLog() {
        final List<String> messages = new CopyOnWriteArrayList<String>();
        AbstractAppender appender = new AbstractAppender("capture-" + System.nanoTime(),
                null, null, true, null) {
            @Override
            public void append(LogEvent event) {
                messages.add(event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        // Ensure a dedicated logger config exists for "zimbra.account" at WARN (the root
        // config sits at ERROR and would otherwise filter the warning out), then attach.
        Configurator.setLevel("zimbra.account", Level.WARN);
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        LoggerConfig cfg = ctx.getConfiguration().getLoggerConfig("zimbra.account");
        cfg.setLevel(Level.WARN);
        cfg.addAppender(appender, Level.WARN, null);
        ctx.updateLoggers();
        return messages;
    }

    @Test
    public void isAdequateAdminAccountGlobalAdminReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = account("ga@example.com", attrs);

        // Act + Assert
        assertTrue("a global admin is an adequate admin account",
                mgr.isAdequateAdminAccount(admin));
    }

    @Test
    public void isAdequateAdminAccountDomainAdminReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account da = account("da@example.com", attrs);

        // Act + Assert
        assertTrue("a domain admin is an adequate admin account",
                mgr.isAdequateAdminAccount(da));
    }

    @Test
    public void isAdequateAdminAccountPlainUserReturnsFalse() throws Exception {
        // Arrange
        Account user = account("plain@example.com", new HashMap<String, Object>());

        // Act + Assert
        assertFalse("a non-admin account is not adequate", mgr.isAdequateAdminAccount(user));
    }

    @Test
    public void canAccessAccountNullCredentialsReturnsFalse() throws Exception {
        // Arrange
        Account target = account("t@example.com", new HashMap<String, Object>());

        // Act + Assert — null credentials can never access anything
        assertFalse(mgr.canAccessAccount((Account) null, target, true));
    }

    @Test
    public void canAccessAccountGlobalAdminAsAdminAlwaysSucceeds() throws Exception {
        // Arrange — admin in one domain, target in another
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = account("admin@example.com", adminAttrs);
        Account target = account("victim@other.com", new HashMap<String, Object>());

        // Act + Assert — global admin acting as admin wins regardless of domain
        assertTrue(mgr.canAccessAccount(admin, target, true));
    }

    @Test
    public void canAccessAccountParentAccountSucceedsEvenWhenNotAdmin() throws Exception {
        // Arrange — credentials list the target id as a child account
        Account child = account("child@example.com", new HashMap<String, Object>());
        Map<String, Object> parentAttrs = new HashMap<String, Object>();
        parentAttrs.put(Provisioning.A_zimbraChildAccount, child.getId());
        Account parent = account("parent@example.com", parentAttrs);

        // Act + Assert — parent-of relationship grants access without admin flags
        assertTrue(mgr.canAccessAccount(parent, child, true));
    }

    @Test
    public void canAccessAccountNotActingAsAdminReturnsFalseForNonParent() throws Exception {
        // Arrange — two unrelated same-domain accounts
        Account a = account("a@example.com", new HashMap<String, Object>());
        Account b = account("b@example.com", new HashMap<String, Object>());

        // Act + Assert — asAdmin=false and not a parent => denied
        assertFalse(mgr.canAccessAccount(a, b, false));
    }

    @Test
    public void canAccessAccountDomainAdminSameDomainSucceeds() throws Exception {
        // Arrange — domain admin and a plain target in the SAME domain
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account da = account("da@example.com", daAttrs);
        Account target = account("user@example.com", new HashMap<String, Object>());

        // Act + Assert — same-domain domain admin can access the target
        assertTrue(mgr.canAccessAccount(da, target, true));
    }

    @Test
    public void canAccessAccountDomainAdminTargetIsGlobalAdminReturnsFalse() throws Exception {
        // Arrange — domain admin must NOT be able to touch a global admin's account
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account da = account("da@example.com", daAttrs);

        Map<String, Object> gaAttrs = new HashMap<String, Object>();
        gaAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account globalAdminTarget = account("ga@example.com", gaAttrs);

        // Act + Assert
        assertFalse(mgr.canAccessAccount(da, globalAdminTarget, true));
    }

    @Test
    public void canAccessAccountDomainAdminDifferentDomainReturnsFalse() throws Exception {
        // Arrange — domain admin in example.com, plain target in other.com
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account da = account("da@example.com", daAttrs);
        Account target = account("user@other.com", new HashMap<String, Object>());

        // Act + Assert — cross-domain domain admin is denied
        assertFalse(mgr.canAccessAccount(da, target, true));
    }

    @Test
    public void canAccessAccountTwoArgOverloadDefaultsToAsAdminTrue() throws Exception {
        // Arrange
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = account("admin@example.com", adminAttrs);
        Account target = account("user@example.com", new HashMap<String, Object>());

        // Act + Assert — the 2-arg form delegates with asAdmin=true
        assertTrue(mgr.canAccessAccount(admin, target));
    }

    @Test
    public void canAccessAccountSameDomainNonDomainAdminReturnsFalse() throws Exception {
        // Arrange — credentials are NOT an admin, NOT a domain admin, NOT a parent,
        // and live in the SAME domain as the target.  This drives the final
        // "isDomainTheSame -> return getBooleanAttr(A_zimbraIsDomainAdminAccount)" line
        // (L99): the account lacks the domain-admin flag so the decision is FALSE.
        Account creds = account("nobody@example.com", new HashMap<String, Object>());
        Account target = account("peer@example.com", new HashMap<String, Object>());

        // Act + Assert — same domain but not a domain admin => denied (must be exactly false)
        assertFalse("same-domain non-domain-admin must be denied",
                mgr.canAccessAccount(creds, target, true));
    }

    @Test
    public void canAccessAccountTwoArgOverloadDeniesWhenSubcheckFails() throws Exception {
        // Arrange — a plain user, different domain, not admin/parent.  The 2-arg overload
        // delegates to (creds,target,true); the 3-arg path returns FALSE here, so the 2-arg
        // form must return FALSE too (kills a forced "return true" on the delegating line L106).
        Account creds = account("plain2@example.com", new HashMap<String, Object>());
        Account target = account("victim2@other.com", new HashMap<String, Object>());

        // Act + Assert
        assertFalse("2-arg overload must propagate the false decision",
                mgr.canAccessAccount(creds, target));
    }

    @Test
    public void canAccessAccountDifferentSuspendedDomainThrowsPermDenied() throws Exception {
        // Arrange — credentials in example.com, target in a SUSPENDED foreign domain.
        // Because the domains differ, checkDomainStatus(target) must run (L82) and reject
        // the suspended domain.  Removing that call would let the method return normally.
        suspendedDomain("suspended.example");
        Account creds = account("c-susp@example.com", new HashMap<String, Object>());
        Account target = account("t-susp@suspended.example", new HashMap<String, Object>());

        // Act + Assert
        try {
            mgr.canAccessAccount(creds, target, true);
            fail("expected PERM_DENIED because the target's foreign domain is suspended");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void canAccessAccountSameSuspendedDomainDoesNotCheckStatus() throws Exception {
        // Arrange — BOTH credentials and target live in the SAME suspended domain.
        // The guard is `if (!isDomainTheSame) checkDomainStatus(target)` (L81): since the
        // domains match, the status check must be SKIPPED and no exception thrown.  A negated
        // conditional would (wrongly) invoke checkDomainStatus and raise PERM_DENIED.
        suspendedDomain("susp-same.example");
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account creds = account("c-same@susp-same.example", adminAttrs);
        Account target = account("t-same@susp-same.example", new HashMap<String, Object>());

        // Act + Assert — same-domain admin is granted access without any domain-status throw
        assertTrue("same-domain access must skip the status check and succeed",
                mgr.canAccessAccount(creds, target, true));
    }

    @Test
    public void canSetMailQuotaAdminAccountAlwaysTrue() throws Exception {
        // Arrange — uses the AuthToken-free static path via a real admin account token
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = account("admin@example.com", adminAttrs);
        AuthToken at = new ZimbraAuthToken(admin, true, null);
        Account target = account("user@example.com", new HashMap<String, Object>());

        // Act + Assert
        assertTrue(DomainAccessManager.canSetMailQuota(at, target, 1000L));
    }

    @Test
    public void canSetMailQuotaDomainAdminUnlimitedMaxAllowsAnyQuota() throws Exception {
        // Arrange — domain admin with max=0 (unlimited authority over quotas).
        // Create the target first so the domain admin owns a distinct, stable id;
        // canSetMailQuota reloads the admin by AuthToken id, and a shared default id
        // would otherwise resolve back to the attribute-less target account.
        Account target = account("user@example.com", new HashMap<String, Object>());
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        daAttrs.put(Provisioning.A_zimbraDomainAdminMaxMailQuota, "0");
        Account da = account("da@example.com", daAttrs);
        AuthToken at = new ZimbraAuthToken(da, false, null);

        // Act + Assert — max==0 means "may set anything"
        assertTrue(DomainAccessManager.canSetMailQuota(at, target, 99999L));
    }

    @Test
    public void canSetMailQuotaDomainAdminQuotaExceedsMaxReturnsFalse() throws Exception {
        // Arrange — domain admin capped at 500, attempting to set 1000.
        // Create the target first so the domain admin owns a distinct, stable id
        // that canSetMailQuota can reload back to its capped attribute.
        Account target = account("user@example.com", new HashMap<String, Object>());
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        daAttrs.put(Provisioning.A_zimbraDomainAdminMaxMailQuota, "500");
        Account da = account("da@example.com", daAttrs);
        AuthToken at = new ZimbraAuthToken(da, false, null);

        // Act + Assert — requested quota above the cap is rejected
        assertFalse(DomainAccessManager.canSetMailQuota(at, target, 1000L));
    }

    @Test
    public void canSetMailQuotaDomainAdminWithinMaxReturnsTrue() throws Exception {
        // Arrange — domain admin capped at 1000, setting 400.
        // Create the target first so the domain admin owns a distinct, stable id;
        // canSetMailQuota reloads the admin by AuthToken id, and a shared default id
        // would otherwise resolve back to the attribute-less target account.
        Account target = account("user@example.com", new HashMap<String, Object>());
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        daAttrs.put(Provisioning.A_zimbraDomainAdminMaxMailQuota, "1000");
        Account da = account("da@example.com", daAttrs);
        AuthToken at = new ZimbraAuthToken(da, false, null);

        // Act + Assert — under the cap and non-zero => allowed
        assertTrue(DomainAccessManager.canSetMailQuota(at, target, 400L));
    }

    @Test
    public void canSetMailQuotaQuotaExactlyEqualsMaxReturnsTrue() throws Exception {
        // Arrange — domain admin capped at 500 setting EXACTLY 500.  The rejection guard is
        // `quota > maxQuota` (L221); 500 is not greater than 500, so this must be ALLOWED.
        // A boundary mutation to `>=` would (wrongly) reject the equal-to-max request.
        Account target = account("user-eq@example.com", new HashMap<String, Object>());
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        daAttrs.put(Provisioning.A_zimbraDomainAdminMaxMailQuota, "500");
        Account da = account("da-eq@example.com", daAttrs);
        AuthToken at = new ZimbraAuthToken(da, false, null);

        // Act + Assert — quota == max is on the allowed side of the boundary
        assertTrue("quota exactly equal to the max must be permitted",
                DomainAccessManager.canSetMailQuota(at, target, 500L));
    }

    @Test
    public void canSetMailQuotaQuotaOneOverMaxReturnsTrueAtBoundary() throws Exception {
        // Arrange — companion to the previous test: 501 against a cap of 500 must be REJECTED,
        // pinning the boundary so neither side can drift (L221 `quota > maxQuota`).
        Account target = account("user-over1@example.com", new HashMap<String, Object>());
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        daAttrs.put(Provisioning.A_zimbraDomainAdminMaxMailQuota, "500");
        Account da = account("da-over1@example.com", daAttrs);
        AuthToken at = new ZimbraAuthToken(da, false, null);

        // Act + Assert — one above the cap is rejected
        assertFalse("quota one above the max must be rejected",
                DomainAccessManager.canSetMailQuota(at, target, 501L));
    }

    @Test
    public void canSetMailQuotaOverCapLogsWarning() throws Exception {
        // Arrange — capture the "zimbra.account" logger so we can prove the rejection branch
        // emits its warning (the ZimbraLog.account.warn call on L225).  Removing that call
        // leaves the captured-log list without the diagnostic message.
        Account target = account("user-logq@example.com", new HashMap<String, Object>());
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        daAttrs.put(Provisioning.A_zimbraDomainAdminMaxMailQuota, "500");
        Account da = account("da-logq@example.com", daAttrs);
        AuthToken at = new ZimbraAuthToken(da, false, null);
        List<String> logged = captureAccountLog();

        // Act — request a quota above the cap so the warn-and-deny branch runs
        boolean allowed = DomainAccessManager.canSetMailQuota(at, target, 1000L);

        // Assert — denied AND the warning was logged with the admin's name
        assertFalse("over-cap request must be denied", allowed);
        boolean sawWarning = false;
        for (String m : logged) {
            if (m.contains("invalid attempt to change quota") && m.contains("da-logq@example.com")) {
                sawWarning = true;
                break;
            }
        }
        assertTrue("the over-cap rejection must log a warning (L225)", sawWarning);
    }

    @Test
    public void canSetMailQuotaNoPermissionDefaultMaxReturnsFalse() throws Exception {
        // Arrange — domain admin without the max-quota attr (default -1 = no permission)
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account da = account("da@example.com", daAttrs);
        AuthToken at = new ZimbraAuthToken(da, false, null);
        Account target = account("user@example.com", new HashMap<String, Object>());

        // Act + Assert — default max (-1) means they may not change any quota
        assertFalse(DomainAccessManager.canSetMailQuota(at, target, 100L));
    }

    // ------------------------------------------------------------------
    // isDomainAdminOnly
    // ------------------------------------------------------------------

    @Test
    public void isDomainAdminOnlyDomainAdminNotGlobalReturnsTrue() throws Exception {
        // Arrange — a pure domain admin (domain-admin flag, not global admin).
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account da = accountWithId("dao@example.com", "id-dao", attrs);
        AuthToken at = token(da, true);

        // Act + Assert — domain admin and not global admin => domain-admin-only.
        assertTrue(mgr.isDomainAdminOnly(at));
    }

    @Test
    public void isDomainAdminOnlyGlobalAdminReturnsFalse() throws Exception {
        // Arrange — a global admin is not "domain admin only".
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account ga = accountWithId("gao@example.com", "id-gao", attrs);
        AuthToken at = token(ga, true);

        // Act + Assert
        assertFalse(mgr.isDomainAdminOnly(at));
    }

    // ------------------------------------------------------------------
    // canAccessAccount(AuthToken, ...)
    // ------------------------------------------------------------------

    @Test
    public void canAccessAccountTokenNonZimbraUserReturnsFalse() throws Exception {
        // Arrange — a guest (non-Zimbra) auth token can never access an account.
        Account target = account("t-tok@example.com", new HashMap<String, Object>());
        AuthToken guest = new ZimbraAuthToken(
                new GuestAccount("g@x.com", "pw").getId(), null, null, null, 0L);

        // Act + Assert
        assertFalse(mgr.canAccessAccount(guest, target, true));
    }

    @Test
    public void canAccessAccountTokenGlobalAdminAsAdminSucceeds() throws Exception {
        // Arrange — global admin in one domain, target in another.
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = accountWithId("admin-tok@example.com", "id-admin-tok", adminAttrs);
        Account target = account("victim-tok@other.com", new HashMap<String, Object>());
        AuthToken at = token(admin, true);

        // Act + Assert — global admin acting as admin wins regardless of domain.
        assertTrue(mgr.canAccessAccount(at, target, true));
    }

    @Test
    public void canAccessAccountTokenDomainAdminSameDomainSucceeds() throws Exception {
        // Arrange — domain admin and target in the SAME domain.
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account da = accountWithId("da-tok@example.com", "id-da-tok", daAttrs);
        Account target = account("user-tok@example.com", new HashMap<String, Object>());
        AuthToken at = token(da, true);

        // Act + Assert
        assertTrue(mgr.canAccessAccount(at, target, true));
    }

    @Test
    public void canAccessAccountTokenDomainAdminTargetGlobalAdminReturnsFalse() throws Exception {
        // Arrange — a domain admin must not be able to touch a global admin's account.
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account da = accountWithId("da-tok2@example.com", "id-da-tok2", daAttrs);
        Map<String, Object> gaAttrs = new HashMap<String, Object>();
        gaAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account gaTarget = account("ga-target@example.com", gaAttrs);
        AuthToken at = token(da, true);

        // Act + Assert
        assertFalse(mgr.canAccessAccount(at, gaTarget, true));
    }

    @Test
    public void canAccessAccountTokenNotAsAdminAndNotParentReturnsFalse() throws Exception {
        // Arrange — a plain user token, not acting as admin, not a parent.
        Account user = accountWithId("plain-tok@example.com", "id-plain-tok",
                new HashMap<String, Object>());
        Account target = account("other-tok@example.com", new HashMap<String, Object>());
        AuthToken at = token(user, false);

        // Act + Assert — asAdmin path requires admin/domain-admin flags.
        assertFalse(mgr.canAccessAccount(at, target, true));
    }

    @Test
    public void canAccessAccountTokenTwoArgOverloadDefaultsToAsAdminTrue() throws Exception {
        // Arrange
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = accountWithId("admin-2arg@example.com", "id-admin-2arg", adminAttrs);
        Account target = account("user-2arg@example.com", new HashMap<String, Object>());
        AuthToken at = token(admin, true);

        // Act + Assert — 2-arg overload delegates with asAdmin=true.
        assertTrue(mgr.canAccessAccount(at, target));
    }

    @Test
    public void canAccessAccountTokenDomainAdminDifferentDomainReturnsFalse() throws Exception {
        // Arrange — domain admin in example.com acting on a plain target in other.com.
        // The last line of the token path is `return isDomainTheSame` (L58); domains differ,
        // so the decision must be FALSE (kills a forced "return true").
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account da = accountWithId("da-diff-tok@example.com", "id-da-diff-tok", daAttrs);
        Account target = account("user-diff-tok@other.com", new HashMap<String, Object>());
        AuthToken at = token(da, true);

        // Act + Assert — cross-domain domain admin denied (exactly false)
        assertFalse("cross-domain domain admin via token must be denied",
                mgr.canAccessAccount(at, target, true));
    }

    @Test
    public void canAccessAccountTokenTwoArgOverloadDeniesWhenSubcheckFails() throws Exception {
        // Arrange — a plain user token, not admin/parent, different domain.  The 2-arg token
        // overload delegates to (at,target,true); that path returns FALSE, so the 2-arg form
        // must return FALSE (kills a forced "return true" on the delegating line L63).
        Account user = accountWithId("plain-2arg-tok@example.com", "id-plain-2arg-tok",
                new HashMap<String, Object>());
        Account target = account("victim-2arg-tok@other.com", new HashMap<String, Object>());
        AuthToken at = token(user, false);

        // Act + Assert
        assertFalse("2-arg token overload must propagate the false decision",
                mgr.canAccessAccount(at, target));
    }

    @Test
    public void canAccessAccountTokenDifferentSuspendedDomainThrowsPermDenied() throws Exception {
        // Arrange — admin token in example.com, target in a SUSPENDED foreign domain.
        // Domains differ so checkDomainStatus(target) (L50) must run and reject the domain;
        // removing it would let the method fall through to "asAdmin && isAdmin -> true".
        suspendedDomain("susp-tok.example");
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = accountWithId("admin-susp-tok@example.com", "id-admin-susp-tok", adminAttrs);
        Account target = account("t-susp-tok@susp-tok.example", new HashMap<String, Object>());
        AuthToken at = token(admin, true);

        // Act + Assert
        try {
            mgr.canAccessAccount(at, target, true);
            fail("expected PERM_DENIED because the target's foreign domain is suspended");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void canAccessAccountTokenSameSuspendedDomainDoesNotCheckStatus() throws Exception {
        // Arrange — admin token and target in the SAME suspended domain.  The guard
        // `if (!isDomainTheSame) checkDomainStatus(target)` (L49) must SKIP the check because
        // the domains match; a negated conditional would throw PERM_DENIED instead of granting.
        suspendedDomain("susp-same-tok.example");
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = accountWithId("admin-ss-tok@susp-same-tok.example", "id-admin-ss-tok",
                adminAttrs);
        Account target = account("t-ss-tok@susp-same-tok.example", new HashMap<String, Object>());
        AuthToken at = token(admin, true);

        // Act + Assert — same-domain admin granted without any status throw
        assertTrue("same-domain token access must skip the status check and succeed",
                mgr.canAccessAccount(at, target, true));
    }

    // ------------------------------------------------------------------
    // canAccessDomain
    // ------------------------------------------------------------------

    @Test
    public void canAccessDomainGlobalAdminReturnsTrueForAnyDomain() throws Exception {
        // Arrange
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = accountWithId("admin-dom@example.com", "id-admin-dom", adminAttrs);
        AuthToken at = token(admin, true);

        // Act + Assert — global admin can access any domain.
        assertTrue(mgr.canAccessDomain(at, "other.com"));
    }

    @Test
    public void canAccessDomainDomainAdminOwnDomainReturnsTrue() throws Exception {
        // Arrange — domain admin in example.com.
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account da = accountWithId("da-dom@example.com", "id-da-dom", daAttrs);
        AuthToken at = token(da, true);

        // Act + Assert — domain admin can access its own domain (case-insensitive).
        assertTrue(mgr.canAccessDomain(at, "EXAMPLE.COM"));
    }

    @Test
    public void canAccessDomainDomainAdminForeignDomainReturnsFalse() throws Exception {
        // Arrange — domain admin in example.com asking about other.com.
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account da = accountWithId("da-dom2@example.com", "id-da-dom2", daAttrs);
        AuthToken at = token(da, true);

        // Act + Assert
        assertFalse(mgr.canAccessDomain(at, "other.com"));
    }

    @Test
    public void canAccessDomainPlainUserReturnsFalse() throws Exception {
        // Arrange — neither global nor domain admin.
        Account user = accountWithId("plain-dom@example.com", "id-plain-dom",
                new HashMap<String, Object>());
        AuthToken at = token(user, false);

        // Act + Assert — canAccessDomainInternal returns false for a non-admin.
        assertFalse(mgr.canAccessDomain(at, "example.com"));
    }

    @Test
    public void canAccessDomainNonZimbraUserReturnsFalse() throws Exception {
        // Arrange — guest token is not a Zimbra user.
        AuthToken guest = new ZimbraAuthToken(
                new GuestAccount("g2@x.com", "pw").getId(), null, null, null, 0L);

        // Act + Assert
        assertFalse(mgr.canAccessDomain(guest, "example.com"));
    }

    @Test
    public void canAccessDomainByObjectDomainAdminOwnDomainReturnsTrue() throws Exception {
        // Arrange — Domain-object overload for a domain admin's own domain.
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account da = accountWithId("da-domobj@example.com", "id-da-domobj", daAttrs);
        AuthToken at = token(da, true);
        Domain domain = prov.get(Key.DomainBy.name, "example.com");

        // Act + Assert
        assertTrue(mgr.canAccessDomain(at, domain));
    }

    @Test
    public void canAccessDomainByNameSuspendedDomainThrowsPermDenied() throws Exception {
        // Arrange — even a global admin must be stopped when the named domain is suspended,
        // because canAccessDomain(at, name) calls checkDomainStatus(name) (L123) before the
        // internal admin check.  Removing it would let the admin sail through.
        suspendedDomain("susp-byname.example");
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = accountWithId("admin-byname@example.com", "id-admin-byname", adminAttrs);
        AuthToken at = token(admin, true);

        // Act + Assert
        try {
            mgr.canAccessDomain(at, "susp-byname.example");
            fail("expected PERM_DENIED for a suspended domain accessed by name");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void canAccessDomainByObjectSuspendedDomainThrowsPermDenied() throws Exception {
        // Arrange — the Domain-object overload calls checkDomainStatus(domain) (L131) before
        // the internal admin check.  A suspended Domain object must be rejected.
        suspendedDomain("susp-byobj.example");
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = accountWithId("admin-byobj@example.com", "id-admin-byobj", adminAttrs);
        AuthToken at = token(admin, true);
        Domain domain = prov.get(Key.DomainBy.name, "susp-byobj.example");

        // Act + Assert
        try {
            mgr.canAccessDomain(at, domain);
            fail("expected PERM_DENIED for a suspended Domain object");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void canAccessDomainByObjectPlainUserReturnsFalse() throws Exception {
        // Arrange — Domain-object overload for a non-admin in an active domain.  The method
        // returns canAccessDomainInternal(...) (L132); a non-admin yields exactly FALSE, so a
        // forced "return true" on that delegating line is caught.
        Account user = accountWithId("plain-byobj@example.com", "id-plain-byobj",
                new HashMap<String, Object>());
        AuthToken at = token(user, false);
        Domain domain = prov.get(Key.DomainBy.name, "example.com");

        // Act + Assert
        assertFalse("non-admin must not access a domain via the Domain-object overload",
                mgr.canAccessDomain(at, domain));
    }

    // ------------------------------------------------------------------
    // canAccessCos
    // ------------------------------------------------------------------

    @Test
    public void canAccessCosGlobalAdminReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = accountWithId("admin-cos@example.com", "id-admin-cos", adminAttrs);
        AuthToken at = token(admin, true);
        Cos cos = prov.createCos("cos-admin", new HashMap<String, Object>());

        // Act + Assert — global admin can access any COS.
        assertTrue(mgr.canAccessCos(at, cos));
        prov.deleteCos(cos.getId());
    }

    @Test
    public void canAccessCosPlainUserReturnsFalse() throws Exception {
        // Arrange — a non-admin cannot access a COS.
        Account user = accountWithId("plain-cos@example.com", "id-plain-cos",
                new HashMap<String, Object>());
        AuthToken at = token(user, false);
        Cos cos = prov.createCos("cos-plain", new HashMap<String, Object>());

        // Act + Assert
        assertFalse(mgr.canAccessCos(at, cos));
        prov.deleteCos(cos.getId());
    }

    @Test
    public void canAccessCosDomainAdminCosInAllowedListReturnsTrue() throws Exception {
        // Arrange — domain admin whose domain lists the COS id in zimbraDomainCOSMaxAccounts.
        Cos cos = prov.createCos("cos-allowed", new HashMap<String, Object>());
        Map<String, Object> domAttrs = new HashMap<String, Object>();
        domAttrs.put(Provisioning.A_zimbraDomainCOSMaxAccounts, cos.getId() + ":50");
        prov.createDomain("cosdomain.example", domAttrs);

        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account da = accountWithId("da@cosdomain.example", "id-da-cos", daAttrs);
        AuthToken at = token(da, true);

        // Act + Assert — the COS appears in the domain's allowed list.
        assertTrue(mgr.canAccessCos(at, cos));
        prov.deleteCos(cos.getId());
    }

    @Test
    public void canAccessCosDomainAdminCosNotInListReturnsFalse() throws Exception {
        // Arrange — domain admin whose domain does NOT list this COS.
        Cos cos = prov.createCos("cos-notallowed", new HashMap<String, Object>());
        Map<String, Object> domAttrs = new HashMap<String, Object>();
        domAttrs.put(Provisioning.A_zimbraDomainCOSMaxAccounts, "some-other-cos-id:50");
        prov.createDomain("cosdomain2.example", domAttrs);

        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account da = accountWithId("da@cosdomain2.example", "id-da-cos2", daAttrs);
        AuthToken at = token(da, true);

        // Act + Assert — the COS id is absent from the allowed list.
        assertFalse(mgr.canAccessCos(at, cos));
        prov.deleteCos(cos.getId());
    }

    @Test
    public void canAccessCosNonZimbraUserReturnsFalse() throws Exception {
        // Arrange
        AuthToken guest = new ZimbraAuthToken(
                new GuestAccount("g3@x.com", "pw").getId(), null, null, null, 0L);
        Cos cos = prov.createCos("cos-guest", new HashMap<String, Object>());

        // Act + Assert
        assertFalse(mgr.canAccessCos(guest, cos));
        prov.deleteCos(cos.getId());
    }

    // ------------------------------------------------------------------
    // canAccessEmail
    // ------------------------------------------------------------------

    @Test
    public void canAccessEmailInvalidEmailThrowsInvalidRequest() throws Exception {
        // Arrange
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = accountWithId("admin-email@example.com", "id-admin-email", adminAttrs);
        AuthToken at = token(admin, true);

        // Act + Assert — a string without a domain part is rejected.
        try {
            mgr.canAccessEmail(at, "no-at-sign");
            fail("expected INVALID_REQUEST for a malformed email address");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void canAccessEmailGlobalAdminValidDomainReturnsTrue() throws Exception {
        // Arrange — admin can access an email whose domain it can access.
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = accountWithId("admin-email2@example.com", "id-admin-email2", adminAttrs);
        AuthToken at = token(admin, true);

        // Act + Assert — falls through to canAccessDomain (true for a global admin).
        assertTrue(mgr.canAccessEmail(at, "anyone@example.com"));
    }

    @Test
    public void canAccessEmailParentOfTargetReturnsTrue() throws Exception {
        // Arrange — credentials list the target account id as a child.
        Account child = accountWithId("child-email@example.com", "id-child-email",
                new HashMap<String, Object>());
        Map<String, Object> parentAttrs = new HashMap<String, Object>();
        parentAttrs.put(Provisioning.A_zimbraChildAccount, "id-child-email");
        Account parent = accountWithId("parent-email@example.com", "id-parent-email", parentAttrs);
        AuthToken at = token(parent, false);

        // Act + Assert — parent-of relationship grants email access without admin.
        assertTrue(mgr.canAccessEmail(at, "child-email@example.com"));
    }

    @Test
    public void canAccessEmailNonAdminNotParentReturnsFalse() throws Exception {
        // Arrange — a plain user (not admin, not a parent of the target) querying an email
        // in their own domain.  canAccessEmail falls through to `return canAccessDomain(...)`
        // (L195), which for a non-admin is FALSE; this kills a forced "return true" there.
        Account user = accountWithId("plain-email@example.com", "id-plain-email",
                new HashMap<String, Object>());
        Account other = accountWithId("other-email@example.com", "id-other-email",
                new HashMap<String, Object>());
        AuthToken at = token(user, false);

        // Act + Assert — must be exactly false
        assertFalse("non-admin non-parent must not access another mailbox's email",
                mgr.canAccessEmail(at, "other-email@example.com"));
    }

    // ------------------------------------------------------------------
    // canModifyMailQuota
    // ------------------------------------------------------------------

    @Test
    public void canModifyMailQuotaGlobalAdminReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = accountWithId("admin-q@example.com", "id-admin-q", adminAttrs);
        Account target = account("user-q@example.com", new HashMap<String, Object>());
        AuthToken at = token(admin, true);

        // Act + Assert — admin can access the account AND set the quota.
        assertTrue(mgr.canModifyMailQuota(at, target, 1000L));
    }

    @Test
    public void canModifyMailQuotaCanAccessButQuotaTooBigReturnsFalse() throws Exception {
        // Arrange — a domain admin CAN access a same-domain target (so the canAccessAccount
        // gate passes), but is capped at 500 and tries to set 1000.  canModifyMailQuota's
        // final line `return canSetMailQuota(...)` (L203) must therefore yield FALSE, proving
        // the result is taken from canSetMailQuota and not hard-wired to true.
        Account target = accountWithId("user-modq@example.com", "id-user-modq",
                new HashMap<String, Object>());
        Map<String, Object> daAttrs = new HashMap<String, Object>();
        daAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        daAttrs.put(Provisioning.A_zimbraDomainAdminMaxMailQuota, "500");
        Account da = accountWithId("da-modq@example.com", "id-da-modq", daAttrs);
        // isAdmin=true on the token activates the domain-admin flag (isDomainAdmin = isAdmin &&
        // A_zimbraIsDomainAdminAccount); for a pure domain admin isAdmin() still resolves to false,
        // so canSetMailQuota takes the quota-cap path rather than the global-admin shortcut.
        AuthToken at = token(da, true);

        // Sanity: the admin really can access the target (so we are past the access gate).
        assertTrue("domain admin must be able to access the same-domain target",
                mgr.canAccessAccount(at, target, true));

        // Act + Assert — access granted, but the over-cap quota makes the overall answer false.
        assertFalse("over-cap quota must make canModifyMailQuota return false",
                mgr.canModifyMailQuota(at, target, 1000L));
    }

    @Test
    public void canModifyMailQuotaCannotAccessAccountReturnsFalse() throws Exception {
        // Arrange — a plain user (not admin/parent) cannot access the target at all.
        Account user = accountWithId("plain-q@example.com", "id-plain-q",
                new HashMap<String, Object>());
        Account target = account("victim-q@other.com", new HashMap<String, Object>());
        AuthToken at = token(user, false);

        // Act + Assert — denied at the canAccessAccount gate.
        assertFalse(mgr.canModifyMailQuota(at, target, 1000L));
    }

    // ------------------------------------------------------------------
    // ACL-based methods are unsupported by DomainAccessManager (always deny)
    // ------------------------------------------------------------------

    @Test
    public void aclMethodsAlwaysReturnFalse() throws Exception {
        // Arrange
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = accountWithId("admin-acl@example.com", "id-admin-acl", adminAttrs);
        Account target = account("target-acl@example.com", new HashMap<String, Object>());
        AuthToken at = token(admin, true);
        Right right = RightManager.getInstance().getRight("viewFreeBusy");
        java.util.Set<String> attrs = new java.util.HashSet<String>();
        attrs.add("displayName");
        Map<String, Object> attrMap = new HashMap<String, Object>();
        attrMap.put("displayName", "x");

        // Act + Assert — DomainAccessManager does not implement ACL-based access.
        assertFalse(mgr.canDo(at, target, right, true));
        assertFalse(mgr.canDo((MailTarget) admin, target, right, true));
        assertFalse(mgr.canDo("id-admin-acl", target, right, true));
        assertFalse(mgr.canGetAttrs(admin, target, attrs, true));
        assertFalse(mgr.canGetAttrs(at, target, attrs, true));
        assertFalse(mgr.canSetAttrs(admin, target, attrs, true));
        assertFalse(mgr.canSetAttrs(at, target, attrs, true));
        assertFalse(mgr.canSetAttrs(admin, target, attrMap, true));
        assertFalse(mgr.canSetAttrs(at, target, attrMap, true));
    }

    @Test
    public void groupMethodsAlwaysReturnFalse() throws Exception {
        // Arrange
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = accountWithId("admin-grp@example.com", "id-admin-grp", adminAttrs);
        AuthToken at = token(admin, true);

        // Act + Assert — group create/access is not supported by DomainAccessManager.
        assertFalse(mgr.canCreateGroup(at, "group@example.com"));
        assertFalse(mgr.canCreateGroup(admin, "group@example.com"));
        assertFalse(mgr.canAccessGroup(at, (Group) null));
        assertFalse(mgr.canAccessGroup(admin, (Group) null, true));
    }
}
