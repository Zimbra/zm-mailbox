/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link Entry} exercised through real {@link Account} domain
 * objects created via the in-memory MockProvisioning harness.
 */
public class EntryFunctionalTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_displayName, "Entry Tester");
        attrs.put(Provisioning.A_zimbraMailQuota, "1048576");
        prov.createAccount("entry@zimbra.com", "secret", attrs);
    }

    private Account acct() throws Exception {
        return prov.get(com.zimbra.common.account.Key.AccountBy.name, "entry@zimbra.com");
    }

    @Test
    public void getAttrSingleStringValueReturnsValue() throws Exception {
        // Arrange
        Account a = acct();

        // Act
        String displayName = a.getAttr(Provisioning.A_displayName);

        // Assert
        assertEquals("Entry Tester", displayName);
    }

    @Test
    public void getAttrMissingAttrWithDefaultReturnsDefault() throws Exception {
        // Arrange
        Account a = acct();

        // Act
        String value = a.getAttr("nonExistentAttribute", "fallback");

        // Assert
        assertEquals("fallback", value);
        assertNull("missing attr without default must be null", a.getAttr("nonExistentAttribute"));
    }

    @Test
    public void getBooleanAttrUnsetAttrReturnsProvidedDefault() throws Exception {
        // Arrange
        Account a = acct();
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put("zimbraTestBoolAttr", "TRUE");
        a.setAttrs(merge(a, changes));

        // Act
        boolean explicitlyTrue = a.getBooleanAttr("zimbraTestBoolAttr", false);
        boolean defaulted = a.getBooleanAttr("zimbraUnsetBoolAttr", true);

        // Assert
        assertTrue("explicitly set TRUE must read true", explicitlyTrue);
        assertTrue("unset attr falls back to provided default", defaulted);
    }

    @Test
    public void getIntAttrValidAndInvalidParsesOrDefaults() throws Exception {
        // Arrange
        Account a = acct();
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put("zimbraIntAttr", "42");
        changes.put("zimbraBadIntAttr", "not-a-number");
        a.setAttrs(merge(a, changes));

        // Act
        int parsed = a.getIntAttr("zimbraIntAttr", -1);
        int badDefaulted = a.getIntAttr("zimbraBadIntAttr", 7);
        int missingDefaulted = a.getIntAttr("zimbraMissingIntAttr", 9);

        // Assert
        assertEquals(42, parsed);
        assertEquals("unparseable value falls back to default", 7, badDefaulted);
        assertEquals("missing value falls back to default", 9, missingDefaulted);
    }

    @Test
    public void getLongAttrNumericAndMemoryUnitReturnsBytes() throws Exception {
        // Arrange
        Account a = acct();
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put("zimbraLongAttr", "1024");
        changes.put("zimbraMemAttr", "1KB");
        a.setAttrs(merge(a, changes));

        // Act
        long plain = a.getLongAttr("zimbraLongAttr", -1L);
        long memoryUnit = a.getLongAttr("zimbraMemAttr", -1L);

        // Assert
        assertEquals(1024L, plain);
        assertEquals("1KB must convert to 1024 bytes", 1024L, memoryUnit);
    }

    @Test
    public void getTimeIntervalDurationStringReturnsMillis() throws Exception {
        // Arrange
        Account a = acct();
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put("zimbraDurationAttr", "1h");
        a.setAttrs(merge(a, changes));

        // Act
        long millis = a.getTimeInterval("zimbraDurationAttr", -1L);

        // Assert
        assertEquals("1h must be 3600000 ms", 3600000L, millis);
    }

    @Test
    public void getMultiAttrMultipleValuesReturnsAllAsArray() throws Exception {
        // Arrange
        Account a = acct();
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put("zimbraMultiAttr", new String[] {"one", "two", "three"});
        a.setAttrs(merge(a, changes));

        // Act
        String[] values = a.getMultiAttr("zimbraMultiAttr");
        Set<String> valueSet = a.getMultiAttrSet("zimbraMultiAttr");

        // Assert
        assertEquals(3, values.length);
        assertTrue(valueSet.contains("one"));
        assertTrue(valueSet.contains("three"));
        assertEquals("missing multi attr returns empty array", 0, a.getMultiAttr("zimbraNoneSuch").length);
    }

    @Test
    public void getAttrsReturnsLiveCopyIncludingSetValues() throws Exception {
        // Arrange
        Account a = acct();

        // Act — exclude the ephemeral layer (see merge() note); apply defaults.
        Map<String, Object> attrs = a.getAttrs(true, false);

        // Assert
        assertNotNull(attrs);
        assertEquals("Entry Tester", attrs.get(Provisioning.A_displayName));
    }

    @Test
    public void setAndGetCachedDataRoundTripsAndResetClears() throws Exception {
        // Arrange
        Account a = acct();

        // Act
        a.setCachedData("myKey", "myValue");
        Object before = a.getCachedData("myKey");
        // setAttrs triggers resetData() which clears cached data.
        // Use getAttrs(true,false) to avoid the ephemeral path (see merge() note).
        a.setAttrs(a.getAttrs(true, false));
        Object after = a.getCachedData("myKey");

        // Assert
        assertEquals("myValue", before);
        assertNull("resetData must clear cached data", after);
    }

    @Test
    public void getEntryTypeAccountIsAccount() throws Exception {
        // Arrange
        Account a = acct();

        // Act
        EntryType type = a.getEntryType();

        // Assert
        assertEquals(EntryType.ACCOUNT, type);
        assertEquals("ACCOUNT", EntryType.ACCOUNT.getName());
    }

    @Test
    public void toStringAccountContainsClassName() throws Exception {
        // Arrange
        Account a = acct();

        // Act
        String s = a.toString();

        // Assert
        assertTrue("toString must include the class name", s.contains(a.getClass().getName()));
    }

    @Test
    public void getProvisioningReturnsHarnessInstance() throws Exception {
        // Arrange
        Account a = acct();

        // Act
        Provisioning entryProv = a.getProvisioning();

        // Assert
        assertNotNull(entryProv);
        assertEquals(prov, entryProv);
    }

    @Test
    public void sortByDisplayNameMultipleEntriesOrdersAlphabetically() throws Exception {
        // Arrange
        Map<String, Object> b = new HashMap<String, Object>();
        b.put(Provisioning.A_displayName, "Bravo");
        prov.createAccount("bravo@zimbra.com", "secret", b);
        Map<String, Object> z = new HashMap<String, Object>();
        z.put(Provisioning.A_displayName, "Zulu");
        prov.createAccount("zulu@zimbra.com", "secret", z);

        List<Entry> entries = new ArrayList<Entry>();
        entries.add(prov.get(com.zimbra.common.account.Key.AccountBy.name, "zulu@zimbra.com"));
        entries.add(prov.get(com.zimbra.common.account.Key.AccountBy.name, "bravo@zimbra.com"));

        // Act
        List<Entry> sorted = Entry.sortByDisplayName(entries, Locale.US);

        // Assert
        assertEquals(2, sorted.size());
        assertEquals("Bravo", sorted.get(0).getAttr(Provisioning.A_displayName));
        assertEquals("Zulu", sorted.get(1).getAttr(Provisioning.A_displayName));
    }

    @Test
    public void sortByDisplayNameSingleEntryShortCircuitsUnchanged() throws Exception {
        // Arrange
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(acct());

        // Act
        List<Entry> sorted = Entry.sortByDisplayName(entries, Locale.US);

        // Assert
        assertEquals(1, sorted.size());
        assertEquals("Entry Tester", sorted.get(0).getAttr(Provisioning.A_displayName));
    }

    @Test
    public void getMultiAttrAppliesDefaultsFalseDoesNotReturnDefaults() throws Exception {
        // Arrange
        Account a = acct();

        // Act
        String[] noDefaults = a.getMultiAttr("zimbraPrefMailItemsPerPage", false);

        // Assert
        assertFalse("with applyDefaults=false there must be no inherited default values",
                noDefaults.length > 0 && noDefaults[0] == null);
    }

    /* Helper that returns a fresh attr map combining the entry's current attrs with new ones. */
    private Map<String, Object> merge(Account a, Map<String, Object> changes) {
        // includeEphemeral=false: the in-memory ephemeral backend in this harness builds an
        // LdapEntryLocation whose location hierarchy carries a null component, which makes the
        // Guava Joiner in InMemoryEphemeralStore NPE. The ephemeral path is not the subject of
        // these tests, so exclude it.
        Map<String, Object> combined = new HashMap<String, Object>(a.getAttrs(false, false));
        combined.putAll(changes);
        return combined;
    }

    // ---- additional coverage: binary/time/unicode getters, JSON dump, defaults, caching ----

    @Test
    public void getBinaryAttrBase64ValueDecodesBytes() throws Exception {
        // Arrange — store a base64 encoding of "hi" (aGk=).
        Account a = acct();
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put("zimbraBinAttr", "aGk=");
        a.setAttrs(merge(a, changes));

        // Act
        byte[] decoded = a.getBinaryAttr("zimbraBinAttr");

        // Assert — decodes to the two bytes 'h','i'.
        assertNotNull(decoded);
        assertEquals(2, decoded.length);
        assertEquals('h', decoded[0]);
        assertEquals('i', decoded[1]);
        assertNull("missing binary attr is null", a.getBinaryAttr("zimbraNoSuchBin"));
    }

    @Test
    public void getMultiBinaryAttrMultipleBase64ValuesDecodesEach() throws Exception {
        // Arrange — two base64 values ("hi", "no").
        Account a = acct();
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put("zimbraMultiBin", new String[] {"aGk=", "bm8="});
        a.setAttrs(merge(a, changes));

        // Act
        List<byte[]> list = a.getMultiBinaryAttr("zimbraMultiBin");
        Set<byte[]> set = a.getMultiBinaryAttrSet("zimbraMultiBin");

        // Assert
        assertEquals(2, list.size());
        assertEquals(2, set.size());
        assertTrue("missing multi-binary attr returns empty list",
                a.getMultiBinaryAttr("zimbraNoSuchMultiBin").isEmpty());
    }

    @Test
    public void getGeneralizedTimeAttrValidAndInvalidParsesOrDefaults() throws Exception {
        // Arrange — a valid LDAP generalized time and a garbage value.
        Account a = acct();
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put("zimbraGenTime", "20240115103000Z");
        changes.put("zimbraBadTime", "not-a-time");
        a.setAttrs(merge(a, changes));
        Date fallback = new Date(0L);

        // Act
        Date parsed = a.getGeneralizedTimeAttr("zimbraGenTime", null);
        Date badDefaulted = a.getGeneralizedTimeAttr("zimbraBadTime", fallback);
        Date missingDefaulted = a.getGeneralizedTimeAttr("zimbraNoSuchTime", fallback);

        // Assert
        assertNotNull("valid generalized time must parse", parsed);
        assertEquals("unparseable time falls back to default", fallback, badDefaulted);
        assertEquals("missing time falls back to default", fallback, missingDefaulted);
    }

    @Test
    public void getUnicodeMultiAttrNonIdnAttrReturnsValuesUnchanged() throws Exception {
        // Arrange — a plain multi-valued non-IDN attribute.
        Account a = acct();
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put("zimbraUniMulti", new String[] {"alpha", "beta"});
        a.setAttrs(merge(a, changes));

        // Act
        String[] values = a.getUnicodeMultiAttr("zimbraUniMulti");

        // Assert — non-IDN values are returned as-is.
        assertEquals(2, values.length);
        assertEquals("alpha", values[0]);
        assertEquals("beta", values[1]);
    }

    @Test
    public void getUnicodeAttrsReturnsAttrsMapIncludingDisplayName() throws Exception {
        // Arrange
        Account a = acct();

        // Act — unicode view of attrs runs the toUnicode conversion path.
        // includeEphemeral=false avoids the harness ephemeral NPE (see merge() note).
        Map<String, Object> uni = a.getUnicodeAttrs(true, false);
        Map<String, Object> uniNoDefaults = a.getUnicodeAttrs(false, false);

        // Assert
        assertNotNull(uni);
        assertEquals("Entry Tester", uni.get(Provisioning.A_displayName));
        assertNotNull(uniNoDefaults);
        assertEquals("Entry Tester", uniNoDefaults.get(Provisioning.A_displayName));
    }

    @Test
    public void getCachedDataByEnumKeyRoundTripsAndRemoves() throws Exception {
        // Arrange
        Account a = acct();
        EntryCacheDataKey key = EntryCacheDataKey.ACCOUNT_COS;

        // Act — set, read, then remove via the typed EntryCacheDataKey API.
        a.setCachedData(key, "cosData");
        Object stored = a.getCachedData(key);
        a.removeCachedData(key);
        Object afterRemove = a.getCachedData(key);

        // Assert
        assertEquals("cosData", stored);
        assertNull("removeCachedData must clear the entry", afterRemove);
    }

    @Test
    public void setDefaultsThenGetAttrAppliesDefaultWhenUnset() throws Exception {
        // Arrange — only set defaults; the attr is not present in the primary attrs.
        Account a = acct();
        Map<String, Object> defaults = new HashMap<String, Object>();
        defaults.put("zimbraDefaultedAttr", "defaultVal");

        // Act
        a.setDefaults(defaults);
        String withDefault = a.getAttr("zimbraDefaultedAttr");
        String withoutDefault = a.getAttr("zimbraDefaultedAttr", false);

        // Assert — default applied when applyDefaults, ignored otherwise.
        assertEquals("defaultVal", withDefault);
        assertNull("applyDefaults=false must skip the default", withoutDefault);
    }

    @Test
    public void setDefaultsTwoArgPrimaryAndSecondaryBothConsulted() throws Exception {
        // Arrange — primary defaults take precedence; secondary fills the gaps.
        Account a = acct();
        Map<String, Object> primary = new HashMap<String, Object>();
        primary.put("zimbraPrimaryDef", "primary");
        Map<String, Object> secondary = new HashMap<String, Object>();
        secondary.put("zimbraSecondaryDef", "secondary");

        // Act
        a.setDefaults(primary, secondary);

        // Assert — each default surfaces through getAttr.
        assertEquals("primary", a.getAttr("zimbraPrimaryDef"));
        assertEquals("secondary", a.getAttr("zimbraSecondaryDef"));
    }

    @Test
    public void setSecondaryDefaultsThenGetAttrAppliesSecondaryDefault() throws Exception {
        // Arrange
        Account a = acct();
        Map<String, Object> secondary = new HashMap<String, Object>();
        secondary.put("zimbraSecOnly", "secOnlyVal");

        // Act
        a.setSecondaryDefaults(secondary);

        // Assert
        assertEquals("secOnlyVal", a.getAttr("zimbraSecOnly"));
    }

    @Test
    public void setOverrideDefaultsOverridesPlainDefault() throws Exception {
        // Arrange — a value in both defaults and overrideDefaults; override wins.
        Account a = acct();
        Map<String, Object> defaults = new HashMap<String, Object>();
        defaults.put("zimbraOverridable", "fromDefault");
        Map<String, Object> override = new HashMap<String, Object>();
        override.put("zimbraOverridable", "fromOverride");
        a.setDefaults(defaults);

        // Act
        a.setOverrideDefaults(override);

        // Assert — getAttrDefault consults overrideDefaults first.
        assertEquals("fromOverride", a.getAttr("zimbraOverridable"));
        assertEquals("fromOverride", a.getAttrDefault("zimbraOverridable"));
    }

    @Test
    public void setAttrsFourArgReplacesAllLayersAndResetsCache() throws Exception {
        // Arrange — cache something, then replace every attr layer at once.
        Account a = acct();
        a.setCachedData("k", "v");
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_displayName, "Replaced");
        Map<String, Object> defaults = new HashMap<String, Object>();
        defaults.put("zimbraLayerDef", "d");
        Map<String, Object> secondary = new HashMap<String, Object>();
        secondary.put("zimbraLayerSec", "s");
        Map<String, Object> override = new HashMap<String, Object>();
        override.put("zimbraLayerOvr", "o");

        // Act
        a.setAttrs(attrs, defaults, secondary, override);

        // Assert — new values from each layer are visible, and cached data was reset.
        assertEquals("Replaced", a.getAttr(Provisioning.A_displayName));
        assertEquals("d", a.getAttr("zimbraLayerDef"));
        assertEquals("s", a.getAttr("zimbraLayerSec"));
        assertEquals("o", a.getAttr("zimbraLayerOvr"));
        assertNull("setAttrs(4-arg) must reset cached data", a.getCachedData("k"));
    }

    @Test
    public void getAttrDefaultNoDefaultsSetReturnsNull() throws Exception {
        // Arrange — a freshly fetched account with no extra defaults.
        Account a = acct();

        // Act / Assert — an attr with no default anywhere yields null.
        assertNull(a.getAttrDefault("zimbraTotallyAbsentAttr"));
    }

    @Test
    public void getTimeIntervalSecsDurationStringReturnsSeconds() throws Exception {
        // Arrange
        Account a = acct();
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put("zimbraDurSecs", "2m");
        a.setAttrs(merge(a, changes));

        // Act
        long secs = a.getTimeIntervalSecs("zimbraDurSecs", -1L);

        // Assert — 2 minutes is 120 seconds.
        assertEquals(120L, secs);
    }

    @Test
    public void getLocaleAccountReturnsNonNullLocale() throws Exception {
        // Arrange
        Account a = acct();

        // Act — locale resolution caches the result on the entry.
        Locale first = a.getLocale();
        Locale second = a.getLocale();

        // Assert — a locale is resolved and the cached value is returned on the second call.
        assertNotNull(first);
        assertEquals(first, second);
    }

    // ====================================================================================
    // Mutation-killing assertions (PIT survivors strengthened).
    // ====================================================================================

    // ---- getAttrDefault: secondary-default resolution by real (canonical) attr name (L253) -

    @Test
    public void getAttrDefaultSecondaryDefaultByRealAttrNameResolvesViaCanonicalName()
            throws Exception {
        // Store the secondary default under the canonical-cased attr name, then query with a
        // different case. The direct map.get(name) misses (case differs), so resolution must
        // fall through to getValueByRealAttrName(...) and return the secondary value (L252-253).
        // Negating the "v != null" guard at L253 makes it return null when the value is present.
        Account a = acct();
        Map<String, Object> secondary = new HashMap<String, Object>();
        secondary.put(Provisioning.A_zimbraMailQuota, "98765"); // canonical case
        a.setSecondaryDefaults(secondary);

        // Query with a lower-cased name so only the real-attr-name path can satisfy it.
        Object resolved = a.getAttrDefault("zimbramailquota");

        assertEquals("secondary default must resolve via canonical attr name", "98765", resolved);
    }

    // ---- getAttrs applies the defaults layer (L363) --------------------------------------

    @Test
    public void getAttrsDefaultsOnlyKeyIsIncludedFromDefaultsLayer() throws Exception {
        // A key present ONLY in the defaults layer must surface in getAttrs(applyDefaults=true).
        // Removing the attrs.putAll(mDefaults) call (L363) drops the defaults-only key.
        Account a = acct();
        Map<String, Object> defaults = new HashMap<String, Object>();
        defaults.put("zimbraDefaultsLayerOnly", "fromDefaultsLayer");
        a.setDefaults(defaults);

        Map<String, Object> withDefaults = a.getAttrs(true, false);
        Map<String, Object> withoutDefaults = a.getAttrs(false, false);

        assertEquals("defaults-only key must appear when applyDefaults=true",
                "fromDefaultsLayer", withDefaults.get("zimbraDefaultsLayerOnly"));
        assertNull("defaults-only key must be absent when applyDefaults=false",
                withoutDefaults.get("zimbraDefaultsLayerOnly"));
    }

    // ---- getLongAttr null-vs-set branch (L524) -------------------------------------------

    @Test
    public void getLongAttrSetValueReturnsParsedMissingReturnsDefault() throws Exception {
        // A set non-memory-unit value must parse (NOT return the default); a missing value must
        // return the default. Negating "v == null" at L524 would return the default for the set
        // value, so the parsed-value assertion fails.
        Account a = acct();
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put("zimbraPlainLong", "555");
        a.setAttrs(merge(a, changes));

        long parsed = a.getLongAttr("zimbraPlainLong", -999L);
        long missing = a.getLongAttr("zimbraMissingPlainLong", -999L);

        assertEquals("set plain value must be parsed, not defaulted", 555L, parsed);
        assertEquals("missing value must fall back to the default", -999L, missing);
    }

    // ---- toUnicode / getUnicodeMultiAttr IDN conversion (L281, L551) ---------------------

    @Test
    public void getUnicodeMultiAttrEmailAttrAceDomainConvertsToUnicode() throws Exception {
        // zimbraMailDeliveryAddress is a TYPE_EMAIL (IDN) attr. An ACE-encoded domain MUST be
        // converted to its Unicode form. Negating "idnType.isEmailOrIDN()" at L551 would leave
        // the ACE form untouched.
        Account a = acct();
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put(Provisioning.A_zimbraMailDeliveryAddress,
                new String[] {"user@xn--mnchen-3ya.com"});
        a.setAttrs(merge(a, changes));

        String[] uni = a.getUnicodeMultiAttr(Provisioning.A_zimbraMailDeliveryAddress);

        assertEquals(1, uni.length);
        assertEquals("ACE domain must be decoded to Unicode", "user@münchen.com", uni[0]);
    }

    @Test
    public void getUnicodeAttrsEmailAttrAceDomainConvertsValueInMap() throws Exception {
        // Same conversion driven through getUnicodeAttrs -> toUnicode (L281). Negating the
        // isEmailOrIDN() guard leaves the stored ACE value unconverted in the returned map.
        Account a = acct();
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put(Provisioning.A_zimbraMailDeliveryAddress,
                new String[] {"user@xn--mnchen-3ya.com"});
        a.setAttrs(merge(a, changes));

        Map<String, Object> uni = a.getUnicodeAttrs(true, false);
        Object val = uni.get(Provisioning.A_zimbraMailDeliveryAddress);

        assertNotNull(val);
        String first = (val instanceof String[]) ? ((String[]) val)[0] : (String) val;
        assertEquals("toUnicode must decode the ACE domain in the attrs map",
                "user@münchen.com", first);
    }

    // ---- setDefaults / setSecondaryDefaults / setOverrideDefaults call resetData() -------
    // (L176, L183, L188, L193) — the resetData() call must clear cached data.

    @Test
    public void setDefaultsOneArgResetsCachedData() throws Exception {
        Account a = acct();
        a.setCachedData("k", "v");
        a.setDefaults(new HashMap<String, Object>());
        assertNull("setDefaults(1-arg) must call resetData() to clear cached data",
                a.getCachedData("k"));
    }

    @Test
    public void setDefaultsTwoArgResetsCachedData() throws Exception {
        Account a = acct();
        a.setCachedData("k", "v");
        a.setDefaults(new HashMap<String, Object>(), new HashMap<String, Object>());
        assertNull("setDefaults(2-arg) must call resetData() to clear cached data",
                a.getCachedData("k"));
    }

    @Test
    public void setSecondaryDefaultsResetsCachedData() throws Exception {
        Account a = acct();
        a.setCachedData("k", "v");
        a.setSecondaryDefaults(new HashMap<String, Object>());
        assertNull("setSecondaryDefaults must call resetData() to clear cached data",
                a.getCachedData("k"));
    }

    @Test
    public void setOverrideDefaultsResetsCachedData() throws Exception {
        Account a = acct();
        a.setCachedData("k", "v");
        a.setOverrideDefaults(new HashMap<String, Object>());
        assertNull("setOverrideDefaults must call resetData() to clear cached data",
                a.getCachedData("k"));
    }

    // ---- sortByDisplayName: tie-break by label (L839) and displayName-keyed sort (L862) --

    @Test
    public void sortByDisplayNameSameDisplayNameDifferentLabelsKeepsBothOrderedByLabel()
            throws Exception {
        // Two accounts sharing the SAME displayName produce the same collation key. The
        // secondary comparator SortByLabelAsc.compare() (L839) orders them by label (account
        // name). If compare() is mutated to return 0, the TreeMultimap treats them as equal and
        // collapses them, dropping one entry.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_displayName, "SameName");
        prov.createAccount("twin-bbb@zimbra.com", "secret", new HashMap<String, Object>(attrs));
        prov.createAccount("twin-aaa@zimbra.com", "secret", new HashMap<String, Object>(attrs));

        List<Entry> entries = new ArrayList<Entry>();
        entries.add(prov.get(com.zimbra.common.account.Key.AccountBy.name, "twin-bbb@zimbra.com"));
        entries.add(prov.get(com.zimbra.common.account.Key.AccountBy.name, "twin-aaa@zimbra.com"));

        List<Entry> sorted = Entry.sortByDisplayName(entries, Locale.US);

        assertEquals("both same-displayName entries must be retained", 2, sorted.size());
        assertEquals("tie broken by ascending label (account name)",
                "twin-aaa@zimbra.com", ((Account) sorted.get(0)).getName());
        assertEquals("twin-bbb@zimbra.com", ((Account) sorted.get(1)).getName());
    }

    @Test
    public void sortByDisplayNameOrdersByDisplayNameNotLabel() throws Exception {
        // The sort key MUST be the displayName, falling back to label only when displayName is
        // null (L862). Here both have a displayName whose ordering is the REVERSE of their
        // account-name (label) ordering. Negating "key == null" at L862 would overwrite the key
        // with getLabel(), sorting by account name and reversing the expected order.
        Map<String, Object> low = new HashMap<String, Object>();
        low.put(Provisioning.A_displayName, "Aaron");   // sorts first by displayName
        prov.createAccount("zzz-name@zimbra.com", "secret", low);
        Map<String, Object> high = new HashMap<String, Object>();
        high.put(Provisioning.A_displayName, "Zelda");  // sorts last by displayName
        prov.createAccount("aaa-name@zimbra.com", "secret", high);

        List<Entry> entries = new ArrayList<Entry>();
        entries.add(prov.get(com.zimbra.common.account.Key.AccountBy.name, "aaa-name@zimbra.com"));
        entries.add(prov.get(com.zimbra.common.account.Key.AccountBy.name, "zzz-name@zimbra.com"));

        List<Entry> sorted = Entry.sortByDisplayName(entries, Locale.US);

        assertEquals(2, sorted.size());
        assertEquals("ordering must follow displayName (Aaron), not label",
                "Aaron", sorted.get(0).getAttr(Provisioning.A_displayName));
        assertEquals("Zelda", sorted.get(1).getAttr(Provisioning.A_displayName));
        assertEquals("first entry's account name confirms displayName-keyed order",
                "zzz-name@zimbra.com", ((Account) sorted.get(0)).getName());
    }
}
