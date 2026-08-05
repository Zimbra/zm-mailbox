/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link PseudoTarget}. Pseudo targets are constructed through the real
 * {@code createPseudoTarget} factory against the in-memory {@link Provisioning} harness, with a
 * real domain and default COS provisioned first. Verifies the constructed entity type, the shared
 * pseudo zimbraId, the pseudo-domain wiring, {@code isPseudoEntry} classification, and the
 * domain-required / unsupported-target error paths.
 */
public class PseudoTargetTest {

    private static final String DOMAIN = "pseudo-example.com";

    private static Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        // Ensure a real domain exists (createDomain throws on duplicate, so guard).
        if (prov.get(Key.DomainBy.name, DOMAIN) == null) {
            prov.createDomain(DOMAIN, new HashMap<String, Object>());
        }
        // Ensure the default COS exists for account/calresource pseudo targets.
        if (prov.get(Key.CosBy.name, Provisioning.DEFAULT_COS_NAME) == null) {
            prov.createCos(Provisioning.DEFAULT_COS_NAME, new HashMap<String, Object>());
        }
    }

    @Test
    public void createPseudoDomainDefaultReturnsPseudoDomainWithPseudoId() throws Exception {
        // Act
        Domain d = PseudoTarget.createPseudoDomain(prov);

        // Assert - it is a Domain flagged as pseudo, carrying the well-known pseudo zimbraId
        assertNotNull(d);
        assertEquals("pseudo.pseudo", d.getName());
        assertTrue("created domain must be a pseudo entry", PseudoTarget.isPseudoEntry(d));
        assertEquals(PseudoTarget.PseudoZimbraId.getPseudoZimbraId(), d.getId());
    }

    @Test
    public void createPseudoDomainWithNameUsesProvidedName() throws Exception {
        // Act - the named overload keeps the real name rather than "pseudo.pseudo"
        Domain d = PseudoTarget.createPseudoDomain(prov, "named.example.org");

        // Assert
        assertEquals("named.example.org", d.getName());
        assertTrue(PseudoTarget.isPseudoEntry(d));
    }

    @Test
    public void createPseudoTargetAccountWithRealDomainReturnsPseudoAccount() throws Exception {
        // Act - account anchored to the real domain
        Entry e = PseudoTarget.createPseudoTarget(prov, TargetType.account,
                Key.DomainBy.name, DOMAIN, false, null, null);

        // Assert
        assertTrue("expected a pseudo account", PseudoTarget.isPseudoEntry(e));
        assertEquals("pseudo@" + DOMAIN, ((Account) e).getName());
        assertEquals(PseudoTarget.PseudoZimbraId.getPseudoZimbraId(), ((Account) e).getId());
    }

    @Test
    public void createPseudoTargetAccountWithPseudoDomainReturnsPseudoAccount() throws Exception {
        // Act - createPseudoDomain=true builds an anonymous pseudo domain on the fly
        Entry e = PseudoTarget.createPseudoTarget(prov, TargetType.account,
                null, null, true, null, null);

        // Assert - account name is anchored to the generated pseudo domain
        assertTrue(PseudoTarget.isPseudoEntry(e));
        assertEquals("pseudo@pseudo.pseudo", ((Account) e).getName());
    }

    @Test
    public void createPseudoTargetCalresourceWithRealDomainReturnsPseudoEntry() throws Exception {
        // Act
        Entry e = PseudoTarget.createPseudoTarget(prov, TargetType.calresource,
                Key.DomainBy.name, DOMAIN, false, null, null);

        // Assert
        assertTrue(PseudoTarget.isPseudoEntry(e));
        assertEquals("pseudo@" + DOMAIN, e.getLabel());
    }

    @Test
    public void createPseudoTargetDlReturnsPseudoEntryAnchoredToDomain() throws Exception {
        // Act
        Entry e = PseudoTarget.createPseudoTarget(prov, TargetType.dl,
                Key.DomainBy.name, DOMAIN, false, null, null);

        // Assert
        assertTrue(PseudoTarget.isPseudoEntry(e));
        assertEquals("pseudo@" + DOMAIN, e.getLabel());
    }

    @Test
    public void createPseudoTargetCosReturnsPseudoCos() throws Exception {
        // Act - cos type needs no domain
        Entry e = PseudoTarget.createPseudoTarget(prov, TargetType.cos,
                null, null, false, null, null);

        // Assert
        assertTrue(PseudoTarget.isPseudoEntry(e));
        assertEquals("pseudocos", e.getLabel());
    }

    @Test
    public void createPseudoTargetServerReturnsPseudoServer() throws Exception {
        // Act
        Entry e = PseudoTarget.createPseudoTarget(prov, TargetType.server,
                null, null, false, null, null);

        // Assert
        assertTrue(PseudoTarget.isPseudoEntry(e));
        assertEquals("pseudo.pseudo", ((Server) e).getName());
    }

    @Test
    public void createPseudoTargetZimletReturnsPseudoZimlet() throws Exception {
        // Act
        Entry e = PseudoTarget.createPseudoTarget(prov, TargetType.zimlet,
                null, null, false, null, null);

        // Assert
        assertTrue(PseudoTarget.isPseudoEntry(e));
        assertEquals("pseudo", e.getLabel());
    }

    @Test
    public void createPseudoTargetDomainedTypeWithoutDomainInfoThrowsInvalidRequest() {
        // Act / Assert - account requires either a real domain or createPseudoDomain=true
        try {
            PseudoTarget.createPseudoTarget(prov, TargetType.account,
                    null, null, false, null, null);
            fail("expected INVALID_REQUEST when no domain info supplied");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void createPseudoTargetNonexistentDomainThrowsNoSuchDomain() {
        // Act / Assert - referencing a domain that does not exist
        try {
            PseudoTarget.createPseudoTarget(prov, TargetType.account,
                    Key.DomainBy.name, "does-not-exist.example", false, null, null);
            fail("expected NO_SUCH_DOMAIN");
        } catch (ServiceException e) {
            assertTrue("expected a no-such-domain error code",
                    e.getCode().contains("NO_SUCH_DOMAIN"));
        }
    }

    @Test
    public void createPseudoTargetGlobalTypeThrowsUnsupportedTarget() {
        // Act / Assert - global is not handled by the factory's switch
        try {
            PseudoTarget.createPseudoTarget(prov, TargetType.global,
                    null, null, false, null, null);
            fail("expected INVALID_REQUEST for unsupported target type");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains("unsupported target"));
        }
    }

    @Test
    public void isPseudoEntryRealAccountReturnsFalse() throws Exception {
        // Arrange - a genuine (non-pseudo) account
        Map<String, Object> attrs = new HashMap<String, Object>();
        Account real = prov.createAccount("realone@" + DOMAIN, "test123", attrs);

        // Act / Assert
        assertFalse("a real account must not be a pseudo entry", PseudoTarget.isPseudoEntry(real));
    }

    @Test
    public void pseudoZimbraIdIsPseudoZimbraIdRecognizesOwnIdAndRejectsOthers() {
        // Arrange
        String pid = PseudoTarget.PseudoZimbraId.getPseudoZimbraId();

        // Act / Assert
        assertTrue(PseudoTarget.PseudoZimbraId.isPseudoZimrbaId(pid));
        assertFalse(PseudoTarget.PseudoZimbraId.isPseudoZimrbaId("some-other-id"));
    }

    @Test
    public void createPseudoTargetGroupReturnsPseudoEntryAnchoredToDomain() throws Exception {
        // Act - dynamic group target anchored to the real domain
        Entry e = PseudoTarget.createPseudoTarget(prov, TargetType.group,
                Key.DomainBy.name, DOMAIN, false, null, null);

        // Assert - a PseudoDynamicGroup is produced, anchored to the domain with the pseudo id.
        // Note: PseudoDynamicGroup is NOT one of the types recognized by isPseudoEntry (it is
        // absent from that method's instanceof chain), so isPseudoEntry returns false here.
        assertTrue("expected a pseudo dynamic group instance",
                e instanceof PseudoTarget.PseudoDynamicGroup);
        assertFalse("isPseudoEntry does not recognize PseudoDynamicGroup",
                PseudoTarget.isPseudoEntry(e));
        assertEquals("pseudo@" + DOMAIN, e.getLabel());
        assertEquals(PseudoTarget.PseudoZimbraId.getPseudoZimbraId(), e.getAttr(Provisioning.A_zimbraId));
    }

    @Test
    public void createPseudoTargetGroupWithPseudoDomainBuildsDomainOnTheFly() throws Exception {
        // Act - createPseudoDomain=true generates an anonymous pseudo domain
        Entry e = PseudoTarget.createPseudoTarget(prov, TargetType.group,
                null, null, true, null, null);

        // Assert - PseudoDynamicGroup anchored to the on-the-fly "pseudo.pseudo" domain.
        // isPseudoEntry does not list PseudoDynamicGroup, so it returns false.
        assertTrue("expected a pseudo dynamic group instance",
                e instanceof PseudoTarget.PseudoDynamicGroup);
        assertFalse("isPseudoEntry does not recognize PseudoDynamicGroup",
                PseudoTarget.isPseudoEntry(e));
        assertEquals("pseudo@pseudo.pseudo", e.getLabel());
    }

    @Test
    public void createPseudoTargetUcserviceReturnsPseudoUCService() throws Exception {
        // Act - ucservice needs no domain
        Entry e = PseudoTarget.createPseudoTarget(prov, TargetType.ucservice,
                null, null, false, null, null);

        // Assert
        assertTrue(PseudoTarget.isPseudoEntry(e));
        assertEquals("pseudo", e.getLabel());
    }

    @Test
    public void createPseudoTargetXmppcomponentReturnsPseudoXMPPComponent() throws Exception {
        // Act
        Entry e = PseudoTarget.createPseudoTarget(prov, TargetType.xmppcomponent,
                null, null, false, null, null);

        // Assert
        assertTrue(PseudoTarget.isPseudoEntry(e));
        assertEquals("pseudo", e.getLabel());
    }

    @Test
    public void createPseudoTargetAlwaysonclusterReturnsPseudoEntry() throws Exception {
        // Act - alwaysoncluster needs no domain and uses null defaults
        Entry e = PseudoTarget.createPseudoTarget(prov, TargetType.alwaysoncluster,
                null, null, false, null, null);

        // Assert - a PseudoAlwaysOnCluster is produced. isPseudoEntry does not list
        // PseudoAlwaysOnCluster in its instanceof chain, so it returns false here.
        assertTrue("expected a pseudo always-on-cluster instance",
                e instanceof PseudoTarget.PseudoAlwaysOnCluster);
        assertFalse("isPseudoEntry does not recognize PseudoAlwaysOnCluster",
                PseudoTarget.isPseudoEntry(e));
        assertEquals("pseudo.pseudo", e.getLabel());
    }

    @Test
    public void createPseudoTargetAccountWithExplicitCosSetsCosId() throws Exception {
        // Arrange - look up the default COS to drive the explicit cosBy/cosStr branch
        com.zimbra.cs.account.Cos cos = prov.get(Key.CosBy.name, Provisioning.DEFAULT_COS_NAME);
        assertNotNull("default cos must exist", cos);

        // Act - pass the COS explicitly so the cosBy/cosStr branch is taken
        Entry e = PseudoTarget.createPseudoTarget(prov, TargetType.account,
                Key.DomainBy.name, DOMAIN, false, Key.CosBy.name, Provisioning.DEFAULT_COS_NAME);

        // Assert - the explicit COS id is stamped on the pseudo account
        assertTrue(PseudoTarget.isPseudoEntry(e));
        assertEquals(cos.getId(), e.getAttr(Provisioning.A_zimbraCOSId));
    }

    @Test
    public void createPseudoTargetAccountWithNonexistentCosThrowsNoSuchCos() {
        // Act / Assert - an explicit but unknown COS is rejected
        try {
            PseudoTarget.createPseudoTarget(prov, TargetType.account,
                    Key.DomainBy.name, DOMAIN, false, Key.CosBy.name, "no-such-cos-here");
            fail("expected NO_SUCH_COS");
        } catch (ServiceException e) {
            assertTrue("expected a no-such-cos error code", e.getCode().contains("NO_SUCH_COS"));
        }
    }

    @Test
    public void createPseudoTargetCalresourceWithPseudoDomainReturnsPseudoEntry() throws Exception {
        // Act - calresource via on-the-fly pseudo domain exercises the createPseudoDomain branch
        Entry e = PseudoTarget.createPseudoTarget(prov, TargetType.calresource,
                null, null, true, null, null);

        // Assert
        assertTrue(PseudoTarget.isPseudoEntry(e));
        assertEquals("pseudo@pseudo.pseudo", e.getLabel());
    }

    @Test
    public void pseudoCalendarResourceGetPseudoDomainReturnsConstructorDomain() throws Exception {
        // Arrange - a real anchoring domain
        Domain domain = prov.get(Key.DomainBy.name, DOMAIN);
        assertNotNull(domain);

        // Act - construct directly with a pseudo domain reference
        PseudoTarget.PseudoCalendarResource cr = new PseudoTarget.PseudoCalendarResource(
                "pseudo@" + DOMAIN, PseudoTarget.PseudoZimbraId.getPseudoZimbraId(),
                new HashMap<String, Object>(), null, prov, domain);

        // Assert - the getter returns the exact domain passed in
        assertEquals(domain, cr.getPseudoDomain());
    }
}
