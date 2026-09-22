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

import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Version;
import com.zimbra.cs.account.AttributeManager.IDNType;
import com.zimbra.cs.account.callback.CallbackContext;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Logic-level functional tests for {@link AttributeManager}, built on the {@code @VisibleForTesting}
 * no-arg constructor + {@link AttributeManager#addAttribute(AttributeInfo)} so attribute metadata is
 * exercised without LDAP. Mirrors the setup style of {@code TestAttributeManager}.
 */
public class AttributeManagerLogicTest {

    private AttributeManager am;

    private AttributeInfo info(String name, AttributeType type, AttributeCardinality card,
            Set<AttributeFlag> flags, Set<AttributeClass> optionalIn) {
        return new AttributeInfo(name, 1, null, 0, null, type, null, "", true, null, null,
                card, null, optionalIn, flags, null, null, null, null, null,
                "desc for " + name, null, null, null);
    }

    /**
     * The {@code @VisibleForTesting} no-arg constructor does not run the private
     * {@code initFlagsToAttrsMap()} (that only fires on the LDAP-backed load path), so the
     * flag-to-attrs map is empty and flag-backed lookups such as {@code idnType} would NPE.
     * Mirror that initialization via reflection so the harness behaves like a loaded manager.
     *
     * @param mgr the AttributeManager instance to initialize
     */
    @SuppressWarnings("unchecked")
    private void initFlagToAttrsMap(AttributeManager mgr) throws Exception {
        Field f = AttributeManager.class.getDeclaredField("mFlagToAttrsMap");
        f.setAccessible(true);
        Map<AttributeFlag, Set<String>> map = (Map<AttributeFlag, Set<String>>) f.get(mgr);
        if (map == null) {
            map = new HashMap<AttributeFlag, Set<String>>();
            f.set(mgr, map);
        }
        for (AttributeFlag flag : AttributeFlag.values()) {
            map.put(flag, new HashSet<String>());
        }
    }

    @Before
    public void setUp() throws Exception {
        am = new AttributeManager();
        initFlagToAttrsMap(am);

        // a single-valued string attribute
        am.addAttribute(info("zimbraSingleString", AttributeType.TYPE_STRING,
                AttributeCardinality.single, null, Sets.newHashSet(AttributeClass.account)));

        // a multi-valued id attribute
        am.addAttribute(info("zimbraMultiId", AttributeType.TYPE_ID,
                AttributeCardinality.multi, null, Sets.newHashSet(AttributeClass.account)));

        // an email-typed attribute (drives IDNType.email)
        am.addAttribute(info("zimbraEmailish", AttributeType.TYPE_EMAIL,
                AttributeCardinality.single, null, Sets.newHashSet(AttributeClass.account)));

        // a binary attribute
        am.addAttribute(info("zimbraBinaryish", AttributeType.TYPE_BINARY,
                AttributeCardinality.single, null, Sets.newHashSet(AttributeClass.account)));

        // a certificate attribute (binary transfer)
        am.addAttribute(info("zimbraCertish", AttributeType.TYPE_CERTIFICATE,
                AttributeCardinality.single, null, Sets.newHashSet(AttributeClass.account)));

        // a non-dynamic ephemeral attribute
        am.addAttribute(info("zimbraEphemSingle", AttributeType.TYPE_ASTRING,
                AttributeCardinality.single, Sets.newHashSet(AttributeFlag.ephemeral),
                Sets.newHashSet(AttributeClass.account)));

        // a dynamic ephemeral attribute
        am.addAttribute(info("zimbraEphemDynamic", AttributeType.TYPE_ASTRING,
                AttributeCardinality.multi,
                Sets.newHashSet(AttributeFlag.ephemeral, AttributeFlag.dynamic),
                Sets.newHashSet(AttributeClass.account)));
    }

    // ---- cardinality ---------------------------------------------------------------------

    @Test
    public void isMultiValuedMultiAttrIsTrue() {
        assertTrue(am.isMultiValued("zimbraMultiId"));
    }

    @Test
    public void isMultiValuedSingleAttrIsFalse() {
        assertFalse(am.isMultiValued("zimbraSingleString"));
    }

    @Test
    public void isMultiValuedUnknownAttrIsFalse() {
        assertFalse("unknown attr is not multi-valued", am.isMultiValued("nopeNotHere"));
    }

    // ---- ephemeral / dynamic -------------------------------------------------------------

    @Test
    public void isEphemeralEphemeralAttrIsTrue() {
        assertTrue(am.isEphemeral("zimbraEphemSingle"));
        assertFalse("non-ephemeral attr reports false", am.isEphemeral("zimbraSingleString"));
    }

    @Test
    public void isDynamicDynamicEphemeralAttrIsTrue() {
        assertTrue(am.isDynamic("zimbraEphemDynamic"));
    }

    @Test
    public void isDynamicNonDynamicEphemeralAttrIsFalse() {
        assertFalse(am.isDynamic("zimbraEphemSingle"));
        assertFalse("unknown attr is not dynamic", am.isDynamic("nopeNotHere"));
    }

    @Test
    public void getNonDynamicEphemeralAttrsAccountIncludesNonDynamicOnly() {
        // Act
        java.util.Map<String, AttributeInfo> attrs =
                am.getNonDynamicEphemeralAttrs(Entry.EntryType.ACCOUNT);

        // Assert — the single ephemeral is present, the dynamic one is excluded
        assertNotNull(attrs);
        assertTrue(attrs.containsKey("zimbraEphemSingle"));
        assertFalse(attrs.containsKey("zimbraEphemDynamic"));
    }

    // ---- IDN typing ----------------------------------------------------------------------

    @Test
    public void idnTypeEmailAttrIsEmail() {
        // Act
        IDNType actual = AttributeManager.idnType(am, "zimbraEmailish");

        // Assert
        assertEquals(IDNType.email, actual);
        assertTrue(actual.isEmailOrIDN());
    }

    @Test
    public void idnTypePlainStringAttrIsNone() {
        // Act
        IDNType actual = AttributeManager.idnType(am, "zimbraSingleString");

        // Assert
        assertEquals(IDNType.none, actual);
        assertFalse(actual.isEmailOrIDN());
    }

    @Test
    public void idnTypeNullManagerIsNone() {
        // Act / Assert — static guard against a null manager
        assertEquals(IDNType.none, AttributeManager.idnType(null, "anything"));
    }

    // ---- binary typing -------------------------------------------------------------------

    @Test
    public void isBinaryTypeBinaryVsStringClassifiesCorrectly() {
        assertTrue(AttributeManager.isBinaryType(AttributeType.TYPE_BINARY));
        assertFalse(AttributeManager.isBinaryType(AttributeType.TYPE_STRING));
    }

    @Test
    public void isBinaryTransferTypeCertificateOnly() {
        assertTrue(AttributeManager.isBinaryTransferType(AttributeType.TYPE_CERTIFICATE));
        assertFalse(AttributeManager.isBinaryTransferType(AttributeType.TYPE_BINARY));
    }

    @Test
    public void containsBinaryDataBinaryAttrIsTrue() {
        // addAttribute does not populate mBinaryAttrs (that happens in loadAttrs), but the
        // static type classification is the load-bearing branch we assert here.
        assertTrue(AttributeManager.isBinaryType(AttributeType.TYPE_BINARY));
        assertFalse("plain string attr holds no binary data", am.containsBinaryData("zimbraSingleString"));
    }

    // ---- attribute info / type lookup ----------------------------------------------------

    @Test
    public void getAttributeInfoKnownAttrReturnsInfo() {
        // Act
        AttributeInfo ai = am.getAttributeInfo("zimbraSingleString");

        // Assert
        assertNotNull(ai);
        assertEquals("zimbraSingleString", ai.getName());
        assertEquals(AttributeType.TYPE_STRING, ai.getType());
    }

    @Test
    public void getAttributeInfoCaseInsensitiveAndNullHandled() {
        // Act / Assert — lookup is case-insensitive and null-safe
        assertNotNull(am.getAttributeInfo("ZIMBRAMULTIID"));
        assertNull(am.getAttributeInfo(null));
        assertNull(am.getAttributeInfo("doesNotExist"));
    }

    @Test
    public void getAttributeTypeKnownAttrReturnsType() throws Exception {
        // Act
        AttributeType type = am.getAttributeType("zimbraMultiId");

        // Assert
        assertEquals(AttributeType.TYPE_ID, type);
    }

    @Test
    public void getAttributeTypeUnknownAttrThrowsInvalidAttrName() {
        // Act / Assert
        try {
            am.getAttributeType("totallyUnknownAttr");
            fail("expected ServiceException for unknown attribute");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().toLowerCase().contains("unknown attribute"));
        }
    }

    // ---- flag-backed predicates ----------------------------------------------------------

    @Test
    public void inheritanceFlagsUnsetAttrAreFalse() {
        // The no-arg AttributeManager does not init the flag map, but these getters
        // tolerate that by returning false for an attr with no flag entries.
        assertFalse(am.isEphemeral("zimbraSingleString"));
    }

    @Test
    public void getEphemeralAttributeNamesContainsRegisteredEphemerals() {
        // Act
        Set<String> names = am.getEphemeralAttributeNames();

        // Assert
        assertTrue(names.contains("zimbraEphemSingle"));
        assertTrue(names.contains("zimbraEphemDynamic"));
        assertFalse(names.contains("zimbraSingleString"));
    }

    // ---- reflection helpers to seed the maps addAttribute does not populate ---------------

    @SuppressWarnings("unchecked")
    private void addFlagAttr(AttributeFlag flag, String attr) throws Exception {
        Field f = AttributeManager.class.getDeclaredField("mFlagToAttrsMap");
        f.setAccessible(true);
        Map<AttributeFlag, Set<String>> map = (Map<AttributeFlag, Set<String>>) f.get(am);
        map.get(flag).add(attr);
    }

    @SuppressWarnings("unchecked")
    private void addClassAttr(String mapField, AttributeClass klass, String attr) throws Exception {
        Field f = AttributeManager.class.getDeclaredField(mapField);
        f.setAccessible(true);
        Map<AttributeClass, Set<String>> map = (Map<AttributeClass, Set<String>>) f.get(am);
        Set<String> set = map.get(klass);
        if (set == null) {
            set = new HashSet<String>();
            map.put(klass, set);
        }
        set.add(attr);
    }

    private AttributeInfo infoWithSince(String name, AttributeType type, List<Version> since)
            throws Exception {
        return new AttributeInfo(name, 1, null, 0, null, type, null, "", true, null, null,
                AttributeCardinality.single, null, Sets.newHashSet(AttributeClass.account),
                null, null, null, null, null, null,
                "desc for " + name, null, since, null);
    }

    // ---- inheritance / flag-backed predicates --------------------------------------------

    @Test
    public void isAccountInheritedFlaggedAttrIsTrueOthersFalse() throws Exception {
        // Arrange — flag one attr as account-inherited
        addFlagAttr(AttributeFlag.accountInherited, "zimbraSingleString");

        // Act / Assert
        assertTrue(am.isAccountInherited("zimbraSingleString"));
        assertFalse("unflagged attr is not account-inherited", am.isAccountInherited("zimbraMultiId"));
    }

    @Test
    public void isAccountCosDomainInheritedFlaggedAttrIsTrue() throws Exception {
        // Arrange
        addFlagAttr(AttributeFlag.accountCosDomainInherited, "zimbraMultiId");

        // Act / Assert
        assertTrue(am.isAccountCosDomainInherited("zimbraMultiId"));
        assertFalse(am.isAccountCosDomainInherited("zimbraSingleString"));
    }

    @Test
    public void isDomainInheritedFlaggedAttrIsTrue() throws Exception {
        // Arrange
        addFlagAttr(AttributeFlag.domainInherited, "zimbraSingleString");

        // Act / Assert
        assertTrue(am.isDomainInherited("zimbraSingleString"));
        assertFalse(am.isDomainInherited("zimbraMultiId"));
    }

    @Test
    public void isServerInheritedFlaggedAttrIsTrue() throws Exception {
        // Arrange
        addFlagAttr(AttributeFlag.serverInherited, "zimbraSingleString");

        // Act / Assert
        assertTrue(am.isServerInherited("zimbraSingleString"));
        assertFalse(am.isServerInherited("zimbraMultiId"));
    }

    @Test
    public void makeDomainAdminModifiableThenIsDomainAdminModifiableIsTrue() throws Exception {
        // Arrange — the attr must be a known attr in the class to pass the guard
        addClassAttr("mClassToAllAttrsMap", AttributeClass.account, "zimbraSingleString");

        // Act — initially not modifiable, then mark it
        boolean before = am.isDomainAdminModifiable("zimbraSingleString", AttributeClass.account);
        am.makeDomainAdminModifiable("zimbraSingleString");
        boolean after = am.isDomainAdminModifiable("zimbraSingleString", AttributeClass.account);

        // Assert — state transition from not-modifiable to modifiable
        assertFalse("attr starts as not domain-admin-modifiable", before);
        assertTrue("attr becomes domain-admin-modifiable after marking", after);
    }

    @Test
    public void isDomainAdminModifiableUnknownAttrInClassThrowsInvalidAttrName() throws Exception {
        // Arrange — the class map must contain the class key (an existing, non-null set) so the
        // guard at AttributeManager#isDomainAdminModifiable reaches the membership check; seed it
        // with a different attr so the queried attr is genuinely unknown for the class.
        addClassAttr("mClassToAllAttrsMap", AttributeClass.account, "zimbraMultiId");
        try {
            // Act
            am.isDomainAdminModifiable("zimbraSingleString", AttributeClass.account);
            fail("expected INVALID_ATTR_NAME for attr not in class");
        } catch (ServiceException e) {
            // Assert
            assertTrue(e.getMessage().toLowerCase().contains("unknown attribute"));
        }
    }

    @Test
    public void hasFlagFlaggedAttrIsTrue() throws Exception {
        // Arrange
        addFlagAttr(AttributeFlag.idn, "zimbraSingleString");

        // Act / Assert — package-private hasFlag (same package)
        assertTrue(am.hasFlag(AttributeFlag.idn, "zimbraSingleString"));
        assertFalse(am.hasFlag(AttributeFlag.idn, "zimbraMultiId"));
    }

    @Test
    public void getAttrsWithFlagReturnsFlaggedSet() throws Exception {
        // Arrange
        addFlagAttr(AttributeFlag.idn, "zimbraSingleString");

        // Act
        Set<String> attrs = am.getAttrsWithFlag(AttributeFlag.idn);

        // Assert
        assertNotNull(attrs);
        assertTrue(attrs.contains("zimbraSingleString"));
    }

    // ---- IDN typing extra branches -------------------------------------------------------

    @Test
    public void idnTypeIdnFlaggedAttrIsIdn() throws Exception {
        // Arrange — a plain string attr that carries the idn flag
        addFlagAttr(AttributeFlag.idn, "zimbraSingleString");

        // Act
        IDNType actual = AttributeManager.idnType(am, "zimbraSingleString");

        // Assert
        assertEquals(IDNType.idn, actual);
        assertTrue(actual.isEmailOrIDN());
    }

    @Test
    public void idnTypeUnknownAttrIsNone() {
        // Act / Assert — attr not in mAttrs at all
        assertEquals(IDNType.none, AttributeManager.idnType(am, "noSuchAttrEver"));
    }

    // ---- version checks ------------------------------------------------------------------

    @Test
    public void versionChecksAttrWithNoSinceAllReturnTrue() throws Exception {
        // The default fixture attr has since=null => inVersion/beforeVersion/addedIn are all true
        assertTrue(am.inVersion("zimbraSingleString", "8.0.0"));
        assertTrue(am.beforeVersion("zimbraSingleString", "8.0.0"));
        assertTrue(am.addedIn("zimbraSingleString", "8.0.0"));
    }

    @Test
    public void inVersionAttrAddedInSameVersionIsInButNotBefore() throws Exception {
        // Arrange — an attr introduced in 8.0.0
        List<Version> since = new ArrayList<Version>();
        since.add(new Version("8.0.0"));
        am.addAttribute(infoWithSince("zimbraVersionedAttr", AttributeType.TYPE_STRING, since));

        // Act / Assert — present in 8.0.0 but not strictly before 8.0.0
        assertTrue("attr is in its introducing version", am.inVersion("zimbraVersionedAttr", "8.0.0"));
        assertFalse("attr is not before its introducing version",
                am.beforeVersion("zimbraVersionedAttr", "8.0.0"));
    }

    @Test
    public void beforeVersionLaterVersionIsBefore() throws Exception {
        // Arrange — introduced in 8.0.0
        List<Version> since = new ArrayList<Version>();
        since.add(new Version("8.0.0"));
        am.addAttribute(infoWithSince("zimbraVersionedAttr2", AttributeType.TYPE_STRING, since));

        // Act / Assert — 8.5.0 is a later minor than the 8.0.0 introduction, so the attr is
        // "before" 8.5.0. addedIn(in=true, before=false) only matches an exact-version
        // introduction, so for a strictly-later version it returns false (it is not *added in*
        // 8.5.0). It is, however, present in 8.5.0 per inVersion.
        assertTrue(am.beforeVersion("zimbraVersionedAttr2", "8.5.0"));
        assertFalse("attr introduced in 8.0.0 was not added in the later 8.5.0",
                am.addedIn("zimbraVersionedAttr2", "8.5.0"));
        assertTrue("attr introduced in 8.0.0 is present in the later 8.5.0",
                am.inVersion("zimbraVersionedAttr2", "8.5.0"));
    }

    @Test
    public void inVersionEarlierVersionThanSinceIsNotIn() throws Exception {
        // Arrange — introduced in 8.5.0
        List<Version> since = new ArrayList<Version>();
        since.add(new Version("8.5.0"));
        am.addAttribute(infoWithSince("zimbraVersionedAttr3", AttributeType.TYPE_STRING, since));

        // Act / Assert — 8.0.0 predates introduction
        assertFalse("attr introduced in 8.5.0 is not in 8.0.0",
                am.inVersion("zimbraVersionedAttr3", "8.0.0"));
    }

    @Test
    public void versionCheckUnknownAttrThrowsInvalidAttrName() throws Exception {
        // Act / Assert
        try {
            am.inVersion("totallyUnknownVersionedAttr", "8.0.0");
            fail("expected INVALID_ATTR_NAME for unknown attr");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().toLowerCase().contains("unknown attribute"));
        }
    }

    @Test
    public void isFutureNonFutureAttrIsFalse() {
        // Default attr has since=null => not future
        assertFalse(am.isFuture("zimbraSingleString"));
    }

    // ---- binary / class accessors --------------------------------------------------------

    @Test
    public void isBinaryTransferTypeAttrNameCertificateIsTransfer() throws Exception {
        // Arrange — seed the static binary-transfer set via reflection
        Field f = AttributeManager.class.getDeclaredField("mBinaryTransferAttrs");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> set = (Set<String>) f.get(null);
        set.add("zimbracertish");

        // Act / Assert — name-based binary-transfer lookup is lowercased
        assertTrue(am.isBinaryTransfer("zimbraCertish"));
        assertFalse(am.isBinaryTransfer("zimbraSingleString"));
    }

    @Test
    public void getBinaryAttrsReturnsNonNullSet() {
        // Act / Assert — the (possibly empty) backing set is returned, never null
        Set<String> binary = am.getBinaryAttrs();
        Set<String> transfer = am.getBinaryTransferAttrs();
        assertNotNull(binary);
        assertNotNull(transfer);
        assertFalse("plain string attr is not registered as binary", binary.contains("zimbrasinglestring"));
    }

    @Test
    public void getEphemeralAttrsIncludesRegisteredEphemerals() {
        // Act
        Map<String, AttributeInfo> ephem = am.getEphemeralAttrs();

        // Assert — keyed by lower-case name
        assertNotNull(ephem);
        assertTrue(ephem.containsKey("zimbraephemsingle"));
        assertTrue(ephem.containsKey("zimbraephemdynamic"));
    }

    @Test
    public void getAttrsInClassEmptyMapReturnsNull() {
        // Act / Assert — addAttribute does not populate mClassToAttrsMap
        assertNull(am.getAttrsInClass(AttributeClass.account));
    }

    @Test
    public void getAttrsInClassSeededMapReturnsAttrs() throws Exception {
        // Arrange
        addClassAttr("mClassToAttrsMap", AttributeClass.account, "zimbraSingleString");

        // Act
        Set<String> attrs = am.getAttrsInClass(AttributeClass.account);

        // Assert
        assertNotNull(attrs);
        assertTrue(attrs.contains("zimbraSingleString"));
    }

    @Test
    public void getLowerCaseAttrsInClassSeededMapReturnsAttrs() throws Exception {
        // Arrange
        addClassAttr("mClassToLowerCaseAttrsMap", AttributeClass.account, "zimbrasinglestring");

        // Act
        Set<String> attrs = am.getLowerCaseAttrsInClass(AttributeClass.account);

        // Assert
        assertNotNull(attrs);
        assertTrue(attrs.contains("zimbrasinglestring"));
    }

    @Test
    public void getAllAttrsInClassSeededMapReturnsAttrs() throws Exception {
        // Arrange
        addClassAttr("mClassToAllAttrsMap", AttributeClass.account, "zimbraSingleString");

        // Act
        Set<String> attrs = am.getAllAttrsInClass(AttributeClass.account);

        // Assert
        assertNotNull(attrs);
        assertTrue(attrs.contains("zimbraSingleString"));
    }

    @Test
    public void getImmutableAttrsIncludesImmutableRegisteredAttrs() {
        // The fixture attrs were all created with immutable=true
        Set<String> immutable = am.getImmutableAttrs();
        assertNotNull(immutable);
        assertTrue(immutable.contains("zimbraSingleString"));
    }

    @Test
    public void getImmutableAttrsInClassSeededMapIncludesImmutableAttr() throws Exception {
        // Arrange — class map references the immutable fixture attr
        addClassAttr("mClassToAttrsMap", AttributeClass.account, "zimbraSingleString");

        // Act
        Set<String> immutable = am.getImmutableAttrsInClass(AttributeClass.account);

        // Assert
        assertTrue(immutable.contains("zimbraSingleString"));
    }

    @Test
    public void setMinimizeTogglesStaticFlagNoThrow() {
        // Act / Assert — pure static setter, exercise both values and restore
        AttributeManager.setMinimize(true);
        AttributeManager.setMinimize(false);
    }

    // ---- preModify / postModify ----------------------------------------------------------

    @Test
    public void preModifyEmptyAttrNameThrowsInvalidAttrName() throws Exception {
        // Arrange — a map with an empty-string key
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("", "value");

        // Act / Assert
        try {
            am.preModify(attrs, null, new CallbackContext(CallbackContext.Op.MODIFY), false);
            fail("expected INVALID_ATTR_NAME for empty attr name");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().toLowerCase().contains("empty attr name"));
        }
    }

    @Test
    public void preModifyKnownStringAttrAppliesCleanly() throws Exception {
        // Arrange — a value that satisfies the string attr
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("zimbraSingleString", "hello");

        // Act — no callback, no immutable check => should not throw
        am.preModify(attrs, null, new CallbackContext(CallbackContext.Op.MODIFY), false);

        // Assert — value preserved
        assertEquals("hello", attrs.get("zimbraSingleString"));
    }

    @Test
    public void preModifyAddPrefixedAttrNameStrippedAndApplied() throws Exception {
        // Arrange — '+' prefix is stripped before lookup. Use the plain string attr so the
        // checkValue path stays pure; the TYPE_ID validation path calls
        // Provisioning.getInstance() which is unreachable under this LDAP-free harness.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("+zimbraSingleString", "hello");

        // Act — '+'-prefixed name resolves to the underlying attr and validates without throwing
        am.preModify(attrs, null, new CallbackContext(CallbackContext.Op.MODIFY), false);

        // Assert — the prefixed key is preserved and its value untouched
        assertTrue(attrs.containsKey("+zimbraSingleString"));
        assertEquals("hello", attrs.get("+zimbraSingleString"));
    }

    @Test
    public void preModifyUnknownAttrIgnoredNoThrow() throws Exception {
        // Arrange — unknown attr name simply logs and continues
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("noSuchAttrEver", "value");

        // Act — no exception expected
        am.preModify(attrs, null, new CallbackContext(CallbackContext.Op.MODIFY), false);

        // Assert
        assertTrue(attrs.containsKey("noSuchAttrEver"));
    }

    @Test
    public void postModifyKnownAttrNoCallbackNoThrow() throws Exception {
        // Arrange — fixture attrs have no callback
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("zimbraSingleString", "hello");

        // Act — should be a no-op, no exception
        am.postModify(attrs, null, new CallbackContext(CallbackContext.Op.MODIFY));

        // Assert
        assertEquals("hello", attrs.get("zimbraSingleString"));
    }

    @Test
    public void postModifyPrefixedAndUnknownAttrsNoThrow() throws Exception {
        // Arrange — exercises the '-' strip branch and the unknown-attr skip
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("-zimbraSingleString", "x");
        attrs.put("noSuchAttrEver", "y");

        // Act / Assert — completes without throwing
        am.postModify(attrs, null, new CallbackContext(CallbackContext.Op.MODIFY), true);
        assertTrue(attrs.containsKey("noSuchAttrEver"));
    }

    // ====================================================================================
    // Mutation-killing assertions (PIT survivors strengthened).
    // ====================================================================================

    /** Records every pre/postModify callback invocation by attr name. */
    private static class RecordingCallback extends AttributeCallback {
        private final List<String> preCalls = new ArrayList<String>();

        private final List<String> postCalls = new ArrayList<String>();

        @Override
        public void preModify(CallbackContext context, String attrName, Object attrValue,
                @SuppressWarnings("rawtypes") Map attrsToModify, Entry entry) {
            preCalls.add(attrName);
        }

        @Override
        public void postModify(CallbackContext context, String attrName, Entry entry) {
            postCalls.add(attrName);
        }
    }

    private AttributeInfo infoWithCallback(String name, AttributeCallback cb, boolean immutable) {
        return new AttributeInfo(name, 1, null, 0, cb, AttributeType.TYPE_STRING, null, "",
                immutable, null, null, AttributeCardinality.single, null,
                Sets.newHashSet(AttributeClass.account), null, null, null, null, null, null,
                "desc for " + name, null, null, null);
    }

    // ---- preModify: checkValue side effect (L1343) + immutable strip (L1330) -------------

    @Test
    public void preModifyImmutableKnownAttrCheckImmutableThrowsImmutable() throws Exception {
        // The fixture zimbraSingleString is immutable=true. With checkImmutable=true the
        // info.checkValue(...) call MUST reject it. If that call is removed, no throw occurs.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("zimbraSingleString", "hello");
        try {
            am.preModify(attrs, null, new CallbackContext(CallbackContext.Op.MODIFY), true);
            fail("checkValue must reject an immutable attr when checkImmutable=true");
        } catch (ServiceException e) {
            assertTrue("expected immutable rejection, got: " + e.getMessage(),
                    e.getMessage().toLowerCase().contains("immutable"));
        }
    }

    @Test
    public void preModifyPlusPrefixedImmutableAttrIsStrippedThenRejected() throws Exception {
        // The '+' prefix MUST be stripped (L1330) so the attr resolves to the immutable
        // fixture and checkValue rejects it. If the strip is skipped, the lookup misses and
        // no exception is thrown.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("+zimbraSingleString", "hello");
        try {
            am.preModify(attrs, null, new CallbackContext(CallbackContext.Op.MODIFY), true);
            fail("'+'-prefixed name must be stripped and the immutable attr rejected");
        } catch (ServiceException e) {
            assertTrue("expected immutable rejection, got: " + e.getMessage(),
                    e.getMessage().toLowerCase().contains("immutable"));
        }
    }

    @Test
    public void preModifyBadIntegerValueCheckValueRejects() throws Exception {
        // A mutable integer attr with a non-numeric value: checkValue (L1343) must throw.
        // Removing the checkValue call lets the bad value slip through silently.
        // info(...) builds immutable=true; checkImmutable=false isolates the value-format branch.
        am.addAttribute(info("zimbraMutInt", AttributeType.TYPE_INTEGER,
                AttributeCardinality.single, null, Sets.newHashSet(AttributeClass.account)));
        // Note: info(...) builds immutable=true, so use checkImmutable=false to isolate the
        // value-format branch rather than the immutable branch.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("zimbraMutInt", "not-an-int");
        try {
            am.preModify(attrs, null, new CallbackContext(CallbackContext.Op.MODIFY), false);
            fail("checkValue must reject a non-numeric integer value");
        } catch (ServiceException e) {
            assertTrue("expected invalid integer rejection, got: " + e.getMessage(),
                    e.getMessage().toLowerCase().contains("integer"));
        }
    }

    // ---- preModify callback invocation (L1344) -------------------------------------------

    @Test
    public void preModifyAttrWithCallbackInvokesCallbackOnce() throws Exception {
        // The info.getCallback().preModify(...) call (L1344) MUST fire exactly once for the
        // matching attr. If the guarded call is removed/negated, preCalls stays empty.
        RecordingCallback cb = new RecordingCallback();
        am.addAttribute(infoWithCallback("zimbraCbAttr", cb, false));

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("zimbraCbAttr", "v");
        am.preModify(attrs, null, new CallbackContext(CallbackContext.Op.MODIFY), false);

        assertEquals("preModify callback must fire exactly once", 1, cb.preCalls.size());
        assertEquals("zimbraCbAttr", cb.preCalls.get(0));
    }

    @Test
    public void preModifyAllowCallbackFalseDoesNotInvokeCallback() throws Exception {
        // allowCallback=false MUST suppress the callback (the '&&' at L1344). If the guard is
        // negated, the callback would fire even though it was disallowed.
        RecordingCallback cb = new RecordingCallback();
        am.addAttribute(infoWithCallback("zimbraCbAttr2", cb, false));

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("zimbraCbAttr2", "v");
        am.preModify(attrs, null, new CallbackContext(CallbackContext.Op.MODIFY), false, false);

        assertEquals("callback must NOT fire when allowCallback=false", 0, cb.preCalls.size());
    }

    // ---- postModify callback invocation (L1355 delegation, L1361 loop, L1364 strip, L1367) -

    @Test
    public void postModifyThreeArgAttrWithCallbackInvokesPostCallbackOnce() throws Exception {
        // The 3-arg postModify delegates to the 4-arg with allowCallback=true (L1355), iterates
        // the keys (L1361), strips +/- (L1364), and invokes the callback (L1367). Asserting the
        // callback fired exactly once for the right key kills all four mutations on this path.
        RecordingCallback cb = new RecordingCallback();
        am.addAttribute(infoWithCallback("zimbraPostCb", cb, false));

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("zimbraPostCb", "v");
        am.postModify(attrs, null, new CallbackContext(CallbackContext.Op.MODIFY));

        assertEquals("postModify callback must fire exactly once via the 3-arg delegate",
                1, cb.postCalls.size());
        assertEquals("zimbraPostCb", cb.postCalls.get(0));
    }

    @Test
    public void postModifyMinusPrefixedAttrIsStrippedThenCallbackFires() throws Exception {
        // A '-'-prefixed key MUST be stripped (L1364) so the underlying attr's callback fires.
        // If the strip is skipped, the lookup misses and the callback never runs.
        RecordingCallback cb = new RecordingCallback();
        am.addAttribute(infoWithCallback("zimbraPostCbStrip", cb, false));

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("-zimbraPostCbStrip", "v");
        am.postModify(attrs, null, new CallbackContext(CallbackContext.Op.MODIFY), true);

        assertEquals("'-'-prefixed key must be stripped and the callback fired",
                1, cb.postCalls.size());
        assertEquals("zimbraPostCbStrip", cb.postCalls.get(0));
    }

    @Test
    public void postModifyAllowCallbackFalseDoesNotInvokeCallback() throws Exception {
        // allowCallback=false MUST suppress the postModify callback (L1367 '&&').
        RecordingCallback cb = new RecordingCallback();
        am.addAttribute(infoWithCallback("zimbraPostCbOff", cb, false));

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("zimbraPostCbOff", "v");
        am.postModify(attrs, null, new CallbackContext(CallbackContext.Op.MODIFY), false);

        assertEquals("postModify callback must NOT fire when allowCallback=false",
                0, cb.postCalls.size());
    }

    // ---- versionCheck later-major/minor branch (L1185) -----------------------------------

    @Test
    public void beforeVersionMultiSinceEarlierEntryIsBeforeViaLaterMajorMinorBranch()
            throws Exception {
        // since = [7.0.0, 9.0.0]; check beforeVersion against 8.0.0.
        //  - iter 7.0.0: 8.0.0 is a later major/minor, so the L1185 branch sets
        //    good = (before && 7.0.0.compare("8.0.0") < 0) = true
        //  - iter 9.0.0: 8.0.0 is NOT a later major/minor, so returns the accumulated good=true
        // Negating the '< 0' at L1185 (>= 0) flips good to false, returning false.
        List<Version> since = new ArrayList<Version>();
        since.add(new Version("7.0.0"));
        since.add(new Version("9.0.0"));
        am.addAttribute(infoWithSince("zimbraMultiSince", AttributeType.TYPE_STRING, since));

        assertTrue("attr introduced in 7.0.0 must be 'before' 8.0.0 via the later-series branch",
                am.beforeVersion("zimbraMultiSince", "8.0.0"));
        // addedIn (in=true, before=false) must be false at 8.0.0: neither 7.0.0 nor 9.0.0
        // equals 8.0.0, so good stays false.
        assertFalse("attr was not *added in* 8.0.0", am.addedIn("zimbraMultiSince", "8.0.0"));
    }

    // ---- getBinaryAttrs / getBinaryTransferAttrs return the live set (L1236, L1240) ------

    @Test
    @SuppressWarnings("unchecked")
    public void getBinaryAttrsReturnsSeededBackingSetNotEmpty() throws Exception {
        // Seed the static backing set, then assert the getter returns THAT populated set.
        // The EmptyObjectReturn mutation would hand back an empty set, failing the membership.
        Field f = AttributeManager.class.getDeclaredField("mBinaryAttrs");
        f.setAccessible(true);
        Set<String> backing = (Set<String>) f.get(null);
        backing.add("zimbraseededbinary");

        Set<String> returned = am.getBinaryAttrs();
        assertTrue("getBinaryAttrs must return the populated backing set",
                returned.contains("zimbraseededbinary"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getBinaryTransferAttrsReturnsSeededBackingSetNotEmpty() throws Exception {
        Field f = AttributeManager.class.getDeclaredField("mBinaryTransferAttrs");
        f.setAccessible(true);
        Set<String> backing = (Set<String>) f.get(null);
        backing.add("zimbraseededtransfer");

        Set<String> returned = am.getBinaryTransferAttrs();
        assertTrue("getBinaryTransferAttrs must return the populated backing set",
                returned.contains("zimbraseededtransfer"));
    }
}
