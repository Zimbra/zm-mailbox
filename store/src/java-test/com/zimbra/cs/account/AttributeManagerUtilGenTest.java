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
import com.zimbra.cs.account.AttributeManagerUtil.CLOptions;
import com.zimbra.cs.account.AttributeManagerUtil.SetterType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for the {@code @VisibleForTesting} code-generation entry points
 * {@link AttributeManagerUtil#generateGetter} and {@link AttributeManagerUtil#generateSetter}
 * over NON-ephemeral attribute types. Complements {@code GenerateEphemeralGettersTest}, which
 * covers the ephemeral branches.
 */
public class AttributeManagerUtilGenTest {

    private AttributeInfo info(String name, AttributeType type, AttributeCardinality card,
            Set<AttributeFlag> flags) {
        return new AttributeInfo(name, 1, null, 0, null, type, null, "", true, null, null,
                card, Sets.newHashSet(AttributeClass.account), null, flags, null, null, null,
                null, null, "Test Attribute", null, null, null);
    }

    /* Variant that lets a test supply a value (used for enum value lists) and defaultCOS values. */
    private AttributeInfo info(String name, AttributeType type, AttributeCardinality card,
            Set<AttributeFlag> flags, String value, List<String> defaultCosValues) {
        return new AttributeInfo(name, 1, null, 0, null, type, null, value, true, null, null,
                card, Sets.newHashSet(AttributeClass.account), null, flags, null, defaultCosValues,
                null, null, null, "Test Attribute", null, null, null);
    }

    private static List<String> list(String... values) {
        List<String> l = new ArrayList<String>();
        for (String v : values) {
            l.add(v);
        }
        return l;
    }

    private void assertContains(StringBuilder generated, String fragment) {
        assertTrue(String.format("generated source should contain '%s' but was:%n%s",
                fragment, generated), generated.toString().contains(fragment));
    }

    private void assertNotContains(StringBuilder generated, String fragment) {
        assertFalse(String.format("generated source should NOT contain '%s' but was:%n%s",
                fragment, generated), generated.toString().contains(fragment));
    }

    /* Variant accepting an explicit description and since-version list. */
    private AttributeInfo infoFull(String name, AttributeType type, AttributeCardinality card,
            String description, List<com.zimbra.common.util.Version> since) {
        return new AttributeInfo(name, 1, null, 0, null, type, null, "", true, null, null,
                card, Sets.newHashSet(AttributeClass.account), null, null, null, null,
                null, null, null, description, null, since, null);
    }

    @SuppressWarnings("unchecked")
    private static String versionListAsString(List<com.zimbra.common.util.Version> versions)
            throws Exception {
        Method m = AttributeManagerUtil.class.getDeclaredMethod("versionListAsString", List.class);
        m.setAccessible(true);
        return (String) m.invoke(null, versions);
    }

    @Test
    public void generateGetterStringEmitsGetAttrBody() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraFoo", AttributeType.TYPE_STRING,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);

        // Assert
        assertContains(sb, "public String getFoo()");
        assertContains(sb, "return getAttr(Provisioning.A_zimbraFoo, null, true);");
    }

    @Test
    public void generateGetterBooleanEmitsIsPrefixAndBooleanBody() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraFlag", AttributeType.TYPE_BOOLEAN,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);

        // Assert — boolean getters use "is" prefix and getBooleanAttr
        assertContains(sb, "public boolean isFlag()");
        assertContains(sb, "getBooleanAttr(Provisioning.A_zimbraFlag, false, true)");
    }

    @Test
    public void generateGetterIntegerEmitsIntBodyWithDefault() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraCount", AttributeType.TYPE_INTEGER,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);

        // Assert — no configured default falls back to -1
        assertContains(sb, "public int getCount()");
        assertContains(sb, "getIntAttr(Provisioning.A_zimbraCount, -1, true)");
    }

    @Test
    public void generateGetterMultiValuedStringEmitsArrayBody() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraTags", AttributeType.TYPE_STRING,
                AttributeCardinality.multi, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);

        // Assert
        assertContains(sb, "public String[] getTags()");
        assertContains(sb, "getMultiAttr(Provisioning.A_zimbraTags, true, true)");
    }

    @Test
    public void generateSetterSetSingleStringEmitsAttrsPutAndModify() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraFoo", AttributeType.TYPE_STRING,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act — noMap=true generates the throws/Provisioning.modifyAttrs variant
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);

        // Assert
        assertContains(sb, "public void setFoo(String zimbraFoo)");
        assertContains(sb, "attrs.put(Provisioning.A_zimbraFoo, zimbraFoo);");
        assertContains(sb, "getProvisioning().modifyAttrs(this, attrs);");
    }

    @Test
    public void generateSetterUnsetEmitsEmptyStringPut() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraFoo", AttributeType.TYPE_STRING,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.unset, true);

        // Assert — unset takes no value parameter and writes an empty string
        assertContains(sb, "public void unsetFoo()");
        assertContains(sb, "attrs.put(Provisioning.A_zimbraFoo, \"\");");
    }

    @Test
    public void generateSetterAddMultiValuedEmitsAddToMultiMap() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraTags", AttributeType.TYPE_STRING,
                AttributeCardinality.multi, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.add, true);

        // Assert — add prepends "+" to the attr key
        assertContains(sb, "public void addTags(String zimbraTags)");
        assertContains(sb, "StringUtil.addToMultiMap(attrs, \"+\" + Provisioning.A_zimbraTags, zimbraTags);");
    }

    @Test
    public void generateSetterRemoveMultiValuedEmitsAddToMultiMapMinus() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraTags", AttributeType.TYPE_STRING,
                AttributeCardinality.multi, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.remove, true);

        // Assert — remove prepends "-" to the attr key
        assertContains(sb, "public void removeTags(String zimbraTags)");
        assertContains(sb, "StringUtil.addToMultiMap(attrs, \"-\" + Provisioning.A_zimbraTags, zimbraTags);");
    }

    @Test
    public void generateSetterSetWithMapVariantReturnsMap() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraFoo", AttributeType.TYPE_STRING,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act — noMap=false generates the Map-returning overload
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, false);

        // Assert
        assertContains(sb, "Map<String,Object> setFoo(String zimbraFoo, Map<String,Object> attrs)");
        assertContains(sb, "if (attrs == null) attrs = new HashMap<String,Object>();");
        assertContains(sb, "return attrs;");
    }

    @Test
    public void generateSetterBooleanSetEmitsTrueFalseTernary() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraFlag", AttributeType.TYPE_BOOLEAN,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);

        // Assert
        assertContains(sb, "public void setFlag(boolean zimbraFlag)");
        assertContains(sb, "attrs.put(Provisioning.A_zimbraFlag, zimbraFlag ? TRUE : FALSE);");
    }

    @Test
    public void generateGetterIntegerVsBooleanDoNotCrossContaminate() throws Exception {
        // Arrange
        StringBuilder ints = new StringBuilder();
        StringBuilder bools = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(ints,
                info("zimbraCount", AttributeType.TYPE_INTEGER, AttributeCardinality.single, null),
                false, AttributeClass.account);
        AttributeManagerUtil.generateGetter(bools,
                info("zimbraFlag", AttributeType.TYPE_BOOLEAN, AttributeCardinality.single, null),
                false, AttributeClass.account);

        // Assert — each generation is self-contained
        assertFalse("int getter must not contain a boolean body", ints.toString().contains("isFlag"));
        assertFalse("boolean getter must not contain an int body", bools.toString().contains("getCount"));
    }

    // ---- additional getter types --------------------------------------------------------

    @Test
    public void generateGetterLongEmitsLongBodyWithDefault() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraSize", AttributeType.TYPE_LONG,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);

        // Assert — no default falls back to -1L
        assertContains(sb, "public long getSize()");
        assertContains(sb, "getLongAttr(Provisioning.A_zimbraSize, -1L, true)");
    }

    @Test
    public void generateGetterBinaryEmitsByteArrayBody() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraBlob", AttributeType.TYPE_BINARY,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);

        // Assert
        assertContains(sb, "public byte[] getBlob()");
        assertContains(sb, "getBinaryAttr(Provisioning.A_zimbraBlob, true)");
    }

    @Test
    public void generateGetterGentimeEmitsDateBody() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraWhen", AttributeType.TYPE_GENTIME,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);

        // Assert — Date return type and an AsString @see hint (asStringDoc branch)
        assertContains(sb, "public Date getWhen()");
        assertContains(sb, "getGeneralizedTimeAttr(Provisioning.A_zimbraWhen, null, true)");
        assertContains(sb, "@see #getWhenAsString()");
    }

    @Test
    public void generateGetterPortEmitsIntBodyAndAsStringHint() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraPort", AttributeType.TYPE_PORT,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);

        // Assert
        assertContains(sb, "public int getPort()");
        assertContains(sb, "getIntAttr(Provisioning.A_zimbraPort, -1, true)");
        assertContains(sb, "@see #getPortAsString()");
    }

    @Test
    public void generateGetterDurationEmitsTimeIntervalBody() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraTtl", AttributeType.TYPE_DURATION,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);

        // Assert — long type, getTimeInterval body, AsString hint
        assertContains(sb, "public long getTtl()");
        assertContains(sb, "getTimeInterval(Provisioning.A_zimbraTtl, -1L, true)");
        assertContains(sb, "@see #getTtlAsString()");
    }

    @Test
    public void generateGetterEnumEmitsZAttrProvisioningTypeAndValidValuesDoc() throws Exception {
        // Arrange — enum value list drives the enum branch and the "Valid values" javadoc
        AttributeInfo ai = info("zimbraColor", AttributeType.TYPE_ENUM,
                AttributeCardinality.single, null, "red,green,blue", null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);

        // Assert
        assertContains(sb, "public ZAttrProvisioning.Color getColor()");
        assertContains(sb, "ZAttrProvisioning.Color.fromString(v)");
        assertContains(sb, "<p>Valid values:");
    }

    @Test
    public void generateGetterAsStringTrueAppendsAsStringSuffix() throws Exception {
        // Arrange — asString forces TYPE_STRING handling and an AsString method name
        AttributeInfo ai = info("zimbraCount", AttributeType.TYPE_INTEGER,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, true, AttributeClass.account);

        // Assert — even though the underlying type is int, asString renders a String getter
        assertContains(sb, "public String getCountAsString()");
        assertContains(sb, "getAttr(Provisioning.A_zimbraCount, null, true)");
    }

    @Test
    public void generateGetterStringWithDefaultCosValueEmitsQuotedDefault() throws Exception {
        // Arrange — a default COS value is rendered as a quoted default in the getter
        AttributeInfo ai = info("zimbraGreeting", AttributeType.TYPE_STRING,
                AttributeCardinality.single, null, "", list("hello"));
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);

        // Assert
        assertContains(sb, "public String getGreeting()");
        assertContains(sb, "getAttr(Provisioning.A_zimbraGreeting, \"hello\", true)");
    }

    @Test
    public void generateGetterMultiValuedWithDefaultEmitsValueLengthGuard() throws Exception {
        // Arrange — multi-valued with a default => "value.length > 0 ? value : default" body
        AttributeInfo ai = info("zimbraTags", AttributeType.TYPE_STRING,
                AttributeCardinality.multi, null, "", list("a", "b"));
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);

        // Assert
        assertContains(sb, "public String[] getTags()");
        assertContains(sb, "value.length > 0 ? value : new String[] {\"a\",\"b\"}");
    }

    @Test
    public void generateGetterDynamicAttrEmitsDynamicComponentParam() throws Exception {
        // Arrange — dynamic flag makes the getter take a dynamicComponent parameter
        Set<AttributeFlag> flags = Sets.newHashSet(AttributeFlag.dynamic);
        AttributeInfo ai = info("zimbraDyn", AttributeType.TYPE_STRING,
                AttributeCardinality.single, flags);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);

        // Assert
        assertContains(sb, "public String getDyn(String dynamicComponent)");
    }

    // ---- additional setter types --------------------------------------------------------

    @Test
    public void generateSetterIntSetEmitsIntegerToString() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraCount", AttributeType.TYPE_INTEGER,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);

        // Assert
        assertContains(sb, "public void setCount(int zimbraCount)");
        assertContains(sb, "attrs.put(Provisioning.A_zimbraCount, Integer.toString(zimbraCount));");
    }

    @Test
    public void generateSetterLongSetEmitsLongToString() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraSize", AttributeType.TYPE_LONG,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);

        // Assert
        assertContains(sb, "public void setSize(long zimbraSize)");
        assertContains(sb, "attrs.put(Provisioning.A_zimbraSize, Long.toString(zimbraSize));");
    }

    @Test
    public void generateSetterBinarySetEmitsEncodeLdapBase64() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraBlob", AttributeType.TYPE_BINARY,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);

        // Assert
        assertContains(sb, "public void setBlob(byte[] zimbraBlob)");
        assertContains(sb, "ByteUtil.encodeLDAPBase64(zimbraBlob)");
    }

    @Test
    public void generateSetterGentimeSetEmitsGeneralizedTime() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraWhen", AttributeType.TYPE_GENTIME,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);

        // Assert
        assertContains(sb, "public void setWhen(Date zimbraWhen)");
        assertContains(sb, "LdapDateUtil.toGeneralizedTime(zimbraWhen)");
    }

    @Test
    public void generateSetterEnumSetEmitsToStringPutAndValidValuesDoc() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraColor", AttributeType.TYPE_ENUM,
                AttributeCardinality.single, null, "red,green", null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);

        // Assert
        assertContains(sb, "public void setColor(ZAttrProvisioning.Color zimbraColor)");
        assertContains(sb, "attrs.put(Provisioning.A_zimbraColor, zimbraColor.toString());");
        assertContains(sb, "<p>Valid values:");
    }

    @Test
    public void generateSetterMapVariantUnsetEmitsMapOnlySignature() throws Exception {
        // Arrange — noMap=false + unset => the Map-only (no value) overload
        AttributeInfo ai = info("zimbraFoo", AttributeType.TYPE_STRING,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.unset, false);

        // Assert
        assertContains(sb, "Map<String,Object> unsetFoo(Map<String,Object> attrs)");
        assertContains(sb, "attrs.put(Provisioning.A_zimbraFoo, \"\");");
        assertContains(sb, "return attrs;");
    }

    @Test
    public void generateSetterWithSinceEmitsSinceJavadoc() throws Exception {
        // Arrange — non-null since list drives the "@since ZCS" javadoc branch
        AttributeInfo ai = new AttributeInfo("zimbraSinceAttr", 1, null, 0, null,
                AttributeType.TYPE_STRING, null, "", true, null, null, AttributeCardinality.single,
                Sets.newHashSet(AttributeClass.account), null, null, null, null, null, null, null,
                "Test Attribute", null, sinceVersions(), null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);

        // Assert
        assertContains(sb, "@since ZCS");
    }

    // ---- CLOptions defaults --------------------------------------------------------------

    @Test
    public void clOptionsBuildVersionUnsetReturnsUnknown() {
        // Arrange — ensure the property is not set
        String saved = System.getProperty("zimbra.version");
        System.clearProperty("zimbra.version");
        try {
            // Act / Assert — default branch
            assertEquals("unknown", CLOptions.buildVersion());
        } finally {
            if (saved != null) {
                System.setProperty("zimbra.version", saved);
            }
        }
    }

    @Test
    public void clOptionsBuildVersionSetReturnsPropertyValue() {
        // Arrange
        String saved = System.getProperty("zimbra.version");
        System.setProperty("zimbra.version", "10.1.99");
        try {
            // Act / Assert — value branch
            assertEquals("10.1.99", CLOptions.buildVersion());
        } finally {
            if (saved == null) {
                System.clearProperty("zimbra.version");
            } else {
                System.setProperty("zimbra.version", saved);
            }
        }
    }

    @Test
    public void clOptionsGetBaseDnUnsetReturnsDefault() {
        // Arrange
        String saved = System.getProperty("acct.basedn");
        System.clearProperty("acct.basedn");
        try {
            // Act / Assert
            assertEquals("cn=zimbra", CLOptions.getBaseDn("acct"));
        } finally {
            if (saved != null) {
                System.setProperty("acct.basedn", saved);
            }
        }
    }

    @Test
    public void clOptionsGetEntryNameAndIdUnsetReturnDefaults() {
        // Arrange
        String savedName = System.getProperty("globalconfig.name");
        String savedId = System.getProperty("globalconfig.id");
        System.clearProperty("globalconfig.name");
        System.clearProperty("globalconfig.id");
        try {
            // Act / Assert — both delegate to the same defaulting helper
            assertEquals("config", CLOptions.getEntryName("globalconfig", "config"));
            assertNull(CLOptions.getEntryId("globalconfig", null));
        } finally {
            if (savedName != null) {
                System.setProperty("globalconfig.name", savedName);
            }
            if (savedId != null) {
                System.setProperty("globalconfig.id", savedId);
            }
        }
    }

    // ---- mutation-killing assertions ----------------------------------------------------

    @Test
    public void generateGetterDurationWithDefaultEmitsConvertedMillisAndDocValue() throws Exception {
        // Arrange — a configured duration default ("1h") drives the defaultValue != null branch (L1116).
        AttributeInfo ai = info("zimbraTtl", AttributeType.TYPE_DURATION,
                AttributeCardinality.single, null, "", list("1h"));
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);

        // Assert — "1h" => 3600000 ms (NOT the -1 fallback the negated branch would emit),
        // and the human-readable "(1h)" appears in the javadoc only when a default is present.
        assertContains(sb, "getTimeInterval(Provisioning.A_zimbraTtl, 3600000L, true)");
        assertContains(sb, "(1h)");
        assertNotContains(sb, "getTimeInterval(Provisioning.A_zimbraTtl, -1L, true)");
    }

    @Test
    public void generateGetterEmitsDescriptionAndOmitsEphemeralAndSinceDocs() throws Exception {
        // Arrange — non-ephemeral attr with a description and NO since list.
        AttributeInfo ai = info("zimbraFoo", AttributeType.TYPE_STRING,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);

        // Assert
        // L1174: description present => the description text is emitted.
        assertContains(sb, "Test Attribute");
        // L1188: NOT ephemeral => the ephemeral javadoc must be absent.
        assertNotContains(sb, "Ephemeral attribute - requests routed to EphemeralStore");
        // L1196: since is null => no @since javadoc.
        assertNotContains(sb, "@since ZCS");
    }

    @Test
    public void generateGetterWithSinceEmitsSinceVersionString() throws Exception {
        // Arrange — non-null since list drives the @since branch (L1196) and versionListAsString.
        AttributeInfo ai = infoFull("zimbraSinceGetter", AttributeType.TYPE_STRING,
                AttributeCardinality.single, "Test Attribute", sinceVersions());
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);

        // Assert — the exact version string is rendered, not just the literal "@since ZCS".
        assertContains(sb, "@since ZCS 8.0.0");
    }

    @Test
    public void generateSetterEmitsDescriptionAndOmitsEphemeralDoc() throws Exception {
        // Arrange — non-ephemeral attr with a description.
        AttributeInfo ai = info("zimbraFoo", AttributeType.TYPE_STRING,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act — noMap=true.
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);

        // Assert
        // L1297: description present => emitted.
        assertContains(sb, "Test Attribute");
        // L1309: NOT ephemeral => no ephemeral paramDoc.
        assertNotContains(sb, "Ephemeral attribute - requests routed to EphemeralStore");
    }

    @Test
    public void generateSetterNoMapTrueEmitsThrowsDocNotMapDoc() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraFoo", AttributeType.TYPE_STRING,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act — noMap=true takes the throws branch (L1359 false side).
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);

        // Assert — the throws javadoc is emitted and the map javadoc is NOT.
        assertContains(sb, "@throws com.zimbra.common.service.ServiceException if error during update");
        assertNotContains(sb, "@param attrs existing map to populate, or null to create a new map");
        assertNotContains(sb, "@return populated map to pass into Provisioning.modifyAttrs");
    }

    @Test
    public void generateSetterNoMapFalseEmitsMapDocNotThrowsDoc() throws Exception {
        // Arrange
        AttributeInfo ai = info("zimbraFoo", AttributeType.TYPE_STRING,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act — noMap=false takes the map branch (L1359 true side).
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, false);

        // Assert — the map javadoc is emitted and the throws javadoc is NOT.
        assertContains(sb, "@param attrs existing map to populate, or null to create a new map");
        assertContains(sb, "@return populated map to pass into Provisioning.modifyAttrs");
        assertNotContains(sb, "@throws com.zimbra.common.service.ServiceException if error during update");
    }

    @Test
    public void generateSetterSetWithParamDocEmitsParamNewValue() throws Exception {
        // Arrange — the "set" case produces a non-empty paramDoc that L1358 appends.
        AttributeInfo ai = info("zimbraFoo", AttributeType.TYPE_STRING,
                AttributeCardinality.single, null);
        StringBuilder sb = new StringBuilder();

        // Act
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);

        // Assert — L1358: paramDoc content ("@param ... new value") is present in the output.
        assertContains(sb, "@param zimbraFoo new value");
    }

    @Test
    public void versionListAsStringNullOrEmptyReturnsEmptyString() throws Exception {
        // L1210: the null/empty guard returns "".
        assertEquals("", versionListAsString(null));
        assertEquals("", versionListAsString(new ArrayList<com.zimbra.common.util.Version>()));
    }

    @Test
    public void versionListAsStringSingleVersionReturnsThatVersionToString() throws Exception {
        // L1212 (size==1 branch) + L1213 (must return the actual version string, not "").
        List<com.zimbra.common.util.Version> versions =
                new ArrayList<com.zimbra.common.util.Version>();
        versions.add(new com.zimbra.common.util.Version("8.7.0"));

        String result = versionListAsString(versions);

        assertEquals("8.7.0", result);
    }

    @Test
    public void versionListAsStringMultipleVersionsJoinsWithCommaNoTrailing() throws Exception {
        // L1212 false side => the else branch joins versions with commas and trims the last one.
        List<com.zimbra.common.util.Version> versions =
                new ArrayList<com.zimbra.common.util.Version>();
        versions.add(new com.zimbra.common.util.Version("8.0.0"));
        versions.add(new com.zimbra.common.util.Version("9.0.0"));

        String result = versionListAsString(versions);

        assertEquals("8.0.0,9.0.0", result);
    }

    /* Builds a single-element since-version list. */
    private static List<com.zimbra.common.util.Version> sinceVersions() throws Exception {
        List<com.zimbra.common.util.Version> versions = new ArrayList<com.zimbra.common.util.Version>();
        versions.add(new com.zimbra.common.util.Version("8.0.0"));
        return versions;
    }
}
