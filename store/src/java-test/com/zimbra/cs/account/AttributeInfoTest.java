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
import com.zimbra.common.util.Version;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link AttributeInfo} — value validation across attribute types,
 * range/duration handling, enum metadata, and flag/type accessors. Uses the in-memory
 * provisioning harness because TYPE_ID validation calls Provisioning.getInstance().idIsUUID().
 */
public class AttributeInfoTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
    }

    private AttributeInfo build(AttributeType type, String value, String min, String max) {
        return new AttributeInfo("testAttr", 1, null, 0, null, type, null, value, false,
                min, max, AttributeCardinality.single, null, null, new HashSet<AttributeFlag>(),
                null, null, null, null, null, "test attribute", null, null, null);
    }

    @Test
    public void checkValueBooleanValidValuePasses() throws Exception {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_BOOLEAN, "", null, null);

        // Act / Assert — no exception means valid
        ai.checkValue("TRUE", false, new HashMap());
        ai.checkValue("FALSE", false, new HashMap());
    }

    @Test
    public void checkValueBooleanInvalidValueThrowsInvalidAttrValue() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_BOOLEAN, "", null, null);

        // Act / Assert
        try {
            ai.checkValue("yes", false, new HashMap());
            fail("expected exception for non-boolean");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.INVALID_ATTR_VALUE, e.getCode());
            assertTrue(e.getMessage().contains("TRUE or FALSE"));
        }
    }

    @Test
    public void checkValueNullValueIsTreatedAsUnsetAndPasses() throws Exception {
        // Arrange — integer attr with a range that the (skipped) check would otherwise enforce
        AttributeInfo ai = build(AttributeType.TYPE_INTEGER, "", "5", "10");

        // Act / Assert — null means delete/unset, returns early
        ai.checkValue((String) null, false, new HashMap());
        ai.checkValue("", false, new HashMap());
    }

    @Test
    public void checkValueIntegerBelowMinThrowsInvalidAttrValue() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_INTEGER, "", "5", "10");

        // Act / Assert
        try {
            ai.checkValue("3", false, new HashMap());
            fail("expected min violation");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.INVALID_ATTR_VALUE, e.getCode());
            assertTrue(e.getMessage().contains("smaller than minimum"));
        }
    }

    @Test
    public void checkValueIntegerAboveMaxThrowsInvalidAttrValue() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_INTEGER, "", "5", "10");

        // Act / Assert
        try {
            ai.checkValue("99", false, new HashMap());
            fail("expected max violation");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("larger than max"));
        }
    }

    @Test
    public void checkValueIntegerWithinRangePasses() throws Exception {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_INTEGER, "", "5", "10");

        // Act / Assert
        ai.checkValue("7", false, new HashMap());
        assertEquals(5, ai.getMin());
        assertEquals(10, ai.getMax());
    }

    @Test
    public void checkValueIntegerNotANumberThrowsInvalidAttrValue() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_INTEGER, "", null, null);

        // Act / Assert
        try {
            ai.checkValue("abc", false, new HashMap());
            fail("expected NumberFormat-derived failure");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("must be a valid integer"));
        }
    }

    @Test
    public void checkValuePortOutOfRangeThrowsInvalidAttrValue() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_PORT, "", null, null);

        // Act / Assert valid
        try {
            ai.checkValue("8080", false, new HashMap());
        } catch (ServiceException e) {
            fail("8080 should be a valid port");
        }
        // Act / Assert invalid
        try {
            ai.checkValue("70000", false, new HashMap());
            fail("expected port range failure");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("valid port"));
        }
    }

    @Test
    public void checkValueEnumNotInSetThrowsInvalidAttrValue() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_ENUM, "red,green,blue", null, null);

        // Act / Assert valid
        try {
            ai.checkValue("green", false, new HashMap());
        } catch (ServiceException e) {
            fail("green is in the enum set");
        }
        // Act / Assert invalid
        try {
            ai.checkValue("purple", false, new HashMap());
            fail("expected enum membership failure");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("must be one of"));
        }
    }

    @Test
    public void getEnumValueMaxLengthEnumTypeReturnsLongestMemberLength() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_ENUM, "a,bbbb,cc", null, null);

        // Act / Assert
        assertEquals(4, ai.getEnumValueMaxLength());
        Set<String> enums = ai.getEnumSet();
        assertEquals(3, enums.size());
        assertTrue(enums.contains("bbbb"));
    }

    @Test
    public void checkValueDurationBadFormatThrowsInvalidAttrValue() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_DURATION, "", null, null);

        // Act / Assert valid
        try {
            ai.checkValue("30m", false, new HashMap());
        } catch (ServiceException e) {
            fail("30m is a valid duration");
        }
        // Act / Assert invalid
        try {
            ai.checkValue("notaduration", false, new HashMap());
            fail("expected duration format failure");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.INVALID_ATTR_VALUE, e.getCode());
        }
    }

    @Test
    public void checkValueGentimeInvalidThrowsInvalidAttrValue() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_GENTIME, "", null, null);

        // Act / Assert valid
        try {
            ai.checkValue("20140101000000Z", false, new HashMap());
        } catch (ServiceException e) {
            fail("well-formed gentime should pass");
        }
        // Act / Assert invalid
        try {
            ai.checkValue("2014-01-01", false, new HashMap());
            fail("expected gentime format failure");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("generalized time"));
        }
    }

    @Test
    public void checkValueEmailMissingDomainThrowsInvalidAttrValue() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_EMAIL, "", null, null);

        // Act / Assert
        try {
            ai.checkValue("nodomain", false, new HashMap());
            fail("expected missing-domain failure");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.INVALID_ATTR_VALUE, e.getCode());
        }
    }

    @Test
    public void checkValueRegexNonMatchingThrowsInvalidAttrValue() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_REGEX, "^[0-9]+$", null, null);

        // Act / Assert valid
        try {
            ai.checkValue("12345", false, new HashMap());
        } catch (ServiceException e) {
            fail("digits should match the regex");
        }
        // Act / Assert invalid
        try {
            ai.checkValue("abc", false, new HashMap());
            fail("expected regex mismatch failure");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("must match the regex"));
        }
    }

    @Test
    public void checkValueImmutableWithCheckImmutableThrowsInvalidRequest() {
        // Arrange — immutable attribute
        AttributeInfo ai = new AttributeInfo("immutableAttr", 1, null, 0, null,
                AttributeType.TYPE_STRING, null, "", true, null, null,
                AttributeCardinality.single, null, null, new HashSet<AttributeFlag>(),
                null, null, null, null, null, "immutable", null, null, null);
        assertTrue(ai.isImmutable());

        // Act / Assert
        try {
            ai.checkValue("anything", true, new HashMap());
            fail("expected immutable failure");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains("immutable"));
        }
    }

    @Test
    public void checkValueStringArrayValidatesEachElement() {
        // Arrange — integer-typed attribute, supply a String[] with one bad element
        AttributeInfo ai = build(AttributeType.TYPE_INTEGER, "", "0", "100");

        // Act / Assert
        try {
            Object value = new String[] {"5", "200" };
            ai.checkValue(value, false, new HashMap());
            fail("expected the out-of-range element to fail");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("larger than max"));
        }
    }

    @Test
    public void validEmailAddressNoAtSignThrowsInvalidAttrValue() {
        // Act / Assert
        try {
            AttributeInfo.validEmailAddress("plainstring", false);
            fail("expected missing-domain failure");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("does not include domain"));
        }
    }

    @Test
    public void accessorsMetadataFieldsReturnConstructorValues() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_STRING, "", null, null);

        // Act / Assert
        assertEquals(1, ai.getId());
        assertEquals(AttributeType.TYPE_STRING, ai.getType());
        assertEquals("test attribute", ai.getDescription());
        assertEquals(AttributeCardinality.single, ai.getCardinality());
        assertFalse(ai.isImmutable());
        assertFalse(ai.isDeprecated());
        assertTrue("string types are case-insensitive", ai.isCaseInsensitive());
        assertFalse(ai.isEphemeral());
    }

    @Test
    public void getDescriptionDurationTypeAppendsDurationDoc() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_DURATION, "", null, null);

        // Act
        String desc = ai.getDescription();

        // Assert
        assertTrue(desc.startsWith("test attribute"));
        assertTrue(desc.contains(AttributeInfo.DURATION_PATTERN_DOC));
    }

    @Test
    public void checkValueLongBelowMinThrowsInvalidAttrValue() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_LONG, "", "100", "1000");

        // Act / Assert valid in range
        try {
            ai.checkValue("500", false, new HashMap());
        } catch (ServiceException e) {
            fail("500 is within the long range");
        }
        // Act / Assert below min
        try {
            ai.checkValue("50", false, new HashMap());
            fail("expected long min violation");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.INVALID_ATTR_VALUE, e.getCode());
            assertTrue(e.getMessage().contains("smaller than minimum"));
        }
    }

    @Test
    public void checkValueLongAboveMaxThrowsInvalidAttrValue() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_LONG, "", "0", "10");

        // Act / Assert
        try {
            ai.checkValue("99", false, new HashMap());
            fail("expected long max violation");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("larger than max"));
        }
    }

    @Test
    public void checkValueLongNotANumberThrowsInvalidAttrValue() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_LONG, "", null, null);

        // Act / Assert
        try {
            ai.checkValue("xyz", false, new HashMap());
            fail("expected long NumberFormat failure");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("must be a valid long"));
        }
        assertEquals(Long.MIN_VALUE, ai.getMin());
        assertEquals(Long.MAX_VALUE, ai.getMax());
    }

    @Test
    public void checkValuePortNotANumberThrowsInvalidAttrValue() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_PORT, "", null, null);

        // Act / Assert
        try {
            ai.checkValue("notaport", false, new HashMap());
            fail("expected port NumberFormat failure");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("valid port"));
        }
    }

    @Test
    public void checkValueStringExceedsMaxThrowsInvalidAttrValue() {
        // Arrange — string attribute with max length 3
        AttributeInfo ai = build(AttributeType.TYPE_STRING, "", null, "3");

        // Act / Assert valid (within length)
        try {
            ai.checkValue("abc", false, new HashMap());
        } catch (ServiceException e) {
            fail("3-char string is within max length 3");
        }
        // Act / Assert too long
        try {
            ai.checkValue("abcdef", false, new HashMap());
            fail("expected string length violation");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("larger than max allowed"));
        }
    }

    @Test
    public void checkValueBinaryExceedsMaxThrowsInvalidAttrValue() {
        // Arrange — TYPE_BINARY with a tiny max so a base64 blob fails on length
        AttributeInfo ai = build(AttributeType.TYPE_BINARY, "", null, "2");

        // Act / Assert — base64 of more than 2 bytes
        try {
            ai.checkValue("aGVsbG8=", false, new HashMap()); // "hello" -> 5 bytes
            fail("expected binary length violation");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.INVALID_ATTR_VALUE, e.getCode());
            assertTrue(e.getMessage().contains("larger than max allowed"));
        }
    }

    @Test
    public void checkValueEmailpValidPersonalPasses() throws Exception {
        // Arrange — personal email accepted for TYPE_EMAILP
        AttributeInfo ai = build(AttributeType.TYPE_EMAILP, "", null, null);

        // Act / Assert — a plain address with a domain validates
        ai.checkValue("user@example.com", false, new HashMap());
    }

    @Test
    public void checkValueCsEmailpMultipleAddressesValidatesEach() {
        // Arrange — comma-separated personal emails
        AttributeInfo ai = build(AttributeType.TYPE_CS_EMAILP, "", null, null);

        // Act / Assert valid list
        try {
            ai.checkValue("a@example.com,b@example.com", false, new HashMap());
        } catch (ServiceException e) {
            fail("comma-separated valid emails should pass");
        }
        // Act / Assert one element missing a domain
        try {
            ai.checkValue("a@example.com,nodomain", false, new HashMap());
            fail("expected failure for element missing domain");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.INVALID_ATTR_VALUE, e.getCode());
        }
    }

    @Test
    public void checkValueIdValidUuidPasses() throws Exception {
        // Arrange — TYPE_ID validates UUID format because the harness idIsUUID() is true
        AttributeInfo ai = build(AttributeType.TYPE_ID, "", null, null);

        // Act / Assert — a well-formed UUID passes
        ai.checkValue("8cf3db5d-cfd7-11d9-884f-e7b38f15492d", false, new HashMap());
    }

    @Test
    public void checkValueIdMalformedThrowsInvalidAttrValue() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_ID, "", null, null);

        // Act / Assert
        try {
            ai.checkValue("not-a-uuid", false, new HashMap());
            fail("expected invalid id failure");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.INVALID_ATTR_VALUE, e.getCode());
            assertTrue(e.getMessage().contains("valid id"));
        }
    }

    @Test
    public void constructorIntegerInvalidMinMaxDefaultsToFullIntRange() {
        // Arrange / Act — non-numeric min/max are warned and ignored, leaving full int range
        AttributeInfo ai = build(AttributeType.TYPE_INTEGER, "", "notanumber", "alsonotanumber");

        // Assert — defaults preserved; any int value validates
        assertEquals(Integer.MIN_VALUE, ai.getMin());
        assertEquals(Integer.MAX_VALUE, ai.getMax());
        try {
            ai.checkValue("42", false, new HashMap());
        } catch (ServiceException e) {
            fail("with defaulted range any int should validate");
        }
    }

    @Test
    public void constructorLongInvalidMinMaxDefaultsToFullLongRange() {
        // Arrange / Act
        AttributeInfo ai = build(AttributeType.TYPE_LONG, "", "bad", "worse");

        // Assert — defaults preserved
        assertEquals(Long.MIN_VALUE, ai.getMin());
        assertEquals(Long.MAX_VALUE, ai.getMax());
    }

    @Test
    public void constructorDurationWithMinMaxEnforcesDurationBounds() {
        // Arrange — duration bounded between 10s and 1h
        AttributeInfo ai = build(AttributeType.TYPE_DURATION, "", "10s", "1h");

        // Act / Assert in-range
        try {
            ai.checkValue("30s", false, new HashMap());
        } catch (ServiceException e) {
            fail("30s is within [10s, 1h]");
        }
        // Act / Assert below min
        try {
            ai.checkValue("5s", false, new HashMap());
            fail("expected duration below-min failure");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("shorter than minimum"));
        }
        // Act / Assert above max
        try {
            ai.checkValue("2h", false, new HashMap());
            fail("expected duration above-max failure");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("longer than max"));
        }
    }

    @Test
    public void constructorDurationInvalidMinDefaultsToZero() {
        // Arrange / Act — an unparseable duration min defaults mMin to 0
        AttributeInfo ai = build(AttributeType.TYPE_DURATION, "", "garbage", null);

        // Assert
        assertEquals(0, ai.getMin());
        assertEquals(Long.MAX_VALUE, ai.getMax());
    }

    @Test
    public void checkValueDeprecatedAttributeAllowedByDefaultDebugConfig() throws Exception {
        // Arrange — a deprecated string attribute
        AttributeInfo ai = new AttributeInfo("deprAttr", 1, null, 0, null,
                AttributeType.TYPE_STRING, null, "", false, null, null,
                AttributeCardinality.single, null, null, new HashSet<AttributeFlag>(),
                null, null, null, null, null, "deprecated", null, null, makeVersion("1.0"));

        // Assert — the attribute is flagged deprecated and carries its deprecated-since version
        assertTrue(ai.isDeprecated());
        assertEquals(0, ai.getDeprecatedSince().compareTo(makeVersion("1.0")));

        // Act / Assert — checkValue gates the "modifying deprecated attribute" failure on
        // DebugConfig.allowModifyingDeprecatedAttributes, which defaults to true (bug 57279).
        // Under the default config the deprecated branch is suppressed and the value validates.
        ai.checkValue("value", false, new HashMap());
    }

    @Test
    public void validEmailAddressValidAddressNonPersonalPasses() throws Exception {
        // Act / Assert — a plain address validates with no personal part
        AttributeInfo.validEmailAddress("user@example.com", false);
    }

    @Test
    public void validEmailAddressPersonalPartWhenNotAllowedThrowsInvalidAttrValue() {
        // Act / Assert — "Name <addr>" form is rejected when personal is not permitted
        try {
            AttributeInfo.validEmailAddress("Joe User <joe@example.com>", false);
            fail("expected invalid email address failure for personal part");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.INVALID_ATTR_VALUE, e.getCode());
            assertTrue(e.getMessage().contains("invalid email address"));
        }
    }

    @Test
    public void accessorsPackageMetadataReturnConstructorValues() {
        // Arrange
        List<String> gcv = new ArrayList<String>();
        gcv.add("gcv");
        List<String> gcvu = new ArrayList<String>();
        gcvu.add("gcvu");
        List<String> dcosExt = new ArrayList<String>();
        dcosExt.add("ext");
        List<String> dcosUp = new ArrayList<String>();
        dcosUp.add("up");
        List<AttributeServerType> restart = new ArrayList<AttributeServerType>();
        restart.add(AttributeServerType.mailbox);
        Version since = makeVersion("8.5");
        List<Version> sinceList = new ArrayList<Version>();
        sinceList.add(since);
        AttributeInfo ai = new AttributeInfo("metaAttr", 7, "parent-oid", 99,
                null, AttributeType.TYPE_STRING, AttributeOrder.integerOrderingMatch, "init",
                false, null, null, AttributeCardinality.multi, null, null,
                new HashSet<AttributeFlag>(), gcv, null, dcosExt, gcvu, dcosUp,
                "meta", restart, sinceList, null);

        // Act / Assert — exercises the previously-uncovered list/metadata getters
        assertEquals(7, ai.getId());
        assertEquals("parent-oid", ai.getParentOid());
        assertEquals(99, ai.getGroupId());
        assertEquals(AttributeOrder.integerOrderingMatch, ai.getOrder());
        assertEquals("init", ai.getValue());
        assertEquals(AttributeCardinality.multi, ai.getCardinality());
        assertEquals(gcv, ai.getGlobalConfigValues());
        assertEquals(gcvu, ai.getGlobalConfigValuesUpgrade());
        assertEquals(dcosExt, ai.getDefaultExternalCosValues());
        assertEquals(dcosUp, ai.getDefaultCosValuesUpgrade());
        assertEquals(restart, ai.getRequiresRestart());
        assertEquals(sinceList, ai.getSince());
        assertFalse(ai.isDeprecated());
    }

    @Test
    public void getCallbackConstructedWithoutCallbackReturnsNull() {
        // Arrange
        AttributeInfo ai = build(AttributeType.TYPE_STRING, "", null, null);

        // Act / Assert
        assertEquals(null, ai.getCallback());
        assertEquals("testAttr", ai.getName());
    }

    private static Version makeVersion(String s) {
        try {
            return new Version(s);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Attaches an in-memory log4j2 appender to the "zimbra.misc" logger (the one used by
     * AttributeInfo's constructor warnings) and returns the live list of captured messages.
     */
    private static List<String> captureMiscLog() {
        final List<String> messages = new CopyOnWriteArrayList<String>();
        AbstractAppender appender = new AbstractAppender("ai-capture-" + System.nanoTime(),
                null, null, true, null) {
            @Override
            public void append(LogEvent event) {
                messages.add(event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        LoggerConfig cfg = ctx.getConfiguration().getLoggerConfig("zimbra.misc");
        // The test runtime config leaves the effective level at ERROR, which would filter out the
        // WARN events AttributeInfo emits for invalid min/max. Lower it to WARN and attach the
        // appender at WARN so the constructor warnings actually reach the capture list.
        cfg.setLevel(Level.WARN);
        cfg.addAppender(appender, Level.WARN, null);
        ctx.updateLoggers();
        return messages;
    }

    private AttributeInfo buildWithCallback(AttributeCallback cb) {
        return new AttributeInfo("cbAttr", 1, null, 0, cb, AttributeType.TYPE_STRING, null, "",
                false, null, null, AttributeCardinality.single, null, null,
                new HashSet<AttributeFlag>(), null, null, null, null, null, "cb", null, null, null);
    }

    /** A trivial concrete callback so getCallback() has a non-null reference to return. */
    private static class StubCallback extends AttributeCallback {
        @Override
        public void preModify(com.zimbra.cs.account.callback.CallbackContext context, String attrName,
                Object attrValue, Map attrsToModify, com.zimbra.cs.account.Entry entry) {
        }

        @Override
        public void postModify(com.zimbra.cs.account.callback.CallbackContext context, String attrName,
                com.zimbra.cs.account.Entry entry) {
        }
    }

    /**
     * getCallback() must return the EXACT callback passed to the constructor (kills L453
     * NullReturnVals — a mutant returning null would not equal the stub).
     */
    @Test
    public void getCallbackConstructedWithCallbackReturnsSameInstance() {
        StubCallback cb = new StubCallback();
        AttributeInfo ai = buildWithCallback(cb);
        assertSame("getCallback must return the constructed callback", cb, ai.getCallback());
    }

    /**
     * getRequiredIn() must return the non-empty constructor set (kills L516 EmptyObjectReturnVals,
     * which would return an empty set). We assert membership + size, not just non-null.
     */
    @Test
    public void getRequiredInNonEmptySetReturnsThatSet() {
        Set<AttributeClass> required = new HashSet<AttributeClass>();
        required.add(AttributeClass.account);
        required.add(AttributeClass.cos);
        AttributeInfo ai = new AttributeInfo("reqAttr", 1, null, 0, null,
                AttributeType.TYPE_STRING, null, "", false, null, null,
                AttributeCardinality.single, required, null, new HashSet<AttributeFlag>(),
                null, null, null, null, null, "req", null, null, null);

        Set<AttributeClass> got = ai.getRequiredIn();
        assertNotNull(got);
        assertEquals(2, got.size());
        assertTrue(got.contains(AttributeClass.account));
        assertTrue(got.contains(AttributeClass.cos));
    }

    /**
     * isCaseInsensitive() must be FALSE for a non-string type (kills L575 BooleanTrueReturnVals,
     * which would always return true). TYPE_INTEGER is neither STRING nor ASTRING.
     */
    @Test
    public void isCaseInsensitiveNonStringTypeReturnsFalse() {
        AttributeInfo ai = build(AttributeType.TYPE_INTEGER, "", null, null);
        assertFalse("integer attributes are not case-insensitive", ai.isCaseInsensitive());
    }

    /**
     * isCaseInsensitive() must be TRUE for ASTRING as well as STRING (covers both arms of the
     * boolean expression so a partial-removal mutation is caught).
     */
    @Test
    public void isCaseInsensitiveAstringTypeReturnsTrue() {
        AttributeInfo ai = build(AttributeType.TYPE_ASTRING, "", null, null);
        assertTrue("astring attributes are case-insensitive", ai.isCaseInsensitive());
    }

    /**
     * since-list of more than one element is sorted ascending in the constructor (kills L181
     * ConditionalsBoundary on size>1 and L183 VoidMethodCall removing Collections.sort).
     * Supplied out of order; getSince() must come back sorted.
     */
    @Test
    public void constructorMultipleSinceVersionsSortsAscending() {
        List<Version> since = new ArrayList<Version>();
        since.add(makeVersion("8.5.0"));
        since.add(makeVersion("8.0.0"));  // out of order on purpose
        AttributeInfo ai = new AttributeInfo("sinceAttr", 1, null, 0, null,
                AttributeType.TYPE_STRING, null, "", false, null, null,
                AttributeCardinality.single, null, null, new HashSet<AttributeFlag>(),
                null, null, null, null, null, "since", null, since, null);

        List<Version> got = ai.getSince();
        assertEquals(2, got.size());
        assertEquals("first element after sort must be the lower version",
                0, got.get(0).compareTo(makeVersion("8.0.0")));
        assertEquals("second element after sort must be the higher version",
                0, got.get(1).compareTo(makeVersion("8.5.0")));
    }

    /**
     * A single-element since list must be left untouched (size > 1 is false; boundary check).
     */
    @Test
    public void constructorSingleSinceVersionPreserved() {
        List<Version> since = new ArrayList<Version>();
        since.add(makeVersion("9.0.0"));
        AttributeInfo ai = new AttributeInfo("sinceAttr1", 1, null, 0, null,
                AttributeType.TYPE_STRING, null, "", false, null, null,
                AttributeCardinality.single, null, null, new HashSet<AttributeFlag>(),
                null, null, null, null, null, "since", null, since, null);
        assertEquals(1, ai.getSince().size());
        assertEquals(0, ai.getSince().get(0).compareTo(makeVersion("9.0.0")));
    }

    /**
     * TYPE_INTEGER boundary: a value EXACTLY at min and EXACTLY at max passes, min-1 and max+1 fail.
     * Kills L384 (v < mMin) and L387 (v > mMax) ConditionalsBoundary (&lt; vs &lt;=).
     */
    @Test
    public void checkValueIntegerExactBoundaries() throws Exception {
        AttributeInfo ai = build(AttributeType.TYPE_INTEGER, "", "5", "10");
        // exactly min and exactly max must pass
        ai.checkValue("5", false, new HashMap());
        ai.checkValue("10", false, new HashMap());
        // min - 1 fails
        try {
            ai.checkValue("4", false, new HashMap());
            fail("4 is below min 5");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("smaller than minimum"));
        }
        // max + 1 fails
        try {
            ai.checkValue("11", false, new HashMap());
            fail("11 is above max 10");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("larger than max"));
        }
    }

    /**
     * TYPE_LONG boundary: exact min/max pass, just-outside fails. Kills L397 (v &lt; mMin) and
     * L400 (v &gt; mMax) ConditionalsBoundary.
     */
    @Test
    public void checkValueLongExactBoundaries() throws Exception {
        AttributeInfo ai = build(AttributeType.TYPE_LONG, "", "100", "200");
        ai.checkValue("100", false, new HashMap());
        ai.checkValue("200", false, new HashMap());
        try {
            ai.checkValue("99", false, new HashMap());
            fail("99 below min 100");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("smaller than minimum"));
        }
        try {
            ai.checkValue("201", false, new HashMap());
            fail("201 above max 200");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("larger than max"));
        }
    }

    /**
     * TYPE_PORT boundary: 0 and 65535 are valid; 65536 and -1 are not. Kills L410
     * ConditionalsBoundary (v &lt;= 65535) and the &gt;= 0 lower bound.
     */
    @Test
    public void checkValuePortExactBoundaries() throws Exception {
        AttributeInfo ai = build(AttributeType.TYPE_PORT, "", null, null);
        ai.checkValue("0", false, new HashMap());
        ai.checkValue("65535", false, new HashMap());
        try {
            ai.checkValue("65536", false, new HashMap());
            fail("65536 is above the max port");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("valid port"));
        }
        try {
            ai.checkValue("-1", false, new HashMap());
            fail("-1 is below the min port");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("valid port"));
        }
    }

    /**
     * String length boundary: a value exactly at max length passes; one over fails. Kills L421/L324
     * style ConditionalsBoundary (length &gt; mMax). Uses TYPE_STRING with max 3.
     */
    @Test
    public void checkValueStringExactMaxLength() throws Exception {
        AttributeInfo ai = build(AttributeType.TYPE_STRING, "", null, "3");
        // exactly 3 passes
        ai.checkValue("abc", false, new HashMap());
        // 4 fails
        try {
            ai.checkValue("abcd", false, new HashMap());
            fail("4-char string exceeds max length 3");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("larger than max allowed"));
        }
    }

    /**
     * Binary length boundary: a base64 blob whose decoded length equals max passes, max+1 fails.
     * Kills L324 ConditionalsBoundary (binary.length &gt; mMax). 2 bytes -> base64 "AAA=".
     */
    @Test
    public void checkValueBinaryExactMaxLength() throws Exception {
        AttributeInfo ai = build(AttributeType.TYPE_BINARY, "", null, "2");
        // exactly 2 bytes (0x00 0x00) -> "AAA=" passes
        ai.checkValue("AAA=", false, new HashMap());
        // 3 bytes -> "AAAA" exceeds
        try {
            ai.checkValue("AAAA", false, new HashMap());
            fail("3-byte blob exceeds max 2");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("larger than max allowed"));
        }
    }

    /**
     * Email length boundary + validation call. max length 14 ("a@example.com" is 13, "ab@example.com"
     * is 14 -> ok; 15 fails on length). Kills L340 ConditionalsBoundary/Negate and the L343/L444
     * validEmailAddress call path.
     */
    @Test
    public void checkValueEmailExactMaxLengthAndValidation() throws Exception {
        AttributeInfo ai = build(AttributeType.TYPE_EMAIL, "", null, "14");
        // exactly 14 chars and valid -> passes
        ai.checkValue("ab@example.com", false, new HashMap());
        // 15 chars -> length failure
        try {
            ai.checkValue("abc@example.com", false, new HashMap());
            fail("15-char email exceeds max length 14");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("larger than max allowed"));
        }
        // within length but malformed -> validation failure (kills validEmailAddress removal)
        AttributeInfo ai2 = build(AttributeType.TYPE_EMAIL, "", null, null);
        try {
            ai2.checkValue("nodomainhere", false, new HashMap());
            fail("email without @ must fail validation");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("does not include domain"));
        }
    }

    /**
     * TYPE_EMAILP: removing the validEmailAddress call (L349 VoidMethodCall) would let a malformed
     * personal email through. Assert it still throws.
     */
    @Test
    public void checkValueEmailpMalformedThrows() {
        AttributeInfo ai = build(AttributeType.TYPE_EMAILP, "", null, null);
        try {
            ai.checkValue("nodomain", false, new HashMap());
            fail("malformed emailp must fail validation");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.INVALID_ATTR_VALUE, e.getCode());
        }
    }

    /**
     * checkValue(Object,...) with a String argument must recurse into per-value validation
     * (kills L291 VoidMethodCall removing checkValue((String)value,...)). An out-of-range integer
     * passed as an Object/String must still throw.
     */
    @Test
    public void checkValueObjectStringValueIsValidated() {
        AttributeInfo ai = build(AttributeType.TYPE_INTEGER, "", "0", "10");
        Object value = "999"; // a String, taken via the Object overload
        try {
            ai.checkValue(value, false, new HashMap());
            fail("999 (as Object String) exceeds max and must be validated");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("larger than max"));
        }
    }

    /**
     * Duration max parsed to exactly 0 ("0s") must be kept as the effective max (kills L265
     * ConditionalsBoundary mMax &lt; 0 -&gt; &lt;= 0, which would reset mMax to Long.MAX_VALUE and let
     * a positive duration through). A 1s value must then exceed the 0 max.
     */
    @Test
    public void constructorDurationMaxZeroEnforcedAsZero() throws Exception {
        AttributeInfo ai = build(AttributeType.TYPE_DURATION, "", null, "0s");
        assertEquals("duration max of 0s must yield mMax == 0", 0, ai.getMax());
        try {
            ai.checkValue("1s", false, new HashMap());
            fail("1s exceeds the 0 max");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("longer than max"));
        }
    }

    /**
     * Invalid integer min/max in the constructor must emit a warning to zimbra.misc (kills L198/L207
     * VoidMethodCall removing ZimbraLog.misc.warn) AND leave the range at the int defaults.
     */
    @Test
    public void constructorIntegerInvalidMinMaxLogsWarningAndKeepsDefaults() {
        List<String> log = captureMiscLog();
        AttributeInfo ai = build(AttributeType.TYPE_INTEGER, "", "notanint", "alsobad");
        assertEquals(Integer.MIN_VALUE, ai.getMin());
        assertEquals(Integer.MAX_VALUE, ai.getMax());
        boolean sawMin = false;
        boolean sawMax = false;
        for (String m : log) {
            if (m.contains("notanint") && m.contains("testAttr")) {
                sawMin = true;
            }
            if (m.contains("alsobad") && m.contains("testAttr")) {
                sawMax = true;
            }
        }
        assertTrue("expected a warning for the bad integer min", sawMin);
        assertTrue("expected a warning for the bad integer max", sawMax);
    }

    /**
     * Invalid long min/max in the constructor must emit warnings to zimbra.misc (kills L221/L230
     * VoidMethodCall) and keep the long defaults.
     */
    @Test
    public void constructorLongInvalidMinMaxLogsWarningAndKeepsDefaults() {
        List<String> log = captureMiscLog();
        AttributeInfo ai = build(AttributeType.TYPE_LONG, "", "badlong", "worselong");
        assertEquals(Long.MIN_VALUE, ai.getMin());
        assertEquals(Long.MAX_VALUE, ai.getMax());
        boolean sawMin = false;
        boolean sawMax = false;
        for (String m : log) {
            if (m.contains("badlong")) {
                sawMin = true;
            }
            if (m.contains("worselong")) {
                sawMax = true;
            }
        }
        assertTrue("expected a warning for the bad long min", sawMin);
        assertTrue("expected a warning for the bad long max", sawMax);
    }

    /**
     * Invalid duration min in the constructor must emit a warning (kills L257 VoidMethodCall) and
     * default mMin to 0.
     */
    @Test
    public void constructorDurationInvalidMinLogsWarning() {
        List<String> log = captureMiscLog();
        AttributeInfo ai = build(AttributeType.TYPE_DURATION, "", "-5x", null);
        assertEquals(0, ai.getMin());
        boolean saw = false;
        for (String m : log) {
            if (m.contains("-5x")) {
                saw = true;
            }
        }
        assertTrue("expected a warning for the invalid duration min", saw);
    }

    /**
     * For a non-numeric-range type (STRING), a bad max string is parsed via parseLong(attrName,...)
     * and must default to Long.MAX_VALUE while logging a warning. Kills L140 VoidMethodCall (warn) and
     * L142 EmptyObjectReturnVals (which would return a non-default value instead of the supplied
     * defaultValue). A 20-char string then validates because the max defaulted high.
     */
    @Test
    public void constructorStringBadMaxDefaultsToLongMaxAndLogs() throws Exception {
        List<String> log = captureMiscLog();
        AttributeInfo ai = build(AttributeType.TYPE_STRING, "", null, "notanumber");
        assertEquals("bad string max must default to Long.MAX_VALUE", Long.MAX_VALUE, ai.getMax());
        // a long string validates because max defaulted to Long.MAX_VALUE
        ai.checkValue("this-is-a-fairly-long-value", false, new HashMap());
        boolean saw = false;
        for (String m : log) {
            if (m.contains("notanumber")) {
                saw = true;
            }
        }
        assertTrue("expected a warning for the bad string max", saw);
    }

    /**
     * checkValue(null,...) early-return guard (L294 ConditionalsBoundary / the null/empty check at
     * L312): a too-long value normally fails, but null and "" return early and pass.
     */
    @Test
    public void checkValueNullAndEmptyReturnEarlyEvenWithTightMax() throws Exception {
        AttributeInfo ai = build(AttributeType.TYPE_STRING, "", null, "1");
        ai.checkValue((String) null, false, new HashMap());
        ai.checkValue("", false, new HashMap());
        // sanity: a 2-char value DOES fail, proving the max is actually enforced
        try {
            ai.checkValue("xy", false, new HashMap());
            fail("2-char value exceeds max length 1");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("larger than max allowed"));
        }
    }

    @Test
    public void requiredAndOptionalInClassReflectConstructorSets() {
        // Arrange
        Set<AttributeClass> required = new HashSet<AttributeClass>();
        required.add(AttributeClass.account);
        Set<AttributeClass> optional = new HashSet<AttributeClass>();
        optional.add(AttributeClass.cos);
        AttributeInfo ai = new AttributeInfo("classedAttr", 1, null, 0, null,
                AttributeType.TYPE_STRING, null, "", false, null, null,
                AttributeCardinality.single, required, optional, new HashSet<AttributeFlag>(),
                null, null, null, null, null, "classed", null, null, null);

        // Act / Assert
        assertTrue(ai.requiredInClass(AttributeClass.account));
        assertFalse(ai.requiredInClass(AttributeClass.cos));
        assertTrue(ai.optionalInClass(AttributeClass.cos));
        assertFalse(ai.optionalInClass(AttributeClass.account));
    }
}
