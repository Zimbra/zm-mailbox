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

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.accesscontrol.Right.RightType;
import com.zimbra.cs.account.accesscontrol.RightCommand.ACE;
import com.zimbra.cs.account.accesscontrol.RightCommand.AllEffectiveRights;
import com.zimbra.cs.account.accesscontrol.RightCommand.DomainedRightsByTargetType;
import com.zimbra.cs.account.accesscontrol.RightCommand.EffectiveAttr;
import com.zimbra.cs.account.accesscontrol.RightCommand.EffectiveRights;
import com.zimbra.cs.account.accesscontrol.RightCommand.Grants;
import com.zimbra.cs.account.accesscontrol.RightCommand.RightAggregation;
import com.zimbra.cs.account.accesscontrol.RightCommand.RightsByTargetType;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link RightCommand}'s serialization/aggregation value classes -
 * {@link Grants}/{@link ACE}, {@link EffectiveRights}/{@link EffectiveAttr},
 * {@link RightsByTargetType} aggregation, and {@link AllEffectiveRights#toXML}. These exercise the
 * real XML round trips (build a SOAP {@code Element}, parse it into the model, serialize it back,
 * and re-parse) and the right-aggregation/dedup logic, none of which require an AccessManager or
 * LDAP. The LDAP/AccessManager-backed static methods (checkRight/getGrants/grantRight/...) are
 * intentionally not driven here (see "skipped" notes) because they require a real ACLAccessManager
 * and directory backend not available under the in-memory harness.
 */
public class RightCommandFunctionalTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        // boots AttributeManager (used by EffectiveRights XML attr parsing) and provisioning
        MailboxTestUtil.initServer();
    }

    /* Builds a &lt;grant&gt; element with the new nested target/grantee/right format. */
    private static Element grantElement(Element parent, String tType, String tId, String tName,
            String gType, String gId, String gName, String right, boolean deny, boolean canDelegate) {
        Element eGrant = parent.addNonUniqueElement(AdminConstants.E_GRANT);
        Element eTarget = eGrant.addNonUniqueElement(AdminConstants.E_TARGET);
        eTarget.addAttribute(AdminConstants.A_TYPE, tType);
        eTarget.addAttribute(AdminConstants.A_ID, tId);
        eTarget.addAttribute(AdminConstants.A_NAME, tName);
        Element eGrantee = eGrant.addNonUniqueElement(AdminConstants.E_GRANTEE);
        eGrantee.addAttribute(AdminConstants.A_TYPE, gType);
        eGrantee.addAttribute(AdminConstants.A_ID, gId);
        eGrantee.addAttribute(AdminConstants.A_NAME, gName);
        Element eRight = eGrant.addNonUniqueElement(AdminConstants.E_RIGHT);
        eRight.addAttribute(AdminConstants.A_DENY, deny);
        eRight.addAttribute(AdminConstants.A_CAN_DELEGATE, canDelegate);
        eRight.setText(right);
        return eGrant;
    }

    // ---- Grants / ACE: parse from XML ----

    @Test
    public void grantsFromXMLSingleGrantPopulatesAceFields() throws Exception {
        // Arrange — a parent element holding one grant
        Element parent = new Element.XMLElement("parent");
        grantElement(parent, "account", "tid-1", "user@zimbra.com",
                "usr", "gid-1", "grantee@zimbra.com", "viewFreeBusy", false, false);

        // Act
        Grants grants = new Grants(parent);

        // Assert — one ACE parsed with all fields intact
        assertEquals("one grant parsed", 1, grants.getACEs().size());
        ACE ace = grants.getACEs().iterator().next();
        assertEquals("account", ace.targetType());
        assertEquals("tid-1", ace.targetId());
        assertEquals("user@zimbra.com", ace.targetName());
        assertEquals("usr", ace.granteeType());
        assertEquals("gid-1", ace.granteeId());
        // granteeName() must return the parsed name verbatim, not an empty string. This pins the
        // EmptyObjectReturnVals mutant at L258 (granteeName), which would otherwise return "".
        assertEquals("grantee@zimbra.com", ace.granteeName());
        assertEquals("viewFreeBusy", ace.right());
        assertEquals("no modifier when neither deny nor canDelegate", null, ace.rightModifier());
    }

    @Test
    public void grantsFromXMLDenyFlagMapsToDenyModifier() throws Exception {
        // Arrange
        Element parent = new Element.XMLElement("parent");
        grantElement(parent, "account", "tid", "t@zimbra.com",
                "usr", "gid", "g@zimbra.com", "viewFreeBusy", true, false);

        // Act
        Grants grants = new Grants(parent);

        // Assert — deny=true => RM_DENY modifier
        ACE ace = grants.getACEs().iterator().next();
        assertEquals(RightModifier.RM_DENY, ace.rightModifier());
    }

    @Test
    public void grantsFromXMLCanDelegateFlagMapsToCanDelegateModifier() throws Exception {
        // Arrange
        Element parent = new Element.XMLElement("parent");
        grantElement(parent, "domain", "tid", "zimbra.com",
                "usr", "gid", "g@zimbra.com", "viewFreeBusy", false, true);

        // Act
        Grants grants = new Grants(parent);

        // Assert
        ACE ace = grants.getACEs().iterator().next();
        assertEquals(RightModifier.RM_CAN_DELEGATE, ace.rightModifier());
    }

    @Test
    public void grantsToXMLThenReparseRoundTripsAce() throws Exception {
        // Arrange — parse a grant, then serialize it back out
        Element in = new Element.XMLElement("parent");
        grantElement(in, "account", "tid-9", "rt@zimbra.com",
                "usr", "gid-9", "rg@zimbra.com", "viewFreeBusy", true, false);
        Grants grants = new Grants(in);

        // Act — toXML into a fresh element, then reparse
        Element out = new Element.XMLElement("parent");
        grants.toXML(out);
        Grants reparsed = new Grants(out);

        // Assert — survives a full round trip with the deny modifier preserved
        assertEquals(1, reparsed.getACEs().size());
        ACE ace = reparsed.getACEs().iterator().next();
        assertEquals("account", ace.targetType());
        assertEquals("gid-9", ace.granteeId());
        assertEquals("viewFreeBusy", ace.right());
        assertEquals(RightModifier.RM_DENY, ace.rightModifier());
    }

    @Test
    public void grantsToXMLDenyGrantEmitsExactBooleanModifierAttributes() throws Exception {
        // Arrange — parse a deny grant, then serialize it back. Grants.toXML maps the right
        // modifier into four boolean attributes on the emitted <right> element:
        //   deny                = (modifier == RM_DENY)                  [L178]
        //   canDelegate         = (modifier == RM_CAN_DELEGATE)          [L179]
        //   disinheritSubGroups = (modifier == RM_DISINHERIT_SUB_GROUPS) [L180]
        //   subDomain           = (modifier == RM_SUBDOMAIN)             [L181]
        // For a deny grant only deny must be true; the other three must be false. Negating any of
        // those comparisons flips the corresponding emitted attribute, which these exact-value
        // assertions catch.
        Element in = new Element.XMLElement("parent");
        grantElement(in, "account", "tid", "t@zimbra.com",
                "usr", "gid", "g@zimbra.com", "viewFreeBusy", true, false);
        Grants grants = new Grants(in);

        // Act
        Element out = new Element.XMLElement("parent");
        grants.toXML(out);

        // Assert — read the emitted <grant>/<right> element's boolean attributes precisely
        Element eRight = out.getElement(AdminConstants.E_GRANT).getElement(AdminConstants.E_RIGHT);
        assertTrue("deny grant must emit deny=true", eRight.getAttributeBool(AdminConstants.A_DENY));
        assertFalse("deny grant must emit canDelegate=false",
                eRight.getAttributeBool(AdminConstants.A_CAN_DELEGATE));
        assertFalse("deny grant must emit disinheritSubGroups=false",
                eRight.getAttributeBool(AdminConstants.A_DISINHERIT_SUB_GROUPS));
        assertFalse("deny grant must emit subDomain=false",
                eRight.getAttributeBool(AdminConstants.A_SUB_DOMAIN));
    }

    @Test
    public void grantsToXMLCanDelegateGrantEmitsExactBooleanModifierAttributes() throws Exception {
        // Arrange — the complementary case: a canDelegate grant. Only canDelegate must be true.
        // Together with the deny test above this pins L178 (deny) and L179 (canDelegate) on both
        // their true and false sides, plus the false side of L180/L181.
        Element in = new Element.XMLElement("parent");
        grantElement(in, "domain", "tid", "zimbra.com",
                "usr", "gid", "g@zimbra.com", "viewFreeBusy", false, true);
        Grants grants = new Grants(in);

        // Act
        Element out = new Element.XMLElement("parent");
        grants.toXML(out);

        // Assert
        Element eRight = out.getElement(AdminConstants.E_GRANT).getElement(AdminConstants.E_RIGHT);
        assertFalse("canDelegate grant must emit deny=false",
                eRight.getAttributeBool(AdminConstants.A_DENY));
        assertTrue("canDelegate grant must emit canDelegate=true",
                eRight.getAttributeBool(AdminConstants.A_CAN_DELEGATE));
        assertFalse("canDelegate grant must emit disinheritSubGroups=false",
                eRight.getAttributeBool(AdminConstants.A_DISINHERIT_SUB_GROUPS));
        assertFalse("canDelegate grant must emit subDomain=false",
                eRight.getAttributeBool(AdminConstants.A_SUB_DOMAIN));
    }

    @Test
    public void grantsFromXMLNoGrantsIsEmpty() throws Exception {
        // Arrange — element with no grant children
        Element parent = new Element.XMLElement("parent");

        // Act
        Grants grants = new Grants(parent);

        // Assert
        assertTrue("no grants => empty set", grants.getACEs().isEmpty());
    }

    @Test
    public void aceTargetIdNullAccessorReturnsEmptyString() throws Exception {
        // Arrange — omit target id attribute entirely
        Element parent = new Element.XMLElement("parent");
        Element eGrant = parent.addNonUniqueElement(AdminConstants.E_GRANT);
        eGrant.addNonUniqueElement(AdminConstants.E_TARGET).addAttribute(AdminConstants.A_TYPE, "account");
        eGrant.addNonUniqueElement(AdminConstants.E_GRANTEE).addAttribute(AdminConstants.A_TYPE, "usr");
        eGrant.addNonUniqueElement(AdminConstants.E_RIGHT).setText("viewFreeBusy");

        // Act
        ACE ace = new Grants(parent).getACEs().iterator().next();

        // Assert — targetId() coalesces a missing id to "" (the AdminConstants default fills "")
        assertEquals("", ace.targetId());
    }

    // ---- EffectiveRights: XML round trip for create-object attrs ----

    @Test
    public void effectiveRightsCreateObjectAttrsRoundTripPreservesAttrAndAllFlag() throws Exception {
        // Arrange — build the model and emit createObjectAttrs XML
        EffectiveRights er = new EffectiveRights("account", "tid", "t@zimbra.com", "gid", "g@zimbra.com");
        TreeMap<String, EffectiveAttr> setAttrs = new TreeMap<String, EffectiveAttr>();
        setAttrs.put("displayName", new EffectiveAttr("displayName", null, null));
        er.setCanSetAttrs(setAttrs);

        Element parent = new Element.XMLElement("parent");
        er.toXML_getCreateObjectAttrs(parent);

        // Act — parse it back
        EffectiveRights parsed = EffectiveRights.fromXML_CreateObjectAttrs(parent);

        // Assert — the single settable attr survived the round trip, all-flag stays false
        assertFalse("specific attrs, not all", parsed.canSetAllAttrs());
        assertTrue("displayName must be settable after round trip",
                parsed.canSetAttrs().containsKey("displayName"));
    }

    @Test
    public void effectiveRightsCreateObjectAttrsAllFlagRoundTripsAsAll() throws Exception {
        // Arrange — emit with the all-attrs flag set
        EffectiveRights er = new EffectiveRights("account", "tid", "t@zimbra.com", "gid", "g@zimbra.com");
        er.setCanSetAllAttrs();
        Element parent = new Element.XMLElement("parent");
        er.toXML_getCreateObjectAttrs(parent);

        // Act
        EffectiveRights parsed = EffectiveRights.fromXML_CreateObjectAttrs(parent);

        // Assert
        assertTrue("all-attrs flag must survive round trip", parsed.canSetAllAttrs());
    }

    @Test
    public void effectiveRightsAccessorsReflectConstructorArgs() throws Exception {
        // Arrange / Act
        EffectiveRights er = new EffectiveRights("domain", "dom-id", "zimbra.com", "g-id", "admin@zimbra.com");

        // Assert — accessors return the constructor values; null targetId coalesces in ctor only
        assertEquals("domain", er.targetType());
        assertEquals("dom-id", er.targetId());
        assertEquals("zimbra.com", er.targetName());
        assertEquals("g-id", er.granteeId());
        assertEquals("admin@zimbra.com", er.granteeName());
        assertTrue("no preset rights yet", er.presetRights().isEmpty());
    }

    @Test
    public void effectiveAttrNullDefaultReturnsEmptySetNotNull() throws Exception {
        // Arrange / Act
        EffectiveAttr ea = new EffectiveAttr("zimbraId", null, null);

        // Assert — getDefault never returns null
        assertNotNull(ea.getDefault());
        assertTrue("null default => empty set", ea.getDefault().isEmpty());
        assertEquals("zimbraId", ea.getAttrName());
    }

    // ---- RightsByTargetType aggregation / dedup ----

    @Test
    public void rightsByTargetTypeEntriesWithSameRightsAreAggregatedTogether() throws Exception {
        // Arrange — two entries with identical (empty) preset rights => identical digest
        RightsByTargetType rbtt = new RightsByTargetType();
        EffectiveRights er1 = presetRights("a@zimbra.com", new String[] {"viewFreeBusy"});
        EffectiveRights er2 = presetRights("b@zimbra.com", new String[] {"viewFreeBusy"});

        // Act — add both via the static add()
        RightsByTargetType.add(rbtt.entries(), "a@zimbra.com", er1);
        RightsByTargetType.add(rbtt.entries(), "b@zimbra.com", er2);

        // Assert — identical rights => single aggregation holding both names
        assertEquals("same rights collapse into one aggregation", 1, rbtt.entries().size());
        RightCommand.RightAggregation ra = rbtt.entries().iterator().next();
        assertEquals(2, ra.entries().size());
        assertTrue(ra.entries().contains("a@zimbra.com"));
        assertTrue(ra.entries().contains("b@zimbra.com"));
    }

    @Test
    public void rightsByTargetTypeEntriesWithDifferentRightsAreSeparate() throws Exception {
        // Arrange — different preset rights => different digests
        RightsByTargetType rbtt = new RightsByTargetType();
        EffectiveRights er1 = presetRights("a@zimbra.com", new String[] {"viewFreeBusy"});
        EffectiveRights er2 = presetRights("b@zimbra.com", new String[] {"invite"});

        // Act
        RightsByTargetType.add(rbtt.entries(), "a@zimbra.com", er1);
        RightsByTargetType.add(rbtt.entries(), "b@zimbra.com", er2);

        // Assert — two distinct aggregations
        assertEquals("different rights => separate aggregations", 2, rbtt.entries().size());
    }

    @Test
    public void rightsByTargetTypeHasNoRightTrueWhenEmpty() throws Exception {
        // Arrange / Act
        RightsByTargetType rbtt = new RightsByTargetType();

        // Assert
        assertTrue("empty rbtt has no rights", rbtt.hasNoRight());
        assertEquals(null, rbtt.all());
    }

    @Test
    public void rightsByTargetTypeAddAggregationMergesIntoExistingWithSameRights() throws Exception {
        // Arrange — seed an aggregation, then add a set of names with identical rights
        RightsByTargetType rbtt = new RightsByTargetType();
        EffectiveRights er = presetRights("seed@zimbra.com", new String[] {"viewFreeBusy"});
        RightsByTargetType.add(rbtt.entries(), "seed@zimbra.com", er);

        Set<String> more = new java.util.HashSet<String>();
        more.add("x@zimbra.com");
        more.add("y@zimbra.com");
        EffectiveRights erSame = presetRights("ignored", new String[] {"viewFreeBusy"});

        // Act
        RightsByTargetType.addAggregation(rbtt.entries(), more, erSame);

        // Assert — merged into the single existing aggregation
        assertEquals(1, rbtt.entries().size());
        RightCommand.RightAggregation ra = rbtt.entries().iterator().next();
        assertTrue(ra.entries().contains("seed@zimbra.com"));
        assertTrue(ra.entries().contains("x@zimbra.com"));
        assertTrue(ra.entries().contains("y@zimbra.com"));
    }

    // ---- AllEffectiveRights.toXML ----

    @Test
    public void allEffectiveRightsToXMLEmitsGranteeAndPerTargetType() throws Exception {
        // Arrange — a grantee with one "all" rights entry on the account target type
        AllEffectiveRights aer = new AllEffectiveRights("usr", "g-id", "admin@zimbra.com");
        EffectiveRights allAcct = presetRights(null, new String[] {"viewFreeBusy"});
        aer.setAll(TargetType.account, allAcct);

        // Act
        Element parent = new Element.XMLElement("parent");
        aer.toXML(parent);

        // Assert — grantee element carries the id/type, and a target element exists per target type
        Element eGrantee = parent.getElement(AdminConstants.E_GRANTEE);
        assertEquals("g-id", eGrantee.getAttribute(AdminConstants.A_ID));
        assertEquals("usr", eGrantee.getAttribute(AdminConstants.A_TYPE));

        boolean sawAccountTarget = false;
        for (Element eTarget : parent.listElements(AdminConstants.E_TARGET)) {
            if ("account".equals(eTarget.getAttribute(AdminConstants.A_TYPE))) {
                sawAccountTarget = true;
                assertNotNull("account target must carry an <all> rights block",
                        eTarget.getOptionalElement(AdminConstants.E_ALL));
            }
        }
        assertTrue("toXML must emit an account target element", sawAccountTarget);
    }

    @Test
    public void allEffectiveRightsAccessorsReflectGrantee() throws Exception {
        // Arrange / Act
        AllEffectiveRights aer = new AllEffectiveRights("grp", "gid", "team@zimbra.com");

        // Assert
        assertEquals("grp", aer.granteeType());
        assertEquals("gid", aer.granteeId());
        assertEquals("team@zimbra.com", aer.granteeName());
        assertFalse("every target type pre-seeded", aer.rightsByTargetType().isEmpty());
    }

    /* Helper: an EffectiveRights with the given preset right names (drives the digest path). */
    private static EffectiveRights presetRights(String targetName, String[] rights) {
        EffectiveRights er = new EffectiveRights("account", "tid-" + targetName, targetName, "gid", "g@zimbra.com");
        List<String> preset = new ArrayList<String>();
        for (String r : rights) {
            preset.add(r);
        }
        er.setPresetRights(preset);
        return er;
    }

    // ---- EffectiveRights.toXML_getEffectiveRights (grantee + target + preset/set/get) ----

    @Test
    public void toXMLGetEffectiveRightsEmitsGranteeTargetAndPresetRights() throws Exception {
        // Arrange — an EffectiveRights carrying a preset right and a settable attr with a default
        EffectiveRights er = new EffectiveRights("account", "tid-1", "u@zimbra.com", "gid-1", "admin@zimbra.com");
        List<String> preset = new ArrayList<String>();
        preset.add("viewFreeBusy");
        er.setPresetRights(preset);

        TreeMap<String, EffectiveAttr> setAttrs = new TreeMap<String, EffectiveAttr>();
        Set<String> defaults = new HashSet<String>();
        defaults.add("Sales");
        setAttrs.put("displayName", new EffectiveAttr("displayName", defaults, null));
        er.setCanSetAttrs(setAttrs);

        // Act
        Element parent = new Element.XMLElement("parent");
        er.toXML_getEffectiveRights(parent);

        // Assert — grantee and target elements carry the ids
        Element eGrantee = parent.getElement(AdminConstants.E_GRANTEE);
        assertEquals("gid-1", eGrantee.getAttribute(AdminConstants.A_ID));
        assertEquals("admin@zimbra.com", eGrantee.getAttribute(AdminConstants.A_NAME));

        Element eTarget = parent.getElement(AdminConstants.E_TARGET);
        assertEquals("account", eTarget.getAttribute(AdminConstants.A_TYPE));
        assertEquals("tid-1", eTarget.getAttribute(AdminConstants.A_ID));

        // preset right rendered under the target
        boolean sawPreset = false;
        for (Element eRight : eTarget.listElements(AdminConstants.E_RIGHT)) {
            if ("viewFreeBusy".equals(eRight.getAttribute(AdminConstants.A_N))) {
                sawPreset = true;
            }
        }
        assertTrue("preset right must be emitted under target", sawPreset);

        // settable attr with its default value rendered under <setAttrs>
        Element eSetAttrs = eTarget.getElement(AdminConstants.E_SET_ATTRS);
        Element eA = eSetAttrs.getElement(AdminConstants.E_A);
        assertEquals("displayName", eA.getAttribute(AdminConstants.A_N));
        Element eDefault = eA.getElement(AdminConstants.E_DEFAULT);
        assertEquals("Sales", eDefault.getElement(AdminConstants.E_VALUE).getText());
    }

    @Test
    public void effectiveRightsGetAttrsRoundTripViaCanGetAttrs() throws Exception {
        // Arrange — exercise the canGetAttrs accessors and all-flag setter
        EffectiveRights er = new EffectiveRights("account", "tid", "t@zimbra.com", "gid", "g@zimbra.com");
        er.setCanGetAllAttrs();
        TreeMap<String, EffectiveAttr> getAttrs = new TreeMap<String, EffectiveAttr>();
        getAttrs.put("zimbraId", new EffectiveAttr("zimbraId", null, null));
        er.setCanGetAttrs(getAttrs);

        // Assert
        assertTrue(er.canGetAllAttrs());
        assertTrue(er.canGetAttrs().containsKey("zimbraId"));
        assertFalse("set-all not toggled", er.canSetAllAttrs());
    }

    // ---- EffectiveAttr with an explicit default ------------------------------------------

    @Test
    public void effectiveAttrWithDefaultReturnsProvidedSet() throws Exception {
        // Arrange
        Set<String> defaults = new HashSet<String>();
        defaults.add("v1");
        defaults.add("v2");

        // Act
        EffectiveAttr ea = new EffectiveAttr("zimbraFoo", defaults, null);

        // Assert — getDefault returns the supplied (non-empty) set
        assertEquals(2, ea.getDefault().size());
        assertTrue(ea.getDefault().contains("v1"));
        assertEquals("zimbraFoo", ea.getAttrName());
    }

    // ---- RightsByTargetType.setAll / all() -----------------------------------------------

    @Test
    public void rightsByTargetTypeSetAllThenAllReturnsItAndHasRight() throws Exception {
        // Arrange
        RightsByTargetType rbtt = new RightsByTargetType();
        EffectiveRights all = presetRights("all", new String[] {"viewFreeBusy"});

        // Act
        rbtt.setAll(all);

        // Assert — all() exposes the set EffectiveRights and hasNoRight flips false
        assertNotNull(rbtt.all());
        assertSame(all, rbtt.all());
        assertFalse("setting all means it has a right", rbtt.hasNoRight());
    }

    // ---- DomainedRightsByTargetType.addDomainEntry ---------------------------------------

    @Test
    public void domainedRightsByTargetTypeAddDomainEntryAggregatesByDomain() throws Exception {
        // Arrange
        DomainedRightsByTargetType drbtt = new DomainedRightsByTargetType();
        EffectiveRights er1 = presetRights("d1", new String[] {"viewFreeBusy"});
        EffectiveRights er2 = presetRights("d2", new String[] {"viewFreeBusy"});

        // Act — identical rights collapse into one domain aggregation
        drbtt.addDomainEntry("domain1.com", er1);
        drbtt.addDomainEntry("domain2.com", er2);

        // Assert
        assertEquals("same rights => single domain aggregation", 1, drbtt.domains().size());
        RightAggregation ra = drbtt.domains().iterator().next();
        assertTrue(ra.entries().contains("domain1.com"));
        assertTrue(ra.entries().contains("domain2.com"));
        assertFalse("domain entries mean it has rights", drbtt.hasNoRight());
    }

    // ---- AllEffectiveRights: addEntry / setAll / toXML domained branch -------------------

    @Test
    public void allEffectiveRightsAddEntryAndSetAllThenToXMLEmitsEntriesAndAll() throws Exception {
        // Arrange — account is a "domained" target type, so the toXML domained branch is exercised
        AllEffectiveRights aer = new AllEffectiveRights("usr", "g-id", "admin@zimbra.com");
        EffectiveRights allAcct = presetRights("all", new String[] {"viewFreeBusy"});
        aer.setAll(TargetType.account, allAcct);
        EffectiveRights entryRights = presetRights("entry", new String[] {"invite"});
        aer.addEntry(TargetType.account, "specific@zimbra.com", entryRights);

        // Act
        Element parent = new Element.XMLElement("parent");
        aer.toXML(parent);

        // Assert — account target carries both an <all> block and an <entries> block
        boolean sawAll = false;
        boolean sawEntry = false;
        for (Element eTarget : parent.listElements(AdminConstants.E_TARGET)) {
            if ("account".equals(eTarget.getAttribute(AdminConstants.A_TYPE))) {
                if (eTarget.getOptionalElement(AdminConstants.E_ALL) != null) {
                    sawAll = true;
                }
                for (Element eEntries : eTarget.listElements(AdminConstants.E_ENTRIES)) {
                    for (Element eEntry : eEntries.listElements(AdminConstants.E_ENTRY)) {
                        if ("specific@zimbra.com".equals(eEntry.getAttribute(AdminConstants.A_NAME))) {
                            sawEntry = true;
                        }
                    }
                }
            }
        }
        assertTrue("account target must carry <all>", sawAll);
        assertTrue("account target must carry the specific entry", sawEntry);
    }

    @Test
    public void allEffectiveRightsAddEntryWithNoRightIsIgnored() throws Exception {
        // Arrange — an EffectiveRights with no rights at all
        AllEffectiveRights aer = new AllEffectiveRights("usr", "g-id", "admin@zimbra.com");
        EffectiveRights empty = new EffectiveRights("account", "tid", "t@zimbra.com", "gid", "g@zimbra.com");

        // Act — addEntry short-circuits on hasNoRight()
        aer.addEntry(TargetType.account, "x@zimbra.com", empty);

        // Assert — the account RightsByTargetType still reports no rights
        assertTrue(aer.rightsByTargetType().get(TargetType.account).hasNoRight());
    }

    // ---- rightToXML for preset / attr / combo rights -------------------------------------

    @Test
    public void rightToXMLPresetRightEmitsNameTypeTargetTypeAndDesc() throws Exception {
        // Arrange — a user preset right with a target type set
        PresetRight pr = new PresetRight("test.preset.right");
        pr.setTargetType(TargetType.account);
        pr.setDesc("a preset right");

        // Act
        Element parent = new Element.XMLElement("parent");
        Element eRight = RightCommand.rightToXML(parent, pr, false, null);

        // Assert
        assertEquals("test.preset.right", eRight.getAttribute(AdminConstants.E_NAME));
        assertEquals("account", eRight.getAttribute(AdminConstants.A_TARGET_TYPE));
        assertNotNull("preset right carries a desc element", eRight.getElement(AdminConstants.E_DESC));
    }

    @Test
    public void rightToXMLAttrRightSpecificAttrsEmitsAttrsBlock() throws Exception {
        // Arrange — a getAttrs right scoped to a couple of named attributes
        AttrRight ar = new AttrRight("test.attr.right", RightType.getAttrs);
        ar.setTargetType(TargetType.account);
        ar.addAttr("displayName");
        ar.addAttr("zimbraId");

        // Act
        Element parent = new Element.XMLElement("parent");
        Element eRight = RightCommand.rightToXML(parent, ar, false, null);

        // Assert — an <attrs> element with the named attributes, not the all-flag
        Element eAttrs = eRight.getElement(AdminConstants.E_ATTRS);
        assertNull("specific attrs => no all flag", eAttrs.getAttribute(AdminConstants.A_ALL, null));
        assertNotNull(eAttrs.getOptionalElement("displayName"));
        assertNotNull(eAttrs.getOptionalElement("zimbraId"));
    }

    @Test
    public void rightToXMLAttrRightAllAttrsEmitsAllFlag() throws Exception {
        // Arrange — a getAttrs right with no specific attrs => allAttrs() is true
        AttrRight ar = new AttrRight("test.attr.all", RightType.getAttrs);
        ar.setTargetType(TargetType.account);

        // Act — expandAllAttrs=false: only the all-flag is emitted, no attribute enumeration
        Element parent = new Element.XMLElement("parent");
        Element eRight = RightCommand.rightToXML(parent, ar, false, null);

        // Assert
        Element eAttrs = eRight.getElement(AdminConstants.E_ATTRS);
        assertTrue("all-attrs flag must be set", eAttrs.getAttributeBool(AdminConstants.A_ALL, false));
    }

    @Test
    public void effectiveAttrGetConstraintReturnsProvidedConstraintNotNull() throws Exception {
        // Arrange — give the EffectiveAttr a real (non-null) AttributeConstraint. getConstraint()
        // (L286) must return that exact instance. The NullReturnVals mutant would return null here.
        AttributeConstraint constraint = new AttributeConstraint("zimbraMailQuota");
        EffectiveAttr ea = new EffectiveAttr("zimbraMailQuota", null, constraint);

        // Assert — exact same instance, not null
        assertNotNull("getConstraint must not return null when one was supplied", ea.getConstraint());
        assertSame(constraint, ea.getConstraint());
    }

    @Test
    public void effectiveAttrGetDefaultWithValuesIsNotEmptyAndNullIsEmpty() throws Exception {
        // Assert — getDefault() (L280) returns the provided non-empty set for one attr and the
        // shared EMPTY_SET for a null default. Pins the EmptyObjectReturnVals mutant which would
        // force an empty set even when a non-empty default was supplied.
        Set<String> defaults = new HashSet<String>();
        defaults.add("only");
        EffectiveAttr withDefault = new EffectiveAttr("a", defaults, null);
        EffectiveAttr noDefault = new EffectiveAttr("b", null, null);

        assertFalse("supplied default must not be reported as empty", withDefault.getDefault().isEmpty());
        assertEquals(1, withDefault.getDefault().size());
        assertTrue("null default coalesces to empty set", noDefault.getDefault().isEmpty());
    }

    @Test
    public void effectiveRightsCanGetAllAttrsDefaultsFalse() throws Exception {
        // Arrange — a freshly constructed EffectiveRights has not toggled the get-all flag.
        // canGetAllAttrs() (L571) must return false; the BooleanTrueReturnVals mutant forces true.
        EffectiveRights er = new EffectiveRights("account", "tid", "t@zimbra.com", "gid", "g@zimbra.com");

        // Assert
        assertFalse("canGetAllAttrs defaults false", er.canGetAllAttrs());
        // and flips to true only after the setter (true side already covered elsewhere)
        er.setCanGetAllAttrs();
        assertTrue(er.canGetAllAttrs());
    }

    @Test
    public void effectiveRightsDigestDiffersWhenAllAttrsFlagSet() throws Exception {
        // The aggregation dedup uses getDigest(). The digest branches on mCanSetAllAttrs (L355) and
        // mCanGetAllAttrs (L364): "all;" vs the hash of the (empty) attr key list. Two otherwise
        // identical EffectiveRights — one with set-all on, one off — must therefore produce DISTINCT
        // digests, which here surfaces as TWO separate aggregations rather than one merged. Negating
        // either conditional would make the digests collapse and the entries merge.
        RightsByTargetType rbtt = new RightsByTargetType();
        EffectiveRights plain = new EffectiveRights("account", "tid-a", "a@zimbra.com", "gid", "g@zimbra.com");
        EffectiveRights setAll = new EffectiveRights("account", "tid-b", "b@zimbra.com", "gid", "g@zimbra.com");
        setAll.setCanSetAllAttrs();

        RightsByTargetType.add(rbtt.entries(), "a@zimbra.com", plain);
        RightsByTargetType.add(rbtt.entries(), "b@zimbra.com", setAll);

        assertEquals("set-all flag must change the digest => no merge", 2, rbtt.entries().size());

        // Same comparison for the get-all flag.
        RightsByTargetType rbtt2 = new RightsByTargetType();
        EffectiveRights plain2 = new EffectiveRights("account", "tid-c", "c@zimbra.com", "gid", "g@zimbra.com");
        EffectiveRights getAll = new EffectiveRights("account", "tid-d", "d@zimbra.com", "gid", "g@zimbra.com");
        getAll.setCanGetAllAttrs();
        RightsByTargetType.add(rbtt2.entries(), "c@zimbra.com", plain2);
        RightsByTargetType.add(rbtt2.entries(), "d@zimbra.com", getAll);
        assertEquals("get-all flag must change the digest => no merge", 2, rbtt2.entries().size());
    }

    @Test
    public void rightsByTargetTypeAddRelocatesEntryWhenRightsChange() throws Exception {
        // The static add() first scans for a RightAggregation that already holds the name and, if
        // found (hasEntry L615 true => the L654 "if (ra.hasEntry(name))" branch), removes it before
        // re-aggregating under the new rights. Re-adding "x" with DIFFERENT rights must therefore
        // move it out of the old aggregation into a new one, leaving exactly two aggregations with
        // "x" appearing once. Mutating hasEntry (always true/false) or negating the L654 branch
        // would either strand a stale duplicate or skip the relocation.
        RightsByTargetType rbtt = new RightsByTargetType();
        EffectiveRights freeBusy = presetRights("seed", new String[] {"viewFreeBusy"});
        EffectiveRights freeBusy2 = presetRights("other", new String[] {"viewFreeBusy"});
        // seed an aggregation containing both "x" and "y" under the viewFreeBusy rights
        RightsByTargetType.add(rbtt.entries(), "x@zimbra.com", freeBusy);
        RightsByTargetType.add(rbtt.entries(), "y@zimbra.com", freeBusy2);
        assertEquals("same rights merged into one aggregation", 1, rbtt.entries().size());

        // Act — re-add "x" with a DIFFERENT right; it must be relocated, not duplicated
        EffectiveRights invite = presetRights("reloc", new String[] {"invite"});
        RightsByTargetType.add(rbtt.entries(), "x@zimbra.com", invite);

        // Assert — two aggregations now; "x" appears exactly once, in the invite group
        assertEquals("relocation must split into two aggregations", 2, rbtt.entries().size());
        int countX = 0;
        boolean yStillWithFreeBusy = false;
        for (RightAggregation ra : rbtt.entries()) {
            if (ra.entries().contains("x@zimbra.com")) {
                countX++;
            }
            if (ra.entries().contains("y@zimbra.com") && ra.entries().size() == 1) {
                yStillWithFreeBusy = true;
            }
        }
        assertEquals("x must appear in exactly one aggregation after relocation", 1, countX);
        assertTrue("y must remain alone in its original aggregation", yStillWithFreeBusy);
    }

    @Test
    public void allEffectiveRightsConstructorUsesDomainedTypeForDomainedTargetsOnly() throws Exception {
        // The ctor (L749 "if (tt.isDomained())") seeds DomainedRightsByTargetType for domained
        // target types and a plain RightsByTargetType otherwise. account/dl are domained; cos/
        // domain/server are not. Negating the conditional would swap these subtypes.
        AllEffectiveRights aer = new AllEffectiveRights("usr", "g-id", "admin@zimbra.com");

        assertTrue("account is domained => DomainedRightsByTargetType",
                aer.rightsByTargetType().get(TargetType.account) instanceof DomainedRightsByTargetType);
        assertTrue("dl is domained => DomainedRightsByTargetType",
                aer.rightsByTargetType().get(TargetType.dl) instanceof DomainedRightsByTargetType);

        assertFalse("cos is NOT domained => plain RightsByTargetType",
                aer.rightsByTargetType().get(TargetType.cos) instanceof DomainedRightsByTargetType);
        assertFalse("domain is NOT domained => plain RightsByTargetType",
                aer.rightsByTargetType().get(TargetType.domain) instanceof DomainedRightsByTargetType);
        assertFalse("server is NOT domained => plain RightsByTargetType",
                aer.rightsByTargetType().get(TargetType.server) instanceof DomainedRightsByTargetType);
    }

    @Test
    public void allEffectiveRightsToXMLAllBlockCarriesPresetRightContent() throws Exception {
        // The toXML domained branch emits er.toXML(eAll) at L856. If that call were removed the
        // <all> element would be present but EMPTY (no preset <right> child). Assert the preset
        // right name actually appears inside the <all> block.
        AllEffectiveRights aer = new AllEffectiveRights("usr", "g-id", "admin@zimbra.com");
        EffectiveRights allAcct = presetRights("all", new String[] {"viewFreeBusy"});
        aer.setAll(TargetType.account, allAcct);

        Element parent = new Element.XMLElement("parent");
        aer.toXML(parent);

        boolean sawPresetInAll = false;
        for (Element eTarget : parent.listElements(AdminConstants.E_TARGET)) {
            if ("account".equals(eTarget.getAttribute(AdminConstants.A_TYPE))) {
                Element eAll = eTarget.getOptionalElement(AdminConstants.E_ALL);
                if (eAll != null) {
                    for (Element eRight : eAll.listElements(AdminConstants.E_RIGHT)) {
                        if ("viewFreeBusy".equals(eRight.getAttribute(AdminConstants.A_N))) {
                            sawPresetInAll = true;
                        }
                    }
                }
            }
        }
        assertTrue("<all> block must contain the preset right (er.toXML must run)", sawPresetInAll);
    }

    @Test
    public void allEffectiveRightsToXMLEntriesBlockCarriesPresetRightContent() throws Exception {
        // The toXML per-entries loop emits er.toXML(eRights) at L883. If removed, the <entries>
        // <rights> element would be empty. Assert the preset right appears inside the entry's
        // <rights> block.
        AllEffectiveRights aer = new AllEffectiveRights("usr", "g-id", "admin@zimbra.com");
        EffectiveRights entryRights = presetRights("entry", new String[] {"invite"});
        aer.addEntry(TargetType.account, "specific@zimbra.com", entryRights);

        Element parent = new Element.XMLElement("parent");
        aer.toXML(parent);

        boolean sawPresetInEntries = false;
        for (Element eTarget : parent.listElements(AdminConstants.E_TARGET)) {
            if ("account".equals(eTarget.getAttribute(AdminConstants.A_TYPE))) {
                for (Element eEntries : eTarget.listElements(AdminConstants.E_ENTRIES)) {
                    Element eRights = eEntries.getOptionalElement(AdminConstants.E_RIGHTS);
                    if (eRights != null) {
                        for (Element eRight : eRights.listElements(AdminConstants.E_RIGHT)) {
                            if ("invite".equals(eRight.getAttribute(AdminConstants.A_N))) {
                                sawPresetInEntries = true;
                            }
                        }
                    }
                }
            }
        }
        assertTrue("<entries> rights block must contain the preset right (er.toXML must run)",
                sawPresetInEntries);
    }

    @Test
    public void toXMLGetEffectiveRightsEmitsGetAttrsBlock() throws Exception {
        // EffectiveRights.toXML(eParent) emits preset, setAttrs, AND getAttrs (the L525 call
        // "toXML(eParent, E_GET_ATTRS, ...)"). If that call were removed the <getAttrs> element
        // would be absent. Drive a gettable attr and assert it surfaces under <getAttrs>.
        EffectiveRights er = new EffectiveRights("account", "tid-1", "u@zimbra.com", "gid-1", "admin@zimbra.com");
        TreeMap<String, EffectiveAttr> getAttrs = new TreeMap<String, EffectiveAttr>();
        getAttrs.put("zimbraId", new EffectiveAttr("zimbraId", null, null));
        er.setCanGetAttrs(getAttrs);

        Element parent = new Element.XMLElement("parent");
        er.toXML_getEffectiveRights(parent);

        Element eTarget = parent.getElement(AdminConstants.E_TARGET);
        Element eGetAttrs = eTarget.getOptionalElement(AdminConstants.E_GET_ATTRS);
        assertNotNull("toXML must emit a <getAttrs> block", eGetAttrs);
        Element eA = eGetAttrs.getElement(AdminConstants.E_A);
        assertEquals("zimbraId", eA.getAttribute(AdminConstants.A_N));
    }

    @Test
    public void rightToXMLPresetRightDescFallsBackToRightGetDescWhenNoLocalizedMessage() throws Exception {
        // rightToXML looks up a localized description; when none exists (L1569 "if (desc == null)")
        // it falls back to right.getDesc(). With no rights message file loaded for this synthetic
        // right name, the localized lookup returns null, so the emitted <desc> must equal the
        // right's own description. Negating L1569 would skip the fallback and emit a null/empty desc.
        PresetRight pr = new PresetRight("test.preset.descfallback");
        pr.setTargetType(TargetType.account);
        pr.setDesc("the fallback description");

        Element parent = new Element.XMLElement("parent");
        Element eRight = RightCommand.rightToXML(parent, pr, false, null);

        Element eDesc = eRight.getElement(AdminConstants.E_DESC);
        assertEquals("desc must fall back to right.getDesc()", "the fallback description", eDesc.getText());
    }

    @Test
    public void rightToXMLAttrRightAllAttrsExpandTrueEnumeratesAttrsFalseDoesNot() throws Exception {
        // For an all-attrs AttrRight, rightToXML emits the all-flag and, only when expandAllAttrs is
        // true (L1598 "if (expandAllAtrts)"), enumerates the concrete attribute names as <a> children.
        // With expand=false there must be NO <a> children; with expand=true there must be some.
        // Negating L1598 would invert this enumeration.
        AttrRight ar = new AttrRight("test.attr.expand", RightType.getAttrs);
        ar.setTargetType(TargetType.account);

        // expand = false => only the all-flag, no enumerated <a> attributes
        Element pFalse = new Element.XMLElement("parent");
        Element eRightFalse = RightCommand.rightToXML(pFalse, ar, false, null);
        Element eAttrsFalse = eRightFalse.getElement(AdminConstants.E_ATTRS);
        assertTrue("all-flag set", eAttrsFalse.getAttributeBool(AdminConstants.A_ALL, false));
        assertTrue("expand=false must NOT enumerate attribute children",
                eAttrsFalse.listElements(AdminConstants.E_A).isEmpty());

        // expand = true => the concrete account attributes are enumerated
        Element pTrue = new Element.XMLElement("parent");
        Element eRightTrue = RightCommand.rightToXML(pTrue, ar, true, null);
        Element eAttrsTrue = eRightTrue.getElement(AdminConstants.E_ATTRS);
        assertTrue("all-flag still set", eAttrsTrue.getAttributeBool(AdminConstants.A_ALL, false));
        assertFalse("expand=true must enumerate the account attribute children",
                eAttrsTrue.listElements(AdminConstants.E_A).isEmpty());
    }

    @Test
    public void rightToXMLComboRightEmitsNestedRights() throws Exception {
        // Arrange — a combo right containing a preset member right
        ComboRight combo = new ComboRight("test.combo.right");
        PresetRight member = new PresetRight("test.member.right");
        member.setTargetType(TargetType.account);
        combo.addRight(member);

        // Act
        Element parent = new Element.XMLElement("parent");
        Element eRight = RightCommand.rightToXML(parent, combo, false, null);

        // Assert — a <rights> element enumerating the nested member right by name
        Element eRights = eRight.getElement(AdminConstants.E_RIGHTS);
        boolean sawMember = false;
        for (Element eR : eRights.listElements(AdminConstants.E_R)) {
            if ("test.member.right".equals(eR.getAttribute(AdminConstants.A_N))) {
                sawMember = true;
            }
        }
        assertTrue("combo right must enumerate its nested member", sawMember);
    }
}
