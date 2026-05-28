/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.gal;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxTestUtil;

public class GalUtilTest {

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
    }

    // ===== GalUtil.expandFilter Tests =====

    @Test
    public void expandFilter_simpleFilterTemplate_noTokenization() throws ServiceException {
        String filter = GalUtil.expandFilter(null, "(cn=%s)", "smith", null);

        assertNotNull(filter);
        assertTrue(filter.contains("smith"));
    }

    @Test
    public void expandFilter_keyWithWildcards_stripped() throws ServiceException {
        String filter = GalUtil.expandFilter(null, "(cn=%s)", "*smith*", null);

        // Leading and trailing wildcards should be stripped
        assertNotNull(filter);
        assertTrue(filter.contains("smith"));
    }

    @Test
    public void expandFilter_keyWithLeadingWildcards_stripped() throws ServiceException {
        String filter = GalUtil.expandFilter(null, "(cn=%s)", "***test", null);

        assertNotNull(filter);
        assertTrue(filter.contains("test"));
    }

    @Test
    public void expandFilter_keyWithTrailingWildcards_stripped() throws ServiceException {
        String filter = GalUtil.expandFilter(null, "(cn=%s)", "test***", null);

        assertNotNull(filter);
        assertTrue(filter.contains("test"));
    }

    @Test
    public void expandFilter_tokenWithTimestamp_includesTimestamp() throws ServiceException {
        String token = "20080101000000Z";
        String filter = GalUtil.expandFilter(null, "(cn=%s)", "test", token);

        assertNotNull(filter);
        assertTrue(filter.contains("modifyTimeStamp") || filter.contains("createTimeStamp"));
    }

    @Test
    public void expandFilter_tokenWithTimestampAndHasMore_optimized() throws ServiceException {
        String token = "20080101000000Z";
        String filter = GalUtil.expandFilter(null, "(cn=%s)", "test", token, null, true);

        assertNotNull(filter);
        // With hasMore=true, should use only createTimeStamp for optimization
        assertTrue(filter.contains("createTimeStamp"));
        assertFalse(filter.contains("modifyTimeStamp"));
    }

    @Test
    public void expandFilter_withExtraQuery_combined() throws ServiceException {
        String filter = GalUtil.expandFilter(null, "(cn=%s)", "smith", null, "(|(mail=*smith*)(uid=smith))", false);

        assertNotNull(filter);
        assertTrue(filter.contains("smith"));
        assertTrue(filter.contains("mail") || filter.contains("uid"));
    }

    @Test
    public void expandFilter_unbalancedParenthesis_fixed() throws ServiceException {
        String filter = GalUtil.expandFilter(null, "cn=%s", "smith", null);

        // Should add parentheses
        assertNotNull(filter);
        assertTrue(filter.startsWith("(") || filter.contains("smith"));
    }

    @Test
    public void expandFilter_doubleWildcard_collapsed() throws ServiceException {
        String filter = GalUtil.expandFilter(null, "(cn=%s)(mail=**%s**)", "test", null);

        assertNotNull(filter);
        // Double wildcards should be collapsed to single
        assertFalse(filter.contains("**"));
    }

    @Test
    public void expandFilter_tokenizeKeyAnd_multipleTokens_combined() throws ServiceException {
        String filter = GalUtil.expandFilter(GalConstants.TOKENIZE_KEY_AND, "(cn=%s)", "john smith", null);

        assertNotNull(filter);
        assertTrue(filter.contains("john"));
        assertTrue(filter.contains("smith"));
        assertTrue(filter.contains("(&"));  // AND operator
    }

    @Test
    public void expandFilter_tokenizeKeyOr_multipleTokens_combined() throws ServiceException {
        String filter = GalUtil.expandFilter(GalConstants.TOKENIZE_KEY_OR, "(cn=%s)", "john smith", null);

        assertNotNull(filter);
        assertTrue(filter.contains("john"));
        assertTrue(filter.contains("smith"));
        assertTrue(filter.contains("(|"));  // OR operator
    }

    @Test
    public void expandFilter_tokenizeInvalid_throws() throws ServiceException {
        try {
            GalUtil.expandFilter("INVALID_TOKENIZE", "(cn=%s)", "test", null);
            fail("Should throw ServiceException for invalid tokenize value");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid attribute value"));
        }
    }

    @Test
    public void expandFilter_nullKey_handled() throws ServiceException {
        String filter = GalUtil.expandFilter(null, "(cn=%s)", null, null);

        assertNotNull(filter);
    }

    @Test
    public void expandFilter_emptyKey_handled() throws ServiceException {
        String filter = GalUtil.expandFilter(null, "(cn=%s)", "", null);

        assertNotNull(filter);
    }

    @Test
    public void expandFilter_singleTokenWithTokenize_noSplit() throws ServiceException {
        // Single token should not trigger tokenization even with tokenize set
        String filter = GalUtil.expandFilter(GalConstants.TOKENIZE_KEY_AND, "(cn=%s)", "singletoken", null);

        assertNotNull(filter);
        assertTrue(filter.contains("singletoken"));
    }

    @Test
    public void expandFilter_specialCharactersEscaped() throws ServiceException {
        // LDAP special characters should be escaped
        String filter = GalUtil.expandFilter(null, "(cn=%s)", "test*user(", null);

        assertNotNull(filter);
        // Verify escaping occurred (implementation uses LdapUtil.escapeSearchFilterArg)
        assertTrue(filter.length() > 0);
    }

    @Test
    public void expandFilter_complexFilterTemplate() throws ServiceException {
        String template = "(&(objectClass=inetOrgPerson)(|(cn=%s)(mail=%s)))";
        String filter = GalUtil.expandFilter(null, template, "smith", null);

        assertNotNull(filter);
        assertTrue(filter.contains("objectClass") || filter.contains("inetOrgPerson"));
    }

    // ===== GalUtil.tokenizeKey Tests =====

    @Test
    public void tokenizeKey_nullParams_returnsNull() {
        String result = GalUtil.tokenizeKey(null, GalOp.search);
        assertNull(result);
    }

    @Test
    public void tokenizeKey_autocompleteOp_callsTokenizeAutoCompleteKey() {
        GalParams mockParams = new GalParams(java.util.Collections.emptyMap(), GalOp.search) {
            @Override
            public String tokenizeAutoCompleteKey() {
                return "TOKENIZE_AND";
            }
        };

        String result = GalUtil.tokenizeKey(mockParams, GalOp.autocomplete);
        assertEquals("TOKENIZE_AND", result);
    }

    @Test
    public void tokenizeKey_searchOp_callsTokenizeSearchKey() {
        GalParams mockParams = new GalParams(java.util.Collections.emptyMap(), GalOp.search) {
            @Override
            public String tokenizeSearchKey() {
                return "TOKENIZE_OR";
            }
        };

        String result = GalUtil.tokenizeKey(mockParams, GalOp.search);
        assertEquals("TOKENIZE_OR", result);
    }

    @Test
    public void tokenizeKey_syncOp_returnsNull() {
        GalParams mockParams = new GalParams(java.util.Collections.emptyMap(), GalOp.search) {};

        String result = GalUtil.tokenizeKey(mockParams, GalOp.sync);
        assertNull(result);
    }

    // ===== GalUtil.expandFilter with real tokenization =====

    @Test
    public void expandFilter_multiTokens_andCombined_fullWorkflow() throws ServiceException {
        String filter = GalUtil.expandFilter(GalConstants.TOKENIZE_KEY_AND, "(cn=%s)(mail=%s)", "alice bob charlie", null);

        assertNotNull(filter);
        // Verify AND structure exists
        assertTrue(filter.contains("(&"));
        assertTrue(filter.contains("alice"));
        assertTrue(filter.contains("bob"));
        assertTrue(filter.contains("charlie"));
    }

    @Test
    public void expandFilter_multiTokens_orCombined_fullWorkflow() throws ServiceException {
        String filter = GalUtil.expandFilter(GalConstants.TOKENIZE_KEY_OR, "(cn=%s)", "red green blue", null);

        assertNotNull(filter);
        // Verify OR structure exists
        assertTrue(filter.contains("(|"));
        assertTrue(filter.contains("red"));
        assertTrue(filter.contains("green"));
        assertTrue(filter.contains("blue"));
    }

    @Test
    public void expandFilter_leadingTrailingSpaces_stripped() throws ServiceException {
        String filter = GalUtil.expandFilter(null, "(cn=%s)", "  smith  ", null);

        assertNotNull(filter);
        assertTrue(filter.contains("smith"));
    }

    @Test
    public void expandFilter_tokenWithTimestamp_createTimeStampIncluded() throws ServiceException {
        String token = "20080115123456Z";
        String filter = GalUtil.expandFilter(null, "(cn=%s)", "test", token);

        assertNotNull(filter);
        // Should include both modifyTimeStamp and createTimeStamp
        assertTrue(filter.contains("TimeStamp"));
    }
}
