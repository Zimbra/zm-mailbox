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

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.soap.type.TargetBy;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link TargetType}. Exercises the enum's classification logic,
 * inheritance metadata, JAXB round-trips, the pure DN {@code getCommonBase} helper, and the
 * {@code lookupTarget} dispatch against real entries from the in-memory
 * {@link com.zimbra.cs.account.MockProvisioning} harness.
 */
public class TargetTypeFunctionalTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        if (prov.get(DomainBy.name, "example.com") == null) {
            prov.createDomain("example.com", new HashMap<String, Object>());
        }
        prov.createAccount("user@example.com", "secret", new HashMap<String, Object>());
    }

    @Test
    public void getTargetTypeAccountReturnsAccount() throws Exception {
        // Arrange
        Account acct = prov.get(com.zimbra.common.account.Key.AccountBy.name, "user@example.com");

        // Act
        TargetType tt = TargetType.getTargetType(acct);

        // Assert
        assertEquals(TargetType.account, tt);
    }

    @Test
    public void getTargetTypeDomainReturnsDomain() throws Exception {
        // Arrange
        Domain domain = prov.get(DomainBy.name, "example.com");

        // Act
        TargetType tt = TargetType.getTargetType(domain);

        // Assert
        assertEquals(TargetType.domain, tt);
    }

    @Test
    public void getTargetTypeConfigReturnsConfig() throws Exception {
        // Arrange
        Config config = prov.getConfig();

        // Act
        TargetType tt = TargetType.getTargetType(config);

        // Assert
        assertEquals(TargetType.config, tt);
    }

    @Test
    public void getTargetTypeNullThrowsFailure() throws Exception {
        // Act / Assert — null is an internal-error condition.
        try {
            TargetType.getTargetType(null);
            fail("expected ServiceException for null target");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("internal error"));
        }
    }

    @Test
    public void isGroupDlAndGroupTrueOthersFalse() throws Exception {
        // Act / Assert — only the two group target types report true.
        assertTrue("dl is a group", TargetType.dl.isGroup());
        assertTrue("group is a group", TargetType.group.isGroup());
        assertFalse("account is not a group", TargetType.account.isGroup());
        assertFalse("domain is not a group", TargetType.domain.isGroup());
    }

    @Test
    public void isDomainedAccountTrueServerFalse() throws Exception {
        // Act / Assert — mIsDomained flag per the enum table.
        assertTrue("account is domained", TargetType.account.isDomained());
        assertTrue("dl is domained", TargetType.dl.isDomained());
        assertFalse("server is not domained", TargetType.server.isDomained());
        assertFalse("config is not domained", TargetType.config.isDomained());
    }

    @Test
    public void needsTargetIdentityAccountTrueConfigFalse() throws Exception {
        // Act / Assert — mNeedsTargetIdentity flag per the enum table.
        assertTrue("account needs identity", TargetType.account.needsTargetIdentity());
        assertFalse("config needs no identity", TargetType.config.needsTargetIdentity());
        assertFalse("global needs no identity", TargetType.global.needsTargetIdentity());
    }

    @Test
    public void getCodeAndPrettyNameAccountMatchEnum() throws Exception {
        // Act / Assert — getCode is name(), prettyName is the constructor value.
        assertEquals("account", TargetType.account.getCode());
        assertEquals("Account", TargetType.account.getPrettyName());
        assertEquals("GlobalGrant", TargetType.global.getPrettyName());
    }

    @Test
    public void fromCodeValidNameReturnsEnumConstant() throws Exception {
        // Act
        TargetType tt = TargetType.fromCode("domain");

        // Assert
        assertEquals(TargetType.domain, tt);
    }

    @Test
    public void fromCodeUnknownNameThrowsInvalidRequest() throws Exception {
        // Act / Assert
        try {
            TargetType.fromCode("bogusType");
            fail("expected ServiceException for unknown target type");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("unknown target type"));
        }
    }

    @Test
    public void toJaxbAndFromJaxbAccountRoundTrips() throws Exception {
        // Act
        com.zimbra.soap.type.TargetType jaxb = TargetType.account.toJaxb();
        TargetType back = TargetType.fromJaxb(jaxb);

        // Assert
        assertEquals(com.zimbra.soap.type.TargetType.account, jaxb);
        assertEquals(TargetType.account, back);
    }

    @Test
    public void canBeInheritedFromDomainTrueAccountFalse() throws Exception {
        // Arrange — a domain has sub-targets; an account does not.
        Domain domain = prov.get(DomainBy.name, "example.com");
        Account acct = prov.get(com.zimbra.common.account.Key.AccountBy.name, "user@example.com");

        // Act / Assert
        assertTrue("domain can be inherited from", TargetType.canBeInheritedFrom(domain));
        assertFalse("account cannot be inherited from", TargetType.canBeInheritedFrom(acct));
    }

    @Test
    public void getIdNamedEntryReturnsIdNonNamedNull() throws Exception {
        // Arrange
        Account acct = prov.get(com.zimbra.common.account.Key.AccountBy.name, "user@example.com");

        // Act / Assert — NamedEntry yields its id; a non-NamedEntry (Config) yields null.
        assertEquals(acct.getId(), TargetType.getId(acct));
        assertNull("config is not a NamedEntry", TargetType.getId(prov.getConfig()));
    }

    @Test
    public void getTargetDomainNameAccountReturnsDomainPart() throws Exception {
        // Arrange
        Account acct = prov.get(com.zimbra.common.account.Key.AccountBy.name, "user@example.com");

        // Act / Assert
        assertEquals("example.com", TargetType.getTargetDomainName(prov, acct));
        // a non-domained entry (Config) returns null
        assertNull(TargetType.getTargetDomainName(prov, prov.getConfig()));
    }

    @Test
    public void getTargetDomainAccountReturnsCreatedDomain() throws Exception {
        // Arrange
        Account acct = prov.get(com.zimbra.common.account.Key.AccountBy.name, "user@example.com");
        Domain domain = prov.get(DomainBy.name, "example.com");

        // Act
        Domain resolved = TargetType.getTargetDomain(prov, acct);

        // Assert
        assertNotNull(resolved);
        assertEquals(domain.getId(), resolved.getId());
    }

    @Test
    public void lookupTargetAccountByNameReturnsAccount() throws Exception {
        // Act
        Entry target = TargetType.lookupTarget(prov, TargetType.account,
                TargetBy.name, "user@example.com");

        // Assert
        assertNotNull(target);
        assertTrue(target instanceof Account);
        assertEquals("user@example.com", ((Account) target).getName());
    }

    @Test
    public void lookupTargetMissingAccountMustFindThrowsNoSuchAccount() throws Exception {
        // Act / Assert
        try {
            TargetType.lookupTarget(prov, TargetType.account, TargetBy.name, "ghost@example.com");
            fail("expected NO_SUCH_ACCOUNT");
        } catch (ServiceException e) {
            assertEquals(com.zimbra.cs.account.AccountServiceException.NO_SUCH_ACCOUNT, e.getCode());
        }
    }

    @Test
    public void lookupTargetMissingAccountMustFindFalseReturnsNull() throws Exception {
        // Act — mustFind=false suppresses the NO_SUCH_ACCOUNT throw.
        Entry target = TargetType.lookupTarget(prov, TargetType.account,
                TargetBy.name, "ghost@example.com", false);

        // Assert
        assertNull(target);
    }

    @Test
    public void lookupTargetConfigReturnsConfig() throws Exception {
        // Act — config lookup ignores the by/value and returns the global config.
        Entry target = TargetType.lookupTarget(prov, TargetType.config, TargetBy.name, "globalconfig");

        // Assert
        assertNotNull(target);
        assertTrue(target instanceof Config);
    }

    @Test
    public void getCommonBaseSharedSuffixReturnsCommonPortion() throws Exception {
        // Arrange — invoke the static package-private pure DN helper via reflection.
        Method m = TargetType.class.getDeclaredMethod("getCommonBase", String.class, String.class);
        m.setAccessible(true);

        // Act
        String common = (String) m.invoke(null, "ou=people,dc=foo,dc=com", "ou=groups,dc=foo,dc=com");

        // Assert — the shared trailing RDNs are returned.
        assertEquals("dc=foo,dc=com", common);
    }

    @Test
    public void getCommonBaseNoSharedSuffixReturnsEmpty() throws Exception {
        // Arrange
        Method m = TargetType.class.getDeclaredMethod("getCommonBase", String.class, String.class);
        m.setAccessible(true);

        // Act
        String common = (String) m.invoke(null, "dc=foo,dc=com", "dc=bar,dc=net");

        // Assert — nothing in common yields an empty base.
        assertEquals("", common);
    }

    @Test
    public void getCommonBaseEmptyArgumentReturnsEmpty() throws Exception {
        // Arrange — an empty DN short-circuits to the empty common base.
        Method m = TargetType.class.getDeclaredMethod("getCommonBase", String.class, String.class);
        m.setAccessible(true);

        // Act
        String common = (String) m.invoke(null, "", "dc=foo,dc=com");

        // Assert
        assertEquals("", common);
    }

    @Test
    public void fromJaxbUnrecognisedTypeThrowsIllegalArgument() throws Exception {
        // Arrange — null is not equal to any constant's jaxb type, so the loop exhausts.
        try {
            // Act
            TargetType.fromJaxb(null);
            fail("expected IllegalArgumentException for unrecognised jaxb type");
        } catch (IllegalArgumentException e) {
            // Assert
            assertTrue(e.getMessage().contains("Unrecognised TargetType"));
        }
    }

    @Test
    public void getTargetTypeCosReturnsCos() throws Exception {
        // Arrange
        com.zimbra.cs.account.Cos cos = prov.createCos("functestcos", new HashMap<String, Object>());

        // Act
        TargetType tt = TargetType.getTargetType(cos);

        // Assert
        assertEquals(TargetType.cos, tt);
    }

    @Test
    public void getTargetTypeServerReturnsServer() throws Exception {
        // Arrange
        com.zimbra.cs.account.Server server = prov.createServer("srv.example.com",
                new HashMap<String, Object>());

        // Act
        TargetType tt = TargetType.getTargetType(server);

        // Assert
        assertEquals(TargetType.server, tt);
    }

    @Test
    public void getTargetTypeDistributionListReturnsDl() throws Exception {
        // Arrange — a concrete DL backed only by the attribute map.
        com.zimbra.cs.account.DistributionList dl =
                new TargetTypeTestDL("list@example.com", "ttdl-1",
                        new HashMap<String, Object>(), prov);

        // Act
        TargetType tt = TargetType.getTargetType(dl);

        // Assert
        assertEquals(TargetType.dl, tt);
    }

    /** Minimal concrete DistributionList for instanceof-based classification tests. */
    private static final class TargetTypeTestDL extends com.zimbra.cs.account.DistributionList {
        TargetTypeTestDL(String name, String id, Map<String, Object> attrs, Provisioning prov) {
            super(name, id, attrs, prov);
        }
    }

    @Test
    public void getAttributeClassAccountReturnsAccountClass() throws Exception {
        // Arrange
        Account acct = prov.get(com.zimbra.common.account.Key.AccountBy.name, "user@example.com");
        Method m = TargetType.class.getDeclaredMethod("getAttributeClass", Entry.class);
        m.setAccessible(true);

        // Act
        com.zimbra.cs.account.AttributeClass klass =
                (com.zimbra.cs.account.AttributeClass) m.invoke(null, acct);

        // Assert
        assertEquals(com.zimbra.cs.account.AttributeClass.account, klass);
    }

    @Test
    public void getAttrsInClassAccountReturnsNonEmptyAttrSet() throws Exception {
        // Arrange
        Account acct = prov.get(com.zimbra.common.account.Key.AccountBy.name, "user@example.com");

        // Act — delegates to AttributeManager for the account attribute class.
        java.util.Set<String> attrs = TargetType.getAttrsInClass(acct);

        // Assert — account class has many attributes; the mail attr is one of them.
        assertNotNull(attrs);
        assertTrue("account attr set should be non-empty", attrs.size() > 0);
    }

    @Test
    public void lookupTargetDomainByNameReturnsDomain() throws Exception {
        // Act
        Entry target = TargetType.lookupTarget(prov, TargetType.domain,
                TargetBy.name, "example.com");

        // Assert
        assertNotNull(target);
        assertTrue(target instanceof Domain);
        assertEquals("example.com", ((Domain) target).getName());
    }

    @Test
    public void lookupTargetMissingDomainMustFindThrowsNoSuchDomain() throws Exception {
        // Act / Assert
        try {
            TargetType.lookupTarget(prov, TargetType.domain, TargetBy.name, "ghost.example.com");
            fail("expected NO_SUCH_DOMAIN");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("no such domain"));
        }
    }

    @Test
    public void lookupTargetCosByNameReturnsCos() throws Exception {
        // Arrange
        prov.createCos("lookupcos", new HashMap<String, Object>());

        // Act
        Entry target = TargetType.lookupTarget(prov, TargetType.cos, TargetBy.name, "lookupcos");

        // Assert
        assertNotNull(target);
        assertTrue(target instanceof com.zimbra.cs.account.Cos);
    }

    @Test
    public void lookupTargetMissingCosMustFindThrowsNoSuchCos() throws Exception {
        // Act / Assert
        try {
            TargetType.lookupTarget(prov, TargetType.cos, TargetBy.name, "nosuchcos");
            fail("expected NO_SUCH_COS");
        } catch (ServiceException e) {
            assertEquals(com.zimbra.cs.account.AccountServiceException.NO_SUCH_COS, e.getCode());
        }
    }

    @Test
    public void lookupTargetServerByNameReturnsServer() throws Exception {
        // Arrange
        prov.createServer("lookupsrv.example.com", new HashMap<String, Object>());

        // Act
        Entry target = TargetType.lookupTarget(prov, TargetType.server,
                TargetBy.name, "lookupsrv.example.com");

        // Assert
        assertNotNull(target);
        assertTrue(target instanceof com.zimbra.cs.account.Server);
    }

    @Test
    public void lookupTargetMissingServerMustFindThrowsNoSuchServer() throws Exception {
        // Act / Assert — an unknown server name has no entry in the server map.
        try {
            TargetType.lookupTarget(prov, TargetType.server, TargetBy.name, "ghostsrv.example.com");
            fail("expected NO_SUCH_SERVER");
        } catch (ServiceException e) {
            assertEquals(com.zimbra.cs.account.AccountServiceException.NO_SUCH_SERVER, e.getCode());
        }
    }

    @Test
    public void lookupTargetGlobalGrantReturnsNullFromHarness() throws Exception {
        // Act — the global case returns prov.getGlobalGrant(), which the harness leaves null.
        Entry target = TargetType.lookupTarget(prov, TargetType.global, TargetBy.name, "globalgrant");

        // Assert — no mustFind guard on the global case, so a null result is returned as-is.
        assertNull(target);
    }

    @Test
    public void lookupTargetDlByNameNoFullDLMissingThrowsNoSuchDistributionList() throws Exception {
        // Act / Assert — getGroupBasic returns null in the harness, so mustFind throws.
        try {
            TargetType.lookupTarget(prov, TargetType.dl, TargetBy.name, "ghostlist@example.com");
            fail("expected NO_SUCH_DISTRIBUTION_LIST");
        } catch (ServiceException e) {
            assertEquals(com.zimbra.cs.account.AccountServiceException.NO_SUCH_DISTRIBUTION_LIST,
                    e.getCode());
        }
    }

    @Test
    public void lookupTargetGroupByNameMissingThrowsNoSuchDistributionList() throws Exception {
        // Act / Assert — dynamic group lookup also uses getGroupBasic (null in harness).
        try {
            TargetType.lookupTarget(prov, TargetType.group, TargetBy.name, "ghostgroup@example.com");
            fail("expected NO_SUCH_DISTRIBUTION_LIST");
        } catch (ServiceException e) {
            assertEquals(com.zimbra.cs.account.AccountServiceException.NO_SUCH_DISTRIBUTION_LIST,
                    e.getCode());
        }
    }

    @Test
    public void lookupTargetZimletByIdNotNameThrowsInvalidRequest() throws Exception {
        // Act / Assert — zimlets may only be looked up by name; by-id is rejected.
        try {
            TargetType.lookupTarget(prov, TargetType.zimlet, TargetBy.id, "somezimlet");
            fail("expected INVALID_REQUEST for zimlet by id");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("zimlet must be by name"));
        }
    }

    @Test
    public void getTargetTypeServerIsDomainedFalse() throws Exception {
        // Arrange / Act / Assert — server target type carries the non-domained flag.
        assertFalse(TargetType.server.isDomained());
        assertFalse(TargetType.cos.isDomained());
    }

    // ------------------------------------------------------------------
    // init() builds the inheritance metadata. The following tests pin the EXACT contents of
    // each target type's subTargetTypes()/inheritFrom() and the isInheritedBy() relation, so
    // that removing any setInheritedByTargetTypes(...) call (VoidMethodCall, L133-L169) or
    // flipping the two loop conditionals (NegateConditionals, L191/L197) makes them fail.
    // ------------------------------------------------------------------

    private static java.util.Set<TargetType> setOf(TargetType... tts) {
        return new java.util.HashSet<TargetType>(java.util.Arrays.asList(tts));
    }

    @Test
    public void initDlSubTargetTypesAreAccountAndCalresourceOnly() throws Exception {
        // L139: dl inheritedBy {account, calresource, dl}; subTargetTypes excludes self (L197).
        assertEquals(setOf(TargetType.account, TargetType.calresource),
                TargetType.dl.subTargetTypes());
        assertTrue("dl is inherited by account", TargetType.dl.isInheritedBy(TargetType.account));
        assertTrue("dl is inherited by calresource",
                TargetType.dl.isInheritedBy(TargetType.calresource));
        assertFalse("dl is NOT inherited by domain", TargetType.dl.isInheritedBy(TargetType.domain));
    }

    @Test
    public void initGroupSubTargetTypesAreAccountAndCalresourceOnly() throws Exception {
        // L142: group inheritedBy {account, calresource, group}.
        assertEquals(setOf(TargetType.account, TargetType.calresource),
                TargetType.group.subTargetTypes());
    }

    @Test
    public void initDomainSubTargetTypesAreAccountCalresourceDlGroup() throws Exception {
        // L145: domain inheritedBy {account, calresource, dl, group, domain}; self excluded.
        assertEquals(setOf(TargetType.account, TargetType.calresource, TargetType.dl,
                TargetType.group), TargetType.domain.subTargetTypes());
        assertTrue("domain is inherited by dl", TargetType.domain.isInheritedBy(TargetType.dl));
        assertTrue("domain is inherited by group", TargetType.domain.isInheritedBy(TargetType.group));
    }

    @Test
    public void initAccountAndServerHaveNoSubTargets() throws Exception {
        // L133/L151: account and server are inheritedBy only themselves, so subTargetTypes empty.
        assertTrue("account has no sub-targets", TargetType.account.subTargetTypes().isEmpty());
        assertTrue("server has no sub-targets", TargetType.server.subTargetTypes().isEmpty());
        assertTrue("calresource has no sub-targets",
                TargetType.calresource.subTargetTypes().isEmpty());
        assertTrue("cos has no sub-targets", TargetType.cos.subTargetTypes().isEmpty());
        assertTrue("config has no sub-targets", TargetType.config.subTargetTypes().isEmpty());
    }

    @Test
    public void initGlobalIsInheritedByEveryOtherType() throws Exception {
        // L169: global inheritedBy all 13 types; subTargetTypes is all except global itself.
        assertEquals(setOf(TargetType.account, TargetType.calresource, TargetType.cos,
                TargetType.dl, TargetType.group, TargetType.domain, TargetType.server,
                TargetType.alwaysoncluster, TargetType.ucservice, TargetType.xmppcomponent,
                TargetType.zimlet, TargetType.config), TargetType.global.subTargetTypes());
        assertFalse("global must not list itself as a sub-target (L197)",
                TargetType.global.subTargetTypes().contains(TargetType.global));
    }

    @Test
    public void initInheritFromAccountIsExactlyDlGroupDomainGlobal() throws Exception {
        // L191: account's inheritFrom() = every type whose inheritedBy contains account, i.e.
        // account itself (L133), dl (L139), group (L142), domain (L145), global (L169).
        assertEquals(setOf(TargetType.account, TargetType.dl, TargetType.group,
                TargetType.domain, TargetType.global), TargetType.account.inheritFrom());
    }

    @Test
    public void initInheritFromServerIsExactlyServerAndGlobal() throws Exception {
        // L191: server is listed in server's own (L151) and global's (L169) inheritedBy sets.
        assertEquals(setOf(TargetType.server, TargetType.global),
                TargetType.server.inheritFrom());
    }

    @Test
    public void getCommonBaseDn1FullSuffixOfDn2ReturnsShorterWhole() throws Exception {
        // Arrange - dn1 ("dc=com", 1 RDN) is a complete suffix of dn2 ("dc=foo,dc=com", 2 RDNs).
        // The shorter array (rdns1) MUST be picked at L686: the while loop runs i up to the
        // shorter length (1). If the comparison is negated/boundary-shifted so the LONGER array
        // is treated as "shorter", the loop runs one extra iteration and indexes rdns2 at -1,
        // throwing ArrayIndexOutOfBounds instead of returning the suffix.
        Method m = TargetType.class.getDeclaredMethod("getCommonBase", String.class, String.class);
        m.setAccessible(true);

        // Act
        String common = (String) m.invoke(null, "dc=com", "dc=foo,dc=com");

        // Assert - the whole shorter DN is the common base.
        assertEquals("dc=com", common);
    }

    @Test
    public void getCommonBaseDn2FullSuffixOfDn1ReturnsShorterWhole() throws Exception {
        // Arrange - mirror: dn2 is the 1-RDN full suffix of the 2-RDN dn1, pinning the other
        // outcome of the rdns1.length < rdns2.length comparison (L686).
        Method m = TargetType.class.getDeclaredMethod("getCommonBase", String.class, String.class);
        m.setAccessible(true);

        // Act
        String common = (String) m.invoke(null, "dc=foo,dc=com", "dc=com");

        // Assert
        assertEquals("dc=com", common);
    }

    @Test
    public void getCommonBaseSingleSharedRdnNoTrailingComma() throws Exception {
        // Arrange - exactly one shared RDN exercises the comma guard at L689: the single shared
        // RDN must be emitted with NO leading/trailing comma.
        Method m = TargetType.class.getDeclaredMethod("getCommonBase", String.class, String.class);
        m.setAccessible(true);

        // Act
        String common = (String) m.invoke(null, "ou=people,dc=com", "ou=groups,dc=com");

        // Assert
        assertEquals("dc=com", common);
    }

    @Test
    public void getTargetDomainNameDomainTargetReturnsNull() throws Exception {
        // Arrange — a Domain entry is neither account/cr/dl/dyngroup, so the helper returns null.
        Domain domain = prov.get(DomainBy.name, "example.com");

        // Act / Assert
        assertNull(TargetType.getTargetDomainName(prov, domain));
        assertNull(TargetType.getTargetDomain(prov, domain));
    }
}
