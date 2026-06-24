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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MailTarget;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.grouphandler.GroupHandler;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.ILdapContext;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link ExternalGroup}. The static {@code get}/{@code searchGroup} lookups
 * require a real LDAP-backed {@link com.zimbra.cs.account.ldap.LdapProv} and external directory and
 * are not reachable under the in-memory harness. These tests therefore drive the reachable
 * instance surface: the package-private constructor (run in-package), the {@code getDN}/
 * {@code getZimbraDomainId} getters and inherited name/id, and the {@code inGroup} dispatch — the
 * {@code MailTarget} overload short-circuits to {@code false} for a non-{@link Account} target
 * (no handler call), while the {@link Account} overload delegates to the injected
 * {@link GroupHandler#inDelegatedAdminGroup}. A lightweight in-memory {@link ZAttributes} and a
 * stub {@link GroupHandler} stand in for the external directory boundary only.
 */
public class ExternalGroupFunctionalTest {

    private static Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    /* Minimal in-memory ZAttributes — only getAttrs() is consulted by the ExternalGroup ctor. */
    private static ZAttributes attrs(final Map<String, Object> map) {
        return new ZAttributes() {
            @Override
            public Map<String, Object> getAttrs(Set<String> extraBinaryAttrs) throws LdapException {
                return map;
            }

            @Override
            protected String getAttrString(String transferAttrName, boolean containsBinaryData)
                    throws LdapException {
                Object v = map.get(transferAttrName);
                return v == null ? null : v.toString();
            }

            @Override
            protected String[] getMultiAttrString(String transferAttrName, boolean containsBinaryData)
                    throws LdapException {
                return new String[0];
            }

            @Override
            public boolean hasAttribute(String attrName) {
                return map.containsKey(attrName);
            }

            @Override
            public boolean hasAttributeValue(String attrName, String value) {
                Object v = map.get(attrName);
                return v != null && v.equals(value);
            }
        };
    }

    /* Stub external-directory group handler returning a controllable membership answer. */
    private static GroupHandler handler(final boolean member) {
        return new GroupHandler() {
            @Override
            public boolean isGroup(IAttributes ldapAttrs) {
                return true;
            }

            @Override
            public String[] getMembers(ILdapContext ldapContext, String searchBase,
                    String entryDN, IAttributes ldapAttrs) throws ServiceException {
                return new String[0];
            }

            @Override
            public boolean inDelegatedAdminGroup(ExternalGroup group, Account acct, boolean asAdmin)
                    throws ServiceException {
                return member;
            }
        };
    }

    private ExternalGroup makeGroup(String dn, String id, String name, String domainId,
            GroupHandler gh) throws Exception {
        return new ExternalGroup(dn, id, name, domainId, attrs(new HashMap<String, Object>()), gh, prov);
    }

    private Account createAccount(String email) throws Exception {
        Map<String, Object> attrMap = new HashMap<>();
        attrMap.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        return prov.createAccount(email, "test123", attrMap);
    }

    @Test
    public void constructorSetsDnAndZimbraDomainId() throws Exception {
        // Arrange / Act
        ExternalGroup group = makeGroup("cn=grp,dc=ext", "domid:grpname", "domname:grpname",
                "domid", handler(false));

        // Assert — explicit fields captured by the ctor
        assertEquals("cn=grp,dc=ext", group.getDN());
        assertEquals("domid", group.getZimbraDomainId());
    }

    @Test
    public void constructorSetsInheritedNameAndId() throws Exception {
        // Arrange / Act — name/id flow into the NamedEntry superclass
        ExternalGroup group = makeGroup("cn=g2,dc=ext", "did2:gname2", "dname2:gname2",
                "did2", handler(false));

        // Assert
        assertEquals("dname2:gname2", group.getName());
        assertEquals("did2:gname2", group.getId());
    }

    @Test
    public void externalGroupIsNamedEntry() throws Exception {
        // Arrange / Act
        ExternalGroup group = makeGroup("cn=g3,dc=ext", "did3:g3", "dn3:g3", "did3", handler(false));

        // Assert — ExternalGroup participates in the NamedEntry hierarchy
        assertTrue("ExternalGroup is a NamedEntry", group instanceof NamedEntry);
    }

    @Test
    public void getDNDistinctGroupsReturnDistinctValues() throws Exception {
        // Arrange
        ExternalGroup a = makeGroup("cn=a,dc=ext", "d:a", "dn:a", "d", handler(false));
        ExternalGroup b = makeGroup("cn=b,dc=ext", "d:b", "dn:b", "d", handler(false));

        // Act / Assert — getDN reflects each ctor argument independently
        assertEquals("cn=a,dc=ext", a.getDN());
        assertEquals("cn=b,dc=ext", b.getDN());
        assertFalse("distinct DNs", a.getDN().equals(b.getDN()));
    }

    @Test
    public void inGroupNonAccountMailTargetReturnsFalseWithoutHandler() throws Exception {
        // Arrange — handler would say "true", but a non-Account target must short-circuit to false
        ExternalGroup group = makeGroup("cn=g4,dc=ext", "did4:g4", "dn4:g4", "did4", handler(true));
        MailTarget notAnAccount = new MailTarget(
                "dl@example.com", UUID.randomUUID().toString(), new HashMap<String, Object>(), null, prov) {
        };

        // Act — MailTarget overload
        boolean result = group.inGroup(notAnAccount, true);

        // Assert — short-circuit, handler never consulted
        assertFalse("non-account target is never in an external admin group", result);
    }

    @Test
    public void inGroupAccountMemberDelegatesToHandlerTrue() throws Exception {
        // Arrange — handler reports membership
        ExternalGroup group = makeGroup("cn=g5,dc=ext", "did5:g5", "dn5:g5", "did5", handler(true));
        Account acct = createAccount("extmember@example.com");

        // Act — Account overload delegates to the handler
        boolean result = group.inGroup(acct, true);

        // Assert
        assertTrue("handler-confirmed membership is reported", result);
    }

    @Test
    public void inGroupAccountNonMemberDelegatesToHandlerFalse() throws Exception {
        // Arrange — handler denies membership
        ExternalGroup group = makeGroup("cn=g6,dc=ext", "did6:g6", "dn6:g6", "did6", handler(false));
        Account acct = createAccount("extnonmember@example.com");

        // Act
        boolean result = group.inGroup(acct, false);

        // Assert
        assertFalse("handler-denied membership is reported", result);
    }

    @Test
    public void inGroupMailTargetThatIsAccountDelegatesToHandler() throws Exception {
        // Arrange — Account passed via the MailTarget overload still routes to the handler
        ExternalGroup group = makeGroup("cn=g7,dc=ext", "did7:g7", "dn7:g7", "did7", handler(true));
        Account acct = createAccount("extmt@example.com");

        // Act — MailTarget overload, but target IS an Account
        boolean result = group.inGroup((MailTarget) acct, true);

        // Assert
        assertTrue("account via MailTarget overload routes to handler", result);
    }

    @Test
    public void getZimbraDomainIdMatchesConstructorArgument() throws Exception {
        // Arrange / Act
        ExternalGroup group = makeGroup("cn=g8,dc=ext", "theDomain:g8", "dn8:g8",
                "theDomain", handler(false));

        // Assert — same string instance flows through
        String domainId = "theDomain";
        assertSame("domain id retained verbatim", domainId.intern(), group.getZimbraDomainId().intern());
        assertEquals("theDomain", group.getZimbraDomainId());
    }

    @Test
    public void getByNameCacheMissFallsToSearchAndFailsWithoutLdapProv() throws Exception {
        // Arrange — the in-memory Provisioning is not an LdapProv, so the cache-miss
        // path drops into searchGroup() which immediately requires a real LdapProv.

        // Act / Assert — DomainBy.name selects the by-name cache lookup branch, misses,
        // then searchGroup() fails because LdapProv.getInst() rejects the mock provisioning.
        try {
            ExternalGroup.get(com.zimbra.common.account.Key.DomainBy.name, "dom:grp", true);
            fail("expected ServiceException - searchGroup requires a real LdapProv");
        } catch (ServiceException e) {
            assertTrue("expected an LdapProv-related failure",
                    e.getMessage() != null && e.getMessage().contains("LdapProv"));
        }
    }

    @Test
    public void getByIdCacheMissFallsToSearchAndFailsWithoutLdapProv() throws Exception {
        // Arrange — any DomainBy other than name selects the by-id cache lookup branch.

        // Act / Assert — DomainBy.id misses the cache then searchGroup() fails on LdapProv.getInst().
        try {
            ExternalGroup.get(com.zimbra.common.account.Key.DomainBy.id, "domid:grp", false);
            fail("expected ServiceException - searchGroup requires a real LdapProv");
        } catch (ServiceException e) {
            assertTrue("expected an LdapProv-related failure",
                    e.getMessage() != null && e.getMessage().contains("LdapProv"));
        }
    }
}
