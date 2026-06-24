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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link XMPPComponent}. The component is constructed via its real public
 * constructor and wired to a real {@link Domain} created in the in-memory harness so that
 * {@link XMPPComponent#getDomain()} and {@link XMPPComponent#getShortName()} resolve against
 * actual provisioning state (no mocking of domain objects).
 */
public class XMPPComponentTest {

    private static Provisioning provisioning;

    private static String domainId;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initProvisioning();
        provisioning = Provisioning.getInstance();
        Domain domain = provisioning.createDomain("xmpp.example.com", new HashMap<String, Object>());
        domainId = domain.getId();
    }

    @Before
    public void setUp() throws Exception {
        provisioning = Provisioning.getInstance();
    }

    private XMPPComponent component(String name, Map<String, Object> attrs) {
        if (!attrs.containsKey(Provisioning.A_zimbraDomainId)) {
            attrs.put(Provisioning.A_zimbraDomainId, domainId);
        }
        return new XMPPComponent(name, "xmpp-id-" + name, attrs, provisioning);
    }

    private Map<String, Object> baseAttrs() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put(Provisioning.A_zimbraXMPPComponentCategory, "gateway");
        m.put(Provisioning.A_zimbraXMPPComponentType, "xmpp");
        m.put(Provisioning.A_zimbraXMPPComponentName, "Conference Service");
        m.put(Provisioning.A_zimbraXMPPComponentClassName, "com.example.Conf");
        return m;
    }

    // ---------- entry type ----------

    @Test
    public void getEntryTypeAlwaysIsXmppComponent() {
        // Arrange
        XMPPComponent c = component("conference.xmpp.example.com", baseAttrs());

        // Act / Assert
        assertEquals("entry type is XMPPCOMPONENT", EntryType.XMPPCOMPONENT, c.getEntryType());
    }

    // ---------- simple attribute getters ----------

    @Test
    public void attributeGettersPopulatedAttrsReturnConfiguredValues() {
        // Arrange
        XMPPComponent c = component("conference.xmpp.example.com", baseAttrs());

        // Act / Assert
        assertEquals("category getter", "gateway", c.getComponentCategory());
        assertEquals("type getter", "xmpp", c.getComponentType());
        assertEquals("long name getter", "Conference Service", c.getLongName());
        assertEquals("class name getter", "com.example.Conf", c.getClassName());
        assertEquals("domain id getter", domainId, c.getDomainId());
    }

    // ---------- getComponentFeatures ----------

    @Test
    public void getComponentFeaturesMultiValuedAttrReturnsAllFeatures() {
        // Arrange
        Map<String, Object> attrs = baseAttrs();
        attrs.put(Provisioning.A_zimbraXMPPComponentFeatures, new String[] {"muc", "pubsub"});
        XMPPComponent c = component("conference.xmpp.example.com", attrs);

        // Act
        List<String> features = c.getComponentFeatures();

        // Assert
        assertEquals("both features returned", 2, features.size());
        assertTrue("contains muc", features.contains("muc"));
        assertTrue("contains pubsub", features.contains("pubsub"));
    }

    @Test
    public void getComponentFeaturesNoFeaturesReturnsEmptyList() {
        // Arrange
        XMPPComponent c = component("conference.xmpp.example.com", baseAttrs());

        // Act
        List<String> features = c.getComponentFeatures();

        // Assert
        assertNotNull("never returns null", features);
        assertTrue("empty when no features attr present", features.isEmpty());
    }

    // ---------- getDomain ----------

    @Test
    public void getDomainValidDomainIdResolvesRealDomain() throws Exception {
        // Arrange
        XMPPComponent c = component("conference.xmpp.example.com", baseAttrs());

        // Act
        Domain d = c.getDomain();

        // Assert
        assertNotNull("domain resolved from harness", d);
        assertEquals("resolves the registered domain", "xmpp.example.com", d.getName());
    }

    // ---------- getShortName ----------

    @Test
    public void getShortNameNameIsSubdomainStripsDomainSuffix() throws Exception {
        // Arrange — name = conference.xmpp.example.com, domain = xmpp.example.com
        XMPPComponent c = component("conference.xmpp.example.com", baseAttrs());

        // Act
        String shortName = c.getShortName();

        // Assert
        assertEquals("domain suffix and dot stripped", "conference", shortName);
    }

    @Test
    public void getShortNameNameNotSubdomainThrowsFailure() throws Exception {
        // Arrange — name does NOT end with the domain name
        XMPPComponent c = component("conference.other.com", baseAttrs());

        // Act / Assert
        try {
            c.getShortName();
            fail("expected failure when name is not a subdomain of its domain");
        } catch (ServiceException e) {
            assertTrue("error mentions subdomain requirement",
                    e.getMessage().contains("subdomain"));
        }
    }

    @Test
    public void getShortNameMissingDomainThrowsFailure() throws Exception {
        // Arrange — point at a domain id that does not exist
        Map<String, Object> attrs = baseAttrs();
        attrs.put(Provisioning.A_zimbraDomainId, "no-such-domain-id");
        XMPPComponent c = new XMPPComponent("conference.ghost.com", "ghost-id", attrs, provisioning);

        // Act / Assert
        try {
            c.getShortName();
            fail("expected failure when domain does not exist");
        } catch (ServiceException e) {
            assertTrue("error mentions nonexistent domain",
                    e.getMessage().contains("nonexistent domain"));
        }
    }

    // ---------- toString ----------

    @Test
    public void toStringIncludesNameCategoryAndResolvedDomain() {
        // Arrange
        XMPPComponent c = component("conference.xmpp.example.com", baseAttrs());

        // Act
        String s = c.toString();

        // Assert — toString swallows domain-resolution exceptions; with a valid domain it adds the name
        assertTrue("includes component name", s.contains("conference.xmpp.example.com"));
        assertTrue("includes category value", s.contains("gateway"));
        assertTrue("includes resolved domain name", s.contains("xmpp.example.com"));
    }
}
