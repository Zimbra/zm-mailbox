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
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link AttributeConstraint}. Constraints are built from the real
 * {@link AttributeManager} (so the correct typed subclass - integer/long/duration/gentime/string -
 * is chosen per attribute), then exercised through the public {@code toString}/{@code fromString}
 * round trip and the {@code violateConstraint} enforcement path. The min/max boundary logic is
 * driven inside real workflows (build constraint -&gt; serialize -&gt; reparse -&gt; check value) rather
 * than in isolation. {@code fromString}/{@code newConstratint}/{@code violateConstraint} are
 * package-private and reached via reflection since the test lives in the same package but those
 * members are static and private.
 */
public class AttributeConstraintFunctionalTest {

    private static AttributeManager am;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        am = AttributeManager.getInstance();
    }

    // ---- reflection helpers for the package-private static surface ----

    private static AttributeConstraint fromString(String s) throws Exception {
        Method m = AttributeConstraint.class.getDeclaredMethod("fromString", AttributeManager.class, String.class);
        m.setAccessible(true);
        try {
            return (AttributeConstraint) m.invoke(null, am, s);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    private static AttributeConstraint newConstraint(String attrName) throws Exception {
        Method m = AttributeConstraint.class.getDeclaredMethod("newConstratint", AttributeManager.class, String.class);
        m.setAccessible(true);
        return (AttributeConstraint) m.invoke(null, am, attrName);
    }

    private static boolean violated(AttributeConstraint c, Object value) throws Exception {
        Method m = AttributeConstraint.class.getDeclaredMethod("violated", Object.class);
        m.setAccessible(true);
        try {
            return (Boolean) m.invoke(c, value);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    // ---- newConstratint: correct subtype per attribute type ----

    @Test
    public void newConstratintIntegerAttrSupportsMinMax() throws Exception {
        // Act — zimbraPasswordMinLength is an integer attribute
        AttributeConstraint c = newConstraint("zimbraPasswordMinLength");
        c.setMin("6");
        c.setMax("10");

        // Assert — the integer subclass actually records min/max (base class would not)
        assertEquals("min must be stored by the integer constraint", "6", c.getMin());
        assertEquals("max must be stored by the integer constraint", "10", c.getMax());
    }

    @Test
    public void newConstratintStringAttrIgnoresMinThrowsFromBase() throws Exception {
        // Arrange — zimbraPrefGroupMailBy is a string/enum attribute => base AttributeConstraint
        AttributeConstraint c = newConstraint("zimbraPrefGroupMailBy");

        // Act / Assert — base class setMin throws PARSE_ERROR (min not supported)
        try {
            c.setMin("5");
            fail("base AttributeConstraint.setMin must throw");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
        }
    }

    // ---- fromString round trip ----

    @Test
    public void fromStringIntegerMinMaxValuesParsesAllParts() throws Exception {
        // Act
        AttributeConstraint c = fromString("zimbraPasswordMinLength:min=6:max=64:values=1,2,3");

        // Assert — toString contains every parsed part, proving min/max/values were captured
        String s = c.toString();
        assertTrue("must keep attr name", s.startsWith("zimbraPasswordMinLength"));
        assertTrue("must keep min", s.contains("min=6"));
        assertTrue("must keep max", s.contains("max=64"));
        assertTrue("must keep values", s.contains("values="));
    }

    @Test
    public void fromStringMissingPartsThrowsParseError() throws Exception {
        // Act / Assert — a bare attr name with no ':' part is rejected
        try {
            fromString("zimbraPasswordMinLength");
            fail("expected PARSE_ERROR for constraint with fewer than 2 parts");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
            assertTrue(e.getMessage().contains("invalid constraint"));
        }
    }

    @Test
    public void fromStringThenToStringRoundTripsMinOnly() throws Exception {
        // Act
        AttributeConstraint c = fromString("zimbraPasswordMinLength:min=6");

        // Assert
        assertEquals("zimbraPasswordMinLength:min=6", c.toString());
    }

    // ---- integer min/max violation workflow ----

    @Test
    public void violatedIntegerBelowMinReturnsTrue() throws Exception {
        // Arrange — min=6, max=10
        AttributeConstraint c = fromString("zimbraPasswordMinLength:min=6:max=10");

        // Act / Assert
        assertTrue("5 < min 6 must violate", violated(c, "5"));
    }

    @Test
    public void violatedIntegerWithinRangeReturnsFalse() throws Exception {
        // Arrange
        AttributeConstraint c = fromString("zimbraPasswordMinLength:min=6:max=10");

        // Act / Assert
        assertFalse("8 is within [6,10]", violated(c, "8"));
    }

    @Test
    public void violatedIntegerAboveMaxReturnsTrue() throws Exception {
        // Arrange
        AttributeConstraint c = fromString("zimbraPasswordMinLength:min=6:max=10");

        // Act / Assert
        assertTrue("11 > max 10 must violate", violated(c, "11"));
    }

    @Test
    public void violatedIntegerNonNumericReturnsTrue() throws Exception {
        // Arrange
        AttributeConstraint c = fromString("zimbraPasswordMinLength:min=6:max=10");

        // Act / Assert — a non-integer value is treated as a violation
        assertTrue("non-numeric value must violate an integer constraint", violated(c, "abc"));
    }

    // ---- values (enum-style) violation workflow ----

    @Test
    public void violatedValueNotInAllowedSetReturnsTrue() throws Exception {
        // Arrange — only 'conversation' allowed
        AttributeConstraint c = fromString("zimbraPrefGroupMailBy:values=conversation");

        // Act / Assert
        assertTrue("'message' is not in the allowed values", violated(c, "message"));
    }

    @Test
    public void violatedValueInAllowedSetReturnsFalse() throws Exception {
        // Arrange
        AttributeConstraint c = fromString("zimbraPrefGroupMailBy:values=conversation");

        // Act / Assert
        assertFalse("'conversation' is allowed", violated(c, "conversation"));
    }

    @Test
    public void violatedMultiValueAllAllowedReturnsFalse() throws Exception {
        // Arrange — multi-value attribute, allowed set A,B,C
        AttributeConstraint c = fromString("zimbraZimletAvailableZimlets:values=A,B,C");

        // Act / Assert — String[] path, all members allowed
        assertFalse("{A,B} are all allowed", violated(c, new String[] {"A", "B"}));
    }

    @Test
    public void violatedMultiValueOneDisallowedReturnsTrue() throws Exception {
        // Arrange
        AttributeConstraint c = fromString("zimbraZimletAvailableZimlets:values=A,B,C");

        // Act / Assert — one member outside the set violates
        assertTrue("X is not allowed", violated(c, new String[] {"A", "X"}));
    }

    @Test
    public void violatedUnsupportedValueTypeThrowsFailure() throws Exception {
        // Arrange
        AttributeConstraint c = fromString("zimbraPrefGroupMailBy:values=conversation");

        // Act / Assert — neither String nor String[] => internal error FAILURE
        try {
            violated(c, Integer.valueOf(5));
            fail("expected FAILURE for non String/String[] value");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
        }
    }

    // ---- long constraint workflow ----

    @Test
    public void violatedLongAboveMaxReturnsTrue() throws Exception {
        // Arrange — zimbraMailQuota is a long attribute
        long max = Long.MAX_VALUE - 1;
        AttributeConstraint c = fromString("zimbraMailQuota:max=" + max);

        // Act / Assert
        assertFalse(max + " == max is allowed", violated(c, "" + max));
        assertTrue(Long.MAX_VALUE + " > max must violate", violated(c, "" + Long.MAX_VALUE));
    }

    // ---- end-to-end on a real COS entry: persist constraint + enforce ----

    @Test
    public void modifyConstraintOnCosPersistsAndEnforcesViaViolateConstraint() throws Exception {
        // Arrange — real COS from the harness and a fresh integer constraint to install.
        // The in-memory provisioning does not pre-register the default COS, so create it here
        // (idempotently) to get a real persisted Cos entry to drive the workflow on.
        Provisioning prov = Provisioning.getInstance();
        Cos cos = prov.getCosByName(Provisioning.DEFAULT_COS_NAME);
        if (cos == null) {
            cos = prov.createCos(Provisioning.DEFAULT_COS_NAME, new HashMap<String, Object>());
        }
        AttributeConstraint c = fromString("zimbraPasswordMinLength:min=6:max=10:values=8,9");
        List<AttributeConstraint> toSet = new ArrayList<AttributeConstraint>();
        toSet.add(c);

        // Act — persist the constraint onto the COS, then reload it from the entry
        AttributeConstraint.modifyConstraint(cos, toSet);
        Map<String, AttributeConstraint> loaded = AttributeConstraint.getConstraint(cos);

        // Assert — the constraint round-tripped through LDAP attrs onto the entry
        assertTrue("constraint for the attr must be present after modify",
                loaded.containsKey("zimbraPasswordMinLength"));

        // And the loaded constraint enforces values: 8 allowed, 5 denied (PERM_DENIED)
        assertFalse("8 satisfies values constraint", violateConstraint(loaded, "zimbraPasswordMinLength", "8"));
        try {
            violateConstraint(loaded, "zimbraPasswordMinLength", "5");
            fail("expected PERM_DENIED for disallowed value 5");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void getConstraintNoConstraintOnEntryReturnsEmptyMap() throws Exception {
        // Arrange — a fresh COS-like entry: use config which has no zimbraConstraint set
        // (use a brand new domain's config-less behaviour by reading global config).
        com.zimbra.cs.account.Config config = Provisioning.getInstance().getConfig();

        // Act
        Map<String, AttributeConstraint> loaded = AttributeConstraint.getConstraint(config);

        // Assert — empty (cached) map, not null
        assertTrue("no constraints set => empty map", loaded.isEmpty());
    }

    private static boolean violateConstraint(Map<String, AttributeConstraint> constraints,
            String attrName, Object value) throws Exception {
        Method m = AttributeConstraint.class.getDeclaredMethod(
                "violateConstraint", Map.class, String.class, Object.class);
        m.setAccessible(true);
        try {
            return (Boolean) m.invoke(null, constraints, attrName, value);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    @Test
    public void violateConstraintNoConstraintForAttrReturnsFalse() throws Exception {
        // Arrange — empty constraint map
        Map<String, AttributeConstraint> constraints = new HashMap<String, AttributeConstraint>();

        // Act / Assert — unknown attr => not violated, no throw
        assertFalse(violateConstraint(constraints, "zimbraId", "anything"));
    }

    @Test
    public void toStringNoMinMaxValuesIsJustAttrName() throws Exception {
        // Arrange — base constraint with nothing set
        AttributeConstraint c = newConstraint("zimbraPrefGroupMailBy");

        // Act / Assert — empty constraint serializes to just the attr name
        assertEquals("zimbraPrefGroupMailBy", c.toString());
        assertNull("base getMin is null", c.getMin());
        assertNull("base getMax is null", c.getMax());
    }

    // ---- base class max not supported ----

    @Test
    public void setMaxOnBaseStringConstraintThrowsParseError() throws Exception {
        // Arrange — string/enum attr => base AttributeConstraint, which does not support max
        AttributeConstraint c = newConstraint("zimbraPrefGroupMailBy");

        // Act / Assert
        try {
            c.setMax("10");
            fail("base AttributeConstraint.setMax must throw");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
        }
    }

    // ---- integer bad-min / bad-max are silently ignored ----

    @Test
    public void newConstratintIntegerBadMinMaxIgnoredSoNoConstraint() throws Exception {
        // Arrange — integer subclass swallows NumberFormatException on bad min/max
        AttributeConstraint c = newConstraint("zimbraCalendarMaxRevisions");
        c.setMin("notANumber");
        c.setMax("alsoBad");

        // Assert — neither bound was recorded
        assertNull("bad min is ignored", c.getMin());
        assertNull("bad max is ignored", c.getMax());
        // and with no bounds, any integer value is allowed
        assertFalse(violated(c, "12345"));
    }

    @Test
    public void violatedIntegerOnlyMinBelowAndAtBoundary() throws Exception {
        // Arrange — only a min bound on an integer attr
        AttributeConstraint c = fromString("zimbraCalendarMaxRevisions:min=5");

        // Act / Assert
        assertTrue("4 < min 5 violates", violated(c, "4"));
        assertFalse("5 == min 5 is allowed", violated(c, "5"));
        assertFalse("100 > min 5 is allowed when no max", violated(c, "100"));
    }

    // ---- duration constraint workflow ----

    @Test
    public void violatedDurationWithinRangeReturnsFalse() throws Exception {
        // Arrange — zimbraPasswordLockoutDuration is a duration attr; [5h, 1d]
        AttributeConstraint c = fromString("zimbraPasswordLockoutDuration:min=5h:max=1d");

        // Act / Assert — 24h is within [5h, 1d]
        assertFalse("24h is within [5h,1d]", violated(c, "24h"));
        assertFalse("5h == min is allowed", violated(c, "5h"));
    }

    @Test
    public void violatedDurationBelowMinReturnsTrue() throws Exception {
        // Arrange
        AttributeConstraint c = fromString("zimbraPasswordLockoutDuration:min=5h:max=1d");

        // Act / Assert — 3h < 5h
        assertTrue("3h < min 5h violates", violated(c, "3h"));
    }

    @Test
    public void violatedDurationAboveMaxReturnsTrue() throws Exception {
        // Arrange
        AttributeConstraint c = fromString("zimbraPasswordLockoutDuration:min=5h:max=1d");

        // Act / Assert — 25h > 1d
        assertTrue("25h > max 1d violates", violated(c, "25h"));
    }

    @Test
    public void violatedDurationBadValueReturnsTrue() throws Exception {
        // Arrange
        AttributeConstraint c = fromString("zimbraPasswordLockoutDuration:min=5h:max=1d");

        // Act / Assert — an unparseable interval is treated as a violation
        assertTrue("garbage interval violates", violated(c, "notADuration"));
    }

    @Test
    public void durationConstraintBadMinSpecIsIgnored() throws Exception {
        // Arrange — a bad min duration is swallowed, leaving no min bound
        AttributeConstraint c = newConstraint("zimbraPasswordLockoutDuration");
        c.setMin("zz");

        // Assert — no min recorded; a tiny value is allowed
        assertNull(c.getMin());
        assertFalse(violated(c, "1h"));
    }

    @Test
    public void durationConstraintMinMaxRoundTripsThroughToString() throws Exception {
        // Arrange — durations are stored as milliseconds internally
        // (DateUtil.getTimeInterval returns millis).
        AttributeConstraint c = fromString("zimbraPasswordLockoutDuration:min=5h:max=1d");

        // Act — re-serialize
        String s = c.toString();

        // Assert — min/max are present (as millisecond counts)
        assertTrue("min present", s.contains("min="));
        assertTrue("max present", s.contains("max="));
        // 5h == 18000000ms, 1d == 86400000ms
        assertEquals("18000000", c.getMin());
        assertEquals("86400000", c.getMax());
    }

    // ---- gentime constraint workflow ----

    @Test
    public void violatedGentimeWithinRangeReturnsFalse() throws Exception {
        // Arrange — zimbraPrefPop3DownloadSince is a gentime attr
        AttributeConstraint c = fromString("zimbraPrefPop3DownloadSince:min=20060315023000Z");

        // Act / Assert — value at the min is allowed
        assertFalse("value == min is allowed", violated(c, "20060315023000Z"));
        assertFalse("later value is allowed", violated(c, "20060315023001Z"));
    }

    @Test
    public void violatedGentimeBelowMinReturnsTrue() throws Exception {
        // Arrange
        AttributeConstraint c = fromString("zimbraPrefPop3DownloadSince:min=20060315023000Z");

        // Act / Assert — earlier than min
        assertTrue("earlier value violates min", violated(c, "20050315023000Z"));
    }

    @Test
    public void violatedGentimeBadValueReturnsTrue() throws Exception {
        // Arrange
        AttributeConstraint c = fromString("zimbraPrefPop3DownloadSince:min=20060315023000Z");

        // Act / Assert — an unparseable gentime violates
        assertTrue("garbage gentime violates", violated(c, "not-a-time"));
    }

    @Test
    public void violatedGentimeAboveMaxReturnsTrue() throws Exception {
        // Arrange — max bound only
        AttributeConstraint c = fromString("zimbraPrefPop3DownloadSince:max=20060315023000Z");

        // Act / Assert — later than max
        assertTrue("later than max violates", violated(c, "20070315023000Z"));
        assertFalse("at max is allowed", violated(c, "20060315023000Z"));
    }

    @Test
    public void gentimeConstraintBadMinSpecLeavesNoMin() throws Exception {
        // Arrange — unparseable gentime min is ignored (no bound recorded)
        AttributeConstraint c = newConstraint("zimbraPrefPop3DownloadSince");
        c.setMin("not-a-gentime");

        // Assert — no min, so any parseable gentime is allowed
        assertNull(c.getMin());
        assertFalse(violated(c, "20060315023000Z"));
    }

    // ---- newConstratint subtype selection: long & integer ----

    @Test
    public void newConstratintLongAttrRecordsMaxAsLong() throws Exception {
        // Arrange — zimbraMailQuota is a long attribute => LongConstraint
        AttributeConstraint c = newConstraint("zimbraMailQuota");
        c.setMax("100");

        // Assert — long subclass records the max
        assertEquals("100", c.getMax());
        assertNull("no min set", c.getMin());
    }

    @Test
    public void violatedLongBadValueReturnsTrue() throws Exception {
        // Arrange
        AttributeConstraint c = fromString("zimbraMailQuota:min=10:max=100");

        // Act / Assert — non-numeric long violates
        assertTrue("garbage long violates", violated(c, "notALong"));
        assertTrue("5 < min 10 violates", violated(c, "5"));
        assertFalse("50 within [10,100]", violated(c, "50"));
    }

    @Test
    public void longConstraintBadMinMaxSpecsAreIgnored() throws Exception {
        // Arrange — bad numeric specs are swallowed
        AttributeConstraint c = newConstraint("zimbraMailQuota");
        c.setMin("xx");
        c.setMax("yy");

        // Assert — no bounds recorded
        assertNull(c.getMin());
        assertNull(c.getMax());
    }

    // ---- getConstraintEntry: Account -> COS, Domain/Server -> Config ----

    @Test
    public void getConstraintEntryAccountReturnsCos() throws Exception {
        // Arrange — a real account whose COS resolves through the harness. The in-memory
        // provisioning does not pre-register the default COS, and getCOS falls back to the
        // COS named DEFAULT_COS_NAME, so create it here (idempotently) so the account resolves
        // to a real Cos entry.
        Provisioning prov = Provisioning.getInstance();
        if (prov.getCosByName(Provisioning.DEFAULT_COS_NAME) == null) {
            prov.createCos(Provisioning.DEFAULT_COS_NAME, new HashMap<String, Object>());
        }
        if (prov.get(com.zimbra.common.account.Key.DomainBy.name, "ctest.com") == null) {
            prov.createDomain("ctest.com", new HashMap<String, Object>());
        }
        com.zimbra.cs.account.Account acct =
                prov.createAccount("centry@ctest.com", "secret", new HashMap<String, Object>());

        // Act
        com.zimbra.cs.account.Entry entry = getConstraintEntry(acct);

        // Assert — for an account the constraint entry is its COS
        assertTrue("account constraint entry must be a Cos", entry instanceof Cos);

        // Cleanup
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void getConstraintEntryDomainReturnsConfig() throws Exception {
        // Arrange
        Provisioning prov = Provisioning.getInstance();
        if (prov.get(com.zimbra.common.account.Key.DomainBy.name, "dentry.com") == null) {
            prov.createDomain("dentry.com", new HashMap<String, Object>());
        }
        com.zimbra.cs.account.Domain domain =
                prov.get(com.zimbra.common.account.Key.DomainBy.name, "dentry.com");

        // Act
        com.zimbra.cs.account.Entry entry = getConstraintEntry(domain);

        // Assert — for a domain the constraint entry is global config
        assertTrue("domain constraint entry must be Config",
                entry instanceof com.zimbra.cs.account.Config);
    }

    private static com.zimbra.cs.account.Entry getConstraintEntry(com.zimbra.cs.account.Entry entry)
            throws Exception {
        Method m = AttributeConstraint.class.getDeclaredMethod("getConstraintEntry",
                com.zimbra.cs.account.Entry.class);
        m.setAccessible(true);
        try {
            return (com.zimbra.cs.account.Entry) m.invoke(null, entry);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    // ---- ignoreConstraint: COS-id style attrs are never enforced ----

    @Test
    public void violateConstraintCosIdAttrIsIgnoredReturnsFalse() throws Exception {
        // Arrange — install a constraint keyed on zimbraCOSId (an ignored attr)
        Map<String, AttributeConstraint> constraints = new HashMap<String, AttributeConstraint>();
        AttributeConstraint c = fromString("zimbraCOSId:values=onlythis");
        constraints.put(Provisioning.A_zimbraCOSId, c);

        // Act / Assert — even a disallowed value is ignored for zimbraCOSId
        assertFalse("constraint on zimbraCOSId must be ignored",
                violateConstraint(constraints, Provisioning.A_zimbraCOSId, "somethingElse"));
    }

    // ---- toXML / fromXML round trip ----

    @Test
    public void toXMLThenFromXMLRoundTripsMinMaxValues() throws Exception {
        // Arrange — an integer constraint with min/max/values
        AttributeConstraint c = fromString("zimbraCalendarMaxRevisions:min=1:max=10:values=2,3");

        // Act — serialize into a parent element, then read the child back
        com.zimbra.common.soap.Element parent =
                new com.zimbra.common.soap.Element.XMLElement("parent");
        c.toXML(parent);
        com.zimbra.common.soap.Element eConstraint =
                parent.getElement(com.zimbra.common.soap.AdminConstants.E_CONSTRAINT);
        AttributeConstraint reparsed =
                AttributeConstraint.fromXML(am, "zimbraCalendarMaxRevisions", eConstraint);

        // Assert — min/max survived the XML round trip
        assertEquals("1", reparsed.getMin());
        assertEquals("10", reparsed.getMax());
        // and value enforcement still works
        assertTrue("0 < min 1 violates", violated(reparsed, "0"));
        assertTrue("4 is not in values {2,3}", violated(reparsed, "4"));
        assertFalse("2 is in values", violated(reparsed, "2"));
    }

    @Test
    public void fromXMLEmptyConstraintElementIsEmpty() throws Exception {
        // Arrange — a constraint element with no min/max/values children
        com.zimbra.common.soap.Element eConstraint =
                new com.zimbra.common.soap.Element.XMLElement(
                        com.zimbra.common.soap.AdminConstants.E_CONSTRAINT);

        // Act
        AttributeConstraint c =
                AttributeConstraint.fromXML(am, "zimbraPrefGroupMailBy", eConstraint);

        // Assert — empty constraint serializes back to just the attr name
        assertEquals("zimbraPrefGroupMailBy", c.toString());
    }

    @Test
    public void toXMLEmptyConstraintAddsBareConstraintElement() throws Exception {
        // Arrange — base constraint with nothing set
        AttributeConstraint c = newConstraint("zimbraPrefGroupMailBy");
        com.zimbra.common.soap.Element parent =
                new com.zimbra.common.soap.Element.XMLElement("parent");

        // Act
        c.toXML(parent);

        // Assert — a constraint element exists but has no min/max/values
        com.zimbra.common.soap.Element eConstraint =
                parent.getElement(com.zimbra.common.soap.AdminConstants.E_CONSTRAINT);
        assertNull(eConstraint.getOptionalElement(com.zimbra.common.soap.AdminConstants.E_MIN));
        assertNull(eConstraint.getOptionalElement(com.zimbra.common.soap.AdminConstants.E_VALUES));
    }

    // ---- modifyConstraint: removal path (empty new constraint unsets attr) ----

    @Test
    public void modifyConstraintEmptyReplacementRemovesExistingConstraint() throws Exception {
        // Arrange — install a constraint on a COS, confirm present, then replace with empty
        Provisioning prov = Provisioning.getInstance();
        Cos cos = prov.createCos("modifycos", new HashMap<String, Object>());

        List<AttributeConstraint> install = new ArrayList<AttributeConstraint>();
        install.add(fromString("zimbraPasswordMinLength:min=6:max=10"));
        AttributeConstraint.modifyConstraint(cos, install);
        assertTrue("constraint present after install",
                AttributeConstraint.getConstraint(cos).containsKey("zimbraPasswordMinLength"));

        // Act — replace with an empty constraint for the same attr => removal
        List<AttributeConstraint> remove = new ArrayList<AttributeConstraint>();
        remove.add(newConstraint("zimbraPasswordMinLength")); // empty
        // bypass the entry cache so the next getConstraint reloads from attrs
        cos.setCachedData("CONSTRAINT_CACHE", null);
        AttributeConstraint.modifyConstraint(cos, remove);

        // Assert — the constraint for the attr is gone
        cos.setCachedData("CONSTRAINT_CACHE", null);
        Map<String, AttributeConstraint> after = AttributeConstraint.getConstraint(cos);
        assertFalse("constraint must be removed after empty replacement",
                after.containsKey("zimbraPasswordMinLength"));
    }
}
